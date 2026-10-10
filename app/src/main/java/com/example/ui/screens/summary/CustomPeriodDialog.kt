package com.example.ui.screens.summary

import com.example.ui.components.FinoraDatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BengaliFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CustomPeriodDialog(
    initialStartMs: Long,
    initialEndMs: Long,
    title: String = SummaryStrings.DIALOG_CUSTOM_PERIOD_TITLE,
    useBengaliDigits: Boolean = true,
    onDismiss: () -> Unit,
    onApply: (startMs: Long, endMs: Long) -> Unit
) {
    val context = LocalContext.current
    var startMs by remember { mutableLongStateOf(initialStartMs) }
    var endMs by remember { mutableLongStateOf(initialEndMs) }

    fun formatDate(ms: Long): String {
        val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD"))
        val formatted = sdf.format(Date(ms))
        return if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun showPicker(isStart: Boolean) {
        val initialCal = Calendar.getInstance().apply {
            timeInMillis = if (isStart) startMs else endMs
        }
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    if (isStart) {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                }
                if (isStart) {
                    startMs = chosenCal.timeInMillis
                    if (startMs > endMs) endMs = startMs
                } else {
                    endMs = chosenCal.timeInMillis
                    if (endMs < startMs) startMs = endMs
                }
            },
            initialCal.get(Calendar.YEAR),
            initialCal.get(Calendar.MONTH),
            initialCal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Date Card
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPicker(true) }
                        .testTag("picker_start_date"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "শুরুর তারিখ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "শুরুর তারিখ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatDate(startMs), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // End Date Card
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPicker(false) }
                        .testTag("picker_end_date"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "শেষের তারিখ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "শেষের তারিখ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatDate(endMs), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(startMs, endMs)
                },
                modifier = Modifier.testTag("btn_apply_custom_period")
            ) {
                Text(SummaryStrings.BTN_APPLY)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(SummaryStrings.BTN_CANCEL)
            }
        }
    )
}
