package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object BengaliFormatter {
    val LOCALE_BANGLADESH: Locale = Locale.Builder().setLanguage("bn").setRegion("BD").build()

    private val bengaliMonthNames = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val englishMonthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    /**
     * Formats a timestamp into a standardized date string (e.g. "১২ অক্টোবর, ২০২৬" or "12 October, 2026").
     * Uses explicit Bangladeshi calendar month names to prevent system locale and timezone discrepancies.
     */
    fun formatBangladeshDate(timestamp: Long, useBengaliDigits: Boolean = true): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(java.util.Calendar.MONTH)
        val year = cal.get(java.util.Calendar.YEAR)

        return if (useBengaliDigits) {
            val dayBn = toBengaliDigits(day.toString())
            val monthBn = bengaliMonthNames.getOrElse(monthIdx) { "মাস" }
            val yearBn = toBengaliDigits(year.toString())
            "$dayBn $monthBn, $yearBn"
        } else {
            val monthEn = englishMonthNames.getOrElse(monthIdx) { "Month" }
            "$day $monthEn, $year"
        }
    }

    /**
     * Formats a timestamp into a 12-hour AM/PM time string (e.g. "১০:৩০ AM" or "১০:৩০ পূর্বাহ্ন").
     */
    fun formatBangladeshTime(timestamp: Long, useBengaliDigits: Boolean = true): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour12 = cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(java.util.Calendar.MINUTE)
        val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM

        val minuteStr = minute.toString().padStart(2, '0')
        val timeBase = "$hour12:$minuteStr"

        return if (useBengaliDigits) {
            val amPmBn = if (isAm) "পূর্বাহ্ন" else "অপরাহ্ন"
            "${toBengaliDigits(timeBase)} $amPmBn"
        } else {
            val amPmEn = if (isAm) "AM" else "PM"
            "$timeBase $amPmEn"
        }
    }

    /**
     * Converts an English digit string or number to Bengali digits.
     */
    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(bengaliDigits[ch - '0'])
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Bengali digits to English digits so it can be parsed as Double/Long.
     */
    fun normalizeToEnglishDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            val idx = bengaliDigits.indexOf(ch)
            if (idx != -1) {
                sb.append(englishDigits[idx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    /**
     * Safely parses any number string that may contain Bengali digits, English digits,
     * commas (e.g. 50,000 or ৫০,০০০), spaces, currency symbols (৳), or decimals.
     * Returns null if the input cannot be converted to a valid Double.
     */
    fun parseAmount(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val cleaned = normalizeToEnglishDigits(input)
            .replace("৳", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()
        return cleaned.toDoubleOrNull()
    }

    /**
     * Safely parses an integer quantity from input containing Bengali or English digits,
     * commas, or spaces. Returns null if invalid.
     */
    fun parseInt(input: String?): Int? {
        if (input.isNullOrBlank()) return null
        val cleaned = normalizeToEnglishDigits(input)
            .replace(",", "")
            .replace(" ", "")
            .trim()
        return cleaned.toIntOrNull()
    }

    /**
     * Formats a double value as currency with the Bengali Taka symbol (৳).
     * e.g., ৳ ১,২৫,০৫০.০০ or ৳ 1,25,050.00
     */
    fun formatTaka(value: Double, useBengaliDigits: Boolean = true): String {
        if (value.isNaN() || value.isInfinite()) return "৳ ০.০০"
        val df = DecimalFormat("#,##,##0.00", DecimalFormatSymbols(Locale.US))
        val formatted = df.format(value)
        return if (useBengaliDigits) {
            "৳ ${toBengaliDigits(formatted)}"
        } else {
            "৳ $formatted"
        }
    }

    /**
     * Formats a general decimal number to specified decimal places.
     */
    fun formatNumber(value: Double, decimalPlaces: Int = 2, useBengaliDigits: Boolean = true): String {
        if (value.isNaN() || value.isInfinite()) return if (useBengaliDigits) "০" else "0"
        val pattern = if (decimalPlaces <= 0) "#,##,##0" else "#,##,##0." + "0".repeat(decimalPlaces)
        val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        val formatted = df.format(value)
        return if (useBengaliDigits) toBengaliDigits(formatted) else formatted
    }

    /**
     * Formats percentage value.
     */
    fun formatPercent(value: Double, useBengaliDigits: Boolean = true): String {
        val formatted = formatNumber(value, 2, useBengaliDigits)
        return "$formatted%"
    }

    /**
     * Formats ratio multiplier (e.g., 15.20x).
     */
    fun formatRatio(value: Double, useBengaliDigits: Boolean = true): String {
        val formatted = formatNumber(value, 2, useBengaliDigits)
        return "${formatted}x"
    }

    /**
     * Formats large currency in Lakhs (লাখ) and Crores (কোটি) for clear wealth accumulation displays.
     */
    fun formatCompactTaka(value: Double, useBengaliDigits: Boolean = true): String {
        if (value.isNaN() || value.isInfinite() || value < 0) return formatTaka(value, useBengaliDigits)
        return when {
            value >= 10000000.0 -> {
                val cr = value / 10000000.0
                "৳ ${formatNumber(cr, 2, useBengaliDigits)} কোটি"
            }
            value >= 100000.0 -> {
                val lakh = value / 100000.0
                "৳ ${formatNumber(lakh, 2, useBengaliDigits)} লাখ"
            }
            else -> formatTaka(value, useBengaliDigits)
        }
    }
}
