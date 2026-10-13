package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class InvoiceStatus {
    PAID,
    PARTIAL,
    DUE
}

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["invoice_number"], unique = true),
        Index(value = ["created_at"]),
        Index(value = ["customer_name"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,
    @ColumnInfo(name = "business_name")
    val businessName: String,
    @ColumnInfo(name = "business_phone")
    val businessPhone: String? = null,
    @ColumnInfo(name = "business_address")
    val businessAddress: String? = null,
    @ColumnInfo(name = "customer_name")
    val customerName: String,
    @ColumnInfo(name = "customer_phone")
    val customerPhone: String? = null,
    @ColumnInfo(name = "customer_address")
    val customerAddress: String? = null,
    @ColumnInfo(name = "invoice_date")
    val invoiceDate: Long, // timestamp ms
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,
    @ColumnInfo(name = "subtotal_paisa")
    val subtotalPaisa: Long,
    @ColumnInfo(name = "discount_paisa")
    val discountPaisa: Long = 0L,
    @ColumnInfo(name = "additional_charges_paisa")
    val additionalChargesPaisa: Long = 0L,
    @ColumnInfo(name = "grand_total_paisa")
    val grandTotalPaisa: Long,
    @ColumnInfo(name = "paid_amount_paisa")
    val paidAmountPaisa: Long = 0L,
    @ColumnInfo(name = "due_amount_paisa")
    val dueAmountPaisa: Long = 0L,
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String? = null,
    val notes: String? = null,
    val terms: String? = null,
    val status: String = InvoiceStatus.DUE.name,
    val language: String = "BN", // "BN" or "EN"
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "pdf_file_path")
    val pdfFilePath: String? = null
)
