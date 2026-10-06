package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.data.preferences.AppLanguage
import com.example.ui.LocalAppLanguage
import java.util.Locale
import com.example.ads.AdManager
import com.example.data.FavoritesManager
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.ui.FinoraApp
import com.example.ui.Screen
import com.example.ui.screens.FinoraSplashScreen
import com.example.ui.theme.FinoraTheme
import com.example.widget.PortfolioWidgetProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.scraper.LivePriceScraperWorker

class MainActivity : ComponentActivity() {
    private var navigateToPortfolioEvent by mutableStateOf(0L)
    private var startDirectlyOnPortfolio by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val jsCache = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            val wasmCache = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!jsCache.exists()) jsCache.mkdirs()
            if (!wasmCache.exists()) wasmCache.mkdirs()
        } catch (_: Exception) {
        }
        FavoritesManager.init(this)
        AdManager.initialize(this)
        handleWidgetIntent(intent, isInitial = true)
        enableEdgeToEdge()
        setContent {
            val userPrefs = remember { UserPreferencesRepository.getInstance(applicationContext) }
            val themeMode by userPrefs.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val appLanguage by userPrefs.appLanguage.collectAsState(initial = AppLanguage.BENGALI)

            val currentLocale = remember(appLanguage) {
                if (appLanguage == AppLanguage.ENGLISH) Locale.ENGLISH else Locale.Builder().setLanguage("bn").setRegion("BD").build()
            }
            Locale.setDefault(currentLocale)

            val currentConfig = LocalConfiguration.current
            val localizedConfig = remember(appLanguage, currentConfig) {
                Configuration(currentConfig).apply {
                    setLocale(currentLocale)
                    setLayoutDirection(currentLocale)
                }
            }
            val baseContext = LocalContext.current
            val localizedContext = remember(appLanguage, baseContext) {
                val config = Configuration(baseContext.resources.configuration).apply {
                    setLocale(currentLocale)
                    setLayoutDirection(currentLocale)
                }
                baseContext.createConfigurationContext(config)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig,
                LocalContext provides localizedContext,
                LocalAppLanguage provides appLanguage
            ) {
                val isSystemDark = isSystemInDarkTheme()
                val isDarkTheme = when (themeMode) {
                    AppThemeMode.SYSTEM -> isSystemDark
                    AppThemeMode.DARK -> true
                    AppThemeMode.LIGHT -> false
                }

                FinoraTheme(darkTheme = isDarkTheme) {
                    var showSplash by rememberSaveable { mutableStateOf(!startDirectlyOnPortfolio) }

                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 400),
                        label = "FinoraSplashTransition"
                    ) { isSplash ->
                        if (isSplash) {
                            FinoraSplashScreen(onFinish = { showSplash = false })
                        } else {
                            FinoraApp(
                                onShowSplash = { showSplash = true },
                                userPreferencesRepository = userPrefs,
                                themeMode = themeMode,
                                isDarkTheme = isDarkTheme,
                                appLanguage = appLanguage,
                                initialScreen = if (startDirectlyOnPortfolio) Screen.Portfolio else Screen.Home,
                                openPortfolioTimestamp = navigateToPortfolioEvent
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent, isInitial = false)
    }

    private fun handleWidgetIntent(intent: android.content.Intent?, isInitial: Boolean) {
        if (intent == null) return
        val isWidgetOpen = intent.action == PortfolioWidgetProvider.ACTION_OPEN_PORTFOLIO ||
                intent.getStringExtra(PortfolioWidgetProvider.EXTRA_NAVIGATE_TO) == PortfolioWidgetProvider.DESTINATION_PORTFOLIO

        if (isWidgetOpen) {
            if (isInitial) {
                startDirectlyOnPortfolio = true
            }
            navigateToPortfolioEvent = System.currentTimeMillis()

            // Requirement C: Trigger fresh scrape on widget tap
            try {
                val workRequest = OneTimeWorkRequestBuilder<LivePriceScraperWorker>()
                    .setInputData(workDataOf(LivePriceScraperWorker.KEY_FORCE_REFRESH to true))
                    .build()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    LivePriceScraperWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } catch (e: Exception) {
                // WorkManager enqueue silent fallback
            }
        }
    }
}



