package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LedgerEntryType {
    GAVE,     // দিলাম: I gave money/goods, person owes me -> increases "পাবো"
    RECEIVED  // পেলাম: I received money/goods back -> decreases "পাবো"
}

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = LedgerPartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["party_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["party_id"]),
        Index(value = ["entry_date"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "party_id")
    val partyId: Long,
    val type: String, // "GAVE" or "RECEIVED"
    @ColumnInfo(name = "amount_paisa")
    val amountPaisa: Long, // Stored as integer paisa (৳ 1.00 = 100 paisa)
    val note: String? = null,
    @ColumnInfo(name = "entry_date")
    val entryDate: Long, // Timestamp of entry date (ms)
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // Nullable reserved fields for future modules
    val kind: String? = "credit_ledger",
    val category: String? = null,
    val profile: String? = null
)
