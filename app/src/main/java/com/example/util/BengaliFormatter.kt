package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object BengaliFormatter {
    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

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
