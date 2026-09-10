package com.example.ui.screens.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DividendEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PortfolioSummary(
    val totalInvested: Double = 0.0,
    val totalDividend: Double = 0.0,
    val holdingsCount: Int = 0
)

data class HoldingWithDividends(
    val holding: HoldingEntity,
    val totalDividend: Double = 0.0,
    val returnPerShare: Double = 0.0,
    val dividendCount: Int = 0
)

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PortfolioRepository

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

    val allDividends: StateFlow<List<DividendEntity>> = repository.allDividends
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val holdingsWithDividends: StateFlow<List<HoldingWithDividends>> = combine(
        repository.allHoldings,
        repository.allDividends
    ) { holdingsList, dividendsList ->
        val dividendByHolding = dividendsList.groupBy { it.holdingId }
        holdingsList.map { holding ->
            val divs = dividendByHolding[holding.id].orEmpty()
            val totalDiv = divs.sumOf { it.amount }
            val returnPerShare = if (holding.quantity > 0) totalDiv / holding.quantity else 0.0
            HoldingWithDividends(
                holding = holding,
                totalDividend = totalDiv,
                returnPerShare = returnPerShare,
                dividendCount = divs.size
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val summary: StateFlow<PortfolioSummary> = combine(
        repository.allHoldings,
        repository.allDividends
    ) { holdingsList, dividendsList ->
        val invested = holdingsList.sumOf { it.quantity * it.averagePrice }
        val totalDiv = dividendsList.sumOf { it.amount }
        PortfolioSummary(
            totalInvested = invested,
            totalDividend = totalDiv,
            holdingsCount = holdingsList.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioSummary()
    )

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

