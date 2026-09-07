package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = HoldingEntity::class,
            parentColumns = ["id"],
            childColumns = ["holdingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["holdingId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val holdingId: Long,
    val exchange: String,
    val stockName: String,
    val buyingPrice: Double, // per share before commission
    val quantity: Int,
    val commissionPercent: Double,
    val effectivePricePerShare: Double, // buyingPrice * (1 + commissionPercent/100)
    val totalCost: Double, // quantity * effectivePricePerShare
    val dateTimestamp: Long = System.currentTimeMillis()
)
