package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dividends",
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
data class DividendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val holdingId: Long,
    val amount: Double, // Dividend amount received in BDT (৳)
    val dateTimestamp: Long = System.currentTimeMillis()
)
