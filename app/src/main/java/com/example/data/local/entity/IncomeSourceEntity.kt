package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_sources")
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "is_preset")
    val isPreset: Boolean = false,
    @ColumnInfo(name = "start_day")
    val startDay: Int? = null, // 1-28, default null = calendar month; reserved for later summary module
    val profile: String? = null // nullable, reserved
)
