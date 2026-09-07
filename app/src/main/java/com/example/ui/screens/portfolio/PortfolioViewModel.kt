package com.example.ui.screens.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.PortfolioRepository
import com.example.util.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PortfolioSummary(
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
    val unrealizedGainLoss: Double = 0.0,
    val unrealizedGainLossPercent: Double = 0.0,
    val holdingsCount: Int = 0
)

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PortfolioRepository
    private val context = application.applicationContext

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PortfolioRepository(db.portfolioDao())
    }

    val holdings: StateFlow<List<HoldingEntity>> = repository.allHoldings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val summary: StateFlow<PortfolioSummary> = holdings.map { list ->
        val invested = list.sumOf { it.quantity * it.averagePrice }
        val current = list.sumOf { it.quantity * it.currentPrice }
        val gainLoss = current - invested
        val percent = if (invested > 0.0) (gainLoss / invested) * 100.0 else 0.0
        PortfolioSummary(
            totalInvested = invested,
            totalCurrentValue = current,
            unrealizedGainLoss = gainLoss,
            unrealizedGainLossPercent = percent,
            holdingsCount = list.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioSummary()
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastRefreshTimestamp = MutableStateFlow<Long?>(null)
    val lastRefreshTimestamp: StateFlow<Long?> = _lastRefreshTimestamp.asStateFlow()

    private var autoRefreshJob: Job? = null

    /**
     * Start the 50-60 second periodic auto-refresh timer.
     * Only runs while screen/widget is visible and network is active.
     */
    fun startAutoRefresh() {
        if (autoRefreshJob != null && autoRefreshJob?.isActive == true) return

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(55000L) // 55 seconds (within 50-60s requirement)
                if (NetworkUtils.isInternetAvailable(context)) {
                    refreshPricesInternal()
                }
            }
        }
    }

    /**
     * Stop auto-refresh timer when navigating away.
     */
    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    /**
     * Manual refresh (pull-to-refresh or refresh button).
     */
    fun refreshPricesManually() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshPricesInternal()
            delay(400) // visual feedback
            _isRefreshing.value = false
        }
    }

    private suspend fun refreshPricesInternal() {
        val currentList = holdings.value
        for (holding in currentList) {
            val remotePrice = repository.fetchCurrentPrice(holding.exchange, holding.stockName)
            if (remotePrice != null && remotePrice > 0) {
                repository.updateHoldingCurrentPrice(holding.id, remotePrice)
            }
        }
        _lastRefreshTimestamp.value = System.currentTimeMillis()
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

    fun updateHoldingCurrentPrice(holdingId: Long, newPrice: Double) {
        viewModelScope.launch {
            repository.updateHoldingCurrentPrice(holdingId, newPrice)
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

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
