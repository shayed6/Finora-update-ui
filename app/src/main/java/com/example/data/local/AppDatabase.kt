package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.IncomeExpenseDao
import com.example.data.local.dao.LedgerDao
import com.example.data.local.dao.LivePriceDao
import com.example.data.local.dao.PortfolioDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.entity.DividendEntity
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.IncomeExpenseTransactionEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.local.entity.InvestmentSectorEntity
import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerPartyEntity
import com.example.data.local.entity.LivePriceEntity
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity

@Database(
    entities = [
        HoldingEntity::class,
        TransactionEntity::class,
        DividendEntity::class,
        SavingsGoalEntity::class,
        SavingsEntryEntity::class,
        InvestmentSectorEntity::class,
        LivePriceEntity::class,
        LedgerPartyEntity::class,
        LedgerEntryEntity::class,
        IncomeSourceEntity::class,
        ExpenseCategoryEntity::class,
        IncomeExpenseTransactionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun portfolioDao(): PortfolioDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun livePriceDao(): LivePriceDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun incomeExpenseDao(): IncomeExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finora_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
