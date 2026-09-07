package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val exchange: String, // "DSE" or "CSE"
    val stockName: String,
    val quantity: Int,
    val averagePrice: Double, // Average Buying Price per share including commission
    val currentPrice: Double, // Current Market Price (editable/auto-refreshed)
    val updatedAt: Long = System.currentTimeMillis()
)
