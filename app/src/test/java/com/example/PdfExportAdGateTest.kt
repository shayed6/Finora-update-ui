package com.example

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ads.PdfExportAdGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfExportAdGateTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        PdfExportAdGate.resetForTesting(context)
    }

    @Test
    fun testFirstExportOfDay_requiresAdAndRecordsDateOnlyWhenRewardEarned() {
        // Initially no ad has been shown today
        val today = PdfExportAdGate.getTodayDateString()
        assertTrue("Initially, ad must be required for the first export of the day", PdfExportAdGate.isAdRequiredToday(context))
        assertEquals("", PdfExportAdGate.getLastPdfAdShownDate(context))

        // Simulate reward earned: date is recorded
        PdfExportAdGate.setLastPdfAdShownDate(context, today)
        assertEquals(today, PdfExportAdGate.getLastPdfAdShownDate(context))
        assertFalse("After reward earned, ad must no longer be required today", PdfExportAdGate.isAdRequiredToday(context))
    }

    @Test
    fun testSameDaySubsequentExport_skipsAdEntirelyAcrossAllHisabPatiSections() {
        val today = PdfExportAdGate.getTodayDateString()
        // Simulate ad completed earlier today
        PdfExportAdGate.setLastPdfAdShownDate(context, today)

        // Multiple PDF exports (Bakir Khata, Income-Expense, Savings, Accounts Summary)
        val export1Proceeded = AtomicBoolean(false)
        val export2Proceeded = AtomicBoolean(false)
        val export3Proceeded = AtomicBoolean(false)

        PdfExportAdGate.onPdfExportRequested(activity = null) {
            export1Proceeded.set(true)
        }
        PdfExportAdGate.onPdfExportRequested(activity = null) {
            export2Proceeded.set(true)
        }
        PdfExportAdGate.onPdfExportRequested(activity = null) {
            export3Proceeded.set(true)
        }

        assertTrue("First subsequent export must proceed immediately without ad", export1Proceeded.get())
        assertTrue("Second subsequent export must proceed immediately without ad", export2Proceeded.get())
        assertTrue("Third subsequent export must proceed immediately without ad", export3Proceeded.get())
        assertFalse("Ad is not required today", PdfExportAdGate.isAdRequiredToday(context))
    }

    @Test
    fun testAdLoadFailure_fallbackProceedsWithoutUpdatingLastDate() {
        // Initial state: not shown today
        assertEquals("", PdfExportAdGate.getLastPdfAdShownDate(context))
        assertTrue(PdfExportAdGate.isAdRequiredToday(context))

        // When ad fails to load/show (simulated via null activity or load error fallback),
        // export proceeds so user is not blocked, but date is NOT updated
        var exportProceeded = false
        PdfExportAdGate.onPdfExportRequested(activity = null) {
            exportProceeded = true
        }

        assertTrue("Export must proceed even if ad cannot be shown", exportProceeded)
        // Last date must NOT be updated so ad gate is retried on next export
        assertEquals("Date must not be marked as shown when ad failed to present", "", PdfExportAdGate.getLastPdfAdShownDate(context))
        assertTrue("Ad must still be required on next attempt", PdfExportAdGate.isAdRequiredToday(context))
    }

    @Test
    fun testNextDayExport_rearmsAdGateAutomaticallyAtMidnight() {
        // Simulate ad was shown yesterday
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        PdfExportAdGate.setLastPdfAdShownDate(context, yesterdayStr)
        assertEquals(yesterdayStr, PdfExportAdGate.getLastPdfAdShownDate(context))

        // Today is a new day: gate must automatically re-arm
        assertTrue("Ad must be required again on the next day", PdfExportAdGate.isAdRequiredToday(context))
    }

    @Test
    fun testDateStringFormatting() {
        val todayStr = PdfExportAdGate.getTodayDateString()
        assertTrue("Date string format should be yyyy-MM-dd", todayStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }
}
