package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.entity.InvestmentSectorEntity
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsEntryType
import com.example.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SavingsGoalRepository(private val dao: SavingsGoalDao) {

    val allGoals: Flow<List<SavingsGoalEntity>> = dao.getAllGoals()
    val allEntries: Flow<List<SavingsEntryEntity>> = dao.getAllEntries()
    val allSectors: Flow<List<InvestmentSectorEntity>> = dao.getAllSectors()

    fun getGoalById(id: Long): Flow<SavingsGoalEntity?> = dao.getGoalById(id)
    fun getEntriesForGoal(goalId: Long): Flow<List<SavingsEntryEntity>> = dao.getEntriesForGoal(goalId)

    suspend fun ensureDefaultData() = withContext(Dispatchers.IO) {
        // 1. Ensure default goal: "সাধারণ সঞ্চয়"
        val defaultGoal = dao.getDefaultGoal()
        if (defaultGoal == null) {
            dao.insertGoal(
                SavingsGoalEntity(
                    name = DEFAULT_GOAL_NAME,
                    targetAmount = null,
                    targetDate = null,
                    isDefault = true
                )
            )
        }

        // 2. Ensure preset sectors (NO "শেয়ার" sector!)
        val existingSectors = dao.getAllSectorsList()
        if (existingSectors.isEmpty()) {
            val presets = PRESET_SECTORS.map { name ->
                InvestmentSectorEntity(name = name, isPreset = true)
            }
            dao.insertSectors(presets)
        }
    }

    // --- Goal Operations ---
    suspend fun addGoal(name: String, targetAmountPaisa: Long?, targetDate: Long?): Long = withContext(Dispatchers.IO) {
        val cleanName = name.trim().replace("\\s+".toRegex(), " ")
        if (cleanName.isBlank()) throw IllegalArgumentException("লক্ষ্যের নাম আবশ্যক")
        if (targetAmountPaisa != null && targetAmountPaisa <= 0L) {
            throw IllegalArgumentException("লক্ষ্যের পরিমাণ শূন্যের বেশি হতে হবে")
        }

        dao.insertGoal(
            SavingsGoalEntity(
                name = cleanName,
                targetAmount = targetAmountPaisa,
                targetDate = targetDate,
                isDefault = false
            )
        )
    }

    suspend fun updateGoal(id: Long, name: String, targetAmountPaisa: Long?, targetDate: Long?) = withContext(Dispatchers.IO) {
        val existing = dao.getGoalByIdSync(id) ?: throw IllegalArgumentException("লক্ষ্য পাওয়া যায়নি")
        val cleanName = if (existing.isDefault) {
            existing.name // Default goal name cannot be changed
        } else {
            name.trim().replace("\\s+".toRegex(), " ").ifBlank {
                throw IllegalArgumentException("লক্ষ্যের নাম আবশ্যক")
            }
        }

        dao.updateGoal(
            existing.copy(
                name = cleanName,
                targetAmount = targetAmountPaisa,
                targetDate = targetDate
            )
        )
    }

    suspend fun deleteGoal(goalId: Long) = withContext(Dispatchers.IO) {
        val goal = dao.getGoalByIdSync(goalId) ?: return@withContext
        if (goal.isDefault) {
            throw IllegalStateException("সাধারণ সঞ্চয় মুছে ফেলা যাবে না")
        }

        // Find default goal
        var defaultGoal = dao.getDefaultGoal()
        if (defaultGoal == null) {
            val defId = dao.insertGoal(
                SavingsGoalEntity(
                    name = DEFAULT_GOAL_NAME,
                    isDefault = true
                )
            )
            defaultGoal = dao.getGoalByIdSync(defId)
        }

        val targetDefaultGoalId = defaultGoal?.id ?: throw IllegalStateException("ডিফল্ট লক্ষ্য পাওয়া যায়নি")

        // Move all entries of this goal to default goal
        dao.moveEntriesToGoal(fromGoalId = goalId, toGoalId = targetDefaultGoalId)

        // Delete the goal
        dao.deleteGoalById(goalId)
    }

    // --- Entry Operations ---
    suspend fun addEntry(
        goalId: Long,
        type: String, // "DEPOSIT" or "WITHDRAW"
        amountPaisa: Long,
        sectorId: Long,
        entryDate: Long,
        note: String?
    ): Long = withContext(Dispatchers.IO) {
        if (amountPaisa <= 0L) throw IllegalArgumentException("টাকার পরিমাণ শূন্যের বেশি হতে হবে")
        // Future dates not allowed
        if (entryDate > System.currentTimeMillis() + 120_000L) {
            throw IllegalArgumentException("ভবিষ্যতের তারিখ গ্রহণযোগ্য নয়")
        }

        // Check withdrawal limit: cannot make goal balance negative
        if (type == SavingsEntryType.WITHDRAW.name) {
            val entries = dao.getEntriesForGoalList(goalId)
            val currentTotal = calculateGoalBalance(entries)
            if (amountPaisa > currentTotal) {
                throw IllegalArgumentException("উত্তোলনের পরিমাণ বর্তমান জমার চেয়ে বেশি হতে পারে না")
            }
        }

        val cleanNote = note?.trim()?.ifBlank { null }

        dao.insertEntry(
            SavingsEntryEntity(
                goalId = goalId,
                type = type,
                amount = amountPaisa,
                sectorId = sectorId,
                entryDate = entryDate,
                note = cleanNote
            )
        )
    }

    suspend fun updateEntry(
        entry: SavingsEntryEntity
    ) = withContext(Dispatchers.IO) {
        if (entry.amount <= 0L) throw IllegalArgumentException("টাকার পরিমাণ শূন্যের বেশি হতে হবে")
        if (entry.entryDate > System.currentTimeMillis() + 120_000L) {
            throw IllegalArgumentException("ভবিষ্যতের তারিখ গ্রহণযোগ্য নয়")
        }

        // Check withdrawal limit excluding old entry value
        if (entry.type == SavingsEntryType.WITHDRAW.name) {
            val allOtherEntries = dao.getEntriesForGoalList(entry.goalId).filter { it.id != entry.id }
            val balanceWithoutThis = calculateGoalBalance(allOtherEntries)
            if (entry.amount > balanceWithoutThis) {
                throw IllegalArgumentException("উত্তোলনের পরিমাণ বর্তমান জমার চেয়ে বেশি হতে পারে না")
            }
        }

        val cleanNote = entry.note?.trim()?.ifBlank { null }
        dao.updateEntry(entry.copy(note = cleanNote))
    }

    suspend fun deleteEntry(entryId: Long) = withContext(Dispatchers.IO) {
        dao.deleteEntryById(entryId)
    }

    // --- Sector Operations ---
    suspend fun addCustomSector(name: String): Long = withContext(Dispatchers.IO) {
        val cleanName = normalizeName(name)
        if (cleanName.isBlank()) throw IllegalArgumentException("খাতের নাম আবশ্যক")

        val existing = dao.getAllSectorsList()
        if (existing.any { normalizeName(it.name) == cleanName }) {
            throw IllegalArgumentException("এই নামের খাত ইতিমধ্যে আছে")
        }

        dao.insertSector(
            InvestmentSectorEntity(
                name = name.trim().replace("\\s+".toRegex(), " "),
                isPreset = false
            )
        )
    }

    suspend fun deleteCustomSector(sectorId: Long) = withContext(Dispatchers.IO) {
        val sector = dao.getSectorById(sectorId) ?: return@withContext
        if (sector.isPreset) {
            throw IllegalStateException("ডিফল্ট খাত মুছে ফেলা যাবে না")
        }
        val count = dao.countEntriesForSector(sectorId)
        if (count > 0) {
            throw IllegalStateException("এই খাতে লেনদেন থাকায় এটি মুছে ফেলা যাবে না")
        }
        dao.deleteSectorById(sectorId)
    }

    companion object {
        const val DEFAULT_GOAL_NAME = "সাধারণ সঞ্চয়"

        val PRESET_SECTORS = listOf(
            "ব্যাংক সঞ্চয়/DPS",
            "FDR",
            "সঞ্চয়পত্র",
            "স্বর্ণ",
            "জমি/সম্পত্তি",
            "ব্যবসা",
            "বীমা",
            "অন্যান্য"
        )

        fun normalizeName(name: String): String =
            name.trim().replace("\\s+".toRegex(), " ").lowercase()

        fun calculateGoalBalance(entries: List<SavingsEntryEntity>): Long {
            var depositTotal = 0L
            var withdrawTotal = 0L
            entries.forEach { entry ->
                if (entry.type == SavingsEntryType.DEPOSIT.name) {
                    depositTotal += entry.amount
                } else if (entry.type == SavingsEntryType.WITHDRAW.name) {
                    withdrawTotal += entry.amount
                }
            }
            return depositTotal - withdrawTotal
        }

        @Volatile
        private var INSTANCE: SavingsGoalRepository? = null

        fun getInstance(context: Context): SavingsGoalRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val instance = SavingsGoalRepository(db.savingsGoalDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
