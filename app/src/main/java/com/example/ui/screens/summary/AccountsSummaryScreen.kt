package com.example.ui.screens.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BengaliFormatter

@Composable
fun AccountsSummaryScreen(
    viewModel: AccountsSummaryViewModel,
    useBengaliDigits: Boolean = true,
    onOpenLedger: () -> Unit,
    onOpenCycleSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showMainPeriodDialog by remember { mutableStateOf(false) }
    var customSourceIdForDialog by remember { mutableStateOf<Long?>(null) }

    if (showMainPeriodDialog) {
        CustomPeriodDialog(
            initialStartMs = uiState.period.startTimestamp,
            initialEndMs = uiState.period.endTimestamp,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showMainPeriodDialog = false },
            onApply = { startMs, endMs ->
                val newPeriod = PeriodRange(
                    startTimestamp = startMs,
                    endTimestamp = endMs,
                    label = SummaryDateFormatter.formatPeriodLabel(startMs, endMs, useBengaliDigits),
                    isCustom = true,
                    calendarMonthYear = null
                )
                viewModel.setPeriod(newPeriod)
                showMainPeriodDialog = false
            }
        )
    }

    customSourceIdForDialog?.let { srcId ->
        val card = uiState.sourceCards.find { it.sourceId == srcId }
        val start = card?.currentPeriod?.startTimestamp ?: uiState.period.startTimestamp
        val end = card?.currentPeriod?.endTimestamp ?: uiState.period.endTimestamp
        CustomPeriodDialog(
            initialStartMs = start,
            initialEndMs = end,
            title = "${card?.sourceName ?: ""} - সময়কাল নির্বাচন",
            useBengaliDigits = useBengaliDigits,
            onDismiss = { customSourceIdForDialog = null },
            onApply = { startMs, endMs ->
                val newPeriod = PeriodRange(
                    startTimestamp = startMs,
                    endTimestamp = endMs,
                    label = SummaryDateFormatter.formatPeriodLabel(startMs, endMs, useBengaliDigits),
                    isCustom = true,
                    calendarMonthYear = null
                )
                viewModel.setSourceCustomPeriod(srcId, newPeriod)
                customSourceIdForDialog = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_accounts_summary")
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Period Header with Arrows
        item {
            PeriodHeader(
                periodLabel = uiState.period.label,
                onPreviousClick = { viewModel.stepMainPeriod(false) },
                onNextClick = { viewModel.stepMainPeriod(true) },
                onLabelClick = { showMainPeriodDialog = true }
            )
        }

        // 2. Four Summary Cards
        item {
            SummaryCardsGrid(
                summary = uiState.summary,
                useBengaliDigits = useBengaliDigits
            )
        }

        // 3. Toggle "পাই | লাইন" and Chart Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_chart_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Chart Type Toggle
                    TabRow(
                        selectedTabIndex = if (uiState.chartType == ChartType.PIE) 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("toggle_chart_type")
                    ) {
                        Tab(
                            selected = uiState.chartType == ChartType.PIE,
                            onClick = { viewModel.setChartType(ChartType.PIE) },
                            text = {
                                Text(
                                    text = SummaryStrings.TOGGLE_PIE,
                                    fontWeight = if (uiState.chartType == ChartType.PIE) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_chart_pie")
                        )
                        Tab(
                            selected = uiState.chartType == ChartType.LINE,
                            onClick = { viewModel.setChartType(ChartType.LINE) },
                            text = {
                                Text(
                                    text = SummaryStrings.TOGGLE_LINE,
                                    fontWeight = if (uiState.chartType == ChartType.LINE) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_chart_line")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (uiState.chartType == ChartType.PIE) {
                        SummaryPieChartView(
                            summary = uiState.summary,
                            useBengaliDigits = useBengaliDigits
                        )
                    } else {
                        SummaryLineChartView(
                            trendPoints = uiState.sixMonthsTrend,
                            useBengaliDigits = useBengaliDigits
                        )
                    }
                }
            }
        }

        // 4. Empty State if no transactions in selected period
        if (uiState.summary.totalTransactionsCount == 0 &&
            uiState.summary.incomePaisa == 0L &&
            uiState.summary.expensePaisa == 0L &&
            uiState.summary.savingsPaisa == 0L
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_empty_state_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = SummaryStrings.EMPTY_PERIOD_TITLE,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = SummaryStrings.EMPTY_PERIOD_MSG,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 5. Separate Card "বাকির খাতা"
        item {
            BakirKhataCard(
                ledgerOverview = uiState.ledgerOverview,
                useBengaliDigits = useBengaliDigits,
                onClick = onOpenLedger
            )
        }

        // 6. Section "আয়ের ধরন অনুযায়ী"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SummaryStrings.SECTION_BY_INCOME_SOURCE,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = onOpenCycleSettings,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_cycle_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = SummaryStrings.BTN_CYCLE_SETTINGS,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Source Cards (One per active income source)
        if (uiState.sourceCards.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "লেনদেনকৃত কোনো আয়ের উৎস পাওয়া যায়নি",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(uiState.sourceCards, key = { it.sourceId }) { cardData ->
                IncomeSourceCycleCard(
                    card = cardData,
                    useBengaliDigits = useBengaliDigits,
                    onPreviousCycle = { viewModel.stepSourceCycle(cardData.sourceId, false) },
                    onNextCycle = { viewModel.stepSourceCycle(cardData.sourceId, true) },
                    onSelectCustomRange = { customSourceIdForDialog = cardData.sourceId }
                )
            }
        }

        // Ample bottom spacer to ensure clearance from bottom banner ad
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun PeriodHeader(
    periodLabel: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onLabelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("summary_period_header"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.testTag("btn_period_previous")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "পূর্ববর্তী সময়",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onLabelClick() }
                    .padding(vertical = 8.dp)
                    .testTag("period_label_clickable"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(
                onClick = onNextClick,
                modifier = Modifier.testTag("btn_period_next")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "পরবর্তী সময়",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SummaryCardsGrid(
    summary: PeriodSummary,
    useBengaliDigits: Boolean
) {
    val remainingColor = if (summary.remainingPaisa < 0L) SummaryRed else SummaryGray

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // আয় Card (Green #19C77A)
            SummaryMetricCard(
                title = SummaryStrings.CARD_INCOME,
                amountPaisa = summary.incomePaisa,
                accentColor = SummaryGreen,
                useBengaliDigits = useBengaliDigits,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_summary_income")
            )

            // ব্যয় Card (Red #EF5350)
            SummaryMetricCard(
                title = SummaryStrings.CARD_EXPENSE,
                amountPaisa = summary.expensePaisa,
                accentColor = SummaryRed,
                useBengaliDigits = useBengaliDigits,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_summary_expense")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // সঞ্চয় Card (Blue #1769FF)
            SummaryMetricCard(
                title = SummaryStrings.CARD_SAVINGS,
                amountPaisa = summary.savingsPaisa,
                accentColor = SummaryBlue,
                useBengaliDigits = useBengaliDigits,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_summary_savings")
            )

            // অবশিষ্ট Card (Gray; Red if negative)
            SummaryMetricCard(
                title = SummaryStrings.CARD_REMAINING,
                amountPaisa = summary.remainingPaisa,
                accentColor = remainingColor,
                useBengaliDigits = useBengaliDigits,
                modifier = Modifier
                    .weight(1f)
                    .testTag("card_summary_remaining")
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    amountPaisa: Long,
    accentColor: Color,
    useBengaliDigits: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            val sign = if (amountPaisa < 0L) "-" else ""
            val absVal = kotlin.math.abs(amountPaisa)
            val formatted = SummaryFormatter.formatPaisa(absVal, useBengaliDigits)
            val fullAmount = if (amountPaisa < 0L) "$sign$formatted" else formatted

            Text(
                text = fullAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
private fun BakirKhataCard(
    ledgerOverview: LedgerOverview,
    useBengaliDigits: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_bakir_khata"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "বাকির খাতা",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SummaryStrings.SECTION_LEDGER_TITLE,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "খুলুন →",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // মোট আমি পাবো
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = SummaryStrings.LEDGER_PABO,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = SummaryFormatter.formatPaisa(ledgerOverview.totalPaboPaisa, useBengaliDigits),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SummaryGreen
                        )
                    }
                }

                // মোট আমি দিবো
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = SummaryStrings.LEDGER_DEBO,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = SummaryFormatter.formatPaisa(ledgerOverview.totalDeboPaisa, useBengaliDigits),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SummaryRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeSourceCycleCard(
    card: IncomeSourceCardData,
    useBengaliDigits: Boolean,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
    onSelectCustomRange: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("source_card_${card.sourceName}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Source Name & Cycle Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.sourceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousCycle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "পূর্ববর্তী চক্র",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = card.currentPeriod.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onSelectCustomRange() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    IconButton(
                        onClick = onNextCycle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "পরবর্তী চক্র",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Three values: আয়, ট্যাগ করা ব্যয়, অবশিষ্ট
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = SummaryStrings.CARD_SOURCE_INCOME,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SummaryFormatter.formatPaisa(card.incomePaisa, useBengaliDigits),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SummaryGreen
                    )
                }

                Column {
                    Text(
                        text = SummaryStrings.CARD_SOURCE_TAGGED_EXPENSE,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SummaryFormatter.formatPaisa(card.taggedExpensePaisa, useBengaliDigits),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SummaryRed
                    )
                }

                Column {
                    Text(
                        text = SummaryStrings.CARD_SOURCE_REMAINING,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val remColor = if (card.remainingPaisa < 0L) SummaryRed else SummaryGray
                    val remSign = if (card.remainingPaisa < 0L) "-" else ""
                    val absRem = kotlin.math.abs(card.remainingPaisa)
                    val remFormatted = SummaryFormatter.formatPaisa(absRem, useBengaliDigits)
                    Text(
                        text = if (card.remainingPaisa < 0L) "$remSign$remFormatted" else remFormatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = remColor
                    )
                }
            }
        }
    }
}
