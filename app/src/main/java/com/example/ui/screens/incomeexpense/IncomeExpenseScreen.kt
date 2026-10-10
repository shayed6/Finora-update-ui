package com.example.ui.screens.incomeexpense

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Application
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.CashTransactionType
import com.example.data.local.entity.IncomeExpenseTransactionEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeExpenseScreen(
    useBengaliDigits: Boolean = true,
    viewModel: IncomeExpenseViewModel = viewModel(
        factory = IncomeExpenseViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val context = LocalContext.current
    val orgRepo = remember { com.example.data.preferences.OrganizationInfoRepository.getInstance(context) }
    val workTypePromptShown by orgRepo.workTypePromptShown.collectAsState(initial = null)

    if (workTypePromptShown == false) {
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        com.example.ui.screens.WorkTypeScreen(
            onNavigateBack = {
                scope.launch { orgRepo.setWorkTypePromptShown(true) }
            },
            isFirstTimePrompt = true,
            onComplete = {
                // Completed/skipped inside WorkTypeScreen
            }
        )
        return
    }

    val uiState by viewModel.uiState.collectAsState()

    // Dialog controllers
    var showAddIncomeDialog by rememberSaveable { mutableStateOf(false) }
    var showAddExpenseDialog by rememberSaveable { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<IncomeExpenseTransactionEntity?>(null) }
    var showManageDialog by remember { mutableStateOf(false) }
    var manageInitialTab by remember { mutableIntStateOf(0) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_income_expense"),
        bottomBar = {
            // Two Bottom Action Buttons: Green "+ আয়" & Red "+ ব্যয়"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income_expense_bottom_bar"),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // + আয় Button (Green #19C77A)
                    Button(
                        onClick = { showAddIncomeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_add_income")
                    ) {
                        Text(
                            text = IncomeExpenseStrings.BTN_ADD_INCOME,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    // + ব্যয় Button (Red #EF5350)
                    Button(
                        onClick = { showAddExpenseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_add_expense")
                    ) {
                        Text(
                            text = IncomeExpenseStrings.BTN_ADD_EXPENSE,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Top Bar Controls: Month Chip + Manage Button ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Picker Chip
                Surface(
                    onClick = { showMonthPicker = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("chip_month_picker")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Month Picker",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = IncomeExpenseFormatter.formatMonthYear(uiState.selectedMonthYear, useBengaliDigits),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Management Button ("ব্যবস্থাপনা")
                Surface(
                    onClick = {
                        manageInitialTab = if (uiState.activeTab == CashTransactionType.INCOME) 0 else 1
                        showManageDialog = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("btn_manage_sources_categories")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "ব্যবস্থাপনা",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = IncomeExpenseStrings.BTN_MANAGE,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // --- Three Summary Cards: মোট আয় (#19C77A), মোট ব্যয় (#EF5350), অবশিষ্ট (#1769FF or #EF5350) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("income_expense_summary_cards"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: মোট আয়
                SummaryCard(
                    title = IncomeExpenseStrings.SUMMARY_INCOME,
                    amount = IncomeExpenseFormatter.formatPaisa(uiState.summary.totalIncomePaisa, useBengaliDigits),
                    accentColor = GrowthGreen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_total_income")
                )

                // Card 2: মোট ব্যয়
                SummaryCard(
                    title = IncomeExpenseStrings.SUMMARY_EXPENSE,
                    amount = IncomeExpenseFormatter.formatPaisa(uiState.summary.totalExpensePaisa, useBengaliDigits),
                    accentColor = AlertRed,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_total_expense")
                )

                // Card 3: অবশিষ্ট = আয় - ব্যয় (Blue #1769FF, Red if negative)
                val balanceColor = if (uiState.summary.balancePaisa < 0L) AlertRed else PrimaryBlue
                SummaryCard(
                    title = IncomeExpenseStrings.SUMMARY_BALANCE,
                    amount = IncomeExpenseFormatter.formatPaisa(uiState.summary.balancePaisa, useBengaliDigits),
                    accentColor = balanceColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_balance")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- Segmented Tabs: "আয়" | "ব্যয়" ---
            TabRow(
                selectedTabIndex = if (uiState.activeTab == CashTransactionType.INCOME) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("tab_income_expense_segment")
            ) {
                Tab(
                    selected = uiState.activeTab == CashTransactionType.INCOME,
                    onClick = { viewModel.setTab(CashTransactionType.INCOME) },
                    text = {
                        Text(
                            text = IncomeExpenseStrings.TAB_INCOME,
                            fontWeight = if (uiState.activeTab == CashTransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_income")
                )
                Tab(
                    selected = uiState.activeTab == CashTransactionType.EXPENSE,
                    onClick = { viewModel.setTab(CashTransactionType.EXPENSE) },
                    text = {
                        Text(
                            text = IncomeExpenseStrings.TAB_EXPENSE,
                            fontWeight = if (uiState.activeTab == CashTransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.testTag("tab_expense")
                )
            }

            // --- Filter Chips by Source / Category ---
            FilterChipRow(
                activeTab = uiState.activeTab,
                sources = uiState.incomeSources,
                categories = uiState.expenseCategories,
                selectedSourceId = uiState.selectedSourceFilterId,
                selectedCategoryId = uiState.selectedCategoryFilterId,
                onSelectSource = { viewModel.setSourceFilter(it) },
                onSelectCategory = { viewModel.setCategoryFilter(it) }
            )

            // --- Virtualized Transaction List Grouped by Date (Newest first) ---
            val displayItems = uiState.displayedTransactions
            if (displayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = IncomeExpenseStrings.EMPTY_TITLE,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = IncomeExpenseStrings.EMPTY_SUBTITLE,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Group by date (yyyy-MM-dd start of day)
                val grouped = remember(displayItems) {
                    displayItems.groupBy { item ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = item.transaction.occurredAt
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        cal.timeInMillis
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("lazy_column_transactions"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (dateTimestamp, itemsOnDate) ->
                        // Date Header
                        item(key = "header_$dateTimestamp") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = IncomeExpenseFormatter.formatDate(dateTimestamp, useBengaliDigits),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        items(itemsOnDate, key = { it.transaction.id }) { displayItem ->
                            TransactionRowItem(
                                item = displayItem,
                                useBengaliDigits = useBengaliDigits,
                                onClick = { editingTransaction = displayItem.transaction }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Dialog: Add Income ---
    if (showAddIncomeDialog) {
        AddEditIncomeDialog(
            sources = uiState.incomeSources,
            tagLineSuggestions = uiState.tagLineSuggestions,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showAddIncomeDialog = false },
            onSave = { amount, sourceId, occurredAt, tagLine, note ->
                viewModel.addIncome(
                    amountPaisa = amount,
                    sourceId = sourceId,
                    occurredAt = occurredAt,
                    tagLine = tagLine,
                    note = note,
                    onSuccess = { showAddIncomeDialog = false },
                    onError = { /* handled in dialog */ }
                )
            },
            onAddNewSource = {
                manageInitialTab = 0
                showManageDialog = true
            }
        )
    }

    // --- Dialog: Add Expense ---
    if (showAddExpenseDialog) {
        AddEditExpenseDialog(
            categories = uiState.expenseCategories,
            incomeSources = uiState.incomeSources,
            tagLineSuggestions = uiState.tagLineSuggestions,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { amount, categoryId, occurredAt, tagLine, note, linkedSourceId ->
                viewModel.addExpense(
                    amountPaisa = amount,
                    categoryId = categoryId,
                    occurredAt = occurredAt,
                    tagLine = tagLine,
                    note = note,
                    linkedIncomeSourceId = linkedSourceId,
                    onSuccess = { showAddExpenseDialog = false },
                    onError = { /* handled in dialog */ }
                )
            },
            onAddNewCategory = {
                manageInitialTab = 1
                showManageDialog = true
            }
        )
    }

    // --- Dialog: Edit Transaction ---
    editingTransaction?.let { tx ->
        if (tx.type == CashTransactionType.INCOME.name) {
            AddEditIncomeDialog(
                existingTransaction = tx,
                sources = uiState.incomeSources,
                tagLineSuggestions = uiState.tagLineSuggestions,
                useBengaliDigits = useBengaliDigits,
                onDismiss = { editingTransaction = null },
                onSave = { amount, sourceId, occurredAt, tagLine, note ->
                    viewModel.updateTransaction(
                        transaction = tx.copy(
                            amount = amount,
                            sourceId = sourceId,
                            occurredAt = occurredAt,
                            tagLine = tagLine,
                            note = note
                        ),
                        onSuccess = { editingTransaction = null },
                        onError = { /* handled */ }
                    )
                },
                onDelete = {
                    viewModel.deleteTransaction(
                        transactionId = tx.id,
                        onSuccess = { editingTransaction = null },
                        onError = { /* handled */ }
                    )
                },
                onAddNewSource = {
                    manageInitialTab = 0
                    showManageDialog = true
                }
            )
        } else {
            AddEditExpenseDialog(
                existingTransaction = tx,
                categories = uiState.expenseCategories,
                incomeSources = uiState.incomeSources,
                tagLineSuggestions = uiState.tagLineSuggestions,
                useBengaliDigits = useBengaliDigits,
                onDismiss = { editingTransaction = null },
                onSave = { amount, categoryId, occurredAt, tagLine, note, linkedSourceId ->
                    viewModel.updateTransaction(
                        transaction = tx.copy(
                            amount = amount,
                            categoryId = categoryId,
                            occurredAt = occurredAt,
                            tagLine = tagLine,
                            note = note,
                            linkedIncomeSourceId = linkedSourceId
                        ),
                        onSuccess = { editingTransaction = null },
                        onError = { /* handled */ }
                    )
                },
                onDelete = {
                    viewModel.deleteTransaction(
                        transactionId = tx.id,
                        onSuccess = { editingTransaction = null },
                        onError = { /* handled */ }
                    )
                },
                onAddNewCategory = {
                    manageInitialTab = 1
                    showManageDialog = true
                }
            )
        }
    }

    // --- Dialog: Manage Sources & Categories ---
    if (showManageDialog) {
        ManageSourcesCategoriesDialog(
            incomeSources = uiState.incomeSources,
            expenseCategories = uiState.expenseCategories,
            initialTab = manageInitialTab,
            onDismiss = { showManageDialog = false },
            onAddIncomeSource = { name, onSucc, onErr ->
                viewModel.addCustomIncomeSource(name, { onSucc() }, onErr)
            },
            onRenameIncomeSource = { id, newName, onSucc, onErr ->
                viewModel.renameIncomeSource(id, newName, onSucc, onErr)
            },
            onDeleteIncomeSource = { id, onSucc, onErr ->
                viewModel.deleteIncomeSource(id, onSucc, onErr)
            },
            onAddExpenseCategory = { name, onSucc, onErr ->
                viewModel.addCustomExpenseCategory(name, { onSucc() }, onErr)
            },
            onRenameExpenseCategory = { id, newName, onSucc, onErr ->
                viewModel.renameExpenseCategory(id, newName, onSucc, onErr)
            },
            onDeleteExpenseCategory = { id, onSucc, onErr ->
                viewModel.deleteExpenseCategory(id, onSucc, onErr)
            }
        )
    }

    // --- Dialog: Month Picker ---
    if (showMonthPicker) {
        MonthPickerDialog(
            current = uiState.selectedMonthYear,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showMonthPicker = false },
            onSelect = { selected ->
                viewModel.setMonthYear(selected)
                showMonthPicker = false
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    activeTab: CashTransactionType,
    sources: List<com.example.data.local.entity.IncomeSourceEntity>,
    categories: List<com.example.data.local.entity.ExpenseCategoryEntity>,
    selectedSourceId: Long?,
    selectedCategoryId: Long?,
    onSelectSource: (Long?) -> Unit,
    onSelectCategory: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("filter_chip_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (activeTab == CashTransactionType.INCOME) {
            // "সব আয়ের উৎস"
            item {
                FilterChip(
                    selected = selectedSourceId == null,
                    onClick = { onSelectSource(null) },
                    label = { Text(IncomeExpenseStrings.FILTER_ALL_SOURCES, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrowthGreen.copy(alpha = 0.15f),
                        selectedLabelColor = GrowthGreen
                    ),
                    modifier = Modifier.testTag("chip_filter_all_sources")
                )
            }
            items(sources, key = { it.id }) { src ->
                FilterChip(
                    selected = selectedSourceId == src.id,
                    onClick = {
                        if (selectedSourceId == src.id) onSelectSource(null) else onSelectSource(src.id)
                    },
                    label = { Text(src.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrowthGreen.copy(alpha = 0.15f),
                        selectedLabelColor = GrowthGreen
                    )
                )
            }
        } else {
            // "সব ব্যয়ের খাত"
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text(IncomeExpenseStrings.FILTER_ALL_CATEGORIES, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertRed.copy(alpha = 0.15f),
                        selectedLabelColor = AlertRed
                    ),
                    modifier = Modifier.testTag("chip_filter_all_categories")
                )
            }
            items(categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = {
                        if (selectedCategoryId == cat.id) onSelectCategory(null) else onSelectCategory(cat.id)
                    },
                    label = { Text(cat.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertRed.copy(alpha = 0.15f),
                        selectedLabelColor = AlertRed
                    )
                )
            }
        }
    }
}

@Composable
private fun TransactionRowItem(
    item: TransactionDisplayItem,
    useBengaliDigits: Boolean,
    onClick: () -> Unit
) {
    val isIncome = item.transaction.type == CashTransactionType.INCOME.name
    val accentColor = if (isIncome) GrowthGreen else AlertRed

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tx_item_${item.transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator pill
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details: Source/Category name, Tag line, Linked source, Time
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.categoryOrSourceName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.linkedSourceName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = item.linkedSourceName,
                                fontSize = 10.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (!item.transaction.tagLine.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.transaction.tagLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = IncomeExpenseFormatter.formatTime(item.transaction.occurredAt, useBengaliDigits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            // Amount
            val prefix = if (isIncome) "+ " else "- "
            Text(
                text = prefix + IncomeExpenseFormatter.formatPaisa(item.transaction.amount, useBengaliDigits),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MonthPickerDialog(
    current: MonthYear,
    useBengaliDigits: Boolean,
    onDismiss: () -> Unit,
    onSelect: (MonthYear) -> Unit
) {
    var year by remember { mutableIntStateOf(current.year) }
    val monthNamesBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { year -= 1 }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Year")
                }
                Text(
                    text = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(year.toString()) else year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { year += 1 }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Year")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..2) {
                            val monthIdx = row * 3 + col
                            val isSelected = year == current.year && monthIdx == current.month
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onSelect(MonthYear(year, monthIdx))
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = monthNamesBn[monthIdx],
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(IncomeExpenseStrings.BTN_CANCEL)
            }
        }
    )
}
