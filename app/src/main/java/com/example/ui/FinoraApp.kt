package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.model.CalculatorCategory
import com.example.model.CalculatorDef
import com.example.ads.AdManager
import com.example.ui.components.AdMobBannerSlot
import com.example.ui.components.DrawerDestination
import com.example.ui.components.FinoraDrawerContent
import com.example.ui.components.FinoraTopBar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.CalculatorDetailScreen
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.ContactUsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnStockScreen
import com.example.ui.screens.OrderAppScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.goals.SavingsGoalsScreen
import com.example.ui.screens.portfolio.PortfolioScreen
import com.example.util.AppConfig
import com.example.util.NetworkMonitor
import com.example.util.OfflineBlockingOverlay
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data object Portfolio : Screen()
    data object SavingsGoals : Screen()
    data class Category(val category: CalculatorCategory) : Screen()
    data class Calculator(
        val calculator: CalculatorDef,
        val returnToCategory: CalculatorCategory? = null
    ) : Screen()
    data object LearnStock : Screen()
    data object Settings : Screen()
    data object About : Screen()
    data object PrivacyPolicy : Screen()
    data object ContactUs : Screen()
    data object OrderApp : Screen()
}

@Composable
fun FinoraApp(
    onShowSplash: () -> Unit = {},
    userPreferencesRepository: UserPreferencesRepository? = null,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    isDarkTheme: Boolean = false,
    appLanguage: AppLanguage = AppLanguage.BENGALI
) {
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor.getInstance(context) }
    val isOnline by networkMonitor.isOnline.collectAsState()

    // Part C: Full-screen blocking overlay when offline, replacing entire UI
    if (!isOnline) {
        OfflineBlockingOverlay(
            onRetry = { networkMonitor.checkConnectivity() }
        )
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val screenBackStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = screenBackStack.lastOrNull() ?: Screen.Home

    val userPrefs = remember(userPreferencesRepository) {
        userPreferencesRepository ?: UserPreferencesRepository.getInstance(context)
    }
    val currentThemeMode by userPrefs.themeMode.collectAsState(initial = themeMode)
    val useBengaliDigits by userPrefs.useBengaliDigits.collectAsState(initial = true)

    var pendingCalculatorReturnToHome by remember { mutableStateOf(false) }

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            screenBackStack.add(screen)
        }
    }

    fun popBack(): Boolean {
        if (screenBackStack.size > 1) {
            val leavingScreen = screenBackStack.removeAt(screenBackStack.size - 1)
            val targetScreen = screenBackStack.last()

            // Requirement 2: Only trigger interstitial when user navigates BACK from a calculator to Home
            if (leavingScreen is Screen.Calculator && targetScreen is Screen.Home) {
                pendingCalculatorReturnToHome = false
                findActivity(context)?.let { activity ->
                    AdManager.onNavigateBackFromCalculator(activity)
                }
            } else if (leavingScreen is Screen.Calculator && targetScreen is Screen.Category) {
                // Navigated back from calculator to category; if user subsequently pops to Home, trigger
                pendingCalculatorReturnToHome = true
            } else if (leavingScreen is Screen.Category && targetScreen is Screen.Home && pendingCalculatorReturnToHome) {
                pendingCalculatorReturnToHome = false
                findActivity(context)?.let { activity ->
                    AdManager.onNavigateBackFromCalculator(activity)
                }
            } else {
                pendingCalculatorReturnToHome = false
            }
            return true
        }
        return false
    }

    fun navigateFromDrawer(destination: DrawerDestination) {
        when (destination) {
            DrawerDestination.HOME -> {
                val wasOnCalculator = currentScreen is Screen.Calculator
                screenBackStack.clear()
                screenBackStack.add(Screen.Home)
                if (wasOnCalculator) {
                    findActivity(context)?.let { activity ->
                        AdManager.onNavigateBackFromCalculator(activity)
                    }
                }
            }
            DrawerDestination.PORTFOLIO -> navigateTo(Screen.Portfolio)
            DrawerDestination.SAVINGS_GOALS -> navigateTo(Screen.SavingsGoals)
            DrawerDestination.LEARN_STOCK -> navigateTo(Screen.LearnStock)
            DrawerDestination.SETTINGS -> navigateTo(Screen.Settings)
            DrawerDestination.ABOUT -> navigateTo(Screen.About)
            DrawerDestination.PRIVACY_POLICY -> navigateTo(Screen.PrivacyPolicy)
            DrawerDestination.CONTACT_US -> navigateTo(Screen.ContactUs)
            DrawerDestination.ORDER_APP -> navigateTo(Screen.OrderApp)
        }
    }

    // Back handling: drawer first, then backstack
    BackHandler(enabled = drawerState.isOpen || screenBackStack.size > 1) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            popBack()
        }
    }

    // Determine current Drawer destination
    val activeDrawerDestination = when (currentScreen) {
        is Screen.Home, is Screen.Category, is Screen.Calculator -> DrawerDestination.HOME
        is Screen.Portfolio -> DrawerDestination.PORTFOLIO
        is Screen.SavingsGoals -> DrawerDestination.SAVINGS_GOALS
        is Screen.LearnStock -> DrawerDestination.LEARN_STOCK
        is Screen.Settings -> DrawerDestination.SETTINGS
        is Screen.About -> DrawerDestination.ABOUT
        is Screen.PrivacyPolicy -> DrawerDestination.PRIVACY_POLICY
        is Screen.ContactUs -> DrawerDestination.CONTACT_US
        is Screen.OrderApp -> DrawerDestination.ORDER_APP
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FinoraDrawerContent(
                currentDestination = activeDrawerDestination,
                onNavigate = { dest -> navigateFromDrawer(dest) },
                onCloseDrawer = { scope.launch { drawerState.close() } },
                isDarkTheme = isDarkTheme,
                onToggleDarkMode = {
                    scope.launch {
                        userPrefs.toggleDarkMode(!isDarkTheme)
                    }
                }
            )
        }
    ) {
        val title: String
        val subtitle: String?
        val canNavigateBack: Boolean
        val showAdMobBanner: Boolean

        val isEnglish = appLanguage == AppLanguage.ENGLISH

        when (currentScreen) {
            is Screen.Home -> {
                title = AppConfig.APP_NAME
                subtitle = null
                canNavigateBack = false
                showAdMobBanner = true
            }
            is Screen.Category -> {
                title = if (isEnglish) currentScreen.category.titleEn else currentScreen.category.titleBn
                subtitle = if (isEnglish) currentScreen.category.titleBn else currentScreen.category.titleEn
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.Portfolio -> {
                title = stringResource(R.string.title_portfolio)
                subtitle = stringResource(R.string.subtitle_portfolio)
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.SavingsGoals -> {
                title = stringResource(R.string.title_savings_goals)
                subtitle = stringResource(R.string.subtitle_savings_goals)
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.Calculator -> {
                title = if (isEnglish) currentScreen.calculator.titleEn else currentScreen.calculator.titleBn
                subtitle = if (isEnglish) currentScreen.calculator.category.titleEn else currentScreen.calculator.category.titleBn
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.LearnStock -> {
                title = stringResource(R.string.title_learn_stock)
                subtitle = stringResource(R.string.subtitle_learn_stock)
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.Settings -> {
                title = stringResource(R.string.title_settings)
                subtitle = stringResource(R.string.subtitle_settings)
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.About -> {
                title = stringResource(R.string.title_about)
                subtitle = AppConfig.APP_NAME
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.PrivacyPolicy -> {
                title = stringResource(R.string.title_privacy_policy)
                subtitle = stringResource(R.string.subtitle_privacy_policy)
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.ContactUs -> {
                title = stringResource(R.string.title_contact_us)
                subtitle = stringResource(R.string.subtitle_contact_us)
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.OrderApp -> {
                title = stringResource(R.string.title_order_app)
                subtitle = stringResource(R.string.subtitle_order_app)
                canNavigateBack = true
                showAdMobBanner = false
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                FinoraTopBar(
                    title = title,
                    subtitle = subtitle,
                    canNavigateBack = canNavigateBack,
                    onNavigationClick = {
                        if (canNavigateBack) {
                            popBack()
                        } else {
                            scope.launch { drawerState.open() }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    userPrefs.toggleDarkMode(!isDarkTheme)
                                }
                            },
                            modifier = Modifier.testTag("top_bar_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "লাইট মোড" else "ডার্ক মোড",
                                tint = Color.White
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (showAdMobBanner) {
                    AdMobBannerSlot()
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { (it * 0.05f).toInt() })
                            .togetherWith(
                                fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing)) +
                                        slideOutHorizontally(animationSpec = tween(150, easing = FastOutLinearInEasing)) { -(it * 0.05f).toInt() }
                            )
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        is Screen.Home -> {
                            HomeScreen(
                                onCategoryClick = { category ->
                                    navigateTo(Screen.Category(category))
                                },
                                onCalculatorClick = { calc ->
                                    navigateTo(Screen.Calculator(calc))
                                },
                                onPortfolioClick = {
                                    navigateTo(Screen.Portfolio)
                                },
                                useBengaliDigits = useBengaliDigits
                            )
                        }
                        is Screen.Portfolio -> {
                            PortfolioScreen(
                                useBengaliDigits = useBengaliDigits
                            )
                        }
                        is Screen.SavingsGoals -> {
                            SavingsGoalsScreen(
                                useBengaliDigits = useBengaliDigits
                            )
                        }
                        is Screen.Category -> {
                            CategoryDetailScreen(
                                category = screen.category,
                                onCalculatorClick = { calc ->
                                    navigateTo(Screen.Calculator(calc, screen.category))
                                }
                            )
                        }
                        is Screen.Calculator -> {
                            CalculatorDetailScreen(
                                calculator = screen.calculator,
                                useBengaliDigits = useBengaliDigits,
                                appLanguage = appLanguage
                            )
                        }
                        is Screen.LearnStock -> {
                            LearnStockScreen()
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                themeMode = currentThemeMode,
                                isDarkTheme = isDarkTheme,
                                onThemeModeChange = { mode ->
                                    scope.launch { userPrefs.setThemeMode(mode) }
                                },
                                onToggleDarkMode = { enableDark ->
                                    scope.launch { userPrefs.toggleDarkMode(enableDark) }
                                },
                                useBengaliDigits = useBengaliDigits,
                                onToggleBengaliDigits = { enableBengali ->
                                    scope.launch { userPrefs.setUseBengaliDigits(enableBengali) }
                                },
                                appLanguage = appLanguage,
                                onLanguageChange = { newLang ->
                                    scope.launch { userPrefs.setAppLanguage(newLang) }
                                }
                            )
                        }
                        is Screen.About -> {
                            AboutScreen(onShowSplash = onShowSplash)
                        }
                        is Screen.PrivacyPolicy -> {
                            PrivacyPolicyScreen()
                        }
                        is Screen.ContactUs -> {
                            ContactUsScreen()
                        }
                        is Screen.OrderApp -> {
                            OrderAppScreen()
                        }
                    }
                }
            }
        }
    }
}

private fun findActivity(context: Context): Activity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

