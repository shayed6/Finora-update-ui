package com.example.ui.screens.ledger

import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerEntryType
import com.example.data.local.entity.LedgerPartyEntity
import com.example.util.BengaliFormatter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class BalanceType {
    PABO,    // Net > 0 (Green #19C77A) - পাবো
    DEBO,    // Net < 0 (Red #EF5350) - দেবো
    SETTLED  // Net == 0 (Gray) - মিটে গেছে
}

data class PartyWithBalance(
    val party: LedgerPartyEntity,
    val totalGavePaisa: Long,
    val totalReceivedPaisa: Long,
    val netBalancePaisa: Long,
    val entryCount: Int,
    val lastEntryDate: Long?
) {
    val balanceType: BalanceType
        get() = when {
            netBalancePaisa > 0L -> BalanceType.PABO
            netBalancePaisa < 0L -> BalanceType.DEBO
            else -> BalanceType.SETTLED
        }

    val displayAmountPaisa: Long
        get() = abs(netBalancePaisa)
}

data class LedgerSummary(
    val totalPaboPaisa: Long,
    val totalDeboPaisa: Long,
    val totalPartiesCount: Int
)

/**
 * Centralized strings for বাকির খাতা (credit/debit ledger) for easy editing.
 */
object LedgerStrings {
    const val MODULE_TITLE = "বাকির খাতা"
    const val MODULE_SUBTITLE = "ব্যক্তিগত ও ব্যবসায়িক বাকি-বকেয়ার সহজ খাতা"

    const val SUMMARY_PABO = "মোট আমি পাবো"
    const val SUMMARY_DEBO = "মোট আমি দিবো"

    const val SEARCH_PLACEHOLDER = "নাম বা ফোন নম্বর দিয়ে খুঁজুন..."

    const val EMPTY_TITLE = "কোনো হিসাব নেই"
    const val EMPTY_SUBTITLE = "নতুন ব্যক্তি যুক্ত করে বাকির হিসাব শুরু করুন।"
    const val EMPTY_SEARCH_TITLE = "কোনো ব্যক্তি খুঁজে পাওয়া যায়নি"
    const val EMPTY_SEARCH_SUBTITLE = "বানান বা বিকল্প নামে অনুসন্ধান করুন।"

    const val BTN_NEW_PARTY = "নতুন ব্যক্তি"
    const val BTN_GAVE = "দিলাম"
    const val BTN_RECEIVED = "পেলাম"
    const val BTN_SAVE = "সংরক্ষণ করুন"
    const val BTN_CANCEL = "বাতিল"
    const val BTN_DELETE = "মুছে ফেলুন"
    const val BTN_EDIT = "সম্পাদনা"

    const val STATUS_PABO = "আমি পাবো"
    const val STATUS_DEBO = "আমি দিবো"
    const val STATUS_SETTLED = "মিটে গেছে"
    const val NO_PHONE_LABEL = "ফোন নম্বর নেই"

    const val BTN_OPEN_EXISTING_PARTY = "আগের নামে যুক্ত করুন"
    const val BTN_SAME_PERSON = "হ্যাঁ, একই ব্যক্তি"
    const val BTN_DIFFERENT_PERSON = "না, আলাদা ব্যক্তি"

    const val DIALOG_ADD_PARTY_TITLE = "নতুন ব্যক্তি যোগ করুন"
    const val DIALOG_EDIT_PARTY_TITLE = "ব্যক্তির তথ্য সম্পাদনা"
    const val FIELD_NAME_LABEL = "ব্যক্তির নাম *"
    const val FIELD_NAME_HINT = "যেমন: রহিম ইসলাম"
    const val FIELD_PHONE_LABEL = "ফোন নম্বর (ঐচ্ছিক)"
    const val FIELD_PHONE_HINT = "যেমন: 017xxxxxxxx"

    const val DIALOG_ADD_ENTRY_GAVE = "টাকা দিলাম (GAVE)"
    const val DIALOG_ADD_ENTRY_RECEIVED = "টাকা পেলাম (RECEIVED)"
    const val DIALOG_EDIT_ENTRY_TITLE = "লেনদেন সম্পাদনা"

