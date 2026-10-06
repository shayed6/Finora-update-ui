package com.example

import com.example.data.local.entity.CashTransactionType
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.IncomeExpenseTransactionEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.repository.IncomeExpenseRepository
import com.example.ui.screens.incomeexpense.IncomeExpenseFormatter
import com.example.ui.screens.incomeexpense.MonthYear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class IncomeExpenseModuleTest {

    @Test
    fun testAmountParsing_BengaliAndEnglishDigits() {
        // English digits
        assertEquals(500000L, IncomeExpenseFormatter.parseInputToPaisa("5000"))
        assertEquals(5050L, IncomeExpenseFormatter.parseInputToPaisa("50.50"))
        assertEquals(10000000L, IncomeExpenseFormatter.parseInputToPaisa("1,00,000"))
        assertEquals(25000L, IncomeExpenseFormatter.parseInputToPaisa("৳ 250"))

        // Bengali digits
        assertEquals(500000L, IncomeExpenseFormatter.parseInputToPaisa("৫০০০"))
        assertEquals(5050L, IncomeExpenseFormatter.parseInputToPaisa("৫০.৫০"))
        assertEquals(10000000L, IncomeExpenseFormatter.parseInputToPaisa("১,০০,০০০"))
        assertEquals(25000L, IncomeExpenseFormatter.parseInputToPaisa("৳ ২৫০"))
    }

    @Test
    fun testAmountParsing_RejectZeroNegativeEmptyMaxDigits() {
        // Zero
        assertNull(IncomeExpenseFormatter.parseInputToPaisa("0"))
        assertNull(IncomeExpenseFormatter.parseInputToPaisa("০"))
        assertNull(IncomeExpenseFormatter.parseInputToPaisa("0.00"))

        // Negative
        assertNull(IncomeExpenseFormatter.parseInputToPaisa("-500"))
        assertNull(IncomeExpenseFormatter.parseInputToPaisa("-৫০০"))

        // Empty / Blank
        assertNull(IncomeExpenseFormatter.parseInputToPaisa(""))
        assertNull(IncomeExpenseFormatter.parseInputToPaisa("   "))

        // Over 9 digits
        assertNull(IncomeExpenseFormatter.parseInputToPaisa("1234567890")) // 10 digits
        assertNotNull(IncomeExpenseFormatter.parseInputToPaisa("999999999")) // 9 digits
    }

    @Test
    fun testFormattingPaisa_TakaSymbolAndSeparators() {
        val formattedBn = IncomeExpenseFormatter.formatPaisa(500000L, useBengaliDigits = true)
        assertTrue(formattedBn.contains("৳"))
        assertTrue(formattedBn.contains("৫,০০০"))

        val formattedEn = IncomeExpenseFormatter.formatPaisa(500000L, useBengaliDigits = false)
        assertEquals("৳ 5,000", formattedEn)

        val formattedWithPaisa = IncomeExpenseFormatter.formatPaisa(5075L, useBengaliDigits = false)
        assertEquals("৳ 50.75", formattedWithPaisa)
    }

    @Test
    fun testPresetSourcesAndCategoriesIntegrity() {
        assertEquals(6, IncomeExpenseRepository.PRESET_INCOME_SOURCES.size)
        assertTrue(IncomeExpenseRepository.PRESET_INCOME_SOURCES.contains("বেতন"))
        assertTrue(IncomeExpenseRepository.PRESET_INCOME_SOURCES.contains("ব্যবসার বিক্রি"))
        assertTrue(IncomeExpenseRepository.PRESET_INCOME_SOURCES.contains("মজুরি"))
        assertTrue(IncomeExpenseRepository.PRESET_INCOME_SOURCES.contains("ফ্রিল্যান্স"))
        assertTrue(IncomeExpenseRepository.PRESET_INCOME_SOURCES.contains("ভাড়া"))
        assertTrue(IncomeExpenseRepository.PRESET_INCOME_SOURCES.contains("অন্যান্য"))

        assertEquals(8, IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.size)
        assertTrue(IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.contains("খাবার"))
        assertTrue(IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.contains("যাতায়াত"))
        assertTrue(IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.contains("বিল"))
        assertTrue(IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.contains("বাসাভাড়া"))
        assertTrue(IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.contains("চিকিৎসা"))
        assertTrue(IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.contains("শিক্ষা"))
        assertTrue(IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.contains("কেনাকাটা"))
        assertTrue(IncomeExpenseRepository.PRESET_EXPENSE_CATEGORIES.contains("অন্যান্য"))
    }

    @Test
    fun testNormalizationOfNames() {
        assertEquals("salary", IncomeExpenseRepository.normalizeName("  Salary  "))
        assertEquals("বেতন", IncomeExpenseRepository.normalizeName("  বেতন  "))
        assertEquals("পার্ট টাইম", IncomeExpenseRepository.normalizeName("পার্ট    টাইম"))
    }

    @Test
    fun testMonthYearTimestamps() {
        val my = MonthYear(year = 2026, month = 9) // October 2026 (0-indexed month 9)
        val start = my.toStartTimestamp()
        val end = my.toEndTimestamp()

        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        assertEquals(2026, calStart.get(Calendar.YEAR))
        assertEquals(9, calStart.get(Calendar.MONTH))
        assertEquals(1, calStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))

        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(2026, calEnd.get(Calendar.YEAR))
        assertEquals(9, calEnd.get(Calendar.MONTH))
        assertEquals(31, calEnd.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
    }

    @Test
    fun testSummaryCalculation() {
        val txs = listOf(
            IncomeExpenseTransactionEntity(id = 1, type = CashTransactionType.INCOME.name, amount = 3000000L, occurredAt = 1000L),
            IncomeExpenseTransactionEntity(id = 2, type = CashTransactionType.INCOME.name, amount = 1000000L, occurredAt = 2000L),
            IncomeExpenseTransactionEntity(id = 3, type = CashTransactionType.EXPENSE.name, amount = 1500000L, occurredAt = 3000L),
            IncomeExpenseTransactionEntity(id = 4, type = CashTransactionType.EXPENSE.name, amount = 500000L, occurredAt = 4000L)
        )

        var totalIncome = 0L
        var totalExpense = 0L
        txs.forEach {
            if (it.type == CashTransactionType.INCOME.name) totalIncome += it.amount
            if (it.type == CashTransactionType.EXPENSE.name) totalExpense += it.amount
        }
        val balance = totalIncome - totalExpense

        assertEquals(4000000L, totalIncome) // ৳ 40,000
        assertEquals(2000000L, totalExpense) // ৳ 20,000
        assertEquals(2000000L, balance) // ৳ 20,000
    }
}
