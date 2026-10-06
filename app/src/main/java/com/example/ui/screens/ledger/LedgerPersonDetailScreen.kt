package com.example.ui.screens.ledger

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerEntryType
import com.example.data.local.entity.LedgerPartyEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerPersonDetailScreen(
    partyId: Long,
    viewModel: LedgerViewModel,
    useBengaliDigits: Boolean = true,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val partyFlow = remember(partyId) { viewModel.getParty(partyId) }
    val entriesFlow = remember(partyId) { viewModel.getPartyEntries(partyId) }

    val party by partyFlow.collectAsState(initial = null)
    val entries by entriesFlow.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val allParties = uiState.parties.map { it.party }

    // Calculations for this person
    var totalGavePaisa = 0L
    var totalReceivedPaisa = 0L
    entries.forEach { entry ->
        if (entry.type == LedgerEntryType.GAVE.name) {
            totalGavePaisa += entry.amountPaisa
        } else if (entry.type == LedgerEntryType.RECEIVED.name) {
            totalReceivedPaisa += entry.amountPaisa
        }
    }
    val netBalancePaisa = totalGavePaisa - totalReceivedPaisa
    val balanceType = when {
        netBalancePaisa > 0L -> BalanceType.PABO
        netBalancePaisa < 0L -> BalanceType.DEBO
        else -> BalanceType.SETTLED
    }

    // Dialog state
    var showAddEntryDialog by rememberSaveable { mutableStateOf(false) }
    var entryDialogInitialType by rememberSaveable { mutableStateOf(LedgerEntryType.GAVE) }
    var editingEntry by remember { mutableStateOf<LedgerEntryEntity?>(null) }
    var showEditPartyDialog by rememberSaveable { mutableStateOf(false) }
    var showDeletePartyConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Group entries by formatted Date (newest first)
    val groupedEntries = remember(entries, useBengaliDigits) {
        entries.groupBy { LedgerFormatter.formatDate(it.entryDate, useBengaliDigits) }
    }

    // Confirmation dialog for deleting party
    if (showDeletePartyConfirm) {
        AlertDialog(
            onDismissRequest = { showDeletePartyConfirm = false },
            title = { Text(LedgerStrings.CONFIRM_DELETE_PARTY_TITLE, fontWeight = FontWeight.Bold) },
            text = { Text(LedgerStrings.CONFIRM_DELETE_PARTY_MSG) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletePartyConfirm = false
                        viewModel.deleteParty(partyId)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_party")
                ) {
                    Text(LedgerStrings.BTN_DELETE, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePartyConfirm = false }) {
                    Text(LedgerStrings.BTN_CANCEL)
                }
            }
        )
    }

    // Add / Edit Entry Dialog
    if (showAddEntryDialog) {
        AddEditEntryDialog(
            initialType = entryDialogInitialType,
            existingEntry = editingEntry,
            useBengaliDigits = useBengaliDigits,
            onDismiss = {
                showAddEntryDialog = false
                editingEntry = null
            },
            onSave = { type, amountPaisa, note, entryDate ->
                val current = editingEntry
                if (current != null) {
                    viewModel.updateEntry(
                        current.copy(
                            type = type.name,
                            amountPaisa = amountPaisa,
                            note = note,
                            entryDate = entryDate
                        )
                    )
                } else {
                    viewModel.addEntry(
                        partyId = partyId,
                        type = type,
                        amountPaisa = amountPaisa,
                        note = note,
                        entryDate = entryDate
                    )
                }
                showAddEntryDialog = false
                editingEntry = null
            },
            onDelete = if (editingEntry != null) {
                {
                    editingEntry?.let { viewModel.deleteEntry(it.id) }
                    showAddEntryDialog = false
                    editingEntry = null
                }
            } else null
        )
    }

    // Edit Party Dialog
    if (showEditPartyDialog && party != null) {
        AddEditPartyDialog(
            party = party,
            existingParties = allParties,
            onDismiss = { showEditPartyDialog = false },
            onSave = { name, phone ->
                party?.let {
                    viewModel.updateParty(it.copy(name = name, phone = phone))
                }
                showEditPartyDialog = false
            },
            onOpenExisting = { existingId ->
                showEditPartyDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_ledger_person_detail"),
        bottomBar = {
            // Two large bottom buttons: green "পেলাম" and red "দিলাম"
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Green "পেলাম" (RECEIVED) button
                    Button(
                        onClick = {
                            entryDialogInitialType = LedgerEntryType.RECEIVED
                            editingEntry = null
                            showAddEntryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_received_entry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LedgerStrings.BTN_RECEIVED,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Red "দিলাম" (GAVE) button
                    Button(
                        onClick = {
                            entryDialogInitialType = LedgerEntryType.GAVE
                            editingEntry = null
                            showAddEntryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_gave_entry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LedgerStrings.BTN_GAVE,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
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
            // Header card with Person Name, Phone, Actions and Net Balance
            val balanceColor = when (balanceType) {
                BalanceType.PABO -> GrowthGreen
                BalanceType.DEBO -> AlertRed
                BalanceType.SETTLED -> Color(0xFF757575)
            }
            val balanceStatusText = when (balanceType) {
                BalanceType.PABO -> LedgerStrings.STATUS_PABO
                BalanceType.DEBO -> LedgerStrings.STATUS_DEBO
                BalanceType.SETTLED -> LedgerStrings.STATUS_SETTLED
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("card_person_balance_header"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Top row: Person Name + Phone on left, Edit & Delete actions on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val initials = remember(party?.name) {
                                LedgerFormatter.extractInitials(party?.name ?: "")
                            }
                            Surface(
                                shape = CircleShape,
                                color = PrimaryBlue.copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initials,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = party?.name ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                party?.phone?.let { ph ->
                                    Text(
                                        text = ph,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { showEditPartyDialog = true },
                                modifier = Modifier.size(34.dp).testTag("btn_edit_person")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Person",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showDeletePartyConfirm = true },
                                modifier = Modifier.size(34.dp).testTag("btn_delete_person")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Person",
                                    tint = AlertRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Net Balance summary
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "বর্তমান ব্যালেন্স",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = LedgerFormatter.formatPaisa(kotlin.math.abs(netBalancePaisa), useBengaliDigits),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = balanceColor,
                                modifier = Modifier.testTag("text_person_net_balance")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = balanceColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = balanceStatusText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = balanceColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sub-stats: Total Gave vs Total Received
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "মোট দিলাম",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = LedgerFormatter.formatPaisa(totalGavePaisa, useBengaliDigits),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(26.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "মোট পেলাম",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = LedgerFormatter.formatPaisa(totalReceivedPaisa, useBengaliDigits),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrowthGreen
                                )
                            }
                        }
                    }
                }
            }

            // Entries List grouped by Date (virtualized LazyColumn for 500+ items smooth scroll)
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "কোনো লেনদেন এন্ট্রি নেই",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "নিচের 'দিলাম' বা 'পেলাম' বাটনে ট্যাপ করে নতুন লেনদেন যোগ করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("lazy_column_person_entries"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedEntries.forEach { (dateHeader, dateEntries) ->
                        item(key = "header_$dateHeader") {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }

                        items(dateEntries, key = { it.id }) { entry ->
                            LedgerEntryRow(
                                entry = entry,
                                useBengaliDigits = useBengaliDigits,
                                onClick = {
                                    editingEntry = entry
                                    showAddEntryDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerEntryRow(
    entry: LedgerEntryEntity,
    useBengaliDigits: Boolean,
    onClick: () -> Unit
) {
    val isGave = entry.type == LedgerEntryType.GAVE.name
    val itemColor = if (isGave) AlertRed else GrowthGreen
    val arrowIcon = if (isGave) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
    val typeLabel = if (isGave) LedgerStrings.BTN_GAVE else LedgerStrings.BTN_RECEIVED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("entry_item_${entry.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = itemColor.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = arrowIcon,
                        contentDescription = typeLabel,
                        tint = itemColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Note & Time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (entry.note.isNullOrBlank()) typeLabel else entry.note,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${LedgerFormatter.formatTime(entry.createdAt, useBengaliDigits)} • $typeLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Amount
            Text(
                text = LedgerFormatter.formatPaisa(entry.amountPaisa, useBengaliDigits),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = itemColor
            )
        }
    }
}
