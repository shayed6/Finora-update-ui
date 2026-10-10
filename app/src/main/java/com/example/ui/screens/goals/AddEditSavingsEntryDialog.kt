package com.example.ui.screens.goals

import com.example.ui.components.FinoraDatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InvestmentSectorEntity
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsEntryType
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSavingsEntryDialog(
    existingEntry: SavingsEntryEntity? = null,
    initialType: SavingsEntryType = SavingsEntryType.DEPOSIT,
    sectors: List<InvestmentSectorEntity>,
    currentGoalSavedPaisa: Long,
    useBengaliDigits: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (type: SavingsEntryType, amountPaisa: Long, sectorId: Long, entryDate: Long, note: String?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddNewSector: () -> Unit
) {
    val isEditing = existingEntry != null

    var type by rememberSaveable {
        mutableStateOf(
            if (isEditing) {
                if (existingEntry?.type == SavingsEntryType.WITHDRAW.name) SavingsEntryType.WITHDRAW else SavingsEntryType.DEPOSIT
            } else {
                initialType
            }
        )
    }

    var amountText by rememberSaveable {
        mutableStateOf(
            if (existingEntry != null) {
                val taka = existingEntry.amount / 100L
                if (useBengaliDigits) BengaliFormatter.toBengaliDigits(taka.toString()) else taka.toString()
            } else ""
        )
    }

    var selectedSectorId by rememberSaveable {
        mutableStateOf(existingEntry?.sectorId ?: sectors.firstOrNull()?.id ?: 0L)
    }

    var entryDateTimestamp by rememberSaveable {
        mutableLongStateOf(existingEntry?.entryDate ?: System.currentTimeMillis())
    }

    var noteText by rememberSaveable {
        mutableStateOf(existingEntry?.note ?: "")
    }

    var amountError by rememberSaveable { mutableStateOf<String?>(null) }
    var sectorDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    var showDatePickerDialog by rememberSaveable { mutableStateOf(false) }

    if (showDatePickerDialog) {
        FinoraDatePickerDialog(
            initialDateMillis = entryDateTimestamp,
            allowFutureDates = false,
            onDismissRequest = { showDatePickerDialog = false },
            onDateSelected = { year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val selectedTime = newCal.timeInMillis
                if (selectedTime > System.currentTimeMillis() + 120_000L) {
                    amountError = SavingsStrings.ERROR_FUTURE_DATE
                } else {
                    entryDateTimestamp = selectedTime
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(SavingsStrings.CONFIRM_DELETE_ENTRY_TITLE, fontWeight = FontWeight.Bold) },
            text = { Text(SavingsStrings.CONFIRM_DELETE_ENTRY_MSG) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_entry")
                ) {
                    Text(SavingsStrings.BTN_DELETE, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(SavingsStrings.BTN_CANCEL)
                }
            }
        )
    }

    val selectedSectorName = sectors.firstOrNull { it.id == selectedSectorId }?.name ?: "খাত নির্বাচন করুন"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isEditing -> SavingsStrings.DIALOG_EDIT_ENTRY_TITLE
                        type == SavingsEntryType.DEPOSIT -> SavingsStrings.DIALOG_DEPOSIT_TITLE
                        else -> SavingsStrings.DIALOG_WITHDRAW_TITLE
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isEditing && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("btn_delete_entry")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AlertRed
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Large Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = { Text(SavingsStrings.FIELD_AMOUNT_LABEL) },
                    placeholder = { Text(SavingsStrings.FIELD_AMOUNT_HINT) },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it, color = AlertRed) } },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_entry_amount"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sector Dropdown with "+ নতুন খাত"
                ExposedDropdownMenuBox(
                    expanded = sectorDropdownExpanded,
                    onExpandedChange = { sectorDropdownExpanded = !sectorDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSectorName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(SavingsStrings.FIELD_SECTOR_LABEL) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectorDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("dropdown_entry_sector"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = sectorDropdownExpanded,
                        onDismissRequest = { sectorDropdownExpanded = false }
                    ) {
                        // Option: "+ নতুন খাত"
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(SavingsStrings.FIELD_ADD_NEW_SECTOR, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                sectorDropdownExpanded = false
                                onAddNewSector()
                            },
                            modifier = Modifier.testTag("item_add_new_sector")
                        )

                        sectors.forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec.name) },
                                onClick = {
                                    selectedSectorId = sec.id
                                    sectorDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date Picker
                Surface(
                    onClick = { showDatePickerDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_entry_date")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = SavingsStrings.FIELD_ENTRY_DATE,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SavingsFormatter.formatDate(entryDateTimestamp, useBengaliDigits),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Note (Optional)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(SavingsStrings.FIELD_NOTE_LABEL) },
                    placeholder = { Text(SavingsStrings.FIELD_NOTE_HINT) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_entry_note"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPaisa = SavingsFormatter.parseInputToPaisa(amountText)
                    if (amountText.isBlank() || parsedPaisa == null) {
                        val cleaned = BengaliFormatter.normalizeToEnglishDigits(amountText).replace(",", "").replace("৳", "").trim()
                        val takaVal = cleaned.toDoubleOrNull()
                        if (cleaned.length > 10) {
                            amountError = SavingsStrings.ERROR_AMOUNT_MAX
                        } else if (takaVal != null && takaVal <= 0.0) {
                            amountError = SavingsStrings.ERROR_AMOUNT_ZERO
                        } else {
                            amountError = SavingsStrings.ERROR_AMOUNT_REQUIRED
                        }
                        return@Button
                    }

                    if (parsedPaisa <= 0L) {
                        amountError = SavingsStrings.ERROR_AMOUNT_ZERO
                        return@Button
                    }

                    if (entryDateTimestamp > System.currentTimeMillis() + 120_000L) {
                        amountError = SavingsStrings.ERROR_FUTURE_DATE
                        return@Button
                    }

                    // Withdrawal limit check
                    if (type == SavingsEntryType.WITHDRAW) {
                        val currentLimit = if (isEditing && existingEntry?.type == SavingsEntryType.WITHDRAW.name) {
                            currentGoalSavedPaisa + existingEntry.amount
                        } else {
                            currentGoalSavedPaisa
                        }

                        if (parsedPaisa > currentLimit) {
                            amountError = SavingsStrings.ERROR_WITHDRAW_EXCEED
                            return@Button
                        }
                    }

                    val chosenSectorId = if (selectedSectorId <= 0L && sectors.isNotEmpty()) {
                        sectors.first().id
                    } else {
                        selectedSectorId
                    }

                    onSave(
                        type,
                        parsedPaisa,
                        chosenSectorId,
                        entryDateTimestamp,
                        noteText.trim().ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == SavingsEntryType.DEPOSIT) GrowthGreen else AlertRed
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_save_entry")
            ) {
                Text(SavingsStrings.BTN_SAVE, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_entry")
            ) {
                Text(SavingsStrings.BTN_CANCEL)
            }
        }
    )
}
