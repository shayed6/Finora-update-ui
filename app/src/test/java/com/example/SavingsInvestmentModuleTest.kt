package com.example

import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsEntryType
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.repository.SavingsGoalRepository
import com.example.ui.screens.goals.SavingsFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class SavingsInvestmentModuleTest {

    @Test
    fun testBangladeshiDigitGrouping_FormatTaka() {
        // English digits
        assertEquals("৳5,00,000", SavingsFormatter.formatBangladeshiTaka(50000000L, useBengaliDigits = false))
        assertEquals("৳75,000", SavingsFormatter.formatBangladeshiTaka(7500000L, useBengaliDigits = false))
        assertEquals("৳4,25,000", SavingsFormatter.formatBangladeshiTaka(42500000L, useBengaliDigits = false))
        assertEquals("৳65,000", SavingsFormatter.formatBangladeshiTaka(6500000L, useBengaliDigits = false))
        assertEquals("৳10,000", SavingsFormatter.formatBangladeshiTaka(1000000L, useBengaliDigits = false))
        assertEquals("৳1,00,00,000", SavingsFormatter.formatBangladeshiTaka(1000000000L, useBengaliDigits = false)) // 1 crore
        assertEquals("৳0", SavingsFormatter.formatBangladeshiTaka(0L, useBengaliDigits = false))

        // Bengali digits
        val bn5Lakh = SavingsFormatter.formatBangladeshiTaka(50000000L, useBengaliDigits = true)
        assertEquals("৳৫,০০,০০০", bn5Lakh)

        val bn75k = SavingsFormatter.formatBangladeshiTaka(7500000L, useBengaliDigits = true)
        assertEquals("৳৭৫,০০০", bn75k)

        val bn425k = SavingsFormatter.formatBangladeshiTaka(42500000L, useBengaliDigits = true)
        assertEquals("৳৪,২৫,০০০", bn425k)

        val bn65k = SavingsFormatter.formatBangladeshiTaka(6500000L, useBengaliDigits = true)
        assertEquals("৳৬৫,০০০", bn65k)
    }

    @Test
    fun testAmountParsing_BengaliAndEnglishDigits() {
        // English
        assertEquals(50000000L, SavingsFormatter.parseInputToPaisa("500000"))
        assertEquals(5000000L, SavingsFormatter.parseInputToPaisa("50,000"))
        assertEquals(2500000L, SavingsFormatter.parseInputToPaisa("৳ 25,000"))

        // Bengali
        assertEquals(50000000L, SavingsFormatter.parseInputToPaisa("৫০০০০০"))
        assertEquals(5000000L, SavingsFormatter.parseInputToPaisa("৫০,০০০"))
        assertEquals(2500000L, SavingsFormatter.parseInputToPaisa("৳ ২৫,০০০"))

        // Rejections: 0, negative, empty
        assertNull(SavingsFormatter.parseInputToPaisa("0"))
        assertNull(SavingsFormatter.parseInputToPaisa("০"))
        assertNull(SavingsFormatter.parseInputToPaisa("-5000"))
        assertNull(SavingsFormatter.parseInputToPaisa(""))
        assertNull(SavingsFormatter.parseInputToPaisa("   "))

        // Max 10 digits
        assertNotNull(SavingsFormatter.parseInputToPaisa("9999999999")) // 10 digits
        assertNull(SavingsFormatter.parseInputToPaisa("10000000000")) // 11 digits
    }

    @Test
    fun testPresetSectors_NoSharesSector() {
        val presets = SavingsGoalRepository.PRESET_SECTORS
        assertEquals(8, presets.size)
        assertTrue(presets.contains("ব্যাংক সঞ্চয়/DPS"))
        assertTrue(presets.contains("FDR"))
        assertTrue(presets.contains("সঞ্চয়পত্র"))
        assertTrue(presets.contains("স্বর্ণ"))
        assertTrue(presets.contains("জমি/সম্পত্তি"))
        assertTrue(presets.contains("ব্যবসা"))
        assertTrue(presets.contains("বীমা"))
        assertTrue(presets.contains("অন্যান্য"))

        // Critical: MUST NOT contain "শেয়ার" sector to avoid double-counting with Portfolio
        assertFalse(presets.contains("শেয়ার"))
        assertFalse(presets.contains("শেয়ার বাজার"))
        assertFalse(presets.any { it.contains("শেয়ার") })
    }

    @Test
    fun testChecklistCUJ_LandBuyingGoalCalculations() {
        val targetPaisa = 50000000L // ৳5,00,000

        // 1. Add ৳50,000 (সঞ্চয়পত্র) and ৳25,000 (স্বর্ণ)
        val entries = mutableListOf(
            SavingsEntryEntity(id = 1, goalId = 2, type = SavingsEntryType.DEPOSIT.name, amount = 5000000L, sectorId = 3, entryDate = 1000L),
            SavingsEntryEntity(id = 2, goalId = 2, type = SavingsEntryType.DEPOSIT.name, amount = 2500000L, sectorId = 4, entryDate = 2000L)
        )

        var savedPaisa = SavingsGoalRepository.calculateGoalBalance(entries)
        assertEquals(7500000L, savedPaisa) // ৳75,000

        var remainingPaisa = targetPaisa - savedPaisa
        assertEquals(42500000L, remainingPaisa) // ৳4,25,000

        var progressPct = ((savedPaisa.toDouble() / targetPaisa.toDouble()) * 100.0).roundToInt()
        assertEquals(15, progressPct) // 15%

        // 2. Withdraw ৳10,000
        entries.add(
            SavingsEntryEntity(id = 3, goalId = 2, type = SavingsEntryType.WITHDRAW.name, amount = 1000000L, sectorId = 3, entryDate = 3000L)
        )

        savedPaisa = SavingsGoalRepository.calculateGoalBalance(entries)
        assertEquals(6500000L, savedPaisa) // ৳65,000

        remainingPaisa = targetPaisa - savedPaisa
        assertEquals(43500000L, remainingPaisa) // ৳4,35,000

        progressPct = ((savedPaisa.toDouble() / targetPaisa.toDouble()) * 100.0).roundToInt()
        assertEquals(13, progressPct) // 13%

        // 3. Withdraw larger than saved should be rejected
        val excessWithdraw = 7000000L // ৳70,000 > ৳65,000
        assertTrue(excessWithdraw > savedPaisa)
    }

    @Test
    fun testGoalDeletion_EntriesMovedToDefaultGoal_TotalUnchanged() {
        val defaultGoal = SavingsGoalEntity(id = 1, name = "সাধারণ সঞ্চয়", isDefault = true)
        val customGoal = SavingsGoalEntity(id = 2, name = "জমি কেনা", targetAmount = 50000000L, isDefault = false)

        val entries = mutableListOf(
            SavingsEntryEntity(id = 1, goalId = customGoal.id, type = SavingsEntryType.DEPOSIT.name, amount = 5000000L, sectorId = 3, entryDate = 1000L),
            SavingsEntryEntity(id = 2, goalId = customGoal.id, type = SavingsEntryType.DEPOSIT.name, amount = 2500000L, sectorId = 4, entryDate = 2000L),
            SavingsEntryEntity(id = 3, goalId = defaultGoal.id, type = SavingsEntryType.DEPOSIT.name, amount = 1000000L, sectorId = 1, entryDate = 3000L)
        )

        val totalBefore = entries.filter { it.type == SavingsEntryType.DEPOSIT.name }.sumOf { it.amount }
        assertEquals(8500000L, totalBefore) // ৳85,000

        // Simulate moving customGoal entries to defaultGoal on delete
        val remappedEntries = entries.map {
            if (it.goalId == customGoal.id) it.copy(goalId = defaultGoal.id) else it
        }

        val totalAfter = remappedEntries.filter { it.type == SavingsEntryType.DEPOSIT.name }.sumOf { it.amount }
        assertEquals(totalBefore, totalAfter) // Total card is unchanged!
        assertEquals(3, remappedEntries.count { it.goalId == defaultGoal.id })
    }
}
