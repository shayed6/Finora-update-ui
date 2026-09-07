package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.R

// =========================================================================
// Finora Typography System
// Primary: Inter (Latin, numbers) — weights 400, 500, 600, 700, 800
// Bengali: Noto Sans Bengali — weights 400, 500, 600
// Numeric results use tabular figures (FontFeatureSettings("tnum")) for alignment
// Type scale: Headline 20-22sp, Section title 15-16sp, Body 14sp, Label 11-12sp
// =========================================================================

/**
 * Inter font family — used for all English labels, numbers, and UI chrome.
 * Weights: Regular (400), Medium (500), SemiBold (600), Bold (700), ExtraBold (800)
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
    Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(R.font.inter_bold, weight = FontWeight.Bold),
    Font(R.font.inter_extrabold, weight = FontWeight.ExtraBold)
)

/**
 * Noto Sans Bengali — used as the app-wide font family (Android falls back to
 * its own Bengali shaping engine for conjuncts when not explicitly assigned,
 * but Noto Sans Bengali is the correct Unicode-complete face for Bangla text).
 *
 * Since Compose uses the system font shaper for Bengali script regardless of
 * FontFamily, we set NotoSansBengaliFontFamily as the default fontFamily here
 * so it is picked up wherever Bengali glyphs appear. Inter handles all
 * Latin/ASCII characters within the same text run.
 */
val NotoSansBengaliFontFamily = FontFamily(
    Font(R.font.noto_sans_bengali_regular, weight = FontWeight.Normal),
    Font(R.font.noto_sans_bengali_medium, weight = FontWeight.Medium),
    Font(R.font.noto_sans_bengali_semibold, weight = FontWeight.SemiBold)
)

val Typography = Typography(
    // ---- Headline styles (20-22sp) ----
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),

    // ---- Title / Section styles (15-18sp) ----
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ---- Body styles (14sp) ----
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.4.sp
    ),

    // ---- Label / Caption styles (11-13sp) ----
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Tabular-figures TextStyle modifier for result values.
 * Apply as a base or merge with existing styles for numeric result displays
 * to ensure monospaced digit width for clean column alignment.
 *
 * Usage: Text(text = value, style = Typography.headlineMedium.copy(fontFeatureSettings = "tnum"))
 */
val TabularFigureStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontFeatureSettings = "tnum"
)
