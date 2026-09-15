package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Production-ready modular AdManager for Google AdMob and Google UMP SDK.
 * Implements a light-touch, user-friendly ad system prioritizing UX:
 * 1. Banner: Allowed ONLY on Home and Calculator Category list screens; Never on calculators or portfolio.
 * 2. Interstitial: Max 3 times per session; Only triggers when navigating BACK from calculator to Home;
 *    Minimum 3-minute gap between interstitials; Never in first 90 seconds of session.
 * 3. Rewarded: Optional opt-in perks only (e.g. extra favorite slots); Never forced.
 * 4. UMP Consent: Standard GDPR consent flow with settings review option.
 * 5. Fail-Safe: All failures fail silently without blocking or delaying the UI.
 */
object AdManager {
    private const val TAG = "AdManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Initialization and Consent State
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    // Session and timing guards
    private var sessionStartTimeMs: Long = System.currentTimeMillis()
    private var lastInterstitialShowTimeMs: Long = 0L

    // Interstitial counter (Max 3 per session)
    private val _sessionInterstitialCount = MutableStateFlow(0)
    val sessionInterstitialCount: StateFlow<Int> = _sessionInterstitialCount.asStateFlow()

    // Cached ads
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()
    private var interstitialRetryDelayMs = AdConfig.INITIAL_RETRY_DELAY_MS
    private var interstitialRetryAttempts = 0

    // Rewarded Ad (Optional feature unlocks only - loaded on-demand or when perk dialog opens)
    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false
    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    // Safety guard: Suppress any ad inside portfolio buy/sell transactions
    private var isInsideTransactionFlow = false

    /**
     * Initializes Google UMP SDK and Google Mobile Ads SDK.
     */
    fun initialize(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                _isPrivacyOptionsRequired.value =
                    consentInformation.privacyOptionsRequirementStatus ==
                            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        AdLog.log(
                            AdLog.AdFormat.CONSENT,
                            AdLog.EventType.FAILED_TO_LOAD,
                            "Consent form error [${formError.errorCode}]: ${formError.message}"
                        )
                    } else {
                        AdLog.log(
                            AdLog.AdFormat.CONSENT,
                            AdLog.EventType.CONSENT_UPDATED,
                            "Consent form presented / satisfied"
                        )
                    }

