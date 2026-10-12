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
import com.example.ui.screens.FCoinScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnStockScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.OrderAppScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.goals.SavingsGoalDetailScreen
import com.example.ui.screens.goals.SavingsGoalsViewModel
import com.example.ui.screens.goals.SavingsInvestmentScreen
import com.example.ui.screens.incomeexpense.IncomeExpenseScreen
import com.example.ui.screens.incomeexpense.IncomeExpenseViewModel
import com.example.ui.screens.ledger.LedgerListScreen
import com.example.ui.screens.ledger.LedgerPersonDetailScreen
import com.example.ui.screens.ledger.LedgerViewModel
import com.example.ui.screens.portfolio.PortfolioScreen
import com.example.ui.screens.summary.AccountsSummaryScreen
import com.example.ui.screens.summary.AccountsSummaryViewModel
import com.example.ui.screens.summary.CycleSettingsScreen
import android.app.Application
import com.example.ui.screens.CalculatorsHubScreen
import com.example.ui.screens.HisabPatiHubScreen
import com.example.ui.screens.OrganizationInfoScreen
import com.example.ui.screens.WorkTypeScreen
import com.example.util.AppConfig
import com.example.util.NetworkMonitor
import com.example.util.OfflineBlockingOverlay
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data object CalculatorsHub : Screen()
    data object HisabPati : Screen()
    data object WorkType : Screen()
    data object OrganizationInfo : Screen()
    data object Portfolio : Screen()
    data object LedgerList : Screen()
    data class LedgerPersonDetail(val partyId: Long) : Screen()
    data object IncomeExpense : Screen()
    data object SavingsGoals : Screen()
    data class SavingsGoalDetail(val goalId: Long) : Screen()
    data object AccountsSummary : Screen()
    data object CycleSettings : Screen()
    data object FCoin : Screen()
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
    appLanguage: AppLanguage = AppLanguage.BENGALI,
    initialScreen: Screen = Screen.Home,
    openPortfolioTimestamp: Long = 0L
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

    val screenBackStack = remember { mutableStateListOf<Screen>(initialScreen) }
    val currentScreen = screenBackStack.lastOrNull() ?: Screen.Home

    androidx.compose.runtime.LaunchedEffect(openPortfolioTimestamp) {
        if (openPortfolioTimestamp > 0L) {
            if (screenBackStack.lastOrNull() !is Screen.Portfolio) {
                screenBackStack.add(Screen.Portfolio)
            }
        }
    }

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
            DrawerDestination.HISAB_PATI -> navigateTo(Screen.HisabPati)
            DrawerDestination.WORK_TYPE -> navigateTo(Screen.WorkType)
            DrawerDestination.ORG_INFO -> navigateTo(Screen.OrganizationInfo)
            DrawerDestination.LEDGER -> navigateTo(Screen.LedgerList)
            DrawerDestination.INCOME_EXPENSE -> navigateTo(Screen.IncomeExpense)
            DrawerDestination.SAVINGS_GOALS -> navigateTo(Screen.SavingsGoals)
            DrawerDestination.ACCOUNTS_SUMMARY -> navigateTo(Screen.AccountsSummary)
            DrawerDestination.FCOIN -> navigateTo(Screen.FCoin)
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
        is Screen.Home, is Screen.Category, is Screen.Calculator, is Screen.CalculatorsHub -> DrawerDestination.HOME
        is Screen.Portfolio -> DrawerDestination.PORTFOLIO
        is Screen.HisabPati -> DrawerDestination.HISAB_PATI
        is Screen.WorkType -> DrawerDestination.WORK_TYPE
        is Screen.OrganizationInfo -> DrawerDestination.ORG_INFO
        is Screen.LedgerList, is Screen.LedgerPersonDetail -> DrawerDestination.HISAB_PATI
        is Screen.IncomeExpense -> DrawerDestination.HISAB_PATI
        is Screen.SavingsGoals, is Screen.SavingsGoalDetail -> DrawerDestination.HISAB_PATI
        is Screen.AccountsSummary, is Screen.CycleSettings -> DrawerDestination.HISAB_PATI
        is Screen.FCoin -> DrawerDestination.FCOIN
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
            is Screen.CalculatorsHub -> {
                title = stringResource(R.string.title_calculators)
                subtitle = stringResource(R.string.subtitle_calculators)
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.HisabPati -> {
                title = stringResource(R.string.title_hisab_pati)
                subtitle = stringResource(R.string.subtitle_hisab_pati)
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.WorkType -> {
                title = stringResource(R.string.title_work_type)
                subtitle = stringResource(R.string.subtitle_work_type)
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.OrganizationInfo -> {
                title = stringResource(R.string.title_org_info)
                subtitle = stringResource(R.string.subtitle_org_info)
                canNavigateBack = true
                showAdMobBanner = false
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
            is Screen.LedgerList -> {
                title = stringResource(R.string.title_ledger)
                subtitle = stringResource(R.string.subtitle_ledger)
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.LedgerPersonDetail -> {
                title = stringResource(R.string.title_ledger)
                subtitle = null
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.IncomeExpense -> {
                title = stringResource(R.string.title_income_expense)
                subtitle = stringResource(R.string.subtitle_income_expense)
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.SavingsGoals -> {
                title = stringResource(R.string.title_savings_investment)
                subtitle = stringResource(R.string.subtitle_savings_investment)
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.SavingsGoalDetail -> {
                title = stringResource(R.string.title_savings_investment)
                subtitle = null
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.AccountsSummary -> {
                title = stringResource(R.string.title_accounts_summary)
                subtitle = stringResource(R.string.subtitle_accounts_summary)
                canNavigateBack = true
                showAdMobBanner = true
            }
            is Screen.CycleSettings -> {
                title = stringResource(R.string.title_cycle_settings)
                subtitle = stringResource(R.string.subtitle_cycle_settings)
                canNavigateBack = true
                showAdMobBanner = false
            }
            is Screen.FCoin -> {
                title = "F-Coin"
                subtitle = null
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
                        fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                            .togetherWith(fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing)))
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
                                onFCoinClick = {
                                    navigateTo(Screen.FCoin)
                                },
                                onCalculatorsClick = {
                                    navigateTo(Screen.CalculatorsHub)
                                },
                                onHisabPatiClick = {
                                    navigateTo(Screen.HisabPati)
                                },
                                onNavigateToIncomeExpense = {
                                    navigateTo(Screen.IncomeExpense)
                                },
                                onNavigateToSummary = {
                                    navigateTo(Screen.AccountsSummary)
                                },
                                useBengaliDigits = useBengaliDigits
                            )
                        }
                        is Screen.CalculatorsHub -> {
                            CalculatorsHubScreen(
                                onCategoryClick = { category ->
                                    navigateTo(Screen.Category(category))
                                },
                                onCalculatorClick = { calc ->
                                    navigateTo(Screen.Calculator(calc))
                                },
                                useBengaliDigits = useBengaliDigits
                            )
                        }
                        is Screen.HisabPati -> {
                            HisabPatiHubScreen(
                                onOpenLedger = { navigateTo(Screen.LedgerList) },
                                onOpenIncomeExpense = { navigateTo(Screen.IncomeExpense) },
                                onOpenSavings = { navigateTo(Screen.SavingsGoals) },
                                onOpenSummary = { navigateTo(Screen.AccountsSummary) }
                            )
                        }
                        is Screen.WorkType -> {
                            WorkTypeScreen(
                                onNavigateBack = { popBack() }
                            )
                        }
                        is Screen.OrganizationInfo -> {
                            OrganizationInfoScreen(
                                onNavigateBack = { popBack() }
                            )
                        }
                        is Screen.Portfolio -> {
                            PortfolioScreen(
                                useBengaliDigits = useBengaliDigits
                            )
                        }
                        is Screen.LedgerList -> {
                            val ledgerViewModel: LedgerViewModel = viewModel()
                            LedgerListScreen(
                                viewModel = ledgerViewModel,
                                useBengaliDigits = useBengaliDigits,
                                onPersonClick = { partyId ->
                                    navigateTo(Screen.LedgerPersonDetail(partyId))
                                }
                            )
                        }
                        is Screen.LedgerPersonDetail -> {
                            val ledgerViewModel: LedgerViewModel = viewModel()
                            LedgerPersonDetailScreen(
                                partyId = screen.partyId,
                                viewModel = ledgerViewModel,
                                useBengaliDigits = useBengaliDigits,
                                onNavigateBack = { popBack() }
                            )
                        }
                        is Screen.IncomeExpense -> {
                            val incomeExpenseViewModel: IncomeExpenseViewModel = viewModel(
                                factory = IncomeExpenseViewModel.Factory(context.applicationContext as Application)
                            )
                            IncomeExpenseScreen(
                                useBengaliDigits = useBengaliDigits,
                                viewModel = incomeExpenseViewModel
                            )
                        }
                        is Screen.SavingsGoals -> {
                            val savingsViewModel: SavingsGoalsViewModel = viewModel()
                            SavingsInvestmentScreen(
                                useBengaliDigits = useBengaliDigits,
                                viewModel = savingsViewModel,
                                onGoalClick = { goalId ->
                                    navigateTo(Screen.SavingsGoalDetail(goalId))
                                }
                            )
                        }
                        is Screen.SavingsGoalDetail -> {
                            val savingsViewModel: SavingsGoalsViewModel = viewModel()
                            SavingsGoalDetailScreen(
                                goalId = screen.goalId,
                                viewModel = savingsViewModel,
                                useBengaliDigits = useBengaliDigits,
                                onNavigateBack = { popBack() }
                            )
                        }
                        is Screen.AccountsSummary -> {
                            val summaryViewModel: AccountsSummaryViewModel = viewModel(
                                factory = AccountsSummaryViewModel.Factory(context.applicationContext as Application)
                            )
                            AccountsSummaryScreen(
                                viewModel = summaryViewModel,
                                useBengaliDigits = useBengaliDigits,
                                onOpenLedger = { navigateTo(Screen.LedgerList) },
                                onOpenCycleSettings = { navigateTo(Screen.CycleSettings) }
                            )
                        }
                        is Screen.CycleSettings -> {
                            val summaryViewModel: AccountsSummaryViewModel = viewModel(
                                factory = AccountsSummaryViewModel.Factory(context.applicationContext as Application)
                            )
                            CycleSettingsScreen(
                                viewModel = summaryViewModel,
                                useBengaliDigits = useBengaliDigits,
                                onNavigateBack = { popBack() }
                            )
                        }
                        is Screen.FCoin -> {
                            FCoinScreen(
                                onNavigateBack = { popBack() }
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

