package com.example.ui.screens.incomeexpense

import com.example.ui.components.FinoraDateTimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.IncomeExpenseTransactionEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseDialog(
    existingTransaction: IncomeExpenseTransactionEntity? = null,
    categories: List<ExpenseCategoryEntity>,
    incomeSources: List<IncomeSourceEntity>,
    tagLineSuggestions: List<String>,
    useBengaliDigits: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (amountPaisa: Long, categoryId: Long, occurredAt: Long, tagLine: String?, note: String?, linkedSourceId: Long?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddNewCategory: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = existingTransaction != null

    // Use rememberSaveable to preserve user-entered values if internet drops
    var amountText by rememberSaveable {
        mutableStateOf(
            if (existingTransaction != null) {
                val taka = existingTransaction.amount / 100.0
                val formatted = if (taka % 1.0 == 0.0) taka.toLong().toString() else "%.2f".format(taka)
                if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
            } else ""
        )
    }

    var selectedCategoryId by rememberSaveable {
        mutableStateOf(existingTransaction?.categoryId ?: categories.firstOrNull()?.id ?: 0L)
    }

    var linkedSourceId by rememberSaveable {
        mutableStateOf<Long?>(existingTransaction?.linkedIncomeSourceId)
    }

    var tagLine by rememberSaveable {
        mutableStateOf(existingTransaction?.tagLine ?: "")
    }

    var noteText by rememberSaveable {
        mutableStateOf(existingTransaction?.note ?: "")
    }

    var occurredAtTimestamp by rememberSaveable {
        mutableLongStateOf(existingTransaction?.occurredAt ?: System.currentTimeMillis())
    }

    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var linkedSourceExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var showDateTimePickerDialog by remember { mutableStateOf(false) }

    if (showDateTimePickerDialog) {
        FinoraDateTimePickerDialog(
            initialDateTimeMillis = occurredAtTimestamp,
            allowFutureDateTime = false,
            onDismissRequest = { showDateTimePickerDialog = false },
            onDateTimeSelected = { chosen ->
                occurredAtTimestamp = chosen
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(IncomeExpenseStrings.CONFIRM_DELETE_TX_TITLE, fontWeight = FontWeight.Bold) },
            text = { Text(IncomeExpenseStrings.CONFIRM_DELETE_TX_MSG) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_expense")
                ) {
                    Text(IncomeExpenseStrings.BTN_DELETE, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(IncomeExpenseStrings.BTN_CANCEL)
                }
            }
        )
    }

    val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "খাত নির্বাচন করুন"
    val selectedLinkedSourceName = incomeSources.firstOrNull { it.id == linkedSourceId }?.name ?: IncomeExpenseStrings.LINKED_SOURCE_NONE

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_add_edit_expense"),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) IncomeExpenseStrings.DIALOG_EDIT_EXPENSE_TITLE else IncomeExpenseStrings.DIALOG_ADD_EXPENSE_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isEditing && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(36.dp).testTag("btn_delete_expense")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input
                        amountError = null
                    },
                    label = { Text(IncomeExpenseStrings.FIELD_AMOUNT_LABEL) },
                    placeholder = { Text(IncomeExpenseStrings.FIELD_AMOUNT_HINT) },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertRed,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    isError = amountError != null,
                    supportingText = {
                        amountError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Dropdown with "+ নতুন খাত"
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(IncomeExpenseStrings.FIELD_CATEGORY_LABEL) },
                        leadingIcon = {
                            Icon(Icons.Default.Category, contentDescription = null, tint = AlertRed)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_expense_category"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = IncomeExpenseStrings.FIELD_ADD_NEW_CATEGORY,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue)
                            },
                            onClick = {
                                categoryExpanded = false
                                onAddNewCategory()
                            },
                            modifier = Modifier.testTag("item_add_new_category")
                        )
                    }
                }

                // Optional "কোন আয় থেকে?" Dropdown
                ExposedDropdownMenuBox(
                    expanded = linkedSourceExpanded,
                    onExpandedChange = { linkedSourceExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLinkedSourceName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(IncomeExpenseStrings.FIELD_LINKED_SOURCE_LABEL) },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = linkedSourceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_linked_income_source"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = linkedSourceExpanded,
                        onDismissRequest = { linkedSourceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(IncomeExpenseStrings.LINKED_SOURCE_NONE) },
                            onClick = {
                                linkedSourceId = null
                                linkedSourceExpanded = false
                            }
                        )
                        incomeSources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source.name) },
                                onClick = {
                                    linkedSourceId = source.id
                                    linkedSourceExpanded = false
                                }
                            )
                        }
                    }
                }

                // Tag Line (Max 60 chars) with Autocomplete Suggestions
                Column {
                    OutlinedTextField(
                        value = tagLine,
                        onValueChange = { if (it.length <= 60) tagLine = it },
                        label = { Text(IncomeExpenseStrings.FIELD_TAGLINE_LABEL) },
                        placeholder = { Text(IncomeExpenseStrings.FIELD_TAGLINE_HINT) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().testTag("input_expense_tagline"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Autocomplete suggestion chips if any
                    val matchingSuggestions = remember(tagLine, tagLineSuggestions) {
                        tagLineSuggestions.filter {
                            tagLine.isBlank() || (it.contains(tagLine.trim(), ignoreCase = true) && !it.equals(tagLine.trim(), ignoreCase = true))
                        }.take(4)
                    }

                    if (matchingSuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            matchingSuggestions.forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { tagLine = suggestion }
                                        .testTag("tag_suggestion_$suggestion")
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Date & Time Picker Chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDateTimePickerDialog = true }
                        .testTag("chip_expense_datetime")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = IncomeExpenseStrings.FIELD_DATE_TIME_LABEL,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${IncomeExpenseFormatter.formatDate(occurredAtTimestamp, useBengaliDigits)} • ${IncomeExpenseFormatter.formatTime(occurredAtTimestamp, useBengaliDigits)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Note Field
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(IncomeExpenseStrings.FIELD_NOTE_LABEL) },
                    placeholder = { Text(IncomeExpenseStrings.FIELD_NOTE_HINT) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_note"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPaisa = IncomeExpenseFormatter.parseInputToPaisa(amountText)
                    if (amountText.isBlank() || parsedPaisa == null) {
                        val cleaned = BengaliFormatter.normalizeToEnglishDigits(amountText).replace(",", "").replace("৳", "").trim()
                        val takaVal = cleaned.toDoubleOrNull()
                        if (cleaned.length > 9) {
                            amountError = IncomeExpenseStrings.ERROR_AMOUNT_MAX
                        } else if (takaVal != null && takaVal <= 0.0) {
                            amountError = IncomeExpenseStrings.ERROR_AMOUNT_ZERO
                        } else {
                            amountError = IncomeExpenseStrings.ERROR_AMOUNT_REQUIRED
                        }
                    } else if (parsedPaisa <= 0L) {
                        amountError = IncomeExpenseStrings.ERROR_AMOUNT_ZERO
                    } else if (selectedCategoryId <= 0L && categories.isNotEmpty()) {
                        selectedCategoryId = categories.first().id
                    } else {
                        onSave(
                            parsedPaisa,
                            selectedCategoryId,
                            occurredAtTimestamp,
                            tagLine.trim().ifBlank { null },
                            noteText.trim().ifBlank { null },
                            linkedSourceId
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_save_expense")
            ) {
                Text(IncomeExpenseStrings.BTN_SAVE, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_expense")
            ) {
                Text(IncomeExpenseStrings.BTN_CANCEL)
            }
        }
    )
}
