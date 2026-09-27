package com.example.data.local.entity

import androidx.room.Entity

/**
 * Local Room entity storing real-time scraped stock prices for DSE & CSE.
 * Keyed by composite primary key (exchange, symbol).
 */
@Entity(
    tableName = "live_prices",
    primaryKeys = ["exchange", "symbol"]
)
data class LivePriceEntity(
    val exchange: String, // "DSE" or "CSE"
    val symbol: String,   // Uppercase ticker symbol, e.g., "GP", "ACI", "CITYBANK"
    val ltp: Double,      // Last Traded Price
    val changePercent: Double = 0.0,
    val delta: Double = 0.0,
    val volume: Long = 0L,
    val ycp: Double = 0.0, // Yesterday's close price
    val high: Double = 0.0,
    val low: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)