    const val FIELD_AMOUNT_LABEL = "টাকার পরিমাণ (৳) *"
    const val FIELD_AMOUNT_HINT = "যেমন: ৫০০০ বা 5000"
    const val FIELD_NOTE_LABEL = "বিবরণ / নোট (ঐচ্ছিক)"
    const val FIELD_NOTE_HINT = "যেমন: চাল ও ডাল বা নগদ ধার"
    const val FIELD_DATE_LABEL = "তারিখ"

    const val CONFIRM_DELETE_PARTY_TITLE = "ব্যক্তি মুছে ফেলবেন?"
    const val CONFIRM_DELETE_PARTY_MSG = "এই ব্যক্তির নাম এবং সমস্ত লেনদেনের খতিয়ান স্থায়ীভাবে মুছে যাবে। আপনি কি নিশ্চিত?"

    const val CONFIRM_DELETE_ENTRY_TITLE = "লেনদেন মুছে ফেলবেন?"
    const val CONFIRM_DELETE_ENTRY_MSG = "এই এন্ট্রিটি হিসাব থেকে মুছে ফেলা হবে। আপনি কি নিশ্চিত?"

    const val ERROR_NAME_REQUIRED = "নাম লেখা আবশ্যক"
    const val ERROR_AMOUNT_REQUIRED = "সঠিক টাকার পরিমাণ লিখুন"
    const val ERROR_AMOUNT_MAX = "সর্বোচ্চ ৯ সংখ্যার পরিমাণ লিখুন"
    const val ERROR_AMOUNT_ZERO = "টাকার পরিমাণ শূন্যের বেশি হতে হবে"
}

object LedgerFormatter {
    private val dateFormatter = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatDate(timestamp: Long, useBengaliDigits: Boolean = true): String {
        val formatted = dateFormatter.format(Date(timestamp))
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun formatTime(timestamp: Long, useBengaliDigits: Boolean = true): String {
        val formatted = timeFormatter.format(Date(timestamp))
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun formatPaisa(paisa: Long, useBengaliDigits: Boolean = true): String {
        val taka = paisa / 100.0
        val df = DecimalFormat("#,##,##0", DecimalFormatSymbols(Locale.US))
        val formatted = df.format(taka)
        val withTaka = "৳ $formatted"
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(withTaka) else withTaka
    }

    /**
     * Parses an amount input string in Bengali or English digits into integer paisa.
     * Returns null if empty, <= 0, or exceeds 9 digits.
     */
    fun parseInputToPaisa(input: String): Long? {
        val cleaned = BengaliFormatter.normalizeToEnglishDigits(input)
            .replace("৳", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()

        if (cleaned.isBlank()) return null

        // Check digit length constraint (max 9 digits before decimal)
        val parts = cleaned.split(".")
        if (parts[0].length > 9) return null

        val takaDouble = cleaned.toDoubleOrNull() ?: return null
        if (takaDouble <= 0.0) return null

        return kotlin.math.round(takaDouble * 100.0).toLong()
    }

    /**
     * Extracts circular avatar initials (1 to 2 characters).
     */
    fun extractInitials(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "?"
        val words = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return when {
            words.size >= 2 -> "${words[0].take(1)}${words[1].take(1)}"
            words.isNotEmpty() -> words[0].take(2)
            else -> "?"
        }
    }
}

object LedgerNormalizer {
    /**
     * Normalizes a name: trim spaces, collapse multiple whitespace characters into single space, case-insensitive.
     */
    fun normalizeName(name: String): String {
        return name.trim().replace("\\s+".toRegex(), " ").lowercase()
    }

    /**
     * Normalizes a phone:
     * - Convert Bengali digits to English digits
     * - Remove spaces, dashes, plus signs, brackets, etc.
     * - Remove "+88" or "88" prefix if followed by phone digits
     * - Compare the last 11 digits (e.g. 01XXXXXXXXX)
     */
    fun normalizePhone(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val enDigits = BengaliFormatter.normalizeToEnglishDigits(phone)
        val digitsOnly = enDigits.filter { it.isDigit() }
        if (digitsOnly.isBlank()) return null
        val stripped = when {
            digitsOnly.startsWith("880") -> digitsOnly.removePrefix("88")
            digitsOnly.startsWith("88") && digitsOnly.length > 11 -> digitsOnly.removePrefix("88")
            else -> digitsOnly
        }
        return if (stripped.length >= 11) {
            stripped.takeLast(11)
        } else {
            stripped
        }
    }
}
