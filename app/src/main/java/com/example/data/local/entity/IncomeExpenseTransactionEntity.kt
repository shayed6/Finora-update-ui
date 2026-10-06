package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CashTransactionType {
    INCOME,
    EXPENSE
}

@Entity(
    tableName = "income_expense_transactions",
    indices = [
        Index(value = ["occurred_at"]),
        Index(value = ["type"]),
        Index(value = ["source_id"]),
        Index(value = ["category_id"])
    ]
)
data class IncomeExpenseTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Long, // integer paisa (e.g., ৳ 100 = 10000 paisa)
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long, // date+time timestamp (ms)
    @ColumnInfo(name = "source_id")
    val sourceId: Long? = null, // for INCOME
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null, // for EXPENSE
    @ColumnInfo(name = "tag_line")
    val tagLine: String? = null, // optional text, max 60 chars
    val note: String? = null,
    @ColumnInfo(name = "linked_income_source_id")
    val linkedIncomeSourceId: Long? = null, // nullable, EXPENSE only
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "savings_goal_id")
    val savingsGoalId: Long? = null // nullable reserved field
)
