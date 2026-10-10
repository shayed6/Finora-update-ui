package com.example.ui.screens.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BengaliFormatter
import java.util.Locale

val SummaryGreen = Color(0xFF19C77A)
val SummaryRed = Color(0xFFEF5350)
val SummaryBlue = Color(0xFF1769FF)
val SummaryGray = Color(0xFF78909C)

@Composable
fun SummaryPieChartView(
    summary: PeriodSummary,
    useBengaliDigits: Boolean = true,
    modifier: Modifier = Modifier
) {
    val income = summary.incomePaisa
    val expense = summary.expensePaisa
    val savings = summary.savingsPaisa
    val remaining = summary.remainingPaisa

    // Determine state for pie slices
    val isNoIncome = income <= 0L
    val isExpenseAndSavingsExceed = (expense + savings) > income && income > 0L
    val isSavingsNegative = savings < 0L

    val showNote = isNoIncome || isExpenseAndSavingsExceed || isSavingsNegative
    val noteText = when {
        isNoIncome -> SummaryStrings.PIE_NOTE_NO_INCOME
        isExpenseAndSavingsExceed || isSavingsNegative -> SummaryStrings.PIE_NOTE_EXCEEDS
        else -> null
    }

    // Prepare slices (only positive values)
    data class Slice(val label: String, val value: Long, val color: Color, val percentOfIncome: Double?)

    val slices = mutableListOf<Slice>()
    if (income > 0L && !isExpenseAndSavingsExceed && savings >= 0L) {
        // Normal case: expense, savings, remaining
        val expPct = (expense.toDouble() / income.toDouble()) * 100.0
        val savPct = (savings.toDouble() / income.toDouble()) * 100.0
        val remPct = (remaining.toDouble() / income.toDouble()) * 100.0
        if (expense > 0) slices.add(Slice(SummaryStrings.PIE_EXPENSE, expense, SummaryRed, expPct))
        if (savings > 0) slices.add(Slice(SummaryStrings.PIE_SAVINGS, savings, SummaryBlue, savPct))
        if (remaining > 0) slices.add(Slice(SummaryStrings.PIE_REMAINING, remaining, SummaryGray, remPct))
    } else {
        // Positive parts only: expense and positive savings
        if (expense > 0) {
            val pct = if (income > 0) (expense.toDouble() / income.toDouble()) * 100.0 else null
            slices.add(Slice(SummaryStrings.PIE_EXPENSE, expense, SummaryRed, pct))
        }
        if (savings > 0) {
            val pct = if (income > 0) (savings.toDouble() / income.toDouble()) * 100.0 else null
            slices.add(Slice(SummaryStrings.PIE_SAVINGS, savings, SummaryBlue, pct))
        }
    }

    val totalSliceValue = slices.sumOf { it.value }.coerceAtLeast(1L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_pie_chart_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (slices.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কোনো হিসাব নেই",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Draw Pie Chart
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .padding(12.dp)
                    .testTag("pie_chart_canvas")
            ) {
                val strokeWidth = 36.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val topLeft = Offset((size.width - 2 * radius) / 2f, (size.height - 2 * radius) / 2f)
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f
                for (slice in slices) {
                    val sweepAngle = (slice.value.toFloat() / totalSliceValue.toFloat()) * 360f
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (slice in slices) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(slice.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = slice.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val amountStr = SummaryFormatter.formatPaisa(slice.value, useBengaliDigits)
                        val pctText = if (slice.percentOfIncome != null) {
                            val formattedPct = String.format(Locale.US, "%.1f%%", slice.percentOfIncome)
                            val bnPct = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formattedPct) else formattedPct
                            "$bnPct ($amountStr)"
                        } else {
                            amountStr
                        }

                        Text(
                            text = pctText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Small note if present
        if (showNote && noteText != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("pie_chart_note")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "তথ্য",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryLineChartView(
    trendPoints: List<MonthlyTrendPoint>,
    useBengaliDigits: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (trendPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "লাইন চার্টের পর্যাপ্ত তথ্য নেই",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(trendPoints.lastIndex) }

    // Find min and max for scaling
    val allValues = trendPoints.flatMap { listOf(it.incomePaisa, it.expensePaisa, it.savingsPaisa) }
    val maxVal = allValues.maxOrNull()?.coerceAtLeast(100L) ?: 100L
    val minVal = allValues.minOrNull()?.coerceAtMost(0L) ?: 0L
    val rangeVal = (maxVal - minVal).coerceAtLeast(1L).toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_line_chart_container")
    ) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(label = "আয়", color = SummaryGreen)
            LegendItem(label = "ব্যয়", color = SummaryRed)
            LegendItem(label = "সঞ্চয়", color = SummaryBlue)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Line Chart Canvas
        val primaryColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 24.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(trendPoints) {
                        detectTapGestures { offset ->
                            val pointSpacing = size.width / (trendPoints.size - 1).coerceAtLeast(1)
                            val idx = ((offset.x + pointSpacing / 2) / pointSpacing).toInt()
                            if (idx in trendPoints.indices) {
                                selectedIndex = idx
                            }
                        }
                    }
                    .testTag("line_chart_canvas")
            ) {
                val n = trendPoints.size
                if (n < 2) return@Canvas

                val w = size.width
                val h = size.height
                val topPad = 16f
                val bottomPad = 24f
                val chartH = h - topPad - bottomPad

                fun getY(value: Long): Float {
                    val normalized = (value - minVal) / rangeVal
                    return topPad + chartH * (1f - normalized)
                }

                fun getX(index: Int): Float {
                    return index * (w / (n - 1))
                }

                // Zero line if minVal < 0
                if (minVal < 0L) {
                    val zeroY = getY(0L)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Grid lines (3 horizontal guides)
                listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                    val y = topPad + chartH * fraction
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw 3 lines: Income, Expense, Savings
                fun drawSeries(values: List<Long>, color: Color) {
                    val path = Path()
                    values.forEachIndexed { idx, v ->
                        val x = getX(idx)
                        val y = getY(v)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw points
                    values.forEachIndexed { idx, v ->
                        val x = getX(idx)
                        val y = getY(v)
                        val isSelected = (idx == selectedIndex)
                        drawCircle(
                            color = color,
                            radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        if (isSelected) {
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                drawSeries(trendPoints.map { it.incomePaisa }, SummaryGreen)
                drawSeries(trendPoints.map { it.expensePaisa }, SummaryRed)
                drawSeries(trendPoints.map { it.savingsPaisa }, SummaryBlue)

                // Draw vertical line for selected point
                if (selectedIndex in trendPoints.indices) {
                    val selX = getX(selectedIndex)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.35f),
                        start = Offset(selX, topPad),
                        end = Offset(selX, h - bottomPad),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }

        // Month Labels on X-Axis
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trendPoints.forEachIndexed { idx, pt ->
                Text(
                    text = pt.monthLabel,
                    fontSize = 10.sp,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { selectedIndex = idx }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected point values card
        val selectedPoint = trendPoints.getOrNull(selectedIndex) ?: trendPoints.last()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("line_chart_selected_details"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "মাস: ${selectedPoint.monthLabel}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "আয়", fontSize = 11.sp, color = SummaryGreen, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = SummaryFormatter.formatPaisa(selectedPoint.incomePaisa, useBengaliDigits),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(text = "ব্যয়", fontSize = 11.sp, color = SummaryRed, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = SummaryFormatter.formatPaisa(selectedPoint.expensePaisa, useBengaliDigits),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(text = "সঞ্চয়", fontSize = 11.sp, color = SummaryBlue, fontWeight = FontWeight.SemiBold)
                        val savSign = if (selectedPoint.savingsPaisa < 0) "-" else ""
                        val savFormatted = SummaryFormatter.formatPaisa(kotlin.math.abs(selectedPoint.savingsPaisa), useBengaliDigits)
                        Text(
                            text = if (selectedPoint.savingsPaisa < 0) "$savSign$savFormatted" else savFormatted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
