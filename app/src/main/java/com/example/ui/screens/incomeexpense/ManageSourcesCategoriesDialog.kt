package com.example.ui.screens.incomeexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun ManageSourcesCategoriesDialog(
    incomeSources: List<IncomeSourceEntity>,
    expenseCategories: List<ExpenseCategoryEntity>,
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onAddIncomeSource: (String, () -> Unit, (String) -> Unit) -> Unit,
    onRenameIncomeSource: (Long, String, () -> Unit, (String) -> Unit) -> Unit,
    onDeleteIncomeSource: (Long, () -> Unit, (String) -> Unit) -> Unit,
    onAddExpenseCategory: (String, () -> Unit, (String) -> Unit) -> Unit,
    onRenameExpenseCategory: (Long, String, () -> Unit, (String) -> Unit) -> Unit,
    onDeleteExpenseCategory: (Long, () -> Unit, (String) -> Unit) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    // Dialog sub-states
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var renameItemTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameItemTargetName by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteItemTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteItemTargetName by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("dialog_manage_sources_categories")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = IncomeExpenseStrings.MANAGE_TITLE,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("btn_close_manage")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Error Message banner if any
                errorMessage?.let { msg ->
                    Surface(
                        color = AlertRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                color = AlertRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { errorMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = AlertRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Tabs: আয়ের উৎস vs ব্যয়ের খাত
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            errorMessage = null
                        },
                        text = {
                            Text(
                                text = "${IncomeExpenseStrings.MANAGE_TAB_INCOME} (${incomeSources.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_manage_sources")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            errorMessage = null
                        },
                        text = {
                            Text(
                                text = "${IncomeExpenseStrings.MANAGE_TAB_EXPENSE} (${expenseCategories.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_manage_categories")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add button
                Button(
                    onClick = {
                        errorMessage = null
                        showAddDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) GrowthGreen else AlertRed
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_manage_add_new")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedTab == 0) "+ নতুন আয়ের উৎস" else "+ নতুন ব্যয়ের খাত",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Items List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    if (selectedTab == 0) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("list_manage_sources")
                        ) {
                            items(incomeSources, key = { it.id }) { item ->
                                ManageItemRow(
                                    name = item.name,
                                    isPreset = item.isPreset,
                                    onRename = {
                                        renameItemTargetId = item.id
                                        renameItemTargetName = item.name
                                    },
                                    onDelete = {
                                        deleteItemTargetId = item.id
                                        deleteItemTargetName = item.name
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("list_manage_categories")
                        ) {
                            items(expenseCategories, key = { it.id }) { item ->
                                ManageItemRow(
                                    name = item.name,
                                    isPreset = item.isPreset,
                                    onRename = {
                                        renameItemTargetId = item.id
                                        renameItemTargetName = item.name
                                    },
                                    onDelete = {
                                        deleteItemTargetId = item.id
                                        deleteItemTargetName = item.name
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Sub-dialog: Add New Item ---
    if (showAddDialog) {
        var newName by rememberSaveable { mutableStateOf("") }
        var inputError by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (selectedTab == 0) IncomeExpenseStrings.DIALOG_ADD_SOURCE else IncomeExpenseStrings.DIALOG_ADD_CATEGORY,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            inputError = null
                        },
                        label = { Text(IncomeExpenseStrings.FIELD_NAME) },
                        placeholder = { Text(if (selectedTab == 0) "যেমন: টিউশনি" else "যেমন: উপহার") },
                        isError = inputError != null,
                        supportingText = inputError?.let { { Text(it, color = AlertRed) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_manage_add_name"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newName.trim()
                        if (clean.isBlank()) {
                            inputError = IncomeExpenseStrings.ERROR_NAME_REQUIRED
                            return@Button
                        }
                        if (selectedTab == 0) {
                            onAddIncomeSource(clean, {
                                showAddDialog = false
                            }, { err ->
                                inputError = err
                            })
                        } else {
                            onAddExpenseCategory(clean, {
                                showAddDialog = false
                            }, { err ->
                                inputError = err
                            })
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) GrowthGreen else AlertRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_add_manage")
                ) {
                    Text(IncomeExpenseStrings.BTN_SAVE, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    modifier = Modifier.testTag("btn_cancel_add_manage")
                ) {
                    Text(IncomeExpenseStrings.BTN_CANCEL)
                }
            }
        )
    }

    // --- Sub-dialog: Rename Item ---
    if (renameItemTargetId != null && renameItemTargetName != null) {
        val id = renameItemTargetId!!
        val currentName = renameItemTargetName!!
        var editedName by rememberSaveable { mutableStateOf(currentName) }
        var renameError by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                renameItemTargetId = null
                renameItemTargetName = null
            },
            title = {
                Text(
                    text = if (selectedTab == 0) IncomeExpenseStrings.DIALOG_RENAME_SOURCE else IncomeExpenseStrings.DIALOG_RENAME_CATEGORY,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = {
                            editedName = it
                            renameError = null
                        },
                        label = { Text(IncomeExpenseStrings.FIELD_NAME) },
                        isError = renameError != null,
                        supportingText = renameError?.let { { Text(it, color = AlertRed) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_manage_rename_name"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editedName.trim()
                        if (clean.isBlank()) {
                            renameError = IncomeExpenseStrings.ERROR_NAME_REQUIRED
                            return@Button
                        }
                        if (selectedTab == 0) {
                            onRenameIncomeSource(id, clean, {
                                renameItemTargetId = null
                                renameItemTargetName = null
                            }, { err ->
                                renameError = err
                            })
                        } else {
                            onRenameExpenseCategory(id, clean, {
                                renameItemTargetId = null
                                renameItemTargetName = null
                            }, { err ->
                                renameError = err
                            })
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_rename_manage")
                ) {
                    Text(IncomeExpenseStrings.BTN_SAVE, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renameItemTargetId = null
                        renameItemTargetName = null
                    },
                    modifier = Modifier.testTag("btn_cancel_rename_manage")
                ) {
                    Text(IncomeExpenseStrings.BTN_CANCEL)
                }
            }
        )
    }

    // --- Sub-dialog: Confirm Delete Item ---
    if (deleteItemTargetId != null && deleteItemTargetName != null) {
        val targetId = deleteItemTargetId!!
        val name = deleteItemTargetName!!
        AlertDialog(
            onDismissRequest = {
                deleteItemTargetId = null
                deleteItemTargetName = null
            },
            title = {
                Text(
                    text = IncomeExpenseStrings.CONFIRM_DELETE_TITLE,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("'$name' মুছে ফেলতে চান? লেনদেনহীন অপ্রয়োজনীয় খাত বা উৎসই কেবল মুছে ফেলা যাবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteItemTargetId = null
                        deleteItemTargetName = null
                        if (selectedTab == 0) {
                            onDeleteIncomeSource(targetId, {
                                errorMessage = null
                            }, { err ->
                                errorMessage = err
                            })
                        } else {
                            onDeleteExpenseCategory(targetId, {
                                errorMessage = null
                            }, { err ->
                                errorMessage = err
                            })
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_manage")
                ) {
                    Text(IncomeExpenseStrings.BTN_DELETE, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteItemTargetId = null
                        deleteItemTargetName = null
                    },
                    modifier = Modifier.testTag("btn_cancel_delete_manage")
                ) {
                    Text(IncomeExpenseStrings.BTN_CANCEL)
                }
            }
        )
    }
}

@Composable
private fun ManageItemRow(
    name: String,
    isPreset: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isPreset) "ডিফল্ট" else "কাস্টম",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPreset) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Action: Rename (allowed for all)
            IconButton(
                onClick = onRename,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Action: Delete (only for custom ones)
            if (!isPreset) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AlertRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
