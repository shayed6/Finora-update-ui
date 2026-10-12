package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import java.util.Calendar
import java.util.TimeZone

/**
 * Normalizes a timestamp by extracting its local year, month, and day,
 * and mapping it to UTC midnight (00:00:00.000 UTC).
 * This ensures that Material3 DatePicker displays the exact same calendar date
 * as local time in any timezone, avoiding off-by-one date shifts.
 */
private fun normalizeToUtcMidnight(millis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = millis }
    val y = localCal.get(Calendar.YEAR)
    val m = localCal.get(Calendar.MONTH)
    val d = localCal.get(Calendar.DAY_OF_MONTH)
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(y, m, d, 0, 0, 0)
    }
    return utcCal.timeInMillis
}

/**
 * 100% pure Jetpack Compose DatePicker dialog.
 * Replaces legacy View-based android.app.DatePickerDialog across the application.
 * Completely immune to WindowManager.BadTokenException and timezone shifts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinoraDatePickerDialog(
    initialDateMillis: Long = System.currentTimeMillis(),
    allowFutureDates: Boolean = false,
    confirmButtonText: String = "ঠিক আছে",
    dismissButtonText: String = "বাতিল",
    dismissOnConfirm: Boolean = true,
    onDismissRequest: () -> Unit,
    onDateSelected: (year: Int, month: Int, dayOfMonth: Int) -> Unit
) {
    val safeInitialDateMillis = remember(initialDateMillis) {
        normalizeToUtcMidnight(initialDateMillis)
    }

    val todayUtcMidnight = remember {
        normalizeToUtcMidnight(System.currentTimeMillis())
    }

    val selectableDates = remember(allowFutureDates, safeInitialDateMillis, todayUtcMidnight) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (allowFutureDates) return true
                // Always allow the initial selected date to avoid IllegalArgumentException in rememberDatePickerState
                if (utcTimeMillis == safeInitialDateMillis) return true
                // Do not allow tomorrow or future dates if allowFutureDates is false
                return utcTimeMillis <= todayUtcMidnight
            }

            override fun isSelectableYear(year: Int): Boolean = true
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = safeInitialDateMillis,
        selectableDates = selectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val selectedMillis = datePickerState.selectedDateMillis ?: safeInitialDateMillis
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = selectedMillis
                        }
                        val year = utcCal.get(Calendar.YEAR)
                        val month = utcCal.get(Calendar.MONTH)
                        val day = utcCal.get(Calendar.DAY_OF_MONTH)
                        onDateSelected(year, month, day)
                    } catch (e: Exception) {
                        val localCal = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
                        onDateSelected(
                            localCal.get(Calendar.YEAR),
                            localCal.get(Calendar.MONTH),
                            localCal.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    if (dismissOnConfirm) {
                        onDismissRequest()
                    }
                },
                modifier = Modifier.testTag("btn_confirm_date_picker")
            ) {
                Text(confirmButtonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("btn_dismiss_date_picker")
            ) {
                Text(dismissButtonText)
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * Combined Date & Time Picker Dialog in pure Jetpack Compose.
 * Step 1: User picks Date.
 * Step 2: User confirms or selects exact Time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinoraDateTimePickerDialog(
    initialDateTimeMillis: Long = System.currentTimeMillis(),
    allowFutureDateTime: Boolean = false,
    onDismissRequest: () -> Unit,
    onDateTimeSelected: (timestampMillis: Long) -> Unit
) {
    var isTimeStep by remember { mutableStateOf(false) }

    val initialCal = remember(initialDateTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = initialDateTimeMillis }
    }

    var selectedYear by remember {
        mutableIntStateOf(initialCal.get(Calendar.YEAR))
    }
    var selectedMonth by remember {
        mutableIntStateOf(initialCal.get(Calendar.MONTH))
    }
    var selectedDay by remember {
        mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH))
    }

    if (!isTimeStep) {
        FinoraDatePickerDialog(
            initialDateMillis = initialDateTimeMillis,
            allowFutureDates = allowFutureDateTime,
            confirmButtonText = "পরবর্তী (সময় নির্ধারণ) →",
            dismissButtonText = "বাতিল",
            dismissOnConfirm = false,
            onDismissRequest = onDismissRequest,
            onDateSelected = { year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth
                isTimeStep = true
            }
        )
    } else {
        val timePickerState = rememberTimePickerState(
            initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCal.get(Calendar.MINUTE),
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "সময় নির্বাচন করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { isTimeStep = false },
                        modifier = Modifier.testTag("btn_back_to_date_picker")
                    ) {
                        Text("← তারিখ পরিবর্তন", fontSize = 12.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val formattedDateStr = BengaliFormatter.toBengaliDigits(
                        "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "নির্বাচিত তারিখ: $formattedDateStr",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TimeInput(
                        state = timePickerState,
                        modifier = Modifier.testTag("time_input_picker")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val resultCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, selectedDay)
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val now = System.currentTimeMillis()
                        val finalTime = if (!allowFutureDateTime && resultCal.timeInMillis > now) {
                            now
                        } else {
                            resultCal.timeInMillis
                        }
                        onDateTimeSelected(finalTime)
                        onDismissRequest()
                    },
                    modifier = Modifier.testTag("btn_confirm_time_picker")
                ) {
                    Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            val resultCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                set(Calendar.DAY_OF_MONTH, selectedDay)
                                set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val currentTime = System.currentTimeMillis()
                            val finalTime = if (!allowFutureDateTime && resultCal.timeInMillis > currentTime) {
                                currentTime
                            } else {
                                resultCal.timeInMillis
                            }
                            onDateTimeSelected(finalTime)
                            onDismissRequest()
                        }
                    ) {
                        Text("বর্তমান সময়")
                    }

                    TextButton(onClick = onDismissRequest) {
                        Text("বাতিল")
                    }
                }
            }
        )
    }
}
