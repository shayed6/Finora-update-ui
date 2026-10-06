package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LivePriceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LivePriceDao {
    @Query("SELECT * FROM live_prices")
    fun getAllLivePrices(): Flow<List<LivePriceEntity>>

    @Query("SELECT * FROM live_prices")
    suspend fun getAllLivePricesList(): List<LivePriceEntity>

    @Query("SELECT * FROM live_prices WHERE UPPER(exchange) = UPPER(:exchange) AND UPPER(symbol) = UPPER(:symbol) LIMIT 1")
    fun getLivePrice(exchange: String, symbol: String): Flow<LivePriceEntity?>

    @Query("SELECT * FROM live_prices WHERE UPPER(exchange) = UPPER(:exchange) AND UPPER(symbol) = UPPER(:symbol) LIMIT 1")
    suspend fun getLivePriceSync(exchange: String, symbol: String): LivePriceEntity?

    @Query("SELECT * FROM live_prices WHERE UPPER(exchange) = UPPER(:exchange)")
    suspend fun getPricesForExchange(exchange: String): List<LivePriceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePrices(prices: List<LivePriceEntity>)

    @Query("SELECT MAX(lastUpdated) FROM live_prices WHERE UPPER(exchange) = UPPER(:exchange)")
    suspend fun getLastUpdatedForExchange(exchange: String): Long?

    @Query("SELECT MAX(lastUpdated) FROM live_prices")
    fun getLatestTimestampFlow(): Flow<Long?>
}
