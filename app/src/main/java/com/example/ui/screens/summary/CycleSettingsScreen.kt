package com.example.ui.screens.summary

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleSettingsScreen(
    viewModel: AccountsSummaryViewModel,
    useBengaliDigits: Boolean = true,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val sources by viewModel.allIncomeSources.collectAsState()

    // Map of sourceId -> selected startDay (null = calendar month, 1..28)
    val selectedDays = remember(sources) {
        mutableStateMapOf<Long, Int?>().apply {
            sources.forEach { put(it.id, it.startDay) }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_cycle_settings"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = SummaryStrings.TITLE_CYCLE_SETTINGS,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SummaryStrings.SUBTITLE_CYCLE_SETTINGS,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_cycle_settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cycle_settings_bottom_bar"),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            // Save all updated start days
                            sources.forEach { src ->
                                val day = selectedDays[src.id]
                                if (day != src.startDay) {
                                    viewModel.updateIncomeSourceStartDay(src.id, day)
                                }
                            }
                            Toast.makeText(context, SummaryStrings.TOAST_CYCLE_SAVED, Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_cycles"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = SummaryStrings.BTN_SAVE,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = SummaryStrings.CYCLE_SETTINGS_HINT,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            items(sources, key = { it.id }) { src ->
                val currentDay = selectedDays[src.id]
                SourceCycleRowItem(
                    sourceName = src.name,
                    selectedDay = currentDay,
                    useBengaliDigits = useBengaliDigits,
                    onDaySelected = { newDay ->
                        selectedDays[src.id] = newDay
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SourceCycleRowItem(
    sourceName: String,
    selectedDay: Int?,
    useBengaliDigits: Boolean,
    onDaySelected: (Int?) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    fun toBn(str: String): String = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(str) else str

    val exampleText = if (selectedDay == null) {
        "উদাহরণ: ১ সেপ্টেম্বর – ৩০ সেপ্টেম্বর (ক্যালেন্ডার মাস)"
    } else {
        val endDay = if (selectedDay == 1) 30 else selectedDay - 1
        "উদাহরণ: ${toBn(selectedDay.toString())} সেপ্টেম্বর – ${toBn(endDay.toString())} অক্টোবর"
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cycle_source_item_$sourceName"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = sourceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Selector Box
            Box {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true }
                        .testTag("picker_cycle_day_${sourceName}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            val displayText = if (selectedDay == null) {
                                SummaryStrings.CYCLE_CALENDAR_DEFAULT
                            } else {
                                "${toBn(selectedDay.toString())} তারিখ হতে শুরু"
                            }
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "মেনু")
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(SummaryStrings.CYCLE_CALENDAR_DEFAULT) },
                        onClick = {
                            onDaySelected(null)
                            dropdownExpanded = false
                        }
                    )
                    (1..28).forEach { day ->
                        val dayStr = "${toBn(day.toString())} তারিখ (${toBn(day.toString())} হতে পরবর্তী মাসের ${toBn((if (day == 1) 30 else day - 1).toString())})"
                        DropdownMenuItem(
                            text = { Text(dayStr) },
                            onClick = {
                                onDaySelected(day)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Example text under the picker
            Text(
                text = exampleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
