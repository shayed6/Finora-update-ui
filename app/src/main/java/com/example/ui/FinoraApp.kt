package com.example.ui

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
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.model.CalculatorCategory
import com.example.model.CalculatorDef
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
    isDarkTheme: Boolean = false
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

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            screenBackStack.add(screen)
        }
    }

    fun popBack(): Boolean {
        if (screenBackStack.size > 1) {
            screenBackStack.removeAt(screenBackStack.size - 1)
            return true
        }
        return false
    }

    fun navigateFromDrawer(destination: DrawerDestination) {
        when (destination) {
            DrawerDestination.HOME -> {
                screenBackStack.clear()
                screenBackStack.add(Screen.Home)
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

        when (currentScreen) {
            is Screen.Home -> {
                title = AppConfig.APP_NAME
                subtitle = null
                canNavigateBack = false
                showAdMobBanner = true
            }
            is Screen.Portfolio -> {
                title = "আমার পোর্টফোলিও"
                subtitle = "DSE ও CSE শেয়ারের অন-ডিভাইস হিসাব"
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.SavingsGoals -> {
                title = "সঞ্চয় লক্ষ্য (Savings Goals)"
                subtitle = "আর্থিক লক্ষ্য ও অগ্রগতির হিসাব"
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.Category -> {
                title = currentScreen.category.titleBn
                subtitle = currentScreen.category.titleEn
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.Calculator -> {
                title = currentScreen.calculator.titleBn
                subtitle = currentScreen.calculator.category.titleBn
                canNavigateBack = true
                // Requirement: "visible across all screens except full-screen calculator input"
                showAdMobBanner = false
            }
            is Screen.LearnStock -> {
                title = "Learn Stock"
                subtitle = "শীর্ষস্থানীয় শিক্ষামূলক প্ল্যাটফর্ম"
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.Settings -> {
                title = "সেটিংস (Settings)"
                subtitle = "পছন্দসমূহ ও কনফিগারেশন"
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.About -> {
                title = "আমাদের সম্পর্কে (About)"
                subtitle = AppConfig.APP_NAME
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.PrivacyPolicy -> {
                title = "গোপনীয়তা নীতি"
                subtitle = "১০০% অন-ডিভাইস হিসাব"
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.ContactUs -> {
                title = "যোগাযোগ করুন"
                subtitle = "Shayed Afride • GZ Holdings LTD"
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.OrderApp -> {
                title = "আপনার অ্যাপ অর্ডার করুন"
                subtitle = "কাস্টম অ্যাপ তৈরি ও ডেভেলপমেন্ট"
                canNavigateBack = true
                showAdMobBanner = true
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
                                useBengaliDigits = useBengaliDigits
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
