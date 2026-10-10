package com.example.ui.screens.summary

import com.example.util.BengaliFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SummaryStrings {
    const val MODULE_TITLE = "হিসাব নিকাশ"
    const val MODULE_SUBTITLE = "আয়, ব্যয়, সঞ্চয় ও বাকির সামগ্রিক হিসাব"

    const val CARD_INCOME = "মোট আয়"
    const val CARD_EXPENSE = "মোট ব্যয়"
    const val CARD_SAVINGS = "মোট সঞ্চয়"
    const val CARD_REMAINING = "অবশিষ্ট"

    const val TOGGLE_PIE = "পাই"
    const val TOGGLE_LINE = "লাইন"

    const val PIE_EXPENSE = "ব্যয়"
    const val PIE_SAVINGS = "সঞ্চয়"
    const val PIE_REMAINING = "অবশিষ্ট"

    const val PIE_NOTE_EXCEEDS = "ব্যয় ও সঞ্চয় আয়ের চেয়ে বেশি"
    const val PIE_NOTE_NO_INCOME = "এই সময়ে কোনো আয় নেই"

    const val SECTION_LEDGER_TITLE = "বাকির খাতা"
    const val LEDGER_PABO = "মোট আমি পাবো"
    const val LEDGER_DEBO = "মোট আমি দিবো"

    const val SECTION_BY_INCOME_SOURCE = "আয়ের ধরন অনুযায়ী"
    const val BTN_CYCLE_SETTINGS = "চক্র নির্ধারণ"

    const val CARD_SOURCE_INCOME = "আয়"
    const val CARD_SOURCE_TAGGED_EXPENSE = "ট্যাগ করা ব্যয়"
    const val CARD_SOURCE_REMAINING = "অবশিষ্ট"

    const val EMPTY_PERIOD_TITLE = "এই সময়ে কোনো হিসাব নেই"
    const val EMPTY_PERIOD_MSG = "নির্বাচিত সময়ের মধ্যে আয়-ব্যয় বা সঞ্চয়ের কোনো লেনদেন পাওয়া যায়নি।"

    const val TITLE_CYCLE_SETTINGS = "চক্র নির্ধারণ"
    const val SUBTITLE_CYCLE_SETTINGS = "আয়ের উৎসের হিসাব চক্র নির্ধারণ"
    const val CYCLE_SETTINGS_HINT = "প্রতিটি আয়ের উৎসের জন্য মাসের নির্দিষ্ট শুরুর দিন (১–২৮) অথবা ক্যালেন্ডার মাস নির্বাচন করুন।"
    const val CYCLE_CALENDAR_DEFAULT = "ক্যালেন্ডার মাস (ডিফল্ট)"
    const val CYCLE_DAY_OPTION = "তারিখ %d হতে পরবর্তী মাসের %d পর্যন্ত"
    const val BTN_SAVE = "সংরক্ষণ করুন"
    const val BTN_CANCEL = "বাতিল"
    const val TOAST_CYCLE_SAVED = "চক্র সফলভাবে সংরক্ষণ করা হয়েছে"

    const val DIALOG_CUSTOM_PERIOD_TITLE = "সময়কাল নির্বাচন করুন"
    const val BTN_APPLY = "প্রয়োগ করুন"
}

enum class ChartType {
    PIE,
    LINE
}

data class PeriodRange(
    val startTimestamp: Long,
    val endTimestamp: Long,
    val label: String,
    val isCustom: Boolean = false,
    val calendarMonthYear: MonthYear? = null
)

