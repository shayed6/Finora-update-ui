package com.example.ads

/**
 * Central configuration for Unity LevelPlay (ironSource) Mediation SDK.
 * Holds placement IDs, test mode flags, and global enable switches.
 */
object AdConfig {
    /**
     * LevelPlay (ironSource) Application Key.
     * Replace with your actual App Key from the Unity LevelPlay dashboard.
     * If set to "REPLACE_ME", ads silently do nothing (no crash, no blocking).
     */
    const val LEVELPLAY_APP_KEY = "REPLACE_ME"

    /**
     * LevelPlay Banner Placement Name.
     */
    const val BANNER_PLACEMENT = "REPLACE_ME"

    /**
     * LevelPlay Rewarded Video Placement Name.
     */
    const val REWARDED_PLACEMENT = "REPLACE_ME"

    /**
     * Master switch to enable or disable ads globally.
     */
    const val ADS_ENABLED = true

    /**
     * Toggle LevelPlay test mode / integration helper.
     * Can be switched off in ONE place.
     */
    const val USE_TEST_MODE = true

    /**
     * Timeout for rewarded ad readiness (5 seconds fail-safe).
     */
    const val REWARDED_TIMEOUT_MS = 5000L

    /**
     * Returns true only when ads are enabled and a non-placeholder app key is provided.
     */
    fun isKeyConfigured(): Boolean {
        return ADS_ENABLED &&
            LEVELPLAY_APP_KEY.isNotBlank() &&
            LEVELPLAY_APP_KEY != "REPLACE_ME"
    }
}
