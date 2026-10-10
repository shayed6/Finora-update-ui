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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SavingsGoalEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import java.util.Calendar

@Composable
fun AddEditGoalDialog(
    goal: SavingsGoalEntity? = null,
    useBengaliDigits: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmountPaisa: Long?, targetDate: Long?) -> Unit
) {
    val isEditing = goal != null
    val isDefault = goal?.isDefault == true

    var name by rememberSaveable { mutableStateOf(goal?.name ?: "") }
    var targetAmountText by rememberSaveable {
        mutableStateOf(
            if (goal?.targetAmount != null) {
                val taka = goal.targetAmount / 100L
                if (useBengaliDigits) BengaliFormatter.toBengaliDigits(taka.toString()) else taka.toString()
            } else ""
        )
    }
    var targetDateTimestamp by rememberSaveable { mutableStateOf(goal?.targetDate) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var amountError by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePickerDialog by rememberSaveable { mutableStateOf(false) }

    if (showDatePickerDialog) {
        FinoraDatePickerDialog(
            initialDateMillis = targetDateTimestamp ?: System.currentTimeMillis(),
            allowFutureDates = true,
            onDismissRequest = { showDatePickerDialog = false },
            onDateSelected = { year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                targetDateTimestamp = newCal.timeInMillis
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) SavingsStrings.DIALOG_EDIT_GOAL_TITLE else SavingsStrings.DIALOG_ADD_GOAL_TITLE,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Goal Name (disabled if default goal)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(SavingsStrings.FIELD_GOAL_NAME) },
                    placeholder = { Text(SavingsStrings.FIELD_GOAL_NAME_HINT) },
                    enabled = !isDefault,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = AlertRed) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_goal_name"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Target Amount (Optional)
                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = {
                        targetAmountText = it
                        amountError = null
                    },
                    label = { Text(SavingsStrings.FIELD_TARGET_AMOUNT) },
                    placeholder = { Text(SavingsStrings.FIELD_TARGET_AMOUNT_HINT) },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it, color = AlertRed) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_goal_target_amount"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Target Date Picker (Optional)
                Surface(
                    onClick = { showDatePickerDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_pick_goal_date")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SavingsStrings.FIELD_TARGET_DATE,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = targetDateTimestamp?.let {
                                    SavingsFormatter.formatDate(it, useBengaliDigits)
                                } ?: SavingsStrings.FIELD_TARGET_DATE_NONE,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (targetDateTimestamp != null) {
                            IconButton(
                                onClick = { targetDateTimestamp = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        nameError = SavingsStrings.ERROR_NAME_REQUIRED
                        return@Button
                    }

                    var targetPaisa: Long? = null
                    if (targetAmountText.isNotBlank()) {
                        val parsed = SavingsFormatter.parseInputToPaisa(targetAmountText)
                        if (parsed == null || parsed <= 0L) {
                            amountError = SavingsStrings.ERROR_AMOUNT_ZERO
                            return@Button
                        }
                        targetPaisa = parsed
                    }

                    onSave(cleanName, targetPaisa, targetDateTimestamp)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_save_goal")
            ) {
                Text(SavingsStrings.BTN_SAVE, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_goal")
            ) {
                Text(SavingsStrings.BTN_CANCEL)
            }
        }
    )
}
