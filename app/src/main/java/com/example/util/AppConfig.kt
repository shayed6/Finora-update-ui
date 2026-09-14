package com.example.util

import com.example.ads.AdConfig

object AppConfig {
    /**
     * AdMob Ad Unit IDs
     * Centralized in com.example.ads.AdConfig
     */
    val ADMOB_BANNER_AD_UNIT_ID: String
        get() = AdConfig.BANNER_AD_UNIT_ID
    val ADMOB_INTERSTITIAL_AD_UNIT_ID: String
        get() = AdConfig.INTERSTITIAL_AD_UNIT_ID
    val ADMOB_REWARDED_AD_UNIT_ID: String
        get() = AdConfig.REWARDED_AD_UNIT_ID

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
     * TODO: When published, ensure this matches your actual Play Store package name.
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
