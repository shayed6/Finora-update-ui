package com.example.ads

/**
 * Central configuration for Google AdMob advertising units and settings.
 * Designed for light-touch, user-friendly ad integration:
 * - Low ad density, prioritized user experience
 * - Banner restricted to Home and Category list screens
 * - Max 3 interstitials per session, min 3-min gap, min 90s session delay, back-navigation only
 * - Optional opt-in rewarded ads for feature perks
 */
object AdConfig {
    /**
     * Test AdMob App ID
     */
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /**
     * Adaptive Banner Ad Unit ID (Test)
     */
    var BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    /**
     * Interstitial Ad Unit ID (Test)
     */
    var INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /**
     * Rewarded Ad Unit ID (Test)
     * Standard Google AdMob test rewarded video unit ID
     */
    var REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Interstitial Frequency Settings (Lightweight & User-Friendly):
     * - Show at most 3 times per session
     * - Only trigger when user navigates BACK from a calculator to Home (never after showing a result)
     * - Minimum 3-minute gap (180s) between two interstitials
     * - Never show in first 90 seconds of a session
     */
    const val MAX_INTERSTITIALS_PER_SESSION = 3
    const val INITIAL_SESSION_DELAY_MS = 90_000L // 90 seconds
    const val MIN_INTERSTITIAL_INTERVAL_MS = 180_000L // 3 minutes

    /**
     * Exponential Backoff settings for silent retries
     */
    const val INITIAL_RETRY_DELAY_MS = 5_000L
    const val MAX_RETRY_DELAY_MS = 60_000L
}
