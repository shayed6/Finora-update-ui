package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FavoritesManager
import com.example.data.preferences.AppLanguage
import com.example.model.CalculatorCategory
import com.example.model.CalculatorDef
import com.example.model.CalculatorRepository
import com.example.ui.LocalAppLanguage
import com.example.ui.components.CalculatorListItem
import com.example.ui.components.CurrentInvestmentWidget
import com.example.ui.components.UnlockFavoriteSlotsDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import com.example.ui.screens.incomeexpense.IncomeExpenseFormatter
import com.example.ui.screens.incomeexpense.TransactionDisplayItem
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.local.entity.CashTransactionType
import com.example.ui.screens.incomeexpense.AddEditExpenseDialog
import com.example.ui.screens.incomeexpense.AddEditIncomeDialog
import com.example.ui.screens.incomeexpense.IncomeExpenseViewModel
import com.example.ui.screens.incomeexpense.ManageSourcesCategoriesDialog
import com.example.ui.screens.portfolio.PortfolioViewModel
import com.example.ui.screens.summary.AccountsSummaryViewModel
import com.example.ui.screens.summary.SummaryFormatter
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.GrowthGreenDark
import com.example.ui.theme.GrowthGreenLight
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter

@Composable
fun HomeScreen(
    onCategoryClick: (CalculatorCategory) -> Unit,
    onCalculatorClick: (CalculatorDef) -> Unit,
    onPortfolioClick: () -> Unit,
    onFCoinClick: () -> Unit = {},
    onCalculatorsClick: () -> Unit = {},
    onHisabPatiClick: () -> Unit = {},
    onNavigateToIncomeExpense: () -> Unit = onHisabPatiClick,
    onNavigateToSummary: () -> Unit = onHisabPatiClick,
    useBengaliDigits: Boolean = true,
    portfolioViewModel: PortfolioViewModel = viewModel(),
    accountsSummaryViewModel: AccountsSummaryViewModel = viewModel(),
    incomeExpenseViewModel: IncomeExpenseViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = remember { CalculatorCategory.values().toList() }
    val searchResults = remember(searchQuery) {
        if (searchQuery.isNotBlank()) CalculatorRepository.search(searchQuery) else emptyList()
    }

    val portfolioSummary by portfolioViewModel.summary.collectAsState()
    val summaryUiState by accountsSummaryViewModel.uiState.collectAsState()
    val incomeExpenseUiState by incomeExpenseViewModel.uiState.collectAsState()
    val recentTransactions by incomeExpenseViewModel.recentTransactions.collectAsState()

    val favoriteIds by FavoritesManager.favorites.collectAsState()
    val maxSlots by FavoritesManager.maxSlots.collectAsState()
    var showUnlockSlotsDialog by remember { mutableStateOf(false) }

    // Quick Action Dialog states
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var manageInitialTab by remember { mutableStateOf(1) } // 0 = source, 1 = category

    val handleToggleFavorite: (String) -> Unit = remember {
        { calcId ->
            val success = FavoritesManager.toggleFavorite(calcId)
            if (!success) {
                showUnlockSlotsDialog = true
            }
        }
    }

    val favoriteCalculators = remember(favoriteIds) {
        favoriteIds.mapNotNull { CalculatorRepository.getById(it) }
    }
    val isEnglish = LocalAppLanguage.current == AppLanguage.ENGLISH

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Portfolio Summary Widget (Current Investment)
        item(key = "portfolio_summary") {
            CurrentInvestmentWidget(
                summary = portfolioSummary,
                useBengaliDigits = useBengaliDigits,
                onClick = onPortfolioClick
            )
        }

        // 2. Financial Overview Card (Mini Chart & Total Balance Indicator just below Portfolio)
        item(key = "financial_overview_card") {
            HomeFinancialOverviewCard(
                summary = summaryUiState.summary,
                periodLabel = summaryUiState.period.label,
                useBengaliDigits = useBengaliDigits,
                isEnglish = isEnglish,
                onViewDetailsClick = onNavigateToSummary
            )
        }

        // 3. Recent Transactions List Component (Last 5 financial entries from Room DB beneath summary card)
        item(key = "recent_transactions_card") {
            HomeRecentTransactionsCard(
                transactions = recentTransactions,
                useBengaliDigits = useBengaliDigits,
                isEnglish = isEnglish,
                onViewAllClick = onNavigateToIncomeExpense,
                onAddExpenseClick = { showAddExpenseDialog = true },
                onAddIncomeClick = { showAddIncomeDialog = true }
            )
        }

        // 3. Search Bar
        item(key = "search_field") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isEnglish) "Search calculators (e.g. P/E, SIP, EMI)..." else "যেকোনো ক্যালকুলেটর খুঁজুন (যেমন: P/E, SIP, লাভ/ক্ষতি)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = PrimaryBlue
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // 4. Quick Actions Row (Directly below Portfolio & Search sections)
        item(key = "quick_actions_row") {
            HomeQuickActionsRow(
                isEnglish = isEnglish,
                onAddExpenseClick = { showAddExpenseDialog = true },
                onAddIncomeClick = { showAddIncomeDialog = true },
                onOpenHisabClick = onHisabPatiClick
            )
        }

        if (searchQuery.isNotBlank()) {
            // Search Results Section
            item(key = "search_header") {
                Text(
                    text = if (isEnglish) "Search Results (${searchResults.size})" else "অনুসন্ধানের ফলাফল (${BengaliFormatter.toBengaliDigits(searchResults.size.toString())} টি)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            if (searchResults.isEmpty()) {
                item(key = "search_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "No calculators found.\nTry searching with alternative keywords." else "কোনো ক্যালকুলেটর খুঁজে পাওয়া যায়নি।\nবানান বা ইংরেজিতে অনুসন্ধান করে দেখুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(searchResults, key = { it.id }) { calc ->
                    CalculatorListItem(
                        calculator = calc,
                        onClick = { onCalculatorClick(calc) },
                        isFavorite = favoriteIds.contains(calc.id),
                        onToggleFavorite = { handleToggleFavorite(calc.id) }
                    )
                }
            }
        } else {
            // 5. Favorite Calculators Section (above Calculator and Hisab Pati)
            item(key = "favorite_calculators") {
                FavoriteCalculatorsSection(
                    favoriteCalculators = favoriteCalculators,
                    currentSlots = favoriteCalculators.size,
                    maxSlots = maxSlots,
                    onCalculatorClick = onCalculatorClick,
                    onToggleFavorite = handleToggleFavorite,
                    onUnlockSlotsClick = { showUnlockSlotsDialog = true },
                    useBengaliDigits = useBengaliDigits
                )
            }

            // 4. Two large navigation cards: "ক্যালকুলেটর" & "হিসাব পাতি"
            item(key = "primary_nav_cards") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_top_two_cards"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: ক্যালকুলেটর
                    HomePrimaryNavCard(
                        title = "ক্যালকুলেটর",
                        subtitle = "৩৭টি স্টক ও ব্যাংকিং ক্যালকুলেটর",
                        icon = Icons.Default.Calculate,
                        accentColor = PrimaryBlue,
                        onClick = onCalculatorsClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_card_calculators")
                    )

                    // Card 2: হিসাব পাতি
                    HomePrimaryNavCard(
                        title = "হিসাব পাতি",
                        subtitle = "খাতা, আয়-ব্যয় ও সঞ্চয় হিসাব",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = GrowthGreen,
                        onClick = onHisabPatiClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_card_hisab_pati")
                    )
                }
            }

            // On-device privacy indicator footer
            item(key = "privacy_indicator_footer") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = GrowthGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isEnglish) "All calculations run locally on your device. No financial data is sent to external servers." else "সব হিসাব সম্পূর্ণ আপনার ডিভাইসে ঘটে। কোনো আর্থিক তথ্য সংরক্ষিত বা প্রেরিত হয় না।",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    if (showUnlockSlotsDialog) {
        UnlockFavoriteSlotsDialog(
            currentSlots = favoriteCalculators.size,
            maxSlots = maxSlots,
            onDismiss = { showUnlockSlotsDialog = false }
        )
    }

    // --- Quick Action Dialog: Add Expense directly from Home ---
    if (showAddExpenseDialog) {
        AddEditExpenseDialog(
            categories = incomeExpenseUiState.expenseCategories,
            incomeSources = incomeExpenseUiState.incomeSources,
            tagLineSuggestions = incomeExpenseUiState.tagLineSuggestions,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { amount, categoryId, occurredAt, tagLine, note, linkedSourceId ->
                incomeExpenseViewModel.addExpense(
                    amountPaisa = amount,
                    categoryId = categoryId,
                    occurredAt = occurredAt,
                    tagLine = tagLine,
                    note = note,
                    linkedIncomeSourceId = linkedSourceId,
                    onSuccess = { showAddExpenseDialog = false },
                    onError = { /* handled */ }
                )
            },
            onAddNewCategory = {
                manageInitialTab = 1
                showManageDialog = true
            }
        )
    }

    // --- Quick Action Dialog: Add Income directly from Home ---
    if (showAddIncomeDialog) {
        AddEditIncomeDialog(
            sources = incomeExpenseUiState.incomeSources,
            tagLineSuggestions = incomeExpenseUiState.tagLineSuggestions,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showAddIncomeDialog = false },
            onSave = { amount, sourceId, occurredAt, tagLine, note ->
                incomeExpenseViewModel.addIncome(
                    amountPaisa = amount,
                    sourceId = sourceId,
                    occurredAt = occurredAt,
                    tagLine = tagLine,
                    note = note,
                    onSuccess = { showAddIncomeDialog = false },
                    onError = { /* handled */ }
                )
            },
            onAddNewSource = {
                manageInitialTab = 0
                showManageDialog = true
            }
        )
    }

    // --- Manage Categories / Sources from Quick Dialogs ---
    if (showManageDialog) {
        ManageSourcesCategoriesDialog(
            incomeSources = incomeExpenseUiState.incomeSources,
            expenseCategories = incomeExpenseUiState.expenseCategories,
            initialTab = manageInitialTab,
            onDismiss = { showManageDialog = false },
            onAddIncomeSource = { name, onSucc, onErr ->
                incomeExpenseViewModel.addCustomIncomeSource(name, { onSucc() }, onErr)
            },
            onRenameIncomeSource = { id, newName, onSucc, onErr ->
                incomeExpenseViewModel.renameIncomeSource(id, newName, onSucc, onErr)
            },
            onDeleteIncomeSource = { id, onSucc, onErr ->
                incomeExpenseViewModel.deleteIncomeSource(id, onSucc, onErr)
            },
            onAddExpenseCategory = { name, onSucc, onErr ->
                incomeExpenseViewModel.addCustomExpenseCategory(name, { onSucc() }, onErr)
            },
            onRenameExpenseCategory = { id, newName, onSucc, onErr ->
                incomeExpenseViewModel.renameExpenseCategory(id, newName, onSucc, onErr)
            },
            onDeleteExpenseCategory = { id, onSucc, onErr ->
                incomeExpenseViewModel.deleteExpenseCategory(id, onSucc, onErr)
            }
        )
    }
}