                    checkAndInitMobileAds(activity)
                }
            },
            { requestConsentError ->
                AdLog.log(
                    AdLog.AdFormat.CONSENT,
                    AdLog.EventType.FAILED_TO_LOAD,
                    "Consent info update error [${requestConsentError.errorCode}]: ${requestConsentError.message}"
                )
                // Fallback attempt to initialize ads if allowed
                checkAndInitMobileAds(activity)
            }
        )
    }

    private fun checkAndInitMobileAds(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val canRequest = consentInformation.canRequestAds()
        _canRequestAds.value = canRequest

        _isPrivacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        if (canRequest && !_isInitialized.value) {
            val testConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(testConfig)

            MobileAds.initialize(activity.applicationContext) { initStatus ->
                _isInitialized.value = true
                AdLog.log(
                    AdLog.AdFormat.CONSENT,
                    AdLog.EventType.LOADED,
                    "MobileAds initialized successfully"
                )
                // Preload interstitial silently in background; rewarded ads are purely opt-in
                preloadInterstitial(activity.applicationContext)
            }
        } else if (!canRequest) {
            AdLog.log(
                AdLog.AdFormat.CONSENT,
                AdLog.EventType.CLOSED,
                "Ads cannot be requested yet per consent status"
            )
        }
    }

    /**
     * Shows the Google UMP Privacy Options Form (allows user to review or revoke consent later from Settings)
     */
    fun showPrivacyOptions(activity: Activity, onComplete: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                AdLog.log(
                    AdLog.AdFormat.CONSENT,
                    AdLog.EventType.FAILED_TO_LOAD,
                    "Privacy options error: ${formError.message}"
                )
            } else {
                AdLog.log(
                    AdLog.AdFormat.CONSENT,
                    AdLog.EventType.CONSENT_UPDATED,
                    "Privacy options updated by user"
                )
            }
            checkAndInitMobileAds(activity)
            onComplete()
        }
    }

    /**
     * Builds standard AdRequest.
     */
    fun buildAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    // ==========================================
    // INTERSTITIAL AD MANAGEMENT (LIGHT-TOUCH)
    // ==========================================

    /**
     * Preloads the next interstitial ad silently in the background.
     */
    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading || !_canRequestAds.value) {
            return
        }

        isInterstitialLoading = true
        val adRequest = buildAdRequest()

        InterstitialAd.load(
            context,
            AdConfig.INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    _isInterstitialReady.value = true
                    interstitialRetryDelayMs = AdConfig.INITIAL_RETRY_DELAY_MS // Reset retry delay
                    interstitialRetryAttempts = 0
                    AdLog.log(
                        AdLog.AdFormat.INTERSTITIAL,
                        AdLog.EventType.LOADED,
                        "Interstitial preloaded successfully"
                    )

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            AdLog.log(
                                AdLog.AdFormat.INTERSTITIAL,
                                AdLog.EventType.CLOSED,
                                "Interstitial dismissed by user"
                            )
                            interstitialAd = null
                            _isInterstitialReady.value = false
                            // Preload the next interstitial in background
                            preloadInterstitial(context)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            AdLog.log(
                                AdLog.AdFormat.INTERSTITIAL,
                                AdLog.EventType.FAILED_TO_LOAD,
                                "Interstitial failed to show: ${adError.message}"
                            )
                            interstitialAd = null
                            _isInterstitialReady.value = false
                            preloadInterstitial(context)
                        }

                        override fun onAdShowedFullScreenContent() {
                            AdLog.log(
                                AdLog.AdFormat.INTERSTITIAL,
                                AdLog.EventType.IMPRESSION,
                                "Interstitial showed full screen"
                            )
                            lastInterstitialShowTimeMs = System.currentTimeMillis()
                        }

                        override fun onAdClicked() {
                            AdLog.log(
                                AdLog.AdFormat.INTERSTITIAL,
                                AdLog.EventType.CLICK,
                                "Interstitial ad clicked"
                            )
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    _isInterstitialReady.value = false
                    AdLog.log(
                        AdLog.AdFormat.INTERSTITIAL,
                        AdLog.EventType.FAILED_TO_LOAD,
                        "Interstitial load failed [${loadAdError.code}]: ${loadAdError.message}"
                    )

                    // Retry with exponential backoff silently up to 3 times
                    if (interstitialRetryAttempts < 3) {
                        interstitialRetryAttempts++
                        scope.launch {
                            delay(interstitialRetryDelayMs)
                            interstitialRetryDelayMs = min(
                                interstitialRetryDelayMs * 2,
                                AdConfig.MAX_RETRY_DELAY_MS
                            )
                            preloadInterstitial(context)
                        }
                    }
                }
            }
        )
    }

    /**
     * Requirement 2: Reduced frequency interstitial ad trigger.
     * - Show at most 3 times per session
     * - Only trigger when user navigates BACK from a calculator to Home (never immediately after showing a result)
     * - Minimum 3-minute gap between two interstitials
     * - Never show in first 90 seconds of a session
     * - Preload next interstitial silently in background
     * - If ad not ready or condition not met, fails silently without blocking navigation
     */
    fun onNavigateBackFromCalculator(activity: Activity) {
        if (isInsideTransactionFlow) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Suppressed interstitial: Inside portfolio transaction flow")
            return
        }

        // 1. Session cap: at most 3 times per session
        if (_sessionInterstitialCount.value >= AdConfig.MAX_INTERSTITIALS_PER_SESSION) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Suppressed interstitial: Session cap of 3 reached (${_sessionInterstitialCount.value}/3)")
            return
        }

        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentTime - sessionStartTimeMs
        val timeSinceLastInterstitial = if (lastInterstitialShowTimeMs == 0L) {
            Long.MAX_VALUE
        } else {
            currentTime - lastInterstitialShowTimeMs
        }

        // 2. Never show in first 90 seconds of session
        if (sessionDuration < AdConfig.INITIAL_SESSION_DELAY_MS) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Suppressed interstitial: Session duration ${sessionDuration / 1000}s < 90s")
            return
        }

        // 3. Minimum 3-minute gap (180,000 ms) between two interstitials
        if (timeSinceLastInterstitial < AdConfig.MIN_INTERSTITIAL_INTERVAL_MS) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Suppressed interstitial: Interval ${timeSinceLastInterstitial / 1000}s < 180s (3 min)")
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            _sessionInterstitialCount.value += 1
            ad.show(activity)
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Interstitial ad not preloaded yet; preloading silently in background")
            preloadInterstitial(activity.applicationContext)
        }
    }

    // ==========================================
    // REWARDED AD MANAGEMENT (OPT-IN PERKS ONLY)
    // ==========================================

    /**
     * Preloads the Rewarded Ad silently in background when needed.
     */
    fun preloadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedLoading || !_canRequestAds.value) {
            return
        }

        isRewardedLoading = true
        val adRequest = buildAdRequest()

        RewardedAd.load(
            context,
            AdConfig.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    _isRewardedReady.value = true
                    AdLog.log(
                        AdLog.AdFormat.REWARDED,
                        AdLog.EventType.LOADED,
                        "Rewarded ad preloaded successfully"
                    )
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    _isRewardedReady.value = false
                    AdLog.log(
                        AdLog.AdFormat.REWARDED,
                        AdLog.EventType.FAILED_TO_LOAD,
                        "Rewarded load failed [${loadAdError.code}]: ${loadAdError.message}"
                    )
                }
            }
        )
    }

    /**
     * Shows a Rewarded ad for opt-in perks (e.g. bonus favorite slots).
     * Clear opt-in only, never forced or auto-triggered.
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onDismissedOrFailed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    AdLog.log(
                        AdLog.AdFormat.REWARDED,
                        AdLog.EventType.CLOSED,
                        "Rewarded ad dismissed"
                    )
                    rewardedAd = null
                    _isRewardedReady.value = false
                    onDismissedOrFailed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdLog.log(
                        AdLog.AdFormat.REWARDED,
                        AdLog.EventType.FAILED_TO_LOAD,
                        "Rewarded ad failed to show: ${adError.message}"
                    )
                    rewardedAd = null
                    _isRewardedReady.value = false
                    onDismissedOrFailed()
                }

                override fun onAdShowedFullScreenContent() {
                    AdLog.log(
                        AdLog.AdFormat.REWARDED,
                        AdLog.EventType.IMPRESSION,
                        "Rewarded ad showed"
                    )
                }

                override fun onAdClicked() {
                    AdLog.log(
                        AdLog.AdFormat.REWARDED,
                        AdLog.EventType.CLICK,
                        "Rewarded ad clicked"
                    )
                }
            }
            ad.show(activity) { rewardItem ->
                AdLog.log(
                    AdLog.AdFormat.REWARDED,
                    AdLog.EventType.REWARD_EARNED,
                    "User earned reward: ${rewardItem.type} (${rewardItem.amount})"
                )
                onUserEarnedReward(rewardItem)
            }
        } else {
            // Load on-demand and show when ready
            isRewardedLoading = true
            RewardedAd.load(
                activity,
                AdConfig.REWARDED_AD_UNIT_ID,
                buildAdRequest(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(loadedAd: RewardedAd) {
                        isRewardedLoading = false
                        rewardedAd = loadedAd
                        _isRewardedReady.value = true
                        showRewardedAd(activity, onUserEarnedReward, onDismissedOrFailed)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isRewardedLoading = false
                        rewardedAd = null
                        _isRewardedReady.value = false
                        AdLog.log(
                            AdLog.AdFormat.REWARDED,
                            AdLog.EventType.FAILED_TO_LOAD,
                            "On-demand rewarded load failed: ${loadAdError.message}"
                        )
                        onDismissedOrFailed()
                    }
                }
            )
        }
    }

    // ==========================================
    // TRANSACTION FLOW SAFETY GUARDS
    // ==========================================

    /**
     * Disables any ad display during portfolio buy/sell transactions.
     */
    fun setInsideTransactionFlow(isInside: Boolean) {
        isInsideTransactionFlow = isInside
        if (BuildConfig.DEBUG) {
            if (isInside) {
                Log.d(TAG, "AdManager: Entered transaction flow. Ads locked.")
            } else {
                Log.d(TAG, "AdManager: Exited transaction flow. Ads unlocked.")
            }
        }
    }

    fun isInsideTransactionFlow(): Boolean = isInsideTransactionFlow
}
