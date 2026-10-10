package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar
import java.util.TimeZone

/**
 * Normalizes a timestamp to UTC midnight (00:00:00.000 UTC).
 * Material3 DatePicker REQUIRES initialSelectedDateMillis to be at UTC midnight,
 * otherwise it throws IllegalArgumentException and crashes the app.
 */
private fun normalizeToUtcMidnight(millis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return utcCal.timeInMillis
}

/**
 * 100% pure Jetpack Compose DatePicker dialog.
 * Replaces legacy View-based android.app.DatePickerDialog across the application.
 * Completely immune to WindowManager.BadTokenException and missing XML theme crashes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinoraDatePickerDialog(
    initialDateMillis: Long = System.currentTimeMillis(),
    allowFutureDates: Boolean = false,
    confirmButtonText: String = "ঠিক আছে",
    dismissButtonText: String = "বাতিল",
    onDismissRequest: () -> Unit,
    onDateSelected: (year: Int, month: Int, dayOfMonth: Int) -> Unit
) {
    // Normalize to UTC midnight — Material3 DatePicker crashes with IllegalArgumentException
    // if the initialSelectedDateMillis is not exactly at UTC midnight (00:00:00.000 UTC).
    val normalizedInitial = normalizeToUtcMidnight(initialDateMillis)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = normalizedInitial,
        selectableDates = if (!allowFutureDates) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Allow up to the current day's UTC midnight + 1 day to accommodate all timezones
                    return utcTimeMillis <= normalizeToUtcMidnight(System.currentTimeMillis()) + 86_400_000L
                }
            }
        } else {
            object : SelectableDates {}
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis ?: initialDateMillis
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = selectedMillis
                    }
                    val year = utcCal.get(Calendar.YEAR)
                    val month = utcCal.get(Calendar.MONTH)
                    val day = utcCal.get(Calendar.DAY_OF_MONTH)
                    onDateSelected(year, month, day)
                    onDismissRequest()
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
 * Step 2: User optionally picks or confirms Time.
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

    var selectedYear by remember {
        val cal = Calendar.getInstance().apply { timeInMillis = initialDateTimeMillis }
        mutableStateOf(cal.get(Calendar.YEAR))
    }
    var selectedMonth by remember {
        val cal = Calendar.getInstance().apply { timeInMillis = initialDateTimeMillis }
        mutableStateOf(cal.get(Calendar.MONTH))
    }
    var selectedDay by remember {
        val cal = Calendar.getInstance().apply { timeInMillis = initialDateTimeMillis }
        mutableStateOf(cal.get(Calendar.DAY_OF_MONTH))
    }

    val initialCal = remember(initialDateTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = initialDateTimeMillis }
    }

    if (!isTimeStep) {
        FinoraDatePickerDialog(
            initialDateMillis = initialDateTimeMillis,
            allowFutureDates = allowFutureDateTime,
            confirmButtonText = "পরবর্তী (সময়) →",
            dismissButtonText = "বাতিল",
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
            title = {
                Text(
                    text = "সময় নির্বাচন করুন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
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
                        val finalTime = if (!allowFutureDateTime && resultCal.timeInMillis > System.currentTimeMillis() + 60_000L) {
                            System.currentTimeMillis()
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
                TextButton(
                    onClick = {
                        // User can finish with just the picked date keeping current time
                        val resultCal = Calendar.getInstance().apply {
                            timeInMillis = initialDateTimeMillis
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, selectedDay)
                        }
                        onDateTimeSelected(resultCal.timeInMillis)
                        onDismissRequest()
                    }
                ) {
                    Text("এড়িয়ে যান")
                }
            }
        )
    }
}
