package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.IncomeExpenseDao
import com.example.data.local.dao.InvoiceDao
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
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerPartyEntity
import com.example.data.local.entity.LivePriceEntity
import com.example.data.local.entity.PdfReportEntity
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
        IncomeExpenseTransactionEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        PdfReportEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun portfolioDao(): PortfolioDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun livePriceDao(): LivePriceDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun incomeExpenseDao(): IncomeExpenseDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ledger_parties` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ledger_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `party_id` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount_paisa` INTEGER NOT NULL,
                        `note` TEXT,
                        `entry_date` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `kind` TEXT,
                        `category` TEXT,
                        `profile` TEXT,
                        FOREIGN KEY(`party_id`) REFERENCES `ledger_parties`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_party_id` ON `ledger_entries` (`party_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_entries_entry_date` ON `ledger_entries` (`entry_date`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `target_amount` INTEGER,
                        `target_date` INTEGER,
                        `is_default` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goal_id` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `sector_id` INTEGER NOT NULL,
                        `entry_date` INTEGER NOT NULL,
                        `note` TEXT,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_entries_goal_id` ON `savings_entries` (`goal_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_entries_sector_id` ON `savings_entries` (`sector_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_entries_entry_date` ON `savings_entries` (`entry_date`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `investment_sectors` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `is_preset` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `income_sources` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `is_preset` INTEGER NOT NULL,
                        `start_day` INTEGER,
                        `profile` TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expense_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `is_preset` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `income_expense_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `occurred_at` INTEGER NOT NULL,
                        `source_id` INTEGER,
                        `category_id` INTEGER,
                        `tag_line` TEXT,
                        `note` TEXT,
                        `linked_income_source_id` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `savings_goal_id` INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_expense_transactions_occurred_at` ON `income_expense_transactions` (`occurred_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_expense_transactions_type` ON `income_expense_transactions` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_expense_transactions_source_id` ON `income_expense_transactions` (`source_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_expense_transactions_category_id` ON `income_expense_transactions` (`category_id`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 4 to 5: Schema parity check
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure all tables and indices exist if transitioning from 5 to 6
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `income_sources` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `is_preset` INTEGER NOT NULL,
                        `start_day` INTEGER,
                        `profile` TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expense_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `is_preset` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `income_expense_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `occurred_at` INTEGER NOT NULL,
                        `source_id` INTEGER,
                        `category_id` INTEGER,
                        `tag_line` TEXT,
                        `note` TEXT,
                        `linked_income_source_id` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `savings_goal_id` INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_expense_transactions_occurred_at` ON `income_expense_transactions` (`occurred_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_expense_transactions_type` ON `income_expense_transactions` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_expense_transactions_source_id` ON `income_expense_transactions` (`source_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_expense_transactions_category_id` ON `income_expense_transactions` (`category_id`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expense_categories` ADD COLUMN `profile` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoice_number` TEXT NOT NULL,
                        `business_name` TEXT NOT NULL,
                        `business_phone` TEXT,
                        `business_address` TEXT,
                        `customer_name` TEXT NOT NULL,
                        `customer_phone` TEXT,
                        `customer_address` TEXT,
                        `invoice_date` INTEGER NOT NULL,
                        `due_date` INTEGER,
                        `subtotal_paisa` INTEGER NOT NULL,
                        `discount_paisa` INTEGER NOT NULL,
                        `additional_charges_paisa` INTEGER NOT NULL,
                        `grand_total_paisa` INTEGER NOT NULL,
                        `paid_amount_paisa` INTEGER NOT NULL,
                        `due_amount_paisa` INTEGER NOT NULL,
                        `payment_method` TEXT,
                        `notes` TEXT,
                        `terms` TEXT,
                        `status` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `pdf_file_path` TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_invoice_number` ON `invoices` (`invoice_number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_created_at` ON `invoices` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_customer_name` ON `invoices` (`customer_name`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoice_id` INTEGER NOT NULL,
                        `item_name` TEXT NOT NULL,
                        `description` TEXT,
                        `quantity` REAL NOT NULL,
                        `unit_price_paisa` INTEGER NOT NULL,
                        `total_price_paisa` INTEGER NOT NULL,
                        `sort_order` INTEGER NOT NULL,
                        FOREIGN KEY(`invoice_id`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoice_id` ON `invoice_items` (`invoice_id`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pdf_reports` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `report_type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT,
                        `file_name` TEXT NOT NULL,
                        `file_path` TEXT NOT NULL,
                        `file_uri` TEXT NOT NULL DEFAULT '',
                        `file_size_bytes` INTEGER NOT NULL,
                        `date_range_start` INTEGER,
                        `date_range_end` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pdf_reports_created_at` ON `pdf_reports` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pdf_reports_report_type` ON `pdf_reports` (`report_type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pdf_reports_timestamp` ON `pdf_reports` (`timestamp`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pdf_reports` ADD COLUMN `file_uri` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `pdf_reports` ADD COLUMN `timestamp` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pdf_reports_timestamp` ON `pdf_reports` (`timestamp`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finora_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
