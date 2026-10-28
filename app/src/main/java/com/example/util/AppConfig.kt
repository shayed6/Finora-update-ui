package com.example.util

import com.example.ads.AdConfig

object AppConfig {
    /**
     * Unity LevelPlay Placement IDs & App Key
     * Centralized in com.example.ads.AdConfig
     */
    val LEVELPLAY_APP_KEY: String
        get() = AdConfig.LEVELPLAY_APP_KEY

    val BANNER_PLACEMENT: String
        get() = AdConfig.BANNER_PLACEMENT

    val REWARDED_PLACEMENT: String
        get() = AdConfig.REWARDED_PLACEMENT

    /**
     * Learning Platform External Links
     */
    const val URL_INVESTALOY = "https://investaloy.com"
    const val URL_INVESTMENT_MENTOR_KABIR = "https://www.youtube.com/@Investment-mentor-Kabir"
    const val URL_FINANCE_WITH_RAFYQ = "https://www.youtube.com/@FinancewithRafyq"

    // Backward compatibility aliases
    const val URL_LEARN_WITH_KABIR = URL_INVESTMENT_MENTOR_KABIR
    const val URL_LEARN_WITH_RAFIQ = URL_FINANCE_WITH_RAFYQ

    /**
     * Google Play Store Package ID for "Rate Us" action
     */
    const val PLAY_STORE_PACKAGE_ID = "com.aistudio.finora.bdcalc"
    const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE_ID"

    /**
     * App Branding Constants
     */
    const val APP_NAME = "Finora"
    const val TAGLINE_EN = "Your Money. Your Growth."
    const val TAGLINE_BN = "বিনিয়োগ হোক আরও সহজ"
    const val APP_VERSION = "১.০.০ (v1.0.0)"
}
