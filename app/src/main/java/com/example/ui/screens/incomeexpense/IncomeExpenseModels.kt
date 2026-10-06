package com.example.ui.screens.incomeexpense

import com.example.data.local.entity.IncomeExpenseTransactionEntity
import com.example.util.BengaliFormatter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object IncomeExpenseStrings {
    const val MODULE_TITLE = "আয়-ব্যয়"
    const val MODULE_SUBTITLE = "দৈনিক ও মাসিক আয়-ব্যয়ের সহজ খাতা"

    const val TAB_INCOME = "আয়"
    const val TAB_EXPENSE = "ব্যয়"

    const val SUMMARY_INCOME = "মোট আয়"
    const val SUMMARY_EXPENSE = "মোট ব্যয়"
    const val SUMMARY_BALANCE = "অবশিষ্ট"

    const val BTN_ADD_INCOME = "+ আয়"
    const val BTN_ADD_EXPENSE = "+ ব্যয়"
    const val BTN_MANAGE = "ব্যবস্থাপনা"

    const val FILTER_ALL = "সব"
    const val FILTER_ALL_SOURCES = "সব আয়ের উৎস"
    const val FILTER_ALL_CATEGORIES = "সব ব্যয়ের খাত"

    const val EMPTY_TITLE = "কোনো লেনদেন নেই"
    const val EMPTY_SUBTITLE = "নিচের বোতামে ট্যাপ করে নতুন আয় বা ব্যয় যোগ করুন।"

    // Add/Edit Income
    const val DIALOG_ADD_INCOME_TITLE = "নতুন আয় যোগ করুন"
    const val DIALOG_EDIT_INCOME_TITLE = "আয় সম্পাদনা"
    const val FIELD_SOURCE_LABEL = "আয়ের উৎস *"
    const val FIELD_ADD_NEW_SOURCE = "+ নতুন ধরন"

    // Add/Edit Expense
    const val DIALOG_ADD_EXPENSE_TITLE = "নতুন ব্যয় যোগ করুন"
    const val DIALOG_EDIT_EXPENSE_TITLE = "ব্যয় সম্পাদনা"
    const val FIELD_CATEGORY_LABEL = "ব্যয়ের খাত *"
    const val FIELD_ADD_NEW_CATEGORY = "+ নতুন খাত"
    const val FIELD_LINKED_SOURCE_LABEL = "কোন আয় থেকে? (ঐচ্ছিক)"
    const val LINKED_SOURCE_NONE = "নির্দিষ্ট নয় (None)"

    // Shared Fields
    const val FIELD_AMOUNT_LABEL = "টাকার পরিমাণ (৳) *"
    const val FIELD_AMOUNT_HINT = "যেমন: ৩০০০ বা 3000"
    const val FIELD_TAGLINE_LABEL = "ট্যাগ লাইন (ঐচ্ছিক, সর্বোচ্চ ৬০ অক্ষর)"
    const val FIELD_TAGLINE_HINT = "যেমন: সেপ্টেম্বরের বেতন বা বাজার সদাই"
    const val FIELD_DATE_TIME_LABEL = "তারিখ ও সময়"
    const val FIELD_NOTE_LABEL = "নোট / বিবরণ (ঐচ্ছিক)"
    const val FIELD_NOTE_HINT = "অতিরিক্ত তথ্য লিখুন..."

    const val BTN_SAVE = "সংরক্ষণ করুন"
    const val BTN_CANCEL = "বাতিল"
    const val BTN_DELETE = "মুছে ফেলুন"

    // Confirmations
    const val CONFIRM_DELETE_TX_TITLE = "লেনদেন মুছে ফেলবেন?"
    const val CONFIRM_DELETE_TX_MSG = "এই লেনদেনটি হিসাব থেকে স্থায়ীভাবে মুছে যাবে। আপনি কি নিশ্চিত?"

    // Management
    const val MANAGE_TITLE = "খাত ও উৎস ব্যবস্থাপনা"
    const val MANAGE_TAB_INCOME = "আয়ের উৎস"
    const val MANAGE_TAB_EXPENSE = "ব্যয়ের খাত"
    const val DIALOG_ADD_SOURCE = "নতুন আয়ের উৎস যোগ করুন"
    const val DIALOG_RENAME_SOURCE = "আয়ের উৎস পুনর্নামকরণ"
    const val DIALOG_ADD_CATEGORY = "নতুন ব্যয়ের খাত যোগ করুন"
    const val DIALOG_RENAME_CATEGORY = "ব্যয়ের খাত পুনর্নামকরণ"
    const val FIELD_NAME = "নাম *"
    const val CONFIRM_DELETE_TITLE = "মুছে ফেলতে চান?"

    // Errors
    const val ERROR_AMOUNT_REQUIRED = "টাকার পরিমাণ লিখুন"
    const val ERROR_AMOUNT_ZERO = "টাকার পরিমাণ শূন্যের বেশি হতে হবে"
    const val ERROR_AMOUNT_MAX = "সর্বোচ্চ ৯ সংখ্যার পরিমাণ লিখুন"
    const val ERROR_FUTURE_DATE = "ভবিষ্যতের তারিখ গ্রহণযোগ্য নয়"
    const val ERROR_NAME_REQUIRED = "নাম লেখা আবশ্যক"
}

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

    companion object {
        fun current(): MonthYear {
            val cal = Calendar.getInstance()
            return MonthYear(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH)
            )
        }
    }
}

