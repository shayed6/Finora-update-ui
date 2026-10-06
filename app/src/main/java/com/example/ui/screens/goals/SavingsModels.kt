package com.example.ui.screens.goals

import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.util.BengaliFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object SavingsStrings {
    const val MODULE_TITLE = "সঞ্চয় ও বিনিয়োগ"
    const val MODULE_SUBTITLE = "লক্ষ্যভিত্তিক সঞ্চয় ও বিনিয়োগের হিসাব"

    const val CARD_TOTAL_SAVINGS = "মোট সঞ্চয়/বিনিয়োগ"
    const val HINT_PORTFOLIO = "শেয়ারে বিনিয়োগ Portfolio-তে দেখুন।"

    const val BTN_NEW_GOAL = "+ নতুন লক্ষ্য"
    const val BTN_DEPOSIT = "+ জমা / বিনিয়োগ"
    const val BTN_WITHDRAW = "উত্তোলন"

    const val BADGE_COMPLETED = "লক্ষ্য পূরণ হয়েছে"
    const val BADGE_DEFAULT = "সাধারণ"

    // Detail Screen Cards
    const val CARD_SAVED = "জমা"
    const val CARD_REMAINING = "বাকি"
    const val CARD_TIME_LEFT = "সময় বাকি"
    const val SECTION_SECTOR_BREAKDOWN = "খাতভিত্তিক বিভাজন"
    const val SECTION_TRANSACTIONS = "লেনদেনের ইতিহাস"

    // Dialog: Add/Edit Goal
    const val DIALOG_ADD_GOAL_TITLE = "নতুন সঞ্চয় লক্ষ্য"
    const val DIALOG_EDIT_GOAL_TITLE = "লক্ষ্য সম্পাদনা"
    const val FIELD_GOAL_NAME = "লক্ষ্যের নাম *"
    const val FIELD_GOAL_NAME_HINT = "যেমন: জমি কেনা বা ইমার্জেন্সি ফান্ড"
    const val FIELD_TARGET_AMOUNT = "লক্ষ্যের পরিমাণ (৳) (ঐচ্ছিক)"
    const val FIELD_TARGET_AMOUNT_HINT = "যেমন: ৫,০০,০০০"
    const val FIELD_TARGET_DATE = "মেয়াদ / লক্ষ্য তারিখ (ঐচ্ছিক)"
    const val FIELD_TARGET_DATE_NONE = "তারিখ নির্ধারণ করা হয়নি"

    // Dialog: Add/Edit Entry
    const val DIALOG_DEPOSIT_TITLE = "জমা / বিনিয়োগ যোগ করুন"
    const val DIALOG_WITHDRAW_TITLE = "টাকা উত্তোলন করুন"
    const val DIALOG_EDIT_ENTRY_TITLE = "লেনদেন সম্পাদনা"
    const val FIELD_AMOUNT_LABEL = "টাকার পরিমাণ (৳) *"
    const val FIELD_AMOUNT_HINT = "যেমন: ৫০০০০"
    const val FIELD_SECTOR_LABEL = "বিনিয়োগ / সঞ্চয়ের খাত *"
    const val FIELD_ADD_NEW_SECTOR = "+ নতুন খাত"
    const val FIELD_ENTRY_DATE = "তারিখ"
    const val FIELD_NOTE_LABEL = "নোট / বিবরণ (ঐচ্ছিক)"
    const val FIELD_NOTE_HINT = "অতিরিক্ত বিবরণ লিখুন..."

    const val BTN_SAVE = "সংরক্ষণ করুন"
    const val BTN_CANCEL = "বাতিল"
    const val BTN_DELETE = "মুছে ফেলুন"

    // Confirmations
    const val CONFIRM_DELETE_GOAL_TITLE = "লক্ষ্যটি মুছে ফেলবেন?"
    const val CONFIRM_DELETE_GOAL_MSG = "এই লক্ষ্যটির সমস্ত লেনদেন স্বয়ংক্রিয়ভাবে 'সাধারণ সঞ্চয়'-এ স্থানান্তরিত হবে। মোট জমা অক্ষুণ্ণ থাকবে।"
    const val CONFIRM_DELETE_ENTRY_TITLE = "লেনদেন মুছে ফেলবেন?"
    const val CONFIRM_DELETE_ENTRY_MSG = "এই লেনদেনটি স্থায়ীভাবে মুছে যাবে। আপনি কি নিশ্চিত?"

    // Errors
    const val ERROR_NAME_REQUIRED = "লক্ষ্যের নাম লেখা আবশ্যক"
    const val ERROR_AMOUNT_REQUIRED = "টাকার পরিমাণ লিখুন"
    const val ERROR_AMOUNT_ZERO = "টাকার পরিমাণ শূন্যের বেশি হতে হবে"
    const val ERROR_AMOUNT_MAX = "সর্বোচ্চ ১০ সংখ্যার পরিমাণ লিখুন"
    const val ERROR_FUTURE_DATE = "ভবিষ্যতের তারিখ গ্রহণযোগ্য নয়"
    const val ERROR_WITHDRAW_EXCEED = "উত্তোলনের পরিমাণ বর্তমান জমার চেয়ে বেশি হতে পারে না"
}

