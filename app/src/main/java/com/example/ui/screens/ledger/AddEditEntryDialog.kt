package com.example.ui.screens.ledger

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerEntryType
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import java.util.Calendar

@Composable
fun AddEditEntryDialog(
    initialType: LedgerEntryType = LedgerEntryType.GAVE,
    existingEntry: LedgerEntryEntity? = null,
    useBengaliDigits: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (type: LedgerEntryType, amountPaisa: Long, note: String?, entryDate: Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isEditing = existingEntry != null

    var selectedType by rememberSaveable {
        mutableStateOf(
            if (isEditing) {
                if (existingEntry?.type == LedgerEntryType.RECEIVED.name) LedgerEntryType.RECEIVED else LedgerEntryType.GAVE
            } else {
                initialType
            }
        )
    }

    // Initial amount string
    var amountText by rememberSaveable {
        mutableStateOf(
            if (existingEntry != null) {
                val taka = existingEntry.amountPaisa / 100.0
                val formatted = if (taka % 1.0 == 0.0) taka.toLong().toString() else "%.2f".format(taka)
                if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
            } else ""
        )
    }

    var noteText by rememberSaveable { mutableStateOf(existingEntry?.note ?: "") }
    var entryDateTimestamp by rememberSaveable { mutableLongStateOf(existingEntry?.entryDate ?: System.currentTimeMillis()) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Date picker dialog launcher
    fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = entryDateTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                entryDateTimestamp = newCal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(LedgerStrings.CONFIRM_DELETE_ENTRY_TITLE, fontWeight = FontWeight.Bold) },
            text = { Text(LedgerStrings.CONFIRM_DELETE_ENTRY_MSG) },
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
                    Text(LedgerStrings.BTN_DELETE, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(LedgerStrings.BTN_CANCEL)
                }
            }
        )
    }

    val typeColor = if (selectedType == LedgerEntryType.GAVE) AlertRed else GrowthGreen
    val typeLabel = if (selectedType == LedgerEntryType.GAVE) LedgerStrings.BTN_GAVE else LedgerStrings.BTN_RECEIVED

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_add_edit_entry"),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) LedgerStrings.DIALOG_EDIT_ENTRY_TITLE else "টাকা $typeLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isEditing && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(36.dp).testTag("btn_delete_entry")
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Type selector tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val gaveSelected = selectedType == LedgerEntryType.GAVE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (gaveSelected) AlertRed else Color.Transparent)
                            .clickable { selectedType = LedgerEntryType.GAVE }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "দিলাম (GAVE)",
                            color = if (gaveSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (gaveSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }

                    val receivedSelected = selectedType == LedgerEntryType.RECEIVED
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (receivedSelected) GrowthGreen else Color.Transparent)
                            .clickable { selectedType = LedgerEntryType.RECEIVED }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "পেলাম (RECEIVED)",
                            color = if (receivedSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (receivedSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }

                // Large Amount Field
                Column {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            amountText = input
                            amountError = null
                        },
                        label = { Text(LedgerStrings.FIELD_AMOUNT_LABEL) },
                        placeholder = { Text(LedgerStrings.FIELD_AMOUNT_HINT) },
                        leadingIcon = {
                            Text(
                                text = "৳",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeColor,
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
                            amountError?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_entry_amount"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Note Field
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(LedgerStrings.FIELD_NOTE_LABEL) },
                    placeholder = { Text(LedgerStrings.FIELD_NOTE_HINT) },
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

                // Date Chip with DatePicker
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDatePicker() }
                        .testTag("chip_entry_date")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                text = LedgerStrings.FIELD_DATE_LABEL,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = LedgerFormatter.formatDate(entryDateTimestamp, useBengaliDigits),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPaisa = LedgerFormatter.parseInputToPaisa(amountText)
                    if (amountText.isBlank() || parsedPaisa == null) {
                        val cleaned = BengaliFormatter.normalizeToEnglishDigits(amountText).replace(",", "").replace("৳", "").trim()
                        val takaVal = cleaned.toDoubleOrNull()
                        if (cleaned.length > 9) {
                            amountError = LedgerStrings.ERROR_AMOUNT_MAX
                        } else if (takaVal != null && takaVal <= 0.0) {
                            amountError = LedgerStrings.ERROR_AMOUNT_ZERO
                        } else {
                            amountError = LedgerStrings.ERROR_AMOUNT_REQUIRED
                        }
                    } else if (parsedPaisa <= 0L) {
                        amountError = LedgerStrings.ERROR_AMOUNT_ZERO
                    } else {
                        onSave(selectedType, parsedPaisa, noteText.trim().ifBlank { null }, entryDateTimestamp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = typeColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_save_entry")
            ) {
                Text(LedgerStrings.BTN_SAVE, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_entry")
            ) {
                Text(LedgerStrings.BTN_CANCEL)
            }
        }
    )
}
