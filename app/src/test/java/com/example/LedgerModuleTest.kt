package com.example

import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerEntryType
import com.example.data.local.entity.LedgerPartyEntity
import com.example.ui.screens.ledger.BalanceType
import com.example.ui.screens.ledger.LedgerFormatter
import com.example.ui.screens.ledger.LedgerNormalizer
import com.example.ui.screens.ledger.LedgerStrings
import com.example.ui.screens.ledger.PartyWithBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerModuleTest {

    @Test
    fun testParseInputToPaisa_bengaliAndEnglishDigits() {
        // Bengali digits
        val paisaBn = LedgerFormatter.parseInputToPaisa("৫০০০")
        assertNotNull(paisaBn)
        assertEquals(500000L, paisaBn)

        // English digits
        val paisaEn = LedgerFormatter.parseInputToPaisa("5000")
        assertNotNull(paisaEn)
        assertEquals(500000L, paisaEn)

        // Decimal amount
        val paisaDec = LedgerFormatter.parseInputToPaisa("50.50")
        assertNotNull(paisaDec)
        assertEquals(5050L, paisaDec)

        // Formatted with commas and currency symbol
        val paisaWithSymbol = LedgerFormatter.parseInputToPaisa("৳ ৫,০০০")
        assertNotNull(paisaWithSymbol)
        assertEquals(500000L, paisaWithSymbol)
    }

    @Test
    fun testParseInputToPaisa_rejections() {
        // Zero
        assertNull(LedgerFormatter.parseInputToPaisa("0"))
        assertNull(LedgerFormatter.parseInputToPaisa("০"))
        assertNull(LedgerFormatter.parseInputToPaisa("0.00"))

        // Negative
        assertNull(LedgerFormatter.parseInputToPaisa("-500"))
        assertNull(LedgerFormatter.parseInputToPaisa("-৫০০"))

        // Empty / Blank
        assertNull(LedgerFormatter.parseInputToPaisa(""))
        assertNull(LedgerFormatter.parseInputToPaisa("   "))

        // Exceeds 9 digits
        assertNull(LedgerFormatter.parseInputToPaisa("1000000000")) // 10 digits
        assertNotNull(LedgerFormatter.parseInputToPaisa("999999999")) // 9 digits
    }

    @Test
    fun testFormatPaisa() {
        assertEquals("৳ ৩,০০০", LedgerFormatter.formatPaisa(300000L, useBengaliDigits = true))
        assertEquals("৳ 3,000", LedgerFormatter.formatPaisa(300000L, useBengaliDigits = false))
        assertEquals("৳ ০", LedgerFormatter.formatPaisa(0L, useBengaliDigits = true))
        assertEquals("৳ 0", LedgerFormatter.formatPaisa(0L, useBengaliDigits = false))
    }

    @Test
    fun testBalanceRule_ChecklistCUJ() {
        // Person: রহিম
        val party = LedgerPartyEntity(id = 1L, name = "রহিম", phone = "01712345678")

        // Step 1: Add GAVE ৳5,000
        val gave5000 = LedgerEntryEntity(
            id = 101L,
            partyId = 1L,
            type = LedgerEntryType.GAVE.name,
            amountPaisa = 500000L, // ৳5,000 in paisa
            entryDate = System.currentTimeMillis()
        )

        var gaveTotal = gave5000.amountPaisa
        var receivedTotal = 0L
        var net = gaveTotal - receivedTotal

        assertEquals(500000L, net)
        var item = PartyWithBalance(
            party = party,
            totalGavePaisa = gaveTotal,
            totalReceivedPaisa = receivedTotal,
            netBalancePaisa = net,
            entryCount = 1,
            lastEntryDate = gave5000.entryDate
        )
        assertEquals(BalanceType.PABO, item.balanceType)
        assertEquals("৳ ৫,০০০", LedgerFormatter.formatPaisa(item.displayAmountPaisa, useBengaliDigits = true))

        // Step 2: Add RECEIVED ৳2,000
        val received2000 = LedgerEntryEntity(
            id = 102L,
            partyId = 1L,
            type = LedgerEntryType.RECEIVED.name,
            amountPaisa = 200000L, // ৳2,000 in paisa
            entryDate = System.currentTimeMillis()
        )

        receivedTotal += received2000.amountPaisa
        net = gaveTotal - receivedTotal

        // Net should be ৳3,000 (300000 paisa)
        assertEquals(300000L, net)
        item = PartyWithBalance(
            party = party,
            totalGavePaisa = gaveTotal,
            totalReceivedPaisa = receivedTotal,
            netBalancePaisa = net,
            entryCount = 2,
            lastEntryDate = received2000.entryDate
        )
        // Must show পাবো (green)
        assertEquals(BalanceType.PABO, item.balanceType)
        assertEquals("৳ ৩,০০০", LedgerFormatter.formatPaisa(item.displayAmountPaisa, useBengaliDigits = true))
        assertEquals("৳ 3,000", LedgerFormatter.formatPaisa(item.displayAmountPaisa, useBengaliDigits = false))

        // Step 3: Add RECEIVED ৳3,000 -> Settle balance
        receivedTotal += 300000L
        net = gaveTotal - receivedTotal
        assertEquals(0L, net)
        item = item.copy(
            totalReceivedPaisa = receivedTotal,
            netBalancePaisa = net,
            entryCount = 3
        )
        assertEquals(BalanceType.SETTLED, item.balanceType)

        // Step 4: Add RECEIVED another ৳1,000 -> Negative net (দেবো)
        receivedTotal += 100000L
        net = gaveTotal - receivedTotal
        assertEquals(-100000L, net)
        item = item.copy(
            totalReceivedPaisa = receivedTotal,
            netBalancePaisa = net,
            entryCount = 4
        )
        assertEquals(BalanceType.DEBO, item.balanceType)
        assertEquals(100000L, item.displayAmountPaisa)
        assertEquals("৳ ১,০০০", LedgerFormatter.formatPaisa(item.displayAmountPaisa, useBengaliDigits = true))
    }

    @Test
    fun testExtractInitials() {
        assertEquals("রই", LedgerFormatter.extractInitials("রহিম ইসলাম"))
        assertEquals("রহ", LedgerFormatter.extractInitials("রহিম"))
        assertEquals("RK", LedgerFormatter.extractInitials("Rahim Khan"))
        assertEquals("R", LedgerFormatter.extractInitials("R"))
        assertEquals("?", LedgerFormatter.extractInitials(""))
    }

    @Test
    fun testNormalizerAndDuplicateRules() {
        // Name normalization: trim, collapse spaces, lowercase
        assertEquals("rahim khan", LedgerNormalizer.normalizeName("  Rahim   Khan  "))
        assertEquals("রহিম ইসলাম", LedgerNormalizer.normalizeName("  রহিম    ইসলাম "))

        // Phone normalization: convert Bengali digits to English, remove spaces, dashes, +88, compare last 11 digits
        val norm1 = LedgerNormalizer.normalizePhone("01712345678")
        val norm2 = LedgerNormalizer.normalizePhone("+8801712345678")
        val norm3 = LedgerNormalizer.normalizePhone("+88 017-1234-5678")
        val norm4 = LedgerNormalizer.normalizePhone("০১৭১২৩৪৫৬৭৮")
        val norm5 = LedgerNormalizer.normalizePhone("+৮৮০১৭১২৩৪৫৬৭৮")

        assertEquals("01712345678", norm1)
        assertEquals(norm1, norm2)
        assertEquals(norm1, norm3)
        assertEquals(norm1, norm4)
        assertEquals(norm1, norm5)

        // Different phone
        val otherPhone = LedgerNormalizer.normalizePhone("01899999999")
        assertEquals("01899999999", otherPhone)
        assertTrue(norm1 != otherPhone)
    }

    @Test
    fun testUpdatedLabels() {
        // Confirm labels read "আমি পাবো / আমি দিবো"
        assertEquals("আমি পাবো", LedgerStrings.STATUS_PABO)
        assertEquals("আমি দিবো", LedgerStrings.STATUS_DEBO)
        assertEquals("মোট আমি পাবো", LedgerStrings.SUMMARY_PABO)
        assertEquals("মোট আমি দিবো", LedgerStrings.SUMMARY_DEBO)
    }
}