data class MonthYear(
    val year: Int,
    val month: Int // 0..11
) {
    fun toStartTimestamp(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun toEndTimestamp(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun previous(): MonthYear {
        return if (month == 0) MonthYear(year - 1, 11) else MonthYear(year, month - 1)
    }

    fun next(): MonthYear {
        return if (month == 11) MonthYear(year + 1, 0) else MonthYear(year, month + 1)
    }

    companion object {
        fun current(): MonthYear {
            val cal = Calendar.getInstance()
            return MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
    }
}

object SummaryDateFormatter {
    private val bengaliMonthNames = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    fun formatPeriodLabel(startMs: Long, endMs: Long, useBengaliDigits: Boolean = true): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMs }

        val startDay = startCal.get(Calendar.DAY_OF_MONTH)
        val startMonth = bengaliMonthNames[startCal.get(Calendar.MONTH)]
        val startYear = startCal.get(Calendar.YEAR)

        val endDay = endCal.get(Calendar.DAY_OF_MONTH)
        val endMonth = bengaliMonthNames[endCal.get(Calendar.MONTH)]
        val endYear = endCal.get(Calendar.YEAR)

        val formatted = if (startYear == endYear && startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH)) {
            // Same month: "১ সেপ্টেম্বর – ৩০ সেপ্টেম্বর" or "১ সেপ্টেম্বর – ৩০ সেপ্টেম্বর, ২০২৬"
            "$startDay $startMonth – $endDay $endMonth"
        } else if (startYear == endYear) {
            "$startDay $startMonth – $endDay $endMonth"
        } else {
            "$startDay $startMonth, $startYear – $endDay $endMonth, $endYear"
        }

        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun formatMonthLabel(monthYear: MonthYear, useBengaliDigits: Boolean = true): String {
        val name = bengaliMonthNames[monthYear.month]
        val yr = monthYear.year.toString().takeLast(2)
        val res = "$name '$yr"
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(res) else res
    }

    fun formatShortMonth(monthIdx: Int): String {
        return bengaliMonthNames[monthIdx % 12]
    }
}

object SummaryFormatter {
    fun formatPaisa(paisa: Long, useBengaliDigits: Boolean = true): String {
        val taka = paisa / 100.0
        val pattern = if (paisa % 100L == 0L) "#,##,##0" else "#,##,##0.00"
        val df = java.text.DecimalFormat(pattern, java.text.DecimalFormatSymbols(Locale.US))
        val formatted = df.format(taka)
        val withTaka = "৳ $formatted"
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(withTaka) else withTaka
    }
}

data class PeriodSummary(
    val incomePaisa: Long,
    val expensePaisa: Long,
    val savingsPaisa: Long,
    val remainingPaisa: Long,
    val totalTransactionsCount: Int
)

data class MonthlyTrendPoint(
    val monthYear: MonthYear,
    val monthLabel: String,
    val incomePaisa: Long,
    val expensePaisa: Long,
    val savingsPaisa: Long
)

data class IncomeSourceCardData(
    val sourceId: Long,
    val sourceName: String,
    val startDay: Int?,
    val currentPeriod: PeriodRange,
    val incomePaisa: Long,
    val taggedExpensePaisa: Long,
    val remainingPaisa: Long
)

data class LedgerOverview(
    val totalPaboPaisa: Long,
    val totalDeboPaisa: Long
)

data class AccountsSummaryUiState(
    val period: PeriodRange = defaultCurrentMonthPeriod(),
    val summary: PeriodSummary = PeriodSummary(0L, 0L, 0L, 0L, 0),
    val chartType: ChartType = ChartType.PIE,
    val sixMonthsTrend: List<MonthlyTrendPoint> = emptyList(),
    val sourceCards: List<IncomeSourceCardData> = emptyList(),
    val ledgerOverview: LedgerOverview = LedgerOverview(0L, 0L),
    val isLoading: Boolean = false
)

fun defaultCurrentMonthPeriod(): PeriodRange {
    val my = MonthYear.current()
    val start = my.toStartTimestamp()
    val end = my.toEndTimestamp()
    val label = SummaryDateFormatter.formatPeriodLabel(start, end, true)
    return PeriodRange(
        startTimestamp = start,
        endTimestamp = end,
        label = label,
        isCustom = false,
        calendarMonthYear = my
    )
}

fun computeDefaultCycle(startDay: Int?, referenceMs: Long = System.currentTimeMillis()): PeriodRange {
    if (startDay == null) {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMs }
        val my = MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        val start = my.toStartTimestamp()
        val end = my.toEndTimestamp()
        return PeriodRange(
            startTimestamp = start,
            endTimestamp = end,
            label = SummaryDateFormatter.formatPeriodLabel(start, end, true),
            isCustom = false,
            calendarMonthYear = my
        )
    }

    val refCal = Calendar.getInstance().apply { timeInMillis = referenceMs }
    val refDay = refCal.get(Calendar.DAY_OF_MONTH)
    val startCal = Calendar.getInstance().apply {
        timeInMillis = referenceMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (refDay < startDay) {
            add(Calendar.MONTH, -1)
        }
        set(Calendar.DAY_OF_MONTH, startDay)
    }

    val nextStartCal = (startCal.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
        set(Calendar.DAY_OF_MONTH, startDay)
    }

    val startMs = startCal.timeInMillis
    val endMs = nextStartCal.timeInMillis - 1L

    return PeriodRange(
        startTimestamp = startMs,
        endTimestamp = endMs,
        label = SummaryDateFormatter.formatPeriodLabel(startMs, endMs, true),
        isCustom = false,
        calendarMonthYear = null
    )
}

fun stepCycle(current: PeriodRange, startDay: Int?, stepForward: Boolean): PeriodRange {
    if (startDay == null) {
        val my = current.calendarMonthYear ?: MonthYear.current()
        val nextMy = if (stepForward) my.next() else my.previous()
        val start = nextMy.toStartTimestamp()
        val end = nextMy.toEndTimestamp()
        return PeriodRange(
            startTimestamp = start,
            endTimestamp = end,
            label = SummaryDateFormatter.formatPeriodLabel(start, end, true),
            isCustom = false,
            calendarMonthYear = nextMy
        )
    }

    val startCal = Calendar.getInstance().apply {
        timeInMillis = current.startTimestamp
        add(Calendar.MONTH, if (stepForward) 1 else -1)
        set(Calendar.DAY_OF_MONTH, startDay)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val nextStartCal = (startCal.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
        set(Calendar.DAY_OF_MONTH, startDay)
    }

    val startMs = startCal.timeInMillis
    val endMs = nextStartCal.timeInMillis - 1L

    return PeriodRange(
        startTimestamp = startMs,
        endTimestamp = endMs,
        label = SummaryDateFormatter.formatPeriodLabel(startMs, endMs, true),
        isCustom = false,
        calendarMonthYear = null
    )
}

