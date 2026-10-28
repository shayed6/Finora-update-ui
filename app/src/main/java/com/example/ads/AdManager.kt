package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.integration.IntegrationHelper
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unity LevelPlay (ironSource) Mediation Manager for Finora.
 *
 * Implements lightweight, user-first monetization:
 * 1. Bottom Banner: Displayed across app with reserved 50dp height to eliminate layout jumping.
 * 2. Rewarded Video:
 *    a) First PDF export of the day in হিসাব পাতি (resets midnight local time).
 *    b) Portfolio manual refresh: >3 manual refreshes in rolling 5 minutes triggers one rewarded ad.
 * 3. Asynchronous Initialization: Runs off the main startup path post-first-frame.
 * 4. UX Fail-Safe: If rewarded ad fails or is not ready, lets user proceed anyway after 5 seconds.
 * 5. Safety: No ads on half-filled dialogs/forms or offline screens.
 * 6. Silent No-Op: If AdConfig.LEVELPLAY_APP_KEY == "REPLACE_ME", ads do nothing without crashes.
 */
object AdManager {
    private const val TAG = "LevelPlayAdManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Initialization and state flows
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private val _isSafetyBlocked = MutableStateFlow(false)
    val isSafetyBlocked: StateFlow<Boolean> = _isSafetyBlocked.asStateFlow()

    // Safety guards
    private var isInsideTransactionFlow = false
    private var isOfflineState = false

    // Pending callbacks for rewarded ad
    private var pendingRewardCallback: (() -> Unit)? = null
    private var pendingFallbackCallback: (() -> Unit)? = null
    private var timeoutRunnable: Runnable? = null
    private var isAdShowingNow = false

    // Rolling 5-minute manual refresh timestamps for Portfolio
    private val manualRefreshTimestamps = mutableListOf<Long>()
    private const val FIVE_MINUTES_MS = 5 * 60 * 1000L

