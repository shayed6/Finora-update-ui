package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class InvoiceWithItems(
    @Embedded
    val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoice_id"
    )
    val items: List<InvoiceItemEntity>
)
