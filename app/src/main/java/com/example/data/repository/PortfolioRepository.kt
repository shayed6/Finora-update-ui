package com.example.data.repository

import com.example.data.local.dao.PortfolioDao
import com.example.data.local.entity.DividendEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class PortfolioRepository(private val portfolioDao: PortfolioDao) {

    val allHoldings: Flow<List<HoldingEntity>> = portfolioDao.getAllHoldings()
    val allDividends: Flow<List<DividendEntity>> = portfolioDao.getAllDividends()

    fun getTransactions(holdingId: Long): Flow<List<TransactionEntity>> =
        portfolioDao.getTransactionsForHolding(holdingId)

    suspend fun getTransactionsList(holdingId: Long): List<TransactionEntity> =
        portfolioDao.getTransactionsForHoldingList(holdingId)

    fun getDividends(holdingId: Long): Flow<List<DividendEntity>> =
        portfolioDao.getDividendsForHolding(holdingId)

    suspend fun getDividendsList(holdingId: Long): List<DividendEntity> =
        portfolioDao.getDividendsForHoldingList(holdingId)

    /**
     * Records a buy transaction and applies the auto-averaging logic:
     * - Case-insensitive match on Exchange + Stock Name
     * - Effective price per share = Buying Price * (1 + Commission%/100)
     * - Merges if existing:
     *   New Average Buying Price = (OldQty * OldAvgPrice + NewQty * NewEffectivePrice) / (OldQty + NewQty)
     *   New Total Quantity = OldQty + NewQty
     * - Creates new holding if not found
     * - Saves individual transaction in the transaction history table
     */
    suspend fun recordBuyTransaction(
        exchange: String,
        stockName: String,
        buyingPrice: Double,
        quantity: Int,
        commissionPercent: Double
    ) {
        val trimmedExchange = exchange.trim().uppercase()
        val trimmedStock = stockName.trim().uppercase()

        val effectivePrice = buyingPrice * (1.0 + (commissionPercent / 100.0))
        val totalCost = quantity * effectivePrice

        val existingHolding = portfolioDao.getHoldingByExchangeAndStock(trimmedExchange, trimmedStock)

        val targetHoldingId: Long
        if (existingHolding != null) {
            val oldQty = existingHolding.quantity
            val oldAvgPrice = existingHolding.averagePrice
            val newTotalQty = oldQty + quantity
            val newAvgPrice = ((oldQty * oldAvgPrice) + (quantity * effectivePrice)) / newTotalQty

            val updatedHolding = existingHolding.copy(
                quantity = newTotalQty,
                averagePrice = newAvgPrice,
                currentPrice = newAvgPrice,
                updatedAt = System.currentTimeMillis()
            )
            portfolioDao.updateHolding(updatedHolding)
            targetHoldingId = existingHolding.id
        } else {
            val newHolding = HoldingEntity(
                exchange = trimmedExchange,
                stockName = trimmedStock,
                quantity = quantity,
                averagePrice = effectivePrice,
                currentPrice = effectivePrice,
                updatedAt = System.currentTimeMillis()
            )
            targetHoldingId = portfolioDao.insertHolding(newHolding)
        }

        // Always save transaction in history linked to holding
        val transaction = TransactionEntity(
            holdingId = targetHoldingId,
            exchange = trimmedExchange,
            stockName = trimmedStock,
            buyingPrice = buyingPrice,
            quantity = quantity,
            commissionPercent = commissionPercent,
            effectivePricePerShare = effectivePrice,
            totalCost = totalCost,
            dateTimestamp = System.currentTimeMillis()
        )
        portfolioDao.insertTransaction(transaction)
    }

    /**
     * Adds a dividend entry to a holding.
     */
    suspend fun addDividend(holdingId: Long, amount: Double, timestamp: Long = System.currentTimeMillis()) {
        val dividend = DividendEntity(
            holdingId = holdingId,
            amount = amount,
            dateTimestamp = timestamp
        )
        portfolioDao.insertDividend(dividend)
    }

    suspend fun deleteHolding(holding: HoldingEntity) {
        portfolioDao.deleteTransactionsForHolding(holding.id)
        portfolioDao.deleteDividendsForHolding(holding.id)
        portfolioDao.deleteHolding(holding)
    }

    suspend fun deleteHoldingById(holdingId: Long) {
        portfolioDao.deleteTransactionsForHolding(holdingId)
        portfolioDao.deleteDividendsForHolding(holdingId)
        portfolioDao.deleteHoldingById(holdingId)
    }

    suspend fun deleteDividend(dividendId: Long) {
        portfolioDao.deleteDividendById(dividendId)
    }

    companion object {
        val COMMON_TICKERS = listOf(
            "CITYBANK", "BEXIMCO", "SQURPHARMA", "GP", "BATBC", "RENATA",
            "BRACBANK", "ISLAMIBANK", "OLYMPIC", "WALTONHIL", "UPGDCL", "LHBL",
            "BEACONPHAR", "PUBALIBANK", "EBL", "ALARABANK", "IDLC", "JAMUNABANK",
            "BXPHARMA", "BSCCHITTAG", "ACMELAB", "BSRMSTEEL", "DELTALIFE",
            "IFIC", "LANKABAFIN", "MJLBD", "POWERGRID", "SUMITPOWER", "TITASGAS"
        )
    }
}

