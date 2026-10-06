package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.LedgerDao
import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerEntryType
import com.example.data.local.entity.LedgerPartyEntity
import com.example.ui.screens.ledger.LedgerNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LedgerRepository(private val ledgerDao: LedgerDao) {

    val allParties: Flow<List<LedgerPartyEntity>> = ledgerDao.getAllParties()
    val allEntries: Flow<List<LedgerEntryEntity>> = ledgerDao.getAllEntries()

    fun getParty(partyId: Long): Flow<LedgerPartyEntity?> = ledgerDao.getPartyById(partyId)

    suspend fun getPartyDirect(partyId: Long): LedgerPartyEntity? = withContext(Dispatchers.IO) {
        ledgerDao.getPartyByIdDirect(partyId)
    }

    fun getEntriesForParty(partyId: Long): Flow<List<LedgerEntryEntity>> =
        ledgerDao.getEntriesForParty(partyId)

    suspend fun getEntryById(entryId: Long): LedgerEntryEntity? = withContext(Dispatchers.IO) {
        ledgerDao.getEntryById(entryId)
    }

    suspend fun getAllPartiesList(): List<LedgerPartyEntity> = withContext(Dispatchers.IO) {
        ledgerDao.getAllPartiesList()
    }

    suspend fun findDuplicatePhoneParty(phone: String?, excludePartyId: Long = 0L): LedgerPartyEntity? = withContext(Dispatchers.IO) {
        val normTarget = LedgerNormalizer.normalizePhone(phone) ?: return@withContext null
        ledgerDao.getAllPartiesList().firstOrNull {
            it.id != excludePartyId && LedgerNormalizer.normalizePhone(it.phone) == normTarget
        }
    }

    suspend fun addParty(name: String, phone: String?): Long = withContext(Dispatchers.IO) {
        val cleanPhone = phone?.trim()?.ifBlank { null }
        val normPhone = LedgerNormalizer.normalizePhone(cleanPhone)
        if (normPhone != null) {
            val duplicate = findDuplicatePhoneParty(cleanPhone)
            if (duplicate != null) {
                throw IllegalArgumentException("A party '${duplicate.name}' with phone '$cleanPhone' already exists.")
            }
        }
        ledgerDao.insertParty(
            LedgerPartyEntity(
                name = name.trim(),
                phone = cleanPhone
            )
        )
    }

    suspend fun updateParty(party: LedgerPartyEntity) = withContext(Dispatchers.IO) {
        val cleanPhone = party.phone?.trim()?.ifBlank { null }
        val normPhone = LedgerNormalizer.normalizePhone(cleanPhone)
        if (normPhone != null) {
            val duplicate = findDuplicatePhoneParty(cleanPhone, excludePartyId = party.id)
            if (duplicate != null) {
                throw IllegalArgumentException("A party '${duplicate.name}' with phone '$cleanPhone' already exists.")
            }
        }
        ledgerDao.updateParty(
            party.copy(
                name = party.name.trim(),
                phone = cleanPhone
            )
        )
    }

    suspend fun deleteParty(partyId: Long) = withContext(Dispatchers.IO) {
        ledgerDao.deletePartyAndEntries(partyId)
    }

    suspend fun addEntry(
        partyId: Long,
        type: LedgerEntryType,
        amountPaisa: Long,
        note: String?,
        entryDate: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val cleanNote = note?.trim()?.ifBlank { null }
        ledgerDao.insertEntry(
            LedgerEntryEntity(
                partyId = partyId,
                type = type.name,
                amountPaisa = amountPaisa,
                note = cleanNote,
                entryDate = entryDate
            )
        )
    }

    suspend fun updateEntry(entry: LedgerEntryEntity) = withContext(Dispatchers.IO) {
        val cleanNote = entry.note?.trim()?.ifBlank { null }
        ledgerDao.updateEntry(
            entry.copy(
                note = cleanNote
            )
        )
    }

    suspend fun deleteEntry(entryId: Long) = withContext(Dispatchers.IO) {
        ledgerDao.deleteEntryById(entryId)
    }

    companion object {
        @Volatile
        private var INSTANCE: LedgerRepository? = null

        fun getInstance(context: Context): LedgerRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val instance = LedgerRepository(db.ledgerDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
