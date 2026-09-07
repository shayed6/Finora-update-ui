package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM holdings ORDER BY id DESC")
    fun getAllHoldings(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE UPPER(exchange) = UPPER(:exchange) AND UPPER(stockName) = UPPER(:stockName) LIMIT 1")
    suspend fun getHoldingByExchangeAndStock(exchange: String, stockName: String): HoldingEntity?

    @Query("SELECT * FROM holdings WHERE id = :id LIMIT 1")
    suspend fun getHoldingById(id: Long): HoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: HoldingEntity): Long

    @Update
    suspend fun updateHolding(holding: HoldingEntity)

    @Delete
    suspend fun deleteHolding(holding: HoldingEntity)

    @Query("DELETE FROM holdings WHERE id = :id")
    suspend fun deleteHoldingById(id: Long)

    @Query("UPDATE holdings SET currentPrice = :newPrice, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCurrentPrice(id: Long, newPrice: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM transactions WHERE holdingId = :holdingId ORDER BY dateTimestamp DESC")
    fun getTransactionsForHolding(holdingId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE holdingId = :holdingId ORDER BY dateTimestamp DESC")
    suspend fun getTransactionsForHoldingList(holdingId: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE holdingId = :holdingId")
    suspend fun deleteTransactionsForHolding(holdingId: Long)
}
