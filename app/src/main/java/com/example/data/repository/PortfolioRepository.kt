package com.example.data.repository

import com.example.data.local.dao.PortfolioDao
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

class PortfolioRepository(private val portfolioDao: PortfolioDao) {

    val allHoldings: Flow<List<HoldingEntity>> = portfolioDao.getAllHoldings()

    fun getTransactions(holdingId: Long): Flow<List<TransactionEntity>> =
        portfolioDao.getTransactionsForHolding(holdingId)

    suspend fun getTransactionsList(holdingId: Long): List<TransactionEntity> =
        portfolioDao.getTransactionsForHoldingList(holdingId)

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
                // Keep existing manually set currentPrice or set to new average if current was 0
                currentPrice = if (existingHolding.currentPrice > 0) existingHolding.currentPrice else newAvgPrice,
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
                currentPrice = effectivePrice, // Defaults to average buying price until updated
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

    suspend fun updateHoldingCurrentPrice(holdingId: Long, newCurrentPrice: Double) {
        portfolioDao.updateCurrentPrice(holdingId, newCurrentPrice)
    }

    suspend fun deleteHolding(holding: HoldingEntity) {
        // Transactions will cascade delete or we explicitly delete
        portfolioDao.deleteTransactionsForHolding(holding.id)
        portfolioDao.deleteHolding(holding)
    }

    suspend fun deleteHoldingById(holdingId: Long) {
        portfolioDao.deleteTransactionsForHolding(holdingId)
        portfolioDao.deleteHoldingById(holdingId)
    }

    /**
     * Auto-Refresh / Price Fetcher placeholder:
     * DSE/CSE currently does not have an official open public API.
     * This method is ready to be plugged into a live quote scraper or API.
     * If an external price is available, it returns the Double; otherwise null.
     */
    suspend fun fetchCurrentPrice(exchange: String, stockName: String): Double? {
        // TODO: Wire up to a live external API or scraping service when available.
        // Currently returns null to preserve user's manually entered or initial prices.
        return null
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
