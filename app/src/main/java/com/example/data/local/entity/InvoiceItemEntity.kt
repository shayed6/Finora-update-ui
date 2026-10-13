package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoice_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["invoice_id"])
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "invoice_id")
    val invoiceId: Long,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    val description: String? = null,
    val quantity: Double = 1.0,
    @ColumnInfo(name = "unit_price_paisa")
    val unitPricePaisa: Long,
    @ColumnInfo(name = "total_price_paisa")
    val totalPricePaisa: Long,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)
