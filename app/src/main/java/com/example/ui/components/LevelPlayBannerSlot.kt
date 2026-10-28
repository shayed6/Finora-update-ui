package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdConfig
import com.example.ads.AdLog
import com.example.ads.AdManager
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Unity LevelPlay Bottom Banner Slot.
 * - Shows across the app in docked bottom position.
 * - Reserves fixed 50dp height to ensure zero layout jumping.
 * - Destroys banner properly on lifecycle changes.
 * - Silently does nothing if AdConfig.LEVELPLAY_APP_KEY is "REPLACE_ME".
 * - Never shows while dialogs/forms or offline overlay are active.
 */
@Composable
fun LevelPlayBannerSlot(
    modifier: Modifier = Modifier
) {
    if (!AdConfig.isKeyConfigured()) {
        // Silently do nothing if key is REPLACE_ME or ADS_ENABLED is false
        return
    }

    val isInitialized by AdManager.isInitialized.collectAsState()
    val isSafetyBlocked by AdManager.isSafetyBlocked.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    if (isSafetyBlocked || activity == null) {
        return
    }

    var bannerLayout by remember { mutableStateOf<IronSourceBannerLayout?>(null) }

    DisposableEffect(activity) {
        onDispose {
            bannerLayout?.let {
                try {
                    IronSource.destroyBanner(it)
                } catch (e: Throwable) {
                    Log.w("LevelPlayBannerSlot", "Error destroying banner: ${e.message}")
                }
            }
            bannerLayout = null
        }
    }

    // Reserved fixed 50dp height so layout never jumps
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            .testTag("levelplay_banner_slot"),
        contentAlignment = Alignment.Center
    ) {
        if (isInitialized) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                factory = { ctx ->
                    val container = FrameLayout(ctx)
                    try {
                        val banner = IronSource.createBanner(activity, ISBannerSize.BANNER)
                        banner.levelPlayBannerListener = object : LevelPlayBannerListener {
                            override fun onAdLoaded(adInfo: AdInfo) {
                                AdLog.log(
                                    AdLog.AdFormat.BANNER,
                                    AdLog.EventType.LOADED,
                                    "LevelPlay banner loaded"
                                )
                            }

                            override fun onAdLoadFailed(error: IronSourceError) {
                                AdLog.log(
                                    AdLog.AdFormat.BANNER,
                                    AdLog.EventType.FAILED_TO_LOAD,
                                    "LevelPlay banner failed [${error.errorCode}]: ${error.errorMessage}"
                                )
                            }

                            override fun onAdClicked(adInfo: AdInfo) {
                                AdLog.log(AdLog.AdFormat.BANNER, AdLog.EventType.CLICK, "LevelPlay banner clicked")
                            }

                            override fun onAdLeftApplication(adInfo: AdInfo) {}

                            override fun onAdScreenPresented(adInfo: AdInfo) {
                                AdLog.log(AdLog.AdFormat.BANNER, AdLog.EventType.OPENED, "LevelPlay banner presented")
                            }

                            override fun onAdScreenDismissed(adInfo: AdInfo) {
                                AdLog.log(AdLog.AdFormat.BANNER, AdLog.EventType.CLOSED, "LevelPlay banner dismissed")
                            }
                        }
                        container.addView(banner)
                        bannerLayout = banner
                        if (AdConfig.BANNER_PLACEMENT.isNotBlank() && AdConfig.BANNER_PLACEMENT != "REPLACE_ME") {
                            IronSource.loadBanner(banner, AdConfig.BANNER_PLACEMENT)
                        } else {
                            IronSource.loadBanner(banner)
                        }
                    } catch (e: Throwable) {
                        Log.e("LevelPlayBannerSlot", "Safe banner creation failed: ${e.message}")
                    }
                    container
                }
            )
        }
    }
}
