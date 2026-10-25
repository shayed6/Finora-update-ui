package com.example.ads

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Shared Rewarded-Ad Gate for PDF Export across all of "হিসাব পাতি"
 * (Bakir Khata, Income-Expense, Savings & Investment, Accounts Summary, and Invoices).
 *
 * Rules:
 * 1. Track [KEY_LAST_PDF_AD_SHOWN_DATE] in SharedPreferences.
 * 2. Compare against today's date (local calendar date "yyyy-MM-dd").
 * 3. First PDF export of the day: show rewarded video ad.
 *    Only after ad is watched to completion (reward earned), generate/export PDF and record today's date.
 * 4. Subsequent exports on the same day: skip ad entirely and export immediately.
 * 5. Fallback: If ad fails to load or fails to show within 4 seconds (no fill, network failure),
 *    allow PDF export without ad, show brief toast "বিজ্ঞাপন লোড করা যায়নি, রিপোর্ট ডাউনলোড হচ্ছে",
 *    and do NOT update date so the gate is re-attempted next time.
 */
object PdfExportAdGate {
    const val PREFS_NAME = "finora_pdf_ad_gate"
    const val KEY_LAST_PDF_AD_SHOWN_DATE = "lastPdfAdShownDate"
    const val AD_LOAD_TIMEOUT_MS = 4000L
    const val FALLBACK_TOAST_MESSAGE = "বিজ্ঞাপন লোড করা যায়নি, রিপোর্ট ডাউনলোড হচ্ছে"
    const val INCOMPLETE_AD_TOAST_MESSAGE = "বিজ্ঞাপন সম্পূর্ণ না দেখায় PDF তৈরি করা হয়নি"

    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getLastPdfAdShownDate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_PDF_AD_SHOWN_DATE, "") ?: ""
    }

    fun setLastPdfAdShownDate(context: Context, dateStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_PDF_AD_SHOWN_DATE, dateStr).apply()
    }

    fun isAdRequiredToday(context: Context): Boolean {
        val today = getTodayDateString()
        val lastDate = getLastPdfAdShownDate(context)
        return lastDate != today
    }

    /**
     * Resets gate for automated testing.
     */
    fun resetForTesting(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LAST_PDF_AD_SHOWN_DATE).apply()
    }

    /**
     * Evaluates gate and either proceeds directly, presents rewarded ad, or triggers fallback.
     */
    fun onPdfExportRequested(
        activity: Activity?,
        onProceedWithExport: () -> Unit
    ) {
        if (activity == null) {
            onProceedWithExport()
            return
        }

        val context = activity.applicationContext
        val today = getTodayDateString()
        val lastShownDate = getLastPdfAdShownDate(context)

        // 1. If already shown today: skip ad entirely and export immediately
        if (lastShownDate == today) {
            AdLog.log(
                AdLog.AdFormat.REWARDED,
                AdLog.EventType.CLICK,
                "PDF Export Gate: Ad already shown today ($today). Proceeding directly to export."
            )
            onProceedWithExport()
            return
        }

        // 2. First PDF export of the day across হিসাব পাতি
        AdLog.log(
            AdLog.AdFormat.REWARDED,
            AdLog.EventType.IMPRESSION,
            "PDF Export Gate: First export of the day ($today). Presenting rewarded ad."
        )

        val hasCompleted = AtomicBoolean(false)
        val userEarnedReward = AtomicBoolean(false)
        val adShown = AtomicBoolean(false)

        fun triggerFallback(reason: String) {
            if (hasCompleted.compareAndSet(false, true)) {
                AdLog.log(
                    AdLog.AdFormat.REWARDED,
                    AdLog.EventType.FAILED_TO_LOAD,
                    "PDF Export Gate Fallback: $reason. Allowing export without updating date."
                )
                try {
                    Toast.makeText(context, FALLBACK_TOAST_MESSAGE, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
                onProceedWithExport()
            }
        }

        fun onRewardEarned() {
            if (hasCompleted.compareAndSet(false, true)) {
                setLastPdfAdShownDate(context, today)
                AdLog.log(
                    AdLog.AdFormat.REWARDED,
                    AdLog.EventType.REWARD_EARNED,
                    "PDF Export Gate: Reward earned. Set lastPdfAdShownDate=$today. Exporting PDF."
                )
                onProceedWithExport()
            }
        }

        // 4-second timeout guard: if ad doesn't load/show within timeout, trigger fallback
        val timeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(AD_LOAD_TIMEOUT_MS)
            if (!adShown.get() && !hasCompleted.get()) {
                triggerFallback("Ad load timeout (${AD_LOAD_TIMEOUT_MS}ms)")
            }
        }

        val preloadedAd = AdManager.getLoadedRewardedAd()
        if (preloadedAd != null) {
            timeoutJob.cancel()
            adShown.set(true)
            preloadedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    AdManager.clearRewardedAd()
                    if (userEarnedReward.get()) {
                        onRewardEarned()
                    } else {
                        AdLog.log(
                            AdLog.AdFormat.REWARDED,
                            AdLog.EventType.CLOSED,
                            "PDF Export Gate: User dismissed ad before earning reward. Export cancelled."
                        )
                        try {
                            Toast.makeText(context, INCOMPLETE_AD_TOAST_MESSAGE, Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {}
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdManager.clearRewardedAd()
                    triggerFallback("Ad failed to show: ${adError.message}")
                }
            }
            preloadedAd.show(activity) {
                userEarnedReward.set(true)
            }
        } else {
            // Load on-demand with fallback on failure
            RewardedAd.load(
                activity,
                AdConfig.REWARDED_AD_UNIT_ID,
                AdManager.buildAdRequest(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(loadedAd: RewardedAd) {
                        timeoutJob.cancel()
                        if (hasCompleted.get()) {
                            // Timeout already fired fallback
                            return
                        }
                        adShown.set(true)
                        loadedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                if (userEarnedReward.get()) {
                                    onRewardEarned()
                                } else {
                                    AdLog.log(
                                        AdLog.AdFormat.REWARDED,
                                        AdLog.EventType.CLOSED,
                                        "PDF Export Gate: User dismissed on-demand ad before earning reward. Export cancelled."
                                    )
                                    try {
                                        Toast.makeText(context, INCOMPLETE_AD_TOAST_MESSAGE, Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {}
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                triggerFallback("On-demand ad failed to show: ${adError.message}")
                            }
                        }
                        loadedAd.show(activity) {
                            userEarnedReward.set(true)
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        timeoutJob.cancel()
                        triggerFallback("On-demand load failed [${loadAdError.code}]: ${loadAdError.message}")
                    }
                }
            )
        }
    }
}
