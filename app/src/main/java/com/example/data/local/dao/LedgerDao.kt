package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerPartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    // Party queries
    @Query("SELECT * FROM ledger_parties ORDER BY created_at DESC")
    fun getAllParties(): Flow<List<LedgerPartyEntity>>

    @Query("SELECT * FROM ledger_parties ORDER BY created_at DESC")
    suspend fun getAllPartiesList(): List<LedgerPartyEntity>

    @Query("SELECT * FROM ledger_parties WHERE id = :partyId")
    fun getPartyById(partyId: Long): Flow<LedgerPartyEntity?>

    @Query("SELECT * FROM ledger_parties WHERE id = :partyId")
    suspend fun getPartyByIdDirect(partyId: Long): LedgerPartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: LedgerPartyEntity): Long

    @Update
    suspend fun updateParty(party: LedgerPartyEntity)

    @Delete
    suspend fun deleteParty(party: LedgerPartyEntity)

    @Query("DELETE FROM ledger_parties WHERE id = :partyId")
    suspend fun deletePartyById(partyId: Long)

    // Entry queries
    @Query("SELECT * FROM ledger_entries ORDER BY entry_date DESC, created_at DESC")
    fun getAllEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE party_id = :partyId ORDER BY entry_date DESC, created_at DESC")
    fun getEntriesForParty(partyId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE id = :entryId")
    suspend fun getEntryById(entryId: Long): LedgerEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: LedgerEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: LedgerEntryEntity)

    @Query("DELETE FROM ledger_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Query("DELETE FROM ledger_entries WHERE party_id = :partyId")
    suspend fun deleteEntriesForParty(partyId: Long)

    @Transaction
    suspend fun deletePartyAndEntries(partyId: Long) {
        deleteEntriesForParty(partyId)
        deletePartyById(partyId)
    }
}
