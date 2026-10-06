package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SavingsEntryType {
    DEPOSIT,
    WITHDRAW
}

@Entity(
    tableName = "savings_entries",
    indices = [
        Index(value = ["goal_id"]),
        Index(value = ["sector_id"]),
        Index(value = ["entry_date"])
    ]
)
data class SavingsEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "goal_id")
    val goalId: Long,
    val type: String, // "DEPOSIT" or "WITHDRAW"
    val amount: Long, // integer paisa
    @ColumnInfo(name = "sector_id")
    val sectorId: Long,
    @ColumnInfo(name = "entry_date")
    val entryDate: Long,
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
