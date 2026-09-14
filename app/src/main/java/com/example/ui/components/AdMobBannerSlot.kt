package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdConfig
import com.example.ads.AdLog
import com.example.ads.AdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Production-ready Docked AdMob Adaptive Banner Slot.
 * - Sized adaptively to fit current orientation and device width
 * - Graceful fallback: collapses if ad fails to load or consent not granted
 * - Retries with exponential backoff on network/ad failure
 * - Logs impressions, clicks, loads to AdLog (no PII)
 * - Safe: Suppressed if inside portfolio transaction flow
 */
@Composable
fun AdMobBannerSlot(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val canRequestAds by AdManager.canRequestAds.collectAsState()
    val isInsideTransactionFlow = AdManager.isInsideTransactionFlow()

    // If inside portfolio buy/sell transactions, do not show any ads
    if (isInsideTransactionFlow) {
        return
    }

    var isAdLoaded by remember { mutableStateOf(false) }
    var retryDelayMs by remember { mutableStateOf(AdConfig.INITIAL_RETRY_DELAY_MS) }
    var adViewInstance by remember { mutableStateOf<AdView?>(null) }

    // Adaptive banner ad size calculation
    val screenWidthDp = configuration.screenWidthDp
    val adaptiveAdSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }

    DisposableEffect(Unit) {
        onDispose {
            adViewInstance?.destroy()
            adViewInstance = null
        }
    }

    // Auto-reload when consent changes or ad request becomes allowed
    LaunchedEffect(canRequestAds) {
        if (canRequestAds && !isAdLoaded && adViewInstance != null) {
            adViewInstance?.loadAd(AdManager.buildAdRequest())
        }
    }

    // Outer container: fixed docked bottom bar with navigation insets
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            .padding(vertical = 4.dp)
            .testTag("admob_banner_slot"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    adUnitId = AdConfig.BANNER_AD_UNIT_ID
                    setAdSize(adaptiveAdSize)
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            retryDelayMs = AdConfig.INITIAL_RETRY_DELAY_MS
                            AdLog.log(
                                AdLog.AdFormat.BANNER,
                                AdLog.EventType.LOADED,
                                "Adaptive banner loaded (${adaptiveAdSize.width}x${adaptiveAdSize.height})"
                            )
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            AdLog.log(
                                AdLog.AdFormat.BANNER,
                                AdLog.EventType.FAILED_TO_LOAD,
                                "Adaptive banner failed [${error.code}]: ${error.message}"
                            )

                            // Exponential backoff retry
                            coroutineScope.launch {
                                delay(retryDelayMs)
                                retryDelayMs = min(retryDelayMs * 2, AdConfig.MAX_RETRY_DELAY_MS)
                                if (canRequestAds) {
                                    loadAd(AdManager.buildAdRequest())
                                }
                            }
                        }

                        override fun onAdImpression() {
                            AdLog.log(
                                AdLog.AdFormat.BANNER,
                                AdLog.EventType.IMPRESSION,
                                "Banner impression recorded"
                            )
                        }

                        override fun onAdClicked() {
                            AdLog.log(
                                AdLog.AdFormat.BANNER,
                                AdLog.EventType.CLICK,
                                "Banner ad clicked"
                            )
                        }
                    }

                    adViewInstance = this
                    if (canRequestAds) {
                        loadAd(AdManager.buildAdRequest())
                    }
                }
            },
            update = { adView ->
                // Ensure adView matches current configuration
                if (canRequestAds && !isAdLoaded) {
                    adView.loadAd(AdManager.buildAdRequest())
                }
            }
        )
    }
}