data class TransactionDisplayItem(
    val transaction: IncomeExpenseTransactionEntity,
    val categoryOrSourceName: String,
    val linkedSourceName: String?
)

data class IncomeExpenseSummary(
    val totalIncomePaisa: Long,
    val totalExpensePaisa: Long,
    val balancePaisa: Long
)

object IncomeExpenseFormatter {
    private val dateFormatter = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val monthYearFormatterBn = SimpleDateFormat("MMMM, yyyy", Locale("bn", "BD"))
    private val monthYearFormatterEn = SimpleDateFormat("MMMM yyyy", Locale.US)

    fun formatDate(timestamp: Long, useBengaliDigits: Boolean = true): String {
        val formatted = dateFormatter.format(Date(timestamp))
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun formatTime(timestamp: Long, useBengaliDigits: Boolean = true): String {
        val formatted = timeFormatter.format(Date(timestamp))
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun formatMonthYear(monthYear: MonthYear, useBengaliDigits: Boolean = true): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, monthYear.year)
            set(Calendar.MONTH, monthYear.month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val date = cal.time
        return if (useBengaliDigits) {
            val monthNamesBn = listOf(
                "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
                "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
            )
            val monthName = monthNamesBn.getOrElse(monthYear.month) { "মাস" }
            val yearBn = BengaliFormatter.toBengaliDigits(monthYear.year.toString())
            "$monthName, $yearBn"
        } else {
            monthYearFormatterEn.format(date)
        }
    }

    fun formatPaisa(paisa: Long, useBengaliDigits: Boolean = true): String {
        val taka = paisa / 100.0
        val pattern = if (paisa % 100L == 0L) "#,##,##0" else "#,##,##0.00"
        val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        val formatted = df.format(taka)
        val withTaka = "৳ $formatted"
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(withTaka) else withTaka
    }

    fun parseInputToPaisa(input: String): Long? {
        val cleaned = BengaliFormatter.normalizeToEnglishDigits(input)
            .replace("৳", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()

        if (cleaned.isBlank()) return null

        val parts = cleaned.split(".")
        if (parts[0].length > 9) return null

        val takaDouble = cleaned.toDoubleOrNull() ?: return null
        if (takaDouble <= 0.0) return null

        return kotlin.math.round(takaDouble * 100.0).toLong()
    }
}
