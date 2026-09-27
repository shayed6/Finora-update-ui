package com.example.data.scraper

import android.util.Log
import com.example.data.local.entity.LivePriceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.random.Random

/**
 * On-device live price scraper for Dhaka Stock Exchange (DSE) and Chittagong Stock Exchange (CSE).
 * Uses Jsoup to scrape public web pages without needing external servers or API keys.
 * Implements jitter, aggressive caching (45s threshold), single retry on failure,
 * and robust parsing with silent fallbacks.
 */
class MarketPriceScraper {

    companion object {
        private const val TAG = "MarketPriceScraper"

        private const val DSE_URL = "https://dse.com.bd/markets/latest-share-price?sort=code"
        private const val DSE_FALLBACK_URL = "https://dsebd.org/latest_share_price_all.php"
        private const val CSE_URL = "https://www.cse.com.bd/market/current_price"

        private const val CACHE_DURATION_MS = 45_000L // 45 seconds aggressive caching
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }

    // In-memory cache for fast reuse within the 45s window
    private val memoryCache = ConcurrentHashMap<String, LivePriceEntity>()
    private var lastSuccessfulDseScrape = 0L
    private var lastSuccessfulCseScrape = 0L

    // Permissive SSL context for CSE server certificate chain compatibility
    private val sslSocketFactory by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize custom SSL socket factory", e)
            null
        }
    }

    /**
     * Scrapes both DSE and CSE with jitter, caching, and retry.
     */
    suspend fun fetchAllPrices(forceRefresh: Boolean = false): List<LivePriceEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LivePriceEntity>()
        val dseList = fetchDsePrices(forceRefresh)
        results.addAll(dseList)

        val cseList = fetchCsePrices(forceRefresh)
        results.addAll(cseList)

        results
    }

    /**
     * Scrapes DSE live prices for all instruments in a single network call.
     */
    suspend fun fetchDsePrices(forceRefresh: Boolean = false): List<LivePriceEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastSuccessfulDseScrape) < CACHE_DURATION_MS) {
            val cached = memoryCache.values.filter { it.exchange.equals("DSE", ignoreCase = true) }
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Reusing cached DSE prices (${cached.size} items, age ${now - lastSuccessfulDseScrape}ms)")
                return@withContext cached
            }
        }

        // Apply 1-3 second jitter before request
        val jitter = Random.nextLong(1000L, 3000L)
        delay(jitter)

        // Attempt scrape with 1 retry
        var items = scrapeDseInternal(DSE_URL)
        if (items.isEmpty()) {
            Log.w(TAG, "DSE initial scrape returned 0 items, retrying in 3s...")
            delay(3000L)
            items = scrapeDseInternal(DSE_URL)
            if (items.isEmpty()) {
                items = scrapeDseInternal(DSE_FALLBACK_URL)
            }
        }

        if (items.isNotEmpty()) {
            lastSuccessfulDseScrape = System.currentTimeMillis()
            items.forEach { memoryCache["DSE:${it.symbol}"] = it }
            Log.i(TAG, "Successfully scraped ${items.size} DSE symbols")
        } else {
            Log.w(TAG, "DSE scraping returned empty list, keeping previous cache")
        }

        items
    }

    /**
     * Scrapes CSE live prices for all instruments in a single network call.
     */
    suspend fun fetchCsePrices(forceRefresh: Boolean = false): List<LivePriceEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && (now - lastSuccessfulCseScrape) < CACHE_DURATION_MS) {
            val cached = memoryCache.values.filter { it.exchange.equals("CSE", ignoreCase = true) }
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Reusing cached CSE prices (${cached.size} items, age ${now - lastSuccessfulCseScrape}ms)")
                return@withContext cached
            }
        }

        // Apply 1-3 second jitter before request
        val jitter = Random.nextLong(1000L, 3000L)
        delay(jitter)

        // Attempt scrape with 1 retry
        var items = scrapeCseInternal()
        if (items.isEmpty()) {
            Log.w(TAG, "CSE initial scrape returned 0 items, retrying in 3s...")
            delay(3000L)
            items = scrapeCseInternal()
        }

        if (items.isNotEmpty()) {
            lastSuccessfulCseScrape = System.currentTimeMillis()
            items.forEach { memoryCache["CSE:${it.symbol}"] = it }
            Log.i(TAG, "Successfully scraped ${items.size} CSE symbols")
        } else {
            Log.w(TAG, "CSE scraping returned empty list, keeping previous cache")
        }

        items
    }

    private fun scrapeDseInternal(url: String): List<LivePriceEntity> {
        val priceMap = mutableMapOf<String, LivePriceEntity>()
        val timestamp = System.currentTimeMillis()

        try {
            val connection = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(15_000)
                .followRedirects(true)
                .ignoreContentType(true)

            val doc = connection.get()
            val html = doc.html()

            // Strategy 1: Parse DOM links targeting /company/{CODE}
            val companyLinks = doc.select("a[href*=/company/]")
            for (link in companyLinks) {
                try {
                    val href = link.attr("href")
                    val rawSymbol = href.substringAfter("/company/").substringBefore("?").substringBefore("/").trim().uppercase()
                    if (rawSymbol.isEmpty()) continue

                    val spans = link.select("span")
                    if (spans.size >= 2) {
                        val symbol = spans[0].text().trim().uppercase().ifEmpty { rawSymbol }
                        val ltpText = spans[1].text().replace(",", "").trim()
                        val ltp = ltpText.toDoubleOrNull() ?: continue

                        var changePercent = 0.0
                        var delta = 0.0
                        if (spans.size >= 3) {
                            val changeText = spans[2].text()
                            val pctMatch = Regex("([+\\-−]?[0-9\\.]+)%").find(changeText)
                            if (pctMatch != null) {
                                val cleanPct = pctMatch.groupValues[1].replace("−", "-")
                                changePercent = cleanPct.toDoubleOrNull() ?: 0.0
                            }
                            val deltaMatch = Regex("([+\\-−]?[0-9\\.]+)").find(changeText)
                            if (deltaMatch != null) {
                                val cleanDelta = deltaMatch.groupValues[1].replace("−", "-")
                                delta = cleanDelta.toDoubleOrNull() ?: 0.0
                            }
                        }

                        priceMap[symbol] = LivePriceEntity(
                            exchange = "DSE",
                            symbol = symbol,
                            ltp = ltp,
                            changePercent = changePercent,
                            delta = delta,
                            lastUpdated = timestamp
                        )
                    }
                } catch (e: Exception) {
                    // Silently ignore individual row errors
                }
            }

            // Strategy 2: Parse embedded Next.js JSON array in the HTML payload
            val regex = Regex("""\\?"code\\?":\s*\\?"([A-Za-z0-9().\-]+)\\?",\s*\\?"price\\?":\s*\\?"([0-9.,]+)\\?",\s*\\?"change\\?":\s*([0-9.\-]+)(?:,\s*\\?"delta\\?":\s*([0-9.\-]+))?""")
            for (match in regex.findAll(html)) {
                try {
                    val code = match.groupValues[1].trim().uppercase()
                    val priceStr = match.groupValues[2].replace(",", "").trim()
                    val ltp = priceStr.toDoubleOrNull() ?: continue
                    val changePct = match.groupValues[3].toDoubleOrNull() ?: 0.0
                    val delta = match.groupValues.getOrNull(4)?.toDoubleOrNull() ?: 0.0

                    // Put or update if not present or has more precise data
                    val existing = priceMap[code]
                    if (existing == null || existing.ltp <= 0.0) {
                        priceMap[code] = LivePriceEntity(
                            exchange = "DSE",
                            symbol = code,
                            ltp = ltp,
                            changePercent = changePct,
                            delta = delta,
                            lastUpdated = timestamp
                        )
                    }
                } catch (e: Exception) {
                    // Fail silently per instrument
                }
            }

            // Strategy 3: Standard table fallback if modern Next.js DOM not found
            if (priceMap.isEmpty()) {
                val tableRows = doc.select("table tr")
                for (row in tableRows) {
                    try {
                        val cols = row.select("td")
                        if (cols.size >= 4) {
                            val code = cols[1].text().trim().uppercase()
                            val ltp = cols[2].text().replace(",", "").toDoubleOrNull()
                            if (code.isNotEmpty() && ltp != null && ltp > 0) {
                                priceMap[code] = LivePriceEntity(
                                    exchange = "DSE",
                                    symbol = code,
                                    ltp = ltp,
                                    lastUpdated = timestamp
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping DSE quotes from $url: ${e.message}")
        }

        return priceMap.values.toList()
    }

    private fun scrapeCseInternal(): List<LivePriceEntity> {
        val priceMap = mutableMapOf<String, LivePriceEntity>()
        val timestamp = System.currentTimeMillis()

        try {
            val connection = Jsoup.connect(CSE_URL)
                .userAgent(USER_AGENT)
                .timeout(15_000)
                .followRedirects(true)
                .ignoreContentType(true)

            sslSocketFactory?.let { connection.sslSocketFactory(it) }

            val doc = connection.get()
            val rows = doc.select("#dataTable tbody tr, table.row-border tbody tr, table tr")

            for (row in rows) {
                try {
                    val cols = row.select("td")
                    if (cols.size < 7) continue

                    // Column 1 contains stock code link, e.g. <a class="customHref" href=".../companydetails/1JANATAMF">
                    val link = cols[1].selectFirst("a")
                    val href = link?.attr("href").orEmpty()
                    var symbol = if (href.contains("companydetails/")) {
                        href.substringAfter("companydetails/").substringBefore("?").substringBefore("/").trim().uppercase()
                    } else {
                        cols[1].text().replace(" ", "").trim().uppercase()
                    }

                    if (symbol.isEmpty()) continue

                    // Column 2: LTP
                    val ltpText = cols[2].text().replace(",", "").trim()
                    val ltp = ltpText.toDoubleOrNull() ?: continue

                    // Column 6: YCP (Yesterday's close price)
                    val ycpText = cols.getOrNull(6)?.text()?.replace(",", "")?.trim().orEmpty()
                    val ycp = ycpText.toDoubleOrNull() ?: 0.0

                    // Column 9: Volume
                    val volText = cols.getOrNull(9)?.text()?.replace(",", "")?.trim().orEmpty()
                    val volume = volText.toLongOrNull() ?: 0L

                    // Calculate change%
                    val changePercent = if (ycp > 0.0) {
                        ((ltp - ycp) / ycp) * 100.0
                    } else 0.0

                    val delta = if (ycp > 0.0) ltp - ycp else 0.0

                    priceMap[symbol] = LivePriceEntity(
                        exchange = "CSE",
                        symbol = symbol,
                        ltp = ltp,
                        changePercent = changePercent,
                        delta = delta,
                        volume = volume,
                        ycp = ycp,
                        lastUpdated = timestamp
                    )
                } catch (e: Exception) {
                    // Silently fail per row
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping CSE quotes: ${e.message}")
        }

        return priceMap.values.toList()
    }
}
