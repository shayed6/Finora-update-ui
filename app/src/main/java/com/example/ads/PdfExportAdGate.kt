package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared Rewarded-Ad Gate for PDF Export across all of "হিসাব পাতি".
 *
 * Rules:
 * 1. Tracks [KEY_LAST_PDF_AD_SHOWN_DATE] in SharedPreferences.
 * 2. Compares against today's local calendar date ("yyyy-MM-dd").
 * 3. First PDF export of the day: shows rewarded video ad.
 *    Only after reward is earned in the callback, generates/exports PDF and records today's date.
 * 4. Subsequent exports on the same day: skip ad entirely and export immediately.
 * 5. Fallback: If ad fails to load or fails to show within 5 seconds,
 *    allows PDF export anyway, shows brief message, and does NOT update date so the gate is re-attempted next time.
 * 6. If AdConfig.LEVELPLAY_APP_KEY is "REPLACE_ME" or ads are disabled, silently allows export immediately.
 */
object PdfExportAdGate {
    const val PREFS_NAME = "finora_pdf_ad_gate"
    const val KEY_LAST_PDF_AD_SHOWN_DATE = "lastPdfAdShownDate"
    const val FALLBACK_TOAST_MESSAGE = "বিজ্ঞাপন লোড করা যায়নি, রিপোর্ট ডাউনলোড হচ্ছে"
    const val INCOMPLETE_AD_TOAST_MESSAGE = "বিজ্ঞাপন সম্পূর্ণ না দেখায় PDF তৈরি করা হয়নি"

    fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
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

        // If key is unconfigured, allow export directly
        if (!AdConfig.isKeyConfigured()) {
            onProceedWithExport()
            return
        }

        // 2. First PDF export of the day across হিসাব পাতি
        AdLog.log(
            AdLog.AdFormat.REWARDED,
            AdLog.EventType.IMPRESSION,
            "PDF Export Gate: First export of the day ($today). Presenting rewarded ad."
        )

        AdManager.showRewardedAd(
            activity = activity,
            onUserEarnedReward = {
                setLastPdfAdShownDate(context, today)
                AdLog.log(
                    AdLog.AdFormat.REWARDED,
                    AdLog.EventType.REWARD_EARNED,
                    "PDF Export Gate: Reward earned. Set lastPdfAdShownDate=$today. Exporting PDF."
                )
                onProceedWithExport()
            },
            onDismissedOrFailed = {
                // If ad failed, dismissed without reward, or timed out (5s),
                // do not trap user: proceed with export without recording today's date
                try {
                    Toast.makeText(context, FALLBACK_TOAST_MESSAGE, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
                onProceedWithExport()
            }
        )
    }
}