    /**
     * Initializes LevelPlay SDK asynchronously after the first screen renders.
     */
    fun initialize(activity: Activity) {
        if (!AdConfig.isKeyConfigured()) {
            Log.d(TAG, "AdConfig key is 'REPLACE_ME' or ADS_ENABLED=false. Ads silently disabled.")
            _isInitialized.value = false
            _canRequestAds.value = false
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // COPPA compliance: Child-directed = false
                IronSource.setMetaData("is_child_directed", "false")
                // Consent & Privacy handling
                IronSource.setConsent(true)

                withContext(Dispatchers.Main) {
                    setupRewardedVideoListener()

                    IronSource.init(
                        activity,
                        AdConfig.LEVELPLAY_APP_KEY,
                        {
                            _isInitialized.value = true
                            _canRequestAds.value = true
                            AdLog.log(AdLog.AdFormat.CONSENT, AdLog.EventType.LOADED, "LevelPlay init completed")
                            Log.d(TAG, "Unity LevelPlay initialized successfully.")

                            // Validate adapter integration in test mode
                            if (AdConfig.USE_TEST_MODE) {
                                try {
                                    IntegrationHelper.validateIntegration(activity)
                                } catch (e: Throwable) {
                                    Log.w(TAG, "IntegrationHelper validation note: ${e.message}")
                                }
                            }

                            // Preload rewarded video ad in advance
                            preloadRewardedVideo()
                        },
                        IronSource.AD_UNIT.BANNER,
                        IronSource.AD_UNIT.REWARDED_VIDEO
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Safe LevelPlay init error (non-fatal): ${e.message}")
            }
        }
    }

    /**
     * Preloads Rewarded Video in background.
     */
    fun preloadRewardedVideo() {
        if (!AdConfig.isKeyConfigured() || !_isInitialized.value) return
        try {
            IronSource.loadRewardedVideo()
        } catch (e: Throwable) {
            Log.w(TAG, "Error preloading rewarded video: ${e.message}")
        }
    }

    private fun setupRewardedVideoListener() {
        try {
            IronSource.setLevelPlayRewardedVideoListener(object : LevelPlayRewardedVideoListener {
                override fun onAdAvailable(adInfo: AdInfo) {
                    _isRewardedReady.value = true
                    AdLog.log(AdLog.AdFormat.REWARDED, AdLog.EventType.LOADED, "Rewarded ad available")
                }

                override fun onAdUnavailable() {
                    _isRewardedReady.value = false
                    AdLog.log(AdLog.AdFormat.REWARDED, AdLog.EventType.FAILED_TO_LOAD, "Rewarded ad unavailable")
                }

                override fun onAdOpened(adInfo: AdInfo) {
                    isAdShowingNow = true
                    cancelTimeout()
                    AdLog.log(AdLog.AdFormat.REWARDED, AdLog.EventType.OPENED, "Rewarded ad opened")
                }

                override fun onAdShowFailed(error: IronSourceError, adInfo: AdInfo) {
                    isAdShowingNow = false
                    cancelTimeout()
                    AdLog.log(
                        AdLog.AdFormat.REWARDED,
                        AdLog.EventType.FAILED_TO_LOAD,
                        "Rewarded ad show failed [${error.errorCode}]: ${error.errorMessage}"
                    )
                    val fallback = pendingFallbackCallback
                    clearCallbacks()
                    fallback?.invoke()
                    preloadRewardedVideo()
                }

                override fun onAdClicked(placement: Placement?, adInfo: AdInfo) {
                    AdLog.log(AdLog.AdFormat.REWARDED, AdLog.EventType.CLICK, "Rewarded ad clicked")
                }

                override fun onAdRewarded(placement: Placement?, adInfo: AdInfo) {
                    AdLog.log(AdLog.AdFormat.REWARDED, AdLog.EventType.REWARD_EARNED, "User earned reward")
                    val rewardCb = pendingRewardCallback
                    pendingRewardCallback = null
                    rewardCb?.invoke()
                }

                override fun onAdClosed(adInfo: AdInfo) {
                    isAdShowingNow = false
                    AdLog.log(AdLog.AdFormat.REWARDED, AdLog.EventType.CLOSED, "Rewarded ad closed")
                    val fallback = pendingFallbackCallback
                    clearCallbacks()
                    fallback?.invoke()
                    preloadRewardedVideo()
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting up LevelPlayRewardedVideoListener: ${e.message}")
        }
    }

    /**
     * Triggers a rewarded video ad with a strict 5-second fail-safe timeout.
     * Never traps or blocks the user if an ad fails or is unconfigured.
     */
    fun showRewardedAd(
        activity: Activity?,
        onUserEarnedReward: () -> Unit,
        onDismissedOrFailed: () -> Unit
    ) {
        if (!AdConfig.isKeyConfigured() || activity == null || isInsideTransactionFlow || isOfflineState) {
            // Silently allow user to continue if unconfigured or in safety state
            onUserEarnedReward()
            return
        }

        var isRewardGranted = false
        pendingRewardCallback = {
            isRewardGranted = true
            onUserEarnedReward()
        }
        pendingFallbackCallback = {
            if (!isRewardGranted) {
                onDismissedOrFailed()
            }
        }

        // 5-second fail-safe timeout
        timeoutRunnable = Runnable {
            if (!isAdShowingNow) {
                Log.w(TAG, "Rewarded ad 5-second timeout reached. Releasing user gracefully.")
                AdLog.log(AdLog.AdFormat.REWARDED, AdLog.EventType.FAILED_TO_LOAD, "5s timeout fail-safe triggered")
                val fallback = pendingFallbackCallback
                clearCallbacks()
                fallback?.invoke()
            }
        }
        mainHandler.postDelayed(timeoutRunnable!!, AdConfig.REWARDED_TIMEOUT_MS)

        try {
            val isReady = IronSource.isRewardedVideoAvailable()
            if (isReady) {
                if (AdConfig.REWARDED_PLACEMENT.isNotBlank() && AdConfig.REWARDED_PLACEMENT != "REPLACE_ME") {
                    IronSource.showRewardedVideo(activity, AdConfig.REWARDED_PLACEMENT)
                } else {
                    IronSource.showRewardedVideo(activity)
                }
            } else {
                Toast.makeText(activity, "বিজ্ঞাপন প্রস্তুত হচ্ছে...", Toast.LENGTH_SHORT).show()
                preloadRewardedVideo()
                // Check readiness in a quick poll before 5s timeout
                pollForReadinessAndShow(activity, remainingAttempts = 8)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Safe showRewardedVideo catch: ${e.message}")
            cancelTimeout()
            clearCallbacks()
            onDismissedOrFailed()
        }
    }

    private fun pollForReadinessAndShow(activity: Activity, remainingAttempts: Int) {
        if (remainingAttempts <= 0 || isAdShowingNow || timeoutRunnable == null) return
        mainHandler.postDelayed({
            if (isAdShowingNow || timeoutRunnable == null) return@postDelayed
            try {
                if (IronSource.isRewardedVideoAvailable()) {
                    if (AdConfig.REWARDED_PLACEMENT.isNotBlank() && AdConfig.REWARDED_PLACEMENT != "REPLACE_ME") {
                        IronSource.showRewardedVideo(activity, AdConfig.REWARDED_PLACEMENT)
                    } else {
                        IronSource.showRewardedVideo(activity)
                    }
                } else {
                    pollForReadinessAndShow(activity, remainingAttempts - 1)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Poll show rewarded error: ${e.message}")
            }
        }, 500L)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let {
            mainHandler.removeCallbacks(it)
            timeoutRunnable = null
        }
    }

    private fun clearCallbacks() {
        cancelTimeout()
        pendingRewardCallback = null
        pendingFallbackCallback = null
    }

    /**
     * Portfolio manual refresh ad gate:
     * More than 3 manual refreshes within a rolling 5 minutes triggers one rewarded ad.
     * Failed/missing ad lets the user continue anyway after 5 seconds.
     */
    fun onPortfolioRefreshRequested(activity: Activity?, onProceed: () -> Unit) {
        val now = System.currentTimeMillis()
        val windowStart = now - FIVE_MINUTES_MS

        synchronized(manualRefreshTimestamps) {
            manualRefreshTimestamps.removeAll { it < windowStart }
        }

        val currentCount = synchronized(manualRefreshTimestamps) { manualRefreshTimestamps.size }

        // If user has already refreshed 3 times in past 5 minutes, this 4th refresh triggers rewarded ad
        if (currentCount >= 3) {
            if (!AdConfig.isKeyConfigured() || activity == null) {
                // Silently allow refresh
                onProceed()
                return
            }

            Toast.makeText(
                activity,
                "ঘন ঘন রিফ্রেশ সীমিত রাখতে বিজ্ঞাপন দেখা হচ্ছে...",
                Toast.LENGTH_SHORT
            ).show()

            showRewardedAd(
                activity = activity,
                onUserEarnedReward = {
                    synchronized(manualRefreshTimestamps) {
                        manualRefreshTimestamps.clear()
                    }
                    onProceed()
                },
                onDismissedOrFailed = {
                    onProceed()
                }
            )
        } else {
            synchronized(manualRefreshTimestamps) {
                manualRefreshTimestamps.add(now)
            }
            onProceed()
        }
    }

    /**
     * Backwards compatibility for portfolio refresh callers.
     */
    fun onManualRefreshRequested(activity: Activity?, onProceed: () -> Unit) {
        onPortfolioRefreshRequested(activity, onProceed)
    }

    /**
     * Shared PDF export ad gate caller for হিসাব পাতি.
     */
    fun onPdfExportRequested(activity: Activity?, onProceed: () -> Unit) {
        PdfExportAdGate.onPdfExportRequested(activity, onProceed)
    }

    // Safety guards
    fun setInsideTransactionFlow(inside: Boolean) {
        isInsideTransactionFlow = inside
        updateSafetyBlocked()
    }

    fun setOffline(offline: Boolean) {
        isOfflineState = offline
        updateSafetyBlocked()
    }

    fun isInsideTransactionFlow(): Boolean = isInsideTransactionFlow

    private fun updateSafetyBlocked() {
        _isSafetyBlocked.value = isInsideTransactionFlow || isOfflineState
    }

    // Lifecycle hooks
    fun onResume(activity: Activity) {
        if (AdConfig.isKeyConfigured()) {
            try {
                IronSource.onResume(activity)
            } catch (e: Throwable) {
                Log.w(TAG, "IronSource.onResume error: ${e.message}")
            }
        }
    }

    fun onPause(activity: Activity) {
        if (AdConfig.isKeyConfigured()) {
            try {
                IronSource.onPause(activity)
            } catch (e: Throwable) {
                Log.w(TAG, "IronSource.onPause error: ${e.message}")
            }
        }
    }

    // Dead-code elimination helpers for callers that previously interacted with AdMob
    fun onNavigateBackFromCalculator(activity: Activity?) {
        // No interstitial policy: strictly do nothing
    }

    fun clearRewardedAd() {
        // Handled internally by LevelPlay SDK
    }

    fun getLoadedRewardedAd(): Any? = null
}
