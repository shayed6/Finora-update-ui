package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.IncomeExpenseDao
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.IncomeExpenseTransactionEntity
import com.example.data.local.entity.IncomeSourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class IncomeExpenseRepository(private val dao: IncomeExpenseDao) {

    val allIncomeSources: Flow<List<IncomeSourceEntity>> = dao.getAllIncomeSources()
    val allExpenseCategories: Flow<List<ExpenseCategoryEntity>> = dao.getAllExpenseCategories()
    val allTransactions: Flow<List<IncomeExpenseTransactionEntity>> = dao.getAllTransactions()
    val allDistinctTagLines: Flow<List<String>> = dao.getAllDistinctTagLines()

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<IncomeExpenseTransactionEntity>> =
        dao.getTransactionsInRange(startTime, endTime)

    suspend fun ensurePresets() = withContext(Dispatchers.IO) {
        val existingSources = dao.getAllIncomeSourcesList()
        if (existingSources.isEmpty()) {
            val presetSources = PRESET_INCOME_SOURCES.map { name ->
                IncomeSourceEntity(name = name, isPreset = true)
            }
            dao.insertIncomeSources(presetSources)
        }

        val existingCategories = dao.getAllExpenseCategoriesList()
        if (existingCategories.isEmpty()) {
            val presetCategories = PRESET_EXPENSE_CATEGORIES.map { name ->
                ExpenseCategoryEntity(name = name, isPreset = true)
            }
            dao.insertExpenseCategories(presetCategories)
        }
    }

    // --- Income Sources Management ---
    suspend fun addIncomeSource(name: String): Long = withContext(Dispatchers.IO) {
        val cleanName = normalizeName(name)
        if (cleanName.isBlank()) throw IllegalArgumentException("আয়ের উৎসের নাম খালি হতে পারে না")

        val existing = dao.getAllIncomeSourcesList()
        if (existing.any { normalizeName(it.name) == cleanName }) {
            throw IllegalArgumentException("এই নামের আয়ের উৎস ইতিমধ্যে আছে")
        }

        dao.insertIncomeSource(
            IncomeSourceEntity(
                name = name.trim().replace("\\s+".toRegex(), " "),
                isPreset = false
            )
        )
    }

    suspend fun renameIncomeSource(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val cleanName = normalizeName(newName)
        if (cleanName.isBlank()) throw IllegalArgumentException("আয়ের উৎসের নাম খালি হতে পারে না")

        val current = dao.getIncomeSourceById(id) ?: throw IllegalArgumentException("আয়ের উৎস পাওয়া যায়নি")
        val existing = dao.getAllIncomeSourcesList()
        if (existing.any { it.id != id && normalizeName(it.name) == cleanName }) {
            throw IllegalArgumentException("এই নামের আয়ের উৎস ইতিমধ্যে আছে")
        }

        dao.updateIncomeSource(
            current.copy(name = newName.trim().replace("\\s+".toRegex(), " "))
        )
    }

    suspend fun deleteIncomeSource(id: Long) = withContext(Dispatchers.IO) {
        val current = dao.getIncomeSourceById(id) ?: return@withContext
        if (current.isPreset) {
            throw IllegalStateException("ডিফল্ট আয়ের উৎস মুছে ফেলা যাবে না")
        }
        val count = dao.countTransactionsForSource(id)
        if (count > 0) {
            throw IllegalStateException("এই উৎসে লেনদেন থাকায় এটি মুছে ফেলা যাবে না। শুধু নাম পরিবর্তন করা যাবে।")
        }
        dao.deleteIncomeSourceById(id)
    }

    // --- Expense Categories Management ---
    suspend fun addExpenseCategory(name: String): Long = withContext(Dispatchers.IO) {
        val cleanName = normalizeName(name)
        if (cleanName.isBlank()) throw IllegalArgumentException("ব্যয়ের খাতের নাম খালি হতে পারে না")

        val existing = dao.getAllExpenseCategoriesList()
        if (existing.any { normalizeName(it.name) == cleanName }) {
            throw IllegalArgumentException("এই নামের ব্যয়ের খাত ইতিমধ্যে আছে")
        }

        dao.insertExpenseCategory(
            ExpenseCategoryEntity(
                name = name.trim().replace("\\s+".toRegex(), " "),
                isPreset = false
            )
        )
    }

    suspend fun renameExpenseCategory(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val cleanName = normalizeName(newName)
        if (cleanName.isBlank()) throw IllegalArgumentException("ব্যয়ের খাতের নাম খালি হতে পারে না")

        val current = dao.getExpenseCategoryById(id) ?: throw IllegalArgumentException("ব্যয়ের খাত পাওয়া যায়নি")
        val existing = dao.getAllExpenseCategoriesList()
        if (existing.any { it.id != id && normalizeName(it.name) == cleanName }) {
            throw IllegalArgumentException("এই নামের ব্যয়ের খাত ইতিমধ্যে আছে")
        }

        dao.updateExpenseCategory(
            current.copy(name = newName.trim().replace("\\s+".toRegex(), " "))
        )
    }

    suspend fun deleteExpenseCategory(id: Long) = withContext(Dispatchers.IO) {
        val current = dao.getExpenseCategoryById(id) ?: return@withContext
        if (current.isPreset) {
            throw IllegalStateException("ডিফল্ট ব্যয়ের খাত মুছে ফেলা যাবে না")
        }
        val count = dao.countTransactionsForCategory(id)
        if (count > 0) {
            throw IllegalStateException("এই খাতে লেনদেন থাকায় এটি মুছে ফেলা যাবে না। শুধু নাম পরিবর্তন করা যাবে।")
        }
        dao.deleteExpenseCategoryById(id)
    }

    // --- Transaction CRUD ---
    suspend fun addTransaction(
        type: String, // "INCOME" or "EXPENSE"
        amount: Long, // integer paisa
        occurredAt: Long,
        sourceId: Long? = null,
        categoryId: Long? = null,
        tagLine: String? = null,
        note: String? = null,
        linkedIncomeSourceId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        if (amount <= 0L) throw IllegalArgumentException("টাকার পরিমাণ শূন্যের বেশি হতে হবে")
        // Disallow future dates (allowing small 2-minute margin for clock drift)
        if (occurredAt > System.currentTimeMillis() + 120_000L) {
            throw IllegalArgumentException("ভবিষ্যতের তারিখ গ্রহণযোগ্য নয়")
        }

        val cleanTag = tagLine?.trim()?.replace("\\s+".toRegex(), " ")?.take(60)?.ifBlank { null }
        val cleanNote = note?.trim()?.ifBlank { null }

        dao.insertTransaction(
            IncomeExpenseTransactionEntity(
                type = type,
                amount = amount,
                occurredAt = occurredAt,
                sourceId = sourceId,
                categoryId = categoryId,
                tagLine = cleanTag,
                note = cleanNote,
                linkedIncomeSourceId = linkedIncomeSourceId
            )
        )
    }

    suspend fun updateTransaction(transaction: IncomeExpenseTransactionEntity) = withContext(Dispatchers.IO) {
        if (transaction.amount <= 0L) throw IllegalArgumentException("টাকার পরিমাণ শূন্যের বেশি হতে হবে")
        if (transaction.occurredAt > System.currentTimeMillis() + 120_000L) {
            throw IllegalArgumentException("ভবিষ্যতের তারিখ গ্রহণযোগ্য নয়")
        }
        val cleanTag = transaction.tagLine?.trim()?.replace("\\s+".toRegex(), " ")?.take(60)?.ifBlank { null }
        val cleanNote = transaction.note?.trim()?.ifBlank { null }

        dao.updateTransaction(
            transaction.copy(
                tagLine = cleanTag,
                note = cleanNote
            )
        )
    }

    suspend fun deleteTransaction(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteTransactionById(id)
    }

    suspend fun applyWorkProfilePresets(selectedProfiles: Set<String>) = withContext(Dispatchers.IO) {
        val existingSources = dao.getAllIncomeSourcesList()
        val existingCategories = dao.getAllExpenseCategoriesList()

        for (profileKey in selectedProfiles) {
            val (sourcesToAdd, categoriesToAdd) = when (profileKey) {
                com.example.data.preferences.WorkProfileKey.BUSINESS -> Pair(
                    listOf("বিক্রি", "সেবা আয়"),
                    listOf("পণ্য ক্রয়", "দোকান ভাড়া", "কর্মচারী বেতন", "বিদ্যুৎ ও ইন্টারনেট", "পরিবহন")
                )
                com.example.data.preferences.WorkProfileKey.JOB -> Pair(
                    listOf("বেতন", "বোনাস", "ওভারটাইম"),
                    emptyList()
                )
                com.example.data.preferences.WorkProfileKey.FREELANCE -> Pair(
                    listOf("ক্লায়েন্ট পেমেন্ট"),
                    listOf("সফটওয়্যার ও টুলস", "ইন্টারনেট")
                )
                else -> Pair(emptyList(), emptyList())
            }

            for (srcName in sourcesToAdd) {
                val clean = normalizeName(srcName)
                val existing = existingSources.find { normalizeName(it.name) == clean }
                if (existing != null) {
                    if (existing.profile == null) {
                        dao.updateIncomeSource(existing.copy(profile = profileKey))
                    }
                } else {
                    dao.insertIncomeSource(
                        IncomeSourceEntity(
                            name = srcName,
                            isPreset = true,
                            profile = profileKey
                        )
                    )
                }
            }

            for (catName in categoriesToAdd) {
                val clean = normalizeName(catName)
                val existing = existingCategories.find { normalizeName(it.name) == clean }
                if (existing != null) {
                    if (existing.profile == null) {
                        dao.updateExpenseCategory(existing.copy(profile = profileKey))
                    }
                } else {
                    dao.insertExpenseCategory(
                        ExpenseCategoryEntity(
                            name = catName,
                            isPreset = true,
                            profile = profileKey
                        )
                    )
                }
            }
        }
    }

    companion object {
        val PRESET_INCOME_SOURCES = listOf(
            "বেতন",
            "ব্যবসার বিক্রি",
            "মজুরি",
            "ফ্রিল্যান্স",
            "ভাড়া",
            "অন্যান্য"
        )

        val PRESET_EXPENSE_CATEGORIES = listOf(
            "খাবার",
            "যাতায়াত",
            "বিল",
            "বাসাভাড়া",
            "চিকিৎসা",
            "শিক্ষা",
            "কেনাকাটা",
            "অন্যান্য"
        )

        fun normalizeName(name: String): String =
            name.trim().replace("\\s+".toRegex(), " ").lowercase()

        @Volatile
        private var INSTANCE: IncomeExpenseRepository? = null

        fun getInstance(context: Context): IncomeExpenseRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val instance = IncomeExpenseRepository(db.incomeExpenseDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
