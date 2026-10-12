package com.example.ui.screens.summary

import com.example.ui.components.FinoraDatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var startMs by rememberSaveable { mutableLongStateOf(initialStartMs) }
    var endMs by rememberSaveable { mutableLongStateOf(initialEndMs) }

    // Which picker to show: null = none, true = start, false = end
    var showPickerForStart by rememberSaveable { mutableStateOf<Boolean?>(null) }

    fun formatDate(ms: Long): String {
        return BengaliFormatter.formatBangladeshDate(ms, useBengaliDigits)
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
                        .clickable { showPickerForStart = true }
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
                        .clickable { showPickerForStart = false }
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
