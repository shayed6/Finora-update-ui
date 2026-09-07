package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String,
    val category: String = "সাধারণ",
    val createdAt: Long = System.currentTimeMillis()
)
