package com.example

import com.example.data.scraper.MarketPriceScraper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScraperLiveTest {

    @Test
    fun `test live scrape DSE and CSE symbols`() = runBlocking {
        val scraper = MarketPriceScraper()

        println("=== TESTING DSE LIVE SCRAPER ===")
        val dsePrices = scraper.fetchDsePrices(forceRefresh = true)
        println("Scraped DSE count: ${dsePrices.size}")
        assertTrue("DSE should return stock prices", dsePrices.isNotEmpty())

        val dseMap = dsePrices.associateBy { it.symbol.uppercase() }
        val gp = dseMap["GP"]
        val aci = dseMap["ACI"]
        val city = dseMap["CITYBANK"]

        println("DSE GP: LTP=${gp?.ltp}, Change=${gp?.changePercent}%, Delta=${gp?.delta}")
        println("DSE ACI: LTP=${aci?.ltp}, Change=${aci?.changePercent}%, Delta=${aci?.delta}")
        println("DSE CITYBANK: LTP=${city?.ltp}, Change=${city?.changePercent}%, Delta=${city?.delta}")

        assertNotNull("GP should exist on DSE", gp)
        assertTrue("GP LTP should be > 0", (gp?.ltp ?: 0.0) > 0.0)

        println("=== TESTING CSE LIVE SCRAPER ===")
        val csePrices = scraper.fetchCsePrices(forceRefresh = true)
        println("Scraped CSE count: ${csePrices.size}")

        val cseMap = csePrices.associateBy { it.symbol.uppercase() }
        val cseGp = cseMap["GP"]
        val cseAci = cseMap["ACI"]
        val cseCity = cseMap["CITYBANK"]

        println("CSE GP: LTP=${cseGp?.ltp}, Change=${cseGp?.changePercent}%, YCP=${cseGp?.ycp}")
        println("CSE ACI: LTP=${cseAci?.ltp}, Change=${cseAci?.changePercent}%, YCP=${cseAci?.ycp}")
        println("CSE CITYBANK: LTP=${cseCity?.ltp}, Change=${cseCity?.changePercent}%, YCP=${cseCity?.ycp}")

        assertTrue("Scraper should handle market gracefully", dsePrices.isNotEmpty() || csePrices.isNotEmpty())
    }
}
