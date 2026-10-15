package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object ReportType {
    const val INVOICE = "INVOICE"
    const val INCOME_EXPENSE = "INCOME_EXPENSE"
    const val SAVINGS_INVESTMENT = "SAVINGS_INVESTMENT"
    const val DUE_RECEIVABLE_PAYABLE = "DUE_RECEIVABLE_PAYABLE"
    const val PARTY_STATEMENT = "PARTY_STATEMENT"
}

/**
 * Room database entity storing metadata for all generated PDF reports and invoices.
 * Includes file URI, report type, and timestamp.
 */
@Entity(
    tableName = "pdf_reports",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["report_type"]),
        Index(value = ["timestamp"])
    ]
)
data class PdfReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "file_uri")
    val fileUri: String = "",

    @ColumnInfo(name = "report_type")
    val reportType: String,

    val title: String,

    val subtitle: String? = null,

    @ColumnInfo(name = "file_name")
    val fileName: String = "",

    @ColumnInfo(name = "file_path")
    val filePath: String = fileUri,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long = 0L,

    @ColumnInfo(name = "date_range_start")
    val dateRangeStart: Long? = null,

    @ColumnInfo(name = "date_range_end")
    val dateRangeEnd: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = createdAt
)
