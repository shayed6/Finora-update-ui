package com.example.ui.screens.goals

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsEntryType
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalDetailScreen(
    goalId: Long,
    viewModel: SavingsGoalsViewModel,
    useBengaliDigits: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.getGoalDetailState(goalId, useBengaliDigits).collectAsState(initial = null)
    val mainState by viewModel.uiState.collectAsState()

    var showEditGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteGoalConfirm by rememberSaveable { mutableStateOf(false) }

    var showAddDepositDialog by rememberSaveable { mutableStateOf(false) }
    var showAddWithdrawDialog by rememberSaveable { mutableStateOf(false) }
    var editingEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingEntry = remember(editingEntryId, detailState?.entries) {
        editingEntryId?.let { id -> detailState?.entries?.firstOrNull { it.entry.id == id }?.entry }
    }
    var showAddSectorDialog by rememberSaveable { mutableStateOf(false) }

    val goal = detailState?.goal

    if (showDeleteGoalConfirm && goal != null && !goal.isDefault) {
        AlertDialog(
            onDismissRequest = { showDeleteGoalConfirm = false },
            title = { Text(SavingsStrings.CONFIRM_DELETE_GOAL_TITLE, fontWeight = FontWeight.Bold) },
            text = { Text(SavingsStrings.CONFIRM_DELETE_GOAL_MSG) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteGoalConfirm = false
                        viewModel.deleteGoal(goalId, {
                            onNavigateBack()
                        }, {})
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_goal")
                ) {
                    Text(SavingsStrings.BTN_DELETE, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGoalConfirm = false }) {
                    Text(SavingsStrings.BTN_CANCEL)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_goal_detail"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = goal?.name ?: "",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_goal_detail")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (goal != null) {
                        // Edit Goal Action
                        IconButton(
                            onClick = { showEditGoalDialog = true },
                            modifier = Modifier.testTag("btn_edit_goal")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Goal",
                                tint = Color.White
                            )
                        }

                        // Delete Goal Action (only if not default goal)
                        if (!goal.isDefault) {
                            IconButton(
                                onClick = { showDeleteGoalConfirm = true },
                                modifier = Modifier.testTag("btn_delete_goal")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Goal",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FinoraNavy
                )
            )
        },
        bottomBar = {
            // Two Bottom Action Buttons: green "+ জমা / বিনিয়োগ" and smaller outlined "উত্তোলন"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_detail_bottom_bar"),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Green "+ জমা / বিনিয়োগ"
                    Button(
                        onClick = { showAddDepositDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("btn_add_deposit")
                    ) {
                        Text(
                            text = SavingsStrings.BTN_DEPOSIT,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    // Smaller Outlined "উত্তোলন"
                    OutlinedButton(
                        onClick = { showAddWithdrawDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, AlertRed),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("btn_add_withdraw")
                    ) {
                        Text(
                            text = SavingsStrings.BTN_WITHDRAW,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AlertRed
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (detailState == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("লোড হচ্ছে...")
            }
        } else {
            val state = detailState!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header with Progress & Badge
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (state.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GrowthGreen.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GrowthGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = SavingsStrings.BADGE_COMPLETED,
                                    color = GrowthGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    state.progressPercent?.let { progress ->
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (state.isCompleted) GrowthGreen else PrimaryBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                // Cards: "জমা", "বাকি" (only if target), "সময় বাকি" (only if date)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("goal_detail_cards"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: জমা
                    DetailMetricCard(
                        title = SavingsStrings.CARD_SAVED,
                        value = SavingsFormatter.formatBangladeshiTaka(state.savedPaisa, useBengaliDigits),
                        accentColor = GrowthGreen,
                        modifier = Modifier.weight(1f).testTag("card_detail_saved")
                    )

                    // Card 2: বাকি (if target exists)
                    state.remainingPaisa?.let { rem ->
                        DetailMetricCard(
                            title = SavingsStrings.CARD_REMAINING,
                            value = SavingsFormatter.formatBangladeshiTaka(rem, useBengaliDigits),
                            accentColor = if (rem == 0L) GrowthGreen else AlertRed,
                            modifier = Modifier.weight(1f).testTag("card_detail_remaining")
                        )
                    }

                    // Card 3: সময় বাকি (if target date exists)
                    state.remainingTimeText?.let { timeStr ->
                        DetailMetricCard(
                            title = SavingsStrings.CARD_TIME_LEFT,
                            value = timeStr,
                            accentColor = PrimaryBlue,
                            modifier = Modifier.weight(1f).testTag("card_detail_time_left")
                        )
                    }
                }

                // Sector Breakdown List
                if (state.sectorBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = SavingsStrings.SECTION_SECTOR_BREAKDOWN,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("row_sector_breakdown"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.sectorBreakdown, key = { it.sectorName }) { sec ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${sec.sectorName}: ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = SavingsFormatter.formatBangladeshiTaka(sec.totalPaisa, useBengaliDigits),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section: Entries Grouped by Date
                Text(
                    text = SavingsStrings.SECTION_TRANSACTIONS,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (state.entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো লেনদেন নেই",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Group entries by date
                    val groupedEntries = remember(state.entries) {
                        state.entries.groupBy {
                            SavingsFormatter.formatDate(it.entry.entryDate, useBengaliDigits)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("lazy_column_goal_entries"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedEntries.forEach { (dateStr, items) ->
                            item(key = "header_$dateStr") {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            items(items, key = { it.entry.id }) { item ->
                                EntryRowItem(
                                    item = item,
                                    useBengaliDigits = useBengaliDigits,
                                    onClick = { editingEntryId = item.entry.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialog: Edit Goal ---
    if (showEditGoalDialog && goal != null) {
        AddEditGoalDialog(
            goal = goal,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showEditGoalDialog = false },
            onSave = { name, targetPaisa, targetDate ->
                viewModel.updateGoal(
                    id = goalId,
                    name = name,
                    targetAmountPaisa = targetPaisa,
                    targetDate = targetDate,
                    onSuccess = { showEditGoalDialog = false },
                    onError = {}
                )
            }
        )
    }

    // --- Dialog: Add Deposit ---
    if (showAddDepositDialog) {
        AddEditSavingsEntryDialog(
            initialType = SavingsEntryType.DEPOSIT,
            sectors = mainState.sectors,
            currentGoalSavedPaisa = detailState?.savedPaisa ?: 0L,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showAddDepositDialog = false },
            onSave = { type, amountPaisa, sectorId, entryDate, note ->
                viewModel.addEntry(
                    goalId = goalId,
                    type = type.name,
                    amountPaisa = amountPaisa,
                    sectorId = sectorId,
                    entryDate = entryDate,
                    note = note,
                    onSuccess = { showAddDepositDialog = false },
                    onError = {}
                )
            },
            onAddNewSector = { showAddSectorDialog = true }
        )
    }

    // --- Dialog: Add Withdraw ---
    if (showAddWithdrawDialog) {
        AddEditSavingsEntryDialog(
            initialType = SavingsEntryType.WITHDRAW,
            sectors = mainState.sectors,
            currentGoalSavedPaisa = detailState?.savedPaisa ?: 0L,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showAddWithdrawDialog = false },
            onSave = { type, amountPaisa, sectorId, entryDate, note ->
                viewModel.addEntry(
                    goalId = goalId,
                    type = type.name,
                    amountPaisa = amountPaisa,
                    sectorId = sectorId,
                    entryDate = entryDate,
                    note = note,
                    onSuccess = { showAddWithdrawDialog = false },
                    onError = {}
                )
            },
            onAddNewSector = { showAddSectorDialog = true }
        )
    }

    // --- Dialog: Edit Entry ---
    editingEntry?.let { entry ->
        AddEditSavingsEntryDialog(
            existingEntry = entry,
            sectors = mainState.sectors,
            currentGoalSavedPaisa = detailState?.savedPaisa ?: 0L,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { editingEntryId = null },
            onSave = { type, amountPaisa, sectorId, entryDate, note ->
                viewModel.updateEntry(
                    entry = entry.copy(
                        type = type.name,
                        amount = amountPaisa,
                        sectorId = sectorId,
                        entryDate = entryDate,
                        note = note
                    ),
                    onSuccess = { editingEntryId = null },
                    onError = {}
                )
            },
            onDelete = {
                viewModel.deleteEntry(
                    entryId = entry.id,
                    onSuccess = { editingEntryId = null },
                    onError = {}
                )
            },
            onAddNewSector = { showAddSectorDialog = true }
        )
    }

    // --- Dialog: Add Custom Sector ---
    if (showAddSectorDialog) {
        AddCustomSectorDialog(
            onDismiss = { showAddSectorDialog = false },
            onSave = { name ->
                viewModel.addCustomSector(name, {
                    showAddSectorDialog = false
                }, {})
            }
        )
    }
}

@Composable
private fun DetailMetricCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
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
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EntryRowItem(
    item: SavingsEntryItem,
    useBengaliDigits: Boolean,
    onClick: () -> Unit
) {
    val isDeposit = item.entry.type == SavingsEntryType.DEPOSIT.name
    val accentColor = if (isDeposit) GrowthGreen else AlertRed

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("entry_item_${item.entry.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Icon: Up arrow for deposit, Down arrow for withdraw
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.sectorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!item.entry.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Amount
            val prefix = if (isDeposit) "+ " else "- "
            Text(
                text = prefix + SavingsFormatter.formatBangladeshiTaka(item.entry.amount, useBengaliDigits),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 14.sp
            )
        }
    }
}
