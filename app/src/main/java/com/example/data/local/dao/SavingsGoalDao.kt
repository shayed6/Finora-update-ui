package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.InvestmentSectorEntity
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    // --- Goals ---
    @Query("SELECT * FROM savings_goals ORDER BY is_default DESC, created_at ASC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY is_default DESC, created_at ASC")
    suspend fun getAllGoalsList(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getGoalById(id: Long): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalByIdSync(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultGoal(): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    // --- Entries ---
    @Query("SELECT * FROM savings_entries ORDER BY entry_date DESC, created_at DESC")
    fun getAllEntries(): Flow<List<SavingsEntryEntity>>

    @Query("SELECT * FROM savings_entries WHERE goal_id = :goalId ORDER BY entry_date DESC, created_at DESC")
    fun getEntriesForGoal(goalId: Long): Flow<List<SavingsEntryEntity>>

    @Query("SELECT * FROM savings_entries WHERE goal_id = :goalId ORDER BY entry_date DESC, created_at DESC")
    suspend fun getEntriesForGoalList(goalId: Long): List<SavingsEntryEntity>

    @Query("SELECT * FROM savings_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): SavingsEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: SavingsEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: SavingsEntryEntity)

    @Query("DELETE FROM savings_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("UPDATE savings_entries SET goal_id = :toGoalId WHERE goal_id = :fromGoalId")
    suspend fun moveEntriesToGoal(fromGoalId: Long, toGoalId: Long)

    @Query("SELECT COUNT(*) FROM savings_entries WHERE sector_id = :sectorId")
    suspend fun countEntriesForSector(sectorId: Long): Int

    // --- Sectors ---
    @Query("SELECT * FROM investment_sectors ORDER BY is_preset DESC, id ASC")
    fun getAllSectors(): Flow<List<InvestmentSectorEntity>>

    @Query("SELECT * FROM investment_sectors ORDER BY is_preset DESC, id ASC")
    suspend fun getAllSectorsList(): List<InvestmentSectorEntity>

    @Query("SELECT * FROM investment_sectors WHERE id = :id")
    suspend fun getSectorById(id: Long): InvestmentSectorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSector(sector: InvestmentSectorEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSectors(sectors: List<InvestmentSectorEntity>): List<Long>

    @Update
    suspend fun updateSector(sector: InvestmentSectorEntity)

    @Query("DELETE FROM investment_sectors WHERE id = :id")
    suspend fun deleteSectorById(id: Long)
}
