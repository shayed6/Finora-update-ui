package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.IncomeExpenseTransactionEntity
import com.example.data.local.entity.IncomeSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeExpenseDao {

    // --- Income Sources ---
    @Query("SELECT * FROM income_sources ORDER BY is_preset DESC, id ASC")
    fun getAllIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources ORDER BY is_preset DESC, id ASC")
    suspend fun getAllIncomeSourcesList(): List<IncomeSourceEntity>

    @Query("SELECT * FROM income_sources WHERE id = :id")
    suspend fun getIncomeSourceById(id: Long): IncomeSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(source: IncomeSourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIncomeSources(sources: List<IncomeSourceEntity>): List<Long>

    @Update
    suspend fun updateIncomeSource(source: IncomeSourceEntity)

    @Delete
    suspend fun deleteIncomeSource(source: IncomeSourceEntity)

    @Query("DELETE FROM income_sources WHERE id = :id")
    suspend fun deleteIncomeSourceById(id: Long)

    // --- Expense Categories ---
    @Query("SELECT * FROM expense_categories ORDER BY is_preset DESC, id ASC")
    fun getAllExpenseCategories(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories ORDER BY is_preset DESC, id ASC")
    suspend fun getAllExpenseCategoriesList(): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM expense_categories WHERE id = :id")
    suspend fun getExpenseCategoryById(id: Long): ExpenseCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategory(category: ExpenseCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpenseCategories(categories: List<ExpenseCategoryEntity>): List<Long>

    @Update
    suspend fun updateExpenseCategory(category: ExpenseCategoryEntity)

    @Delete
    suspend fun deleteExpenseCategory(category: ExpenseCategoryEntity)

    @Query("DELETE FROM expense_categories WHERE id = :id")
    suspend fun deleteExpenseCategoryById(id: Long)

    // --- Transactions ---
    @Query("SELECT * FROM income_expense_transactions ORDER BY occurred_at DESC, created_at DESC")
    fun getAllTransactions(): Flow<List<IncomeExpenseTransactionEntity>>

    @Query("SELECT * FROM income_expense_transactions WHERE occurred_at >= :startTime AND occurred_at <= :endTime ORDER BY occurred_at DESC, created_at DESC")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<IncomeExpenseTransactionEntity>>

    @Query("SELECT * FROM income_expense_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): IncomeExpenseTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: IncomeExpenseTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: IncomeExpenseTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: IncomeExpenseTransactionEntity)

    @Query("DELETE FROM income_expense_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // Checks for deletion validation: unused custom source/category
    @Query("SELECT COUNT(*) FROM income_expense_transactions WHERE source_id = :sourceId OR linked_income_source_id = :sourceId")
    suspend fun countTransactionsForSource(sourceId: Long): Int

    @Query("SELECT COUNT(*) FROM income_expense_transactions WHERE category_id = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: Long): Int

    // Autocomplete suggestions for tag line
    @Query("SELECT DISTINCT tag_line FROM income_expense_transactions WHERE tag_line IS NOT NULL AND tag_line != '' ORDER BY occurred_at DESC")
    fun getAllDistinctTagLines(): Flow<List<String>>

    @Query("SELECT DISTINCT tag_line FROM income_expense_transactions WHERE tag_line IS NOT NULL AND tag_line != '' ORDER BY occurred_at DESC")
    suspend fun getDistinctTagLinesList(): List<String>
}
