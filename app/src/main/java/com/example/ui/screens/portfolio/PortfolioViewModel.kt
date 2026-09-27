package com.example.ui.screens.portfolio

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DividendEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.LivePriceEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.PortfolioRepository
import com.example.data.scraper.LivePriceScraperWorker
import com.example.data.scraper.MarketPriceScraper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PortfolioSummary(
    val totalInvested: Double = 0.0,
    val totalDividend: Double = 0.0,
    val holdingsCount: Int = 0,
    val currentTotalValue: Double = 0.0,
    val totalUnrealizedGainLoss: Double = 0.0,
    val hasAnyLivePrice: Boolean = false,
    val lastUpdatedTimestamp: Long? = null
)

data class HoldingWithDividends(
    val holding: HoldingEntity,
    val totalDividend: Double = 0.0,
    val returnPerShare: Double = 0.0,
    val dividendCount: Int = 0,
    val livePrice: LivePriceEntity? = null,
    val currentPrice: Double? = null,
    val unrealizedGainLoss: Double? = null,
    val unrealizedGainLossPercent: Double? = null,
    val isPriceOutdated: Boolean = false
)

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PortfolioRepository
    private val scraper = MarketPriceScraper()

    private var pollingJob: Job? = null
    private var isForegroundActive = false

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PortfolioRepository(db.portfolioDao(), db.livePriceDao())
    }

    val holdings: StateFlow<List<HoldingEntity>> = repository.allHoldings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDividends: StateFlow<List<DividendEntity>> = repository.allDividends
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLivePrices: StateFlow<List<LivePriceEntity>> = repository.allLivePrices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val holdingsWithDividends: StateFlow<List<HoldingWithDividends>> = combine(
        repository.allHoldings,
        repository.allDividends,
        repository.allLivePrices
    ) { holdingsList, dividendsList, livePricesList ->
        val dividendByHolding = dividendsList.groupBy { it.holdingId }
        val priceMap = livePricesList.associateBy {
            "${it.exchange.uppercase().trim()}:${it.symbol.uppercase().trim()}"
        }

        val currentTime = System.currentTimeMillis()

        holdingsList.map { holding ->
            val divs = dividendByHolding[holding.id].orEmpty()
            val totalDiv = divs.sumOf { it.amount }
            val returnPerShare = if (holding.quantity > 0) totalDiv / holding.quantity else 0.0

            val key = "${holding.exchange.uppercase().trim()}:${holding.stockName.uppercase().trim()}"
            val live = priceMap[key]

            val totalPrice = holding.quantity * holding.averagePrice
            val currentPrice = live?.ltp

            val unrealizedGL = if (currentPrice != null && currentPrice > 0.0) {
                (currentPrice * holding.quantity) - totalPrice
            } else null

            val unrealizedGLPct = if (unrealizedGL != null && totalPrice > 0.0) {
                (unrealizedGL / totalPrice) * 100.0
            } else null

            // If price was updated more than 10 minutes ago, mark as outdated
            val isOutdated = if (live != null) {
                (currentTime - live.lastUpdated) > (10 * 60 * 1000L)
            } else false

            HoldingWithDividends(
                holding = holding,
                totalDividend = totalDiv,
                returnPerShare = returnPerShare,
                dividendCount = divs.size,
                livePrice = live,
                currentPrice = currentPrice,
                unrealizedGainLoss = unrealizedGL,
                unrealizedGainLossPercent = unrealizedGLPct,
                isPriceOutdated = isOutdated
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val summary: StateFlow<PortfolioSummary> = combine(
        holdingsWithDividends,
        repository.getLatestPriceTimestamp()
    ) { items, latestTimestamp ->
        val invested = items.sumOf { it.holding.quantity * it.holding.averagePrice }
        val totalDiv = items.sumOf { it.totalDividend }
        var currentVal = 0.0
        var totalGL = 0.0
        var hasAny = false

        items.forEach { item ->
            val cp = item.currentPrice
            if (cp != null && cp > 0.0) {
                currentVal += cp * item.holding.quantity
                totalGL += (item.unrealizedGainLoss ?: 0.0)
                hasAny = true
            } else {
                currentVal += item.holding.quantity * item.holding.averagePrice
            }
        }

        PortfolioSummary(
            totalInvested = invested,
            totalDividend = totalDiv,
            holdingsCount = items.size,
            currentTotalValue = currentVal,
            totalUnrealizedGainLoss = if (hasAny) totalGL else 0.0,
            hasAnyLivePrice = hasAny,
            lastUpdatedTimestamp = latestTimestamp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioSummary()
    )

    /**
     * Starts refreshing live prices every 50-60 seconds while the Portfolio screen is active/foreground.
     * Caches aggressively (45s) and applies jitter.
     */
    fun startLivePricePolling() {
        if (isForegroundActive) return
        isForegroundActive = true

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            // Initial check/refresh
            refreshPricesInternal(forceRefresh = false)

            while (isForegroundActive) {
                // Poll every 55 seconds (between 50-60s)
                delay(55_000L)
                if (isForegroundActive) {
                    refreshPricesInternal(forceRefresh = false)
                }
            }
        }
    }

    /**
     * Stops background polling when Portfolio screen is navigated away or destroyed.
     * Prevents any background scrape when the app is closed/killed.
     */
    fun stopLivePricePolling() {
        isForegroundActive = false
        pollingJob?.cancel()
        pollingJob = null
        try {
            WorkManager.getInstance(getApplication()).cancelUniqueWork(LivePriceScraperWorker.WORK_NAME)
        } catch (e: Exception) {
            Log.w("PortfolioVM", "Error cancelling WorkManager task", e)
        }
    }

    /**
     * Triggers manual immediate one-off fetch.
     */
    fun manualRefreshLivePrices() {
        viewModelScope.launch {
            refreshPricesInternal(forceRefresh = true)

            // Also enqueue WorkManager one-off task to satisfy WorkManager integration
            try {
                val workRequest = OneTimeWorkRequestBuilder<LivePriceScraperWorker>()
                    .setInputData(workDataOf(LivePriceScraperWorker.KEY_FORCE_REFRESH to true))
                    .build()
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    LivePriceScraperWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } catch (e: Exception) {
                Log.w("PortfolioVM", "WorkManager manual enqueue error: ${e.message}")
            }
        }
    }

    private suspend fun refreshPricesInternal(forceRefresh: Boolean) {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        _syncStatusMessage.value = "শেয়ার বাজার থেকে লাইভ দর সংগ্রহ করা হচ্ছে..."

        try {
            val prices = scraper.fetchAllPrices(forceRefresh = forceRefresh)
            if (prices.isNotEmpty()) {
                repository.saveLivePrices(prices)
                _syncStatusMessage.value = "লাইভ দর হালনাগাদ সফল (${prices.size}টি শেয়ার)"
            } else {
                _syncStatusMessage.value = "লাইভ দর সংগ্রহ সম্পন্ন"
            }
        } catch (e: Exception) {
            Log.e("PortfolioVM", "Error during price scrape", e)
            _syncStatusMessage.value = "লাইভ দর সংযোগে সমস্যা, পূর্বের দর প্রদর্শিত হচ্ছে"
        } finally {
            _isRefreshing.value = false
        }
    }

    fun addBuyTransaction(
        exchange: String,
        stockName: String,
        buyingPrice: Double,
        quantity: Int,
        commissionPercent: Double = 0.40
    ) {
        viewModelScope.launch {
            repository.recordBuyTransaction(
                exchange = exchange,
                stockName = stockName,
                buyingPrice = buyingPrice,
                quantity = quantity,
                commissionPercent = commissionPercent
            )
        }
    }

    fun addDividend(holdingId: Long, amount: Double, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.addDividend(holdingId, amount, timestamp)
        }
    }

    fun deleteHolding(holding: HoldingEntity) {
        viewModelScope.launch {
            repository.deleteHolding(holding)
        }
    }

    suspend fun getTransactionHistory(holdingId: Long): List<TransactionEntity> {
        return repository.getTransactionsList(holdingId)
    }

    suspend fun getDividendHistory(holdingId: Long): List<DividendEntity> {
        return repository.getDividendsList(holdingId)
    }

    fun deleteDividend(dividendId: Long) {
        viewModelScope.launch {
            repository.deleteDividend(dividendId)
        }
    }
}