data class GoalWithStats(
    val goal: SavingsGoalEntity,
    val savedPaisa: Long,
    val progressPercent: Float?, // null if targetAmount is null
    val isCompleted: Boolean
)

data class SectorTotal(
    val sectorName: String,
    val totalPaisa: Long
)

data class SavingsEntryItem(
    val entry: SavingsEntryEntity,
    val sectorName: String
)

data class GoalDetailState(
    val goal: SavingsGoalEntity,
    val savedPaisa: Long,
    val remainingPaisa: Long?,
    val remainingTimeText: String?,
    val progressPercent: Float?,
    val isCompleted: Boolean,
    val sectorBreakdown: List<SectorTotal>,
    val entries: List<SavingsEntryItem>
)

object SavingsFormatter {
    private val dateFormatter = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())

    fun formatDate(timestamp: Long, useBengaliDigits: Boolean = true): String {
        val formatted = dateFormatter.format(Date(timestamp))
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    /**
     * Formats paisa into Bangladeshi digit grouped taka (e.g. ৳5,00,000, ৳75,000)
     */
    fun formatBangladeshiTaka(paisa: Long, useBengaliDigits: Boolean = true): String {
        val isNegative = paisa < 0
        val absPaisa = kotlin.math.abs(paisa)
        val taka = absPaisa / 100L
        val takaStr = taka.toString()

        val formattedTaka = if (takaStr.length <= 3) {
            takaStr
        } else {
            val last3 = takaStr.takeLast(3)
            val remaining = takaStr.dropLast(3)
            val chunks = mutableListOf<String>()
            var rem = remaining
            while (rem.length > 2) {
                chunks.add(rem.takeLast(2))
                rem = rem.dropLast(2)
            }
            if (rem.isNotEmpty()) {
                chunks.add(rem)
            }
            chunks.reverse()
            chunks.joinToString(",") + "," + last3
        }

        val fullStr = if (absPaisa % 100L != 0L) {
            val fraction = absPaisa % 100L
            "$formattedTaka.${fraction.toString().padStart(2, '0')}"
        } else {
            formattedTaka
        }

        val withSymbol = if (isNegative) "-৳$fullStr" else "৳$fullStr"
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(withSymbol) else withSymbol
    }

    fun parseInputToPaisa(input: String): Long? {
        val cleaned = BengaliFormatter.normalizeToEnglishDigits(input)
            .replace("৳", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()

        if (cleaned.isBlank()) return null

        val parts = cleaned.split(".")
        if (parts[0].length > 10) return null

        val takaDouble = cleaned.toDoubleOrNull() ?: return null
        if (takaDouble <= 0.0) return null

        return kotlin.math.round(takaDouble * 100.0).toLong()
    }

    fun formatRemainingTime(targetDate: Long?, useBengaliDigits: Boolean = true): String? {
        if (targetDate == null) return null
        val now = System.currentTimeMillis()
        val diff = targetDate - now

        if (diff <= 0) {
            return "সময় শেষ"
        }

        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val months = days / 30
        val years = days / 365

        val text = when {
            years > 0 -> {
                val remMonths = (days % 365) / 30
                if (remMonths > 0) "$years বছর $remMonths মাস বাকি" else "$years বছর বাকি"
            }
            months > 0 -> {
                val remDays = days % 30
                if (remDays > 0) "$months মাস $remDays দিন বাকি" else "$months মাস বাকি"
            }
            else -> "$days দিন বাকি"
        }

        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(text) else text
    }
}
