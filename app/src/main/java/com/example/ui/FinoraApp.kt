package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.model.CalculatorCategory
import com.example.model.CalculatorDef
import com.example.ui.components.AdMobBannerSlot
import com.example.ui.components.DrawerDestination
import com.example.ui.components.FinoraDrawerContent
import com.example.ui.components.FinoraTopBar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.CalculatorDetailScreen
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnStockScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsScreen
import com.example.util.AppConfig
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data class Category(val category: CalculatorCategory) : Screen()
    data class Calculator(
        val calculator: CalculatorDef,
        val returnToCategory: CalculatorCategory? = null
    ) : Screen()
    data object LearnStock : Screen()
    data object Settings : Screen()
    data object About : Screen()
    data object PrivacyPolicy : Screen()
}

@Composable
fun FinoraApp(
    onShowSplash: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val screenBackStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = screenBackStack.lastOrNull() ?: Screen.Home

    var useBengaliDigits by remember { mutableStateOf(true) }

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
            DrawerDestination.LEARN_STOCK -> navigateTo(Screen.LearnStock)
            DrawerDestination.SETTINGS -> navigateTo(Screen.Settings)
            DrawerDestination.ABOUT -> navigateTo(Screen.About)
            DrawerDestination.PRIVACY_POLICY -> navigateTo(Screen.PrivacyPolicy)
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
        is Screen.LearnStock -> DrawerDestination.LEARN_STOCK
        is Screen.Settings -> DrawerDestination.SETTINGS
        is Screen.About -> DrawerDestination.ABOUT
        is Screen.PrivacyPolicy -> DrawerDestination.PRIVACY_POLICY
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FinoraDrawerContent(
                currentDestination = activeDrawerDestination,
                onNavigate = { dest -> navigateFromDrawer(dest) },
                onCloseDrawer = { scope.launch { drawerState.close() } }
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
                        fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 150))
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
                                }
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
                                useBengaliDigits = useBengaliDigits,
                                onToggleBengaliDigits = { useBengaliDigits = it }
                            )
                        }
                        is Screen.About -> {
                            AboutScreen(onShowSplash = onShowSplash)
                        }
                        is Screen.PrivacyPolicy -> {
                            PrivacyPolicyScreen()
                        }
                    }
                }
            }
        }
    }
}