/**
 * Visual Financial Overview Summary Card:
 * Positioned just below the Portfolio section to give users an immediate overview
 * of their income, expense, and remaining net balance with a mini ring/progress indicator.
 */
@Composable
fun HomeFinancialOverviewCard(
    summary: com.example.ui.screens.summary.PeriodSummary,
    periodLabel: String,
    useBengaliDigits: Boolean,
    isEnglish: Boolean,
    onViewDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onViewDetailsClick)
            .testTag("home_financial_overview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title & Period Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isEnglish) "Financial Overview" else "আর্থিক সারসংক্ষেপ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = periodLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Details" else "বিস্তারিত",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Mini Chart / Donut on left, figures on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Visual Chart Indicator
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.dp.toPx()
                        val arcSize = size.width - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        val income = summary.incomePaisa.coerceAtLeast(0L)
                        val expense = summary.expensePaisa.coerceAtLeast(0L)
                        val savings = summary.savingsPaisa.coerceAtLeast(0L)

                        val total = (expense + savings).coerceAtLeast(income).coerceAtLeast(1L)
                        val expenseSweep = (expense.toFloat() / total) * 360f
                        val savingsSweep = (savings.toFloat() / total) * 360f
                        val remainingSweep = (360f - expenseSweep - savingsSweep).coerceAtLeast(0f)

                        // Track Background
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth)
                        )

                        // Expense arc (Red)
                        if (expenseSweep > 0f) {
                            drawArc(
                                color = AlertRed,
                                startAngle = -90f,
                                sweepAngle = expenseSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // Savings arc (Blue)
                        if (savingsSweep > 0f) {
                            drawArc(
                                color = PrimaryBlue,
                                startAngle = -90f + expenseSweep,
                                sweepAngle = savingsSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // Remaining / Income arc (Green)
                        if (remainingSweep > 0f && income > (expense + savings)) {
                            drawArc(
                                color = GrowthGreen,
                                startAngle = -90f + expenseSweep + savingsSweep,
                                sweepAngle = remainingSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEnglish) "Net" else "অবশিষ্ট",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (summary.remainingPaisa >= 0) "✓" else "!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.remainingPaisa >= 0) GrowthGreen else AlertRed
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Figures Column: Income, Expense, Remaining Balance
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Total Balance / Remaining
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Total Balance" else "মোট অবশিষ্ট ব্যালেন্স",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val remFormatted = SummaryFormatter.formatPaisa(summary.remainingPaisa, useBengaliDigits)
                        Text(
                            text = remFormatted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.remainingPaisa >= 0) GrowthGreen else AlertRed
                        )
                    }

                    // Income row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(GrowthGreen)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isEnglish) "Income" else "আয়",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = SummaryFormatter.formatPaisa(summary.incomePaisa, useBengaliDigits),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Expense row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AlertRed)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isEnglish) "Expense" else "ব্যয়",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = SummaryFormatter.formatPaisa(summary.expensePaisa, useBengaliDigits),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recent Transactions List Component:
 * Fetches and displays the last 5 financial entries from the Room database
 * directly beneath the financial overview summary card.
 */
@Composable
fun HomeRecentTransactionsCard(
    transactions: List<TransactionDisplayItem>,
    useBengaliDigits: Boolean,
    isEnglish: Boolean,
    onViewAllClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_recent_transactions_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title, count badge, and "View all" action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isEnglish) "Recent Transactions" else "সাম্প্রতিক লেনদেন",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (transactions.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (isEnglish) "Last ${transactions.size}" else "সর্বশেষ ${BengaliFormatter.toBengaliDigits(transactions.size.toString())}টি",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onViewAllClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.testTag("home_recent_transactions_view_all")
                ) {
                    Text(
                        text = if (isEnglish) "View all" else "সব দেখুন",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (transactions.isEmpty()) {
                // Friendly empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (isEnglish) "No recent transactions found" else "কোনো সাম্প্রতিক লেনদেন পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isEnglish) "Record your first expense or income using quick actions" else "নতুন লেনদেন যোগ করতে নিচের বোতাম ব্যবহার করুন",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                // List of up to 5 transactions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    transactions.take(5).forEachIndexed { index, item ->
                        val isIncome = item.transaction.type == CashTransactionType.INCOME.name
                        val accentColor = if (isIncome) GrowthGreen else AlertRed
                        val iconBg = accentColor.copy(alpha = 0.12f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onViewAllClick() }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("recent_tx_item_${item.transaction.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status icon badge
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = if (isIncome) "Income" else "Expense",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name, tag, date
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.categoryOrSourceName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.linkedSourceName != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PrimaryBlue.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = item.linkedSourceName,
                                                fontSize = 9.5.sp,
                                                color = PrimaryBlue,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!item.transaction.tagLine.isNullOrBlank()) {
                                        Text(
                                            text = item.transaction.tagLine,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                    Text(
                                        text = IncomeExpenseFormatter.formatDate(item.transaction.occurredAt, useBengaliDigits),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Amount
                            val prefix = if (isIncome) "+ ৳" else "- ৳"
                            val amountText = prefix + IncomeExpenseFormatter.formatPaisa(item.transaction.amount, useBengaliDigits)
                            Text(
                                text = amountText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontSize = 13.5.sp
                            )
                        }

                        if (index < transactions.take(5).size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                thickness = 0.6.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Action buttons row below Portfolio & Search sections:
 * Provides one-tap access directly on the home screen to:
 * - "+ ব্যয়" (Quick add expense)
 * - "+ আয়" (Quick add income)
 * - "খতিয়ান / হিসাব" (Open Hisab Pati)
 */
@Composable
fun HomeQuickActionsRow(
    isEnglish: Boolean,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onOpenHisabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_quick_actions_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Action 1: Add Expense (High priority red tone)
        QuickActionButton(
            label = if (isEnglish) "+ Add Expense" else "+ ব্যয় যোগ করুন",
            icon = Icons.Default.ArrowDownward,
            accentColor = AlertRed,
            onClick = onAddExpenseClick,
            modifier = Modifier.weight(1f),
            tag = "btn_quick_add_expense"
        )

        // Quick Action 2: Add Income (Green tone)
        QuickActionButton(
            label = if (isEnglish) "+ Add Income" else "+ আয় যোগ করুন",
            icon = Icons.Default.ArrowUpward,
            accentColor = GrowthGreen,
            onClick = onAddIncomeClick,
            modifier = Modifier.weight(1f),
            tag = "btn_quick_add_income"
        )

        // Quick Action 3: Open Accounts / Khata
        QuickActionButton(
            label = if (isEnglish) "Hisab Pati" else "হিসাব খাতা",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            accentColor = PrimaryBlue,
            onClick = onOpenHisabClick,
            modifier = Modifier.weight(0.9f),
            tag = "btn_quick_open_hisab"
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Favorite Calculators Section as specified in Part D:
 * - Shows favorited calculators in a horizontal scrollable row for one-tap quick access
 * - Shows current slots / max slots with opt-in rewarded unlock button
 * - If no favorites yet, shows friendly hint: "প্রিয় ক্যালকুলেটর যোগ করতে যেকোনো ক্যালকুলেটরে স্টার চাপুন"
 */
@Composable
private fun FavoriteCalculatorsSection(
    favoriteCalculators: List<CalculatorDef>,
    currentSlots: Int,
    maxSlots: Int,
    onCalculatorClick: (CalculatorDef) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onUnlockSlotsClick: () -> Unit,
    useBengaliDigits: Boolean,
    modifier: Modifier = Modifier
) {
    val isEnglish = LocalAppLanguage.current == AppLanguage.ENGLISH
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("favorite_calculators_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) "Favorite Calculators" else "প্রিয় ক্যালকুলেটর (Favorites)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFB800).copy(alpha = 0.14f)
                ) {
                    val currentStr = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(currentSlots.toString()) else currentSlots.toString()
                    val maxStr = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(maxSlots.toString()) else maxSlots.toString()
                    Text(
                        text = if (isEnglish) "$currentStr/$maxStr Slots" else "$currentStr/$maxStr স্লট",
                        color = Color(0xFFB45309),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    border = BorderStroke(0.8.dp, PrimaryBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onUnlockSlotsClick)
                        .testTag("btn_opt_in_unlock_slots")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "+3 Slots" else "+৩ স্লট",
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (favoriteCalculators.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFB800).copy(alpha = 0.12f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (isEnglish) "Tap the star on any calculator to add it to favorites" else "প্রিয় ক্যালকুলেটর যোগ করতে যেকোনো ক্যালকুলেটরে স্টার চাপুন",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(favoriteCalculators, key = { it.id }) { calc ->
                    FavoriteCalculatorCard(
                        calculator = calc,
                        onClick = { onCalculatorClick(calc) },
                        onUnfavorite = { onToggleFavorite(calc.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCalculatorCard(
    calculator: CalculatorDef,
    onClick: () -> Unit,
    onUnfavorite: () -> Unit
) {
    val isGrowth = calculator.category == CalculatorCategory.SIP_INVESTMENT ||
            calculator.category == CalculatorCategory.STOCK_AVG_PL
    val badgeBg = if (isGrowth) GrowthGreenLight else PrimaryBlue.copy(alpha = 0.08f)
    val badgeIconTint = if (isGrowth) GrowthGreenDark else PrimaryBlue

    Card(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("favorite_card_${calculator.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = calculator.category.icon,
                        contentDescription = null,
                        tint = badgeIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onUnfavorite,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Remove from favorites",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val isEnglish = LocalAppLanguage.current == AppLanguage.ENGLISH

            Text(
                text = if (isEnglish) calculator.titleEn else calculator.titleBn,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isEnglish) calculator.category.titleEn else calculator.category.titleBn,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomePrimaryNavCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

