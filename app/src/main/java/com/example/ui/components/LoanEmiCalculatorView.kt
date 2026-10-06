package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import kotlin.math.pow
import kotlin.math.roundToInt

data class EmiYearlySchedule(
    val year: Int,
    val principalPaid: Double,
    val interestPaid: Double,
    val totalPaid: Double,
    val remainingBalance: Double
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoanEmiCalculatorView(
    useBengaliDigits: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Interactive States
    var loanAmount by remember { mutableDoubleStateOf(1000000.0) } // Default 10 Lakh Taka
    var annualInterestRate by remember { mutableDoubleStateOf(9.5) } // Default 9.5%
    var tenureYears by remember { mutableIntStateOf(5) } // Default 5 years
    var isTenureInMonths by remember { mutableStateOf(false) }

    // Text representation for direct editing
    var amountInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("1000000") else "1000000")
    }
    var rateInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("9.5") else "9.5")
    }
    var tenureInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("5") else "5")
    }

    var showAmortizationSchedule by remember { mutableStateOf(false) }

    fun updateAmount(newVal: Double) {
        loanAmount = newVal
        amountInputText = if (useBengaliDigits) {
            BengaliFormatter.toBengaliDigits(newVal.roundToInt().toString())
        } else {
            newVal.roundToInt().toString()
        }
    }

    fun updateRate(newRate: Double) {
        annualInterestRate = newRate
        val formatted = if (newRate % 1.0 == 0.0) newRate.toInt().toString() else "%.2f".format(newRate)
        rateInputText = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun updateTenure(newTenure: Int) {
        tenureYears = newTenure
        tenureInputText = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(newTenure.toString()) else newTenure.toString()
    }

    // EMI Calculation Engine
    val emiCalculation by remember(loanAmount, annualInterestRate, tenureYears, isTenureInMonths) {
        derivedStateOf {
            val totalMonths = if (isTenureInMonths) tenureYears else tenureYears * 12
            val monthlyRate = (annualInterestRate / 12.0) / 100.0

            if (loanAmount <= 0.0 || totalMonths <= 0) {
                Triple(0.0, 0.0, 0.0)
            } else if (monthlyRate <= 0.0) {
                val emi = loanAmount / totalMonths
                val totalPayment = loanAmount
                val totalInterest = 0.0
                Triple(emi, totalInterest, totalPayment)
            } else {
                val compound = (1.0 + monthlyRate).pow(totalMonths.toDouble())
                val emi = (loanAmount * monthlyRate * compound) / (compound - 1.0)
                val totalPayment = emi * totalMonths
                val totalInterest = totalPayment - loanAmount
                Triple(emi, totalInterest, totalPayment)
            }
        }
    }

    val (monthlyEmi, totalInterestPayable, totalPayment) = emiCalculation

    // Amortization Table Engine
    val amortizationSchedule by remember(loanAmount, annualInterestRate, tenureYears, isTenureInMonths, monthlyEmi) {
        derivedStateOf {
            val totalMonths = if (isTenureInMonths) tenureYears else tenureYears * 12
            val monthlyRate = (annualInterestRate / 12.0) / 100.0
            val schedule = mutableListOf<EmiYearlySchedule>()

            if (loanAmount > 0.0 && totalMonths > 0 && monthlyEmi > 0.0) {
                var currentBalance = loanAmount
                val totalYears = (totalMonths + 11) / 12

                for (y in 1..totalYears) {
                    var yearPrincipal = 0.0
                    var yearInterest = 0.0
                    val monthsInThisYear = if (y == totalYears && totalMonths % 12 != 0) totalMonths % 12 else 12

                    for (m in 1..monthsInThisYear) {
                        if (currentBalance <= 0.0) break
                        val interestForMonth = currentBalance * monthlyRate
                        val principalForMonth = monthlyEmi - interestForMonth
                        yearInterest += interestForMonth
                        yearPrincipal += principalForMonth
                        currentBalance -= principalForMonth
                        if (currentBalance < 0.0) currentBalance = 0.0
                    }

                    schedule.add(
                        EmiYearlySchedule(
                            year = y,
                            principalPaid = yearPrincipal,
                            interestPaid = yearInterest,
                            totalPaid = yearPrincipal + yearInterest,
                            remainingBalance = currentBalance
                        )
                    )
                }
            }
            schedule
        }
    }

    // Share & Copy Summary
    fun copySummary() {
        val totalMonths = if (isTenureInMonths) tenureYears else tenureYears * 12
        val summary = buildString {
            append("📌 Finora Loan EMI হিসাব বিবরণী:\n")
            append("• আসল ঋণের পরিমাণ: ${BengaliFormatter.formatTaka(loanAmount, useBengaliDigits)}\n")
            append("• বার্ষিক সুদের হার: ${BengaliFormatter.formatPercent(annualInterestRate, useBengaliDigits)}\n")
            append("• পরিশোধের মেয়াদ: ${if (isTenureInMonths) "$tenureYears মাস" else "$tenureYears বছর ($totalMonths মাস)"}\n")
            append("--------------------------------\n")
            append("• মাসিক কিস্তি (EMI): ${BengaliFormatter.formatTaka(monthlyEmi, useBengaliDigits)}\n")
            append("• মোট প্রদেয় সুদ: ${BengaliFormatter.formatTaka(totalInterestPayable, useBengaliDigits)}\n")
            append("• মোট পরিশোধযোগ্য অর্থ: ${BengaliFormatter.formatTaka(totalPayment, useBengaliDigits)}\n")
            append("\nস্মার্ট আর্থিক হিসাবের জন্য Finora ব্যবহার করুন।")
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Finora Loan EMI", summary))
        Toast.makeText(context, "হিসাবের বিবরণী ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
    }

    fun shareSummary() {
        val totalMonths = if (isTenureInMonths) tenureYears else tenureYears * 12
        val text = "Finora Loan EMI হিসাব:\nঋণের পরিমাণ: ${BengaliFormatter.formatTaka(loanAmount, useBengaliDigits)}\nসুদের হার: ${annualInterestRate}%\nমেয়াদ: ${if (isTenureInMonths) "$tenureYears মাস" else "$tenureYears বছর"}\n👉 মাসিক কিস্তি (EMI): ${BengaliFormatter.formatTaka(monthlyEmi, useBengaliDigits)}\nমোট সুদ: ${BengaliFormatter.formatTaka(totalInterestPayable, useBengaliDigits)}\nমোট পরিশোধ: ${BengaliFormatter.formatTaka(totalPayment, useBengaliDigits)}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Loan EMI রিপোর্ট শেয়ার করুন"))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("loan_emi_calculator_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Result Card: Monthly EMI
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("emi_hero_result_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = PrimaryBlue.copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "মাসিক কিস্তি (Monthly EMI)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = BengaliFormatter.formatTaka(monthlyEmi, useBengaliDigits),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("text_monthly_emi_value")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Grid: Principal vs Interest vs Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "আসল ঋণ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = BengaliFormatter.formatTaka(loanAmount, useBengaliDigits),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "মোট সুদ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD67800)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = BengaliFormatter.formatTaka(totalInterestPayable, useBengaliDigits),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD67800),
                            modifier = Modifier.testTag("text_total_interest_value")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "মোট প্রদেয় অর্থ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = BengaliFormatter.formatTaka(totalPayment, useBengaliDigits),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Visual Proportion Bar (Principal vs. Interest)
                val principalRatio = if (totalPayment > 0.0) (loanAmount / totalPayment).toFloat() else 0.5f
                val interestRatio = 1f - principalRatio

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "আসল: ${BengaliFormatter.formatPercent((principalRatio * 100).toDouble(), useBengaliDigits)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "সুদ: ${BengaliFormatter.formatPercent((interestRatio * 100).toDouble(), useBengaliDigits)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD67800)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(principalRatio.coerceIn(0.01f, 0.99f))
                                .height(10.dp)
                                .background(PrimaryBlue)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(Color(0xFFFFB800))
                        )
                    }
                }
            }
        }

        // 2. Interactive Input Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ঋণের প্রয়োজনীয় তথ্য ইনপুট দিন",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Input A: Loan Amount
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ঋণের পরিমাণ (Loan Amount)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = BengaliFormatter.formatTaka(loanAmount, useBengaliDigits),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = amountInputText,
                        onValueChange = { input ->
                            amountInputText = input
                            val parsed = BengaliFormatter.parseAmount(input)
                            if (parsed != null && parsed >= 0.0) {
                                loanAmount = parsed
                            }
                        },
                        label = { Text("টাকার পরিমাণ (৳)") },
                        leadingIcon = {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_loan_amount"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Slider(
                        value = loanAmount.toFloat().coerceIn(50000f, 10000000f),
                        onValueChange = { newVal ->
                            updateAmount(newVal.toDouble())
                        },
                        valueRange = 50000f..10000000f,
                        steps = 199,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue
                        ),
                        modifier = Modifier.testTag("slider_loan_amount")
                    )

                    // Quick Preset Chips for Amount
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            100000.0 to "১ লাখ",
                            300000.0 to "৩ লাখ",
                            500000.0 to "৫ লাখ",
                            1000000.0 to "১০ লাখ",
                            2500000.0 to "২৫ লাখ",
                            5000000.0 to "৫০ লাখ"
                        ).forEach { (amt, label) ->
                            val isSelected = loanAmount == amt
                            FilterChip(
                                selected = isSelected,
                                onClick = { updateAmount(amt) },
                                label = { Text(label, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryBlue
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                // Input B: Interest Rate
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বার্ষিক সুদের হার (Annual Interest Rate)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${BengaliFormatter.formatNumber(annualInterestRate, 2, useBengaliDigits)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = rateInputText,
                        onValueChange = { input ->
                            rateInputText = input
                            val parsed = BengaliFormatter.parseAmount(input)
                            if (parsed != null && parsed >= 0.0) {
                                annualInterestRate = parsed
                            }
                        },
                        label = { Text("সুদের হার % (যেমন: ৯.৫০)") },
                        leadingIcon = {
                            Icon(Icons.Default.Percent, contentDescription = null, tint = PrimaryBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_interest_rate"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Slider(
                        value = annualInterestRate.toFloat().coerceIn(3f, 24f),
                        onValueChange = { newVal ->
                            updateRate(newVal.toDouble())
                        },
                        valueRange = 3f..24f,
                        steps = 41,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue
                        ),
                        modifier = Modifier.testTag("slider_interest_rate")
                    )

                    // Popular Bank Loan Preset Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            8.5 to "SME ঋণ (৮.৫%)",
                            9.0 to "হোম লোন (৯.০%)",
                            10.5 to "অটো লোন (১০.৫%)",
                            12.0 to "ব্যক্তিগত ঋণ (১২.০%)"
                        ).forEach { (rate, label) ->
                            val isSelected = (annualInterestRate - rate).let { it in -0.05..0.05 }
                            FilterChip(
                                selected = isSelected,
                                onClick = { updateRate(rate) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryBlue
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                // Input C: Tenure (Years / Months)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ঋণের পরিশোধের মেয়াদ (Tenure)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Toggle Years vs Months
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isTenureInMonths) PrimaryBlue else Color.Transparent)
                                    .clickable {
                                        if (isTenureInMonths) {
                                            isTenureInMonths = false
                                            tenureYears = (tenureYears / 12).coerceAtLeast(1)
                                            updateTenure(tenureYears)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "বছর",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isTenureInMonths) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isTenureInMonths) PrimaryBlue else Color.Transparent)
                                    .clickable {
                                        if (!isTenureInMonths) {
                                            isTenureInMonths = true
                                            tenureYears = tenureYears * 12
                                            updateTenure(tenureYears)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "মাস",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTenureInMonths) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = tenureInputText,
                        onValueChange = { input ->
                            tenureInputText = input
                            val parsed = BengaliFormatter.parseInt(input)
                            if (parsed != null && parsed > 0) {
                                tenureYears = parsed
                            }
                        },
                        label = { Text(if (isTenureInMonths) "মেয়াদ (মাস)" else "মেয়াদ (বছর)") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimaryBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_tenure"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    val maxTenure = if (isTenureInMonths) 360f else 30f
                    val minTenure = 1f

                    Slider(
                        value = tenureYears.toFloat().coerceIn(minTenure, maxTenure),
                        onValueChange = { newVal ->
                            updateTenure(newVal.roundToInt())
                        },
                        valueRange = minTenure..maxTenure,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue
                        ),
                        modifier = Modifier.testTag("slider_tenure")
                    )

                    // Quick Tenure Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!isTenureInMonths) {
                            listOf(1, 2, 3, 5, 7, 10, 15, 20).forEach { y ->
                                val isSelected = tenureYears == y
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { updateTenure(y) },
                                    label = { Text("${BengaliFormatter.toBengaliDigits(y.toString())} বছর", fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = PrimaryBlue
                                    )
                                )
                            }
                        } else {
                            listOf(12, 24, 36, 60, 120, 180).forEach { m ->
                                val isSelected = tenureYears == m
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { updateTenure(m) },
                                    label = { Text("${BengaliFormatter.toBengaliDigits(m.toString())} মাস", fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = PrimaryBlue
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Smart Tenure-Saver & Prepayment Insight Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = GrowthGreen.copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.dp, GrowthGreen.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = GrowthGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = GrowthGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "স্মার্ট ঋণ পরামর্শ ও সঞ্চয় টিপস:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "মেয়াদ যত বেশি হবে, প্রতি মাসের কিস্তি তত কমবে—তবে মোট সুদের বোঝা কয়েক গুণ বেড়ে যায়। যদি সম্ভব হয়, মেয়াদ ১-২ বছর কমিয়ে আনলে আপনি উল্লেখযোগ্য পরিমাণ সুদ সাশ্রয় করতে পারবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 4. Yearly Amortization Breakdown Table (Expandable)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAmortizationSchedule = !showAmortizationSchedule }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "বার্ষিক পরিশোধ সূচি (Amortization Schedule)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = if (showAmortizationSchedule) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle schedule",
                        tint = PrimaryBlue
                    )
                }

                AnimatedVisibility(
                    visible = showAmortizationSchedule,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("বছর", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                            Text("আসল পরিশোধ", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                            Text("সুদ পরিশোধ", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                            Text("অবশিষ্ট ব্যালেন্স", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        amortizationSchedule.forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${BengaliFormatter.toBengaliDigits(row.year.toString())}ম",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(0.7f)
                                )
                                Text(
                                    text = BengaliFormatter.formatTaka(row.principalPaid, useBengaliDigits),
                                    fontSize = 11.sp,
                                    color = PrimaryBlue,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = BengaliFormatter.formatTaka(row.interestPaid, useBengaliDigits),
                                    fontSize = 11.sp,
                                    color = Color(0xFFD67800),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.3f)
                                )
                                Text(
                                    text = BengaliFormatter.formatTaka(row.remainingBalance, useBengaliDigits),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // 5. Action Row: Copy & Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { copySummary() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_copy_emi_summary")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("কপি করুন", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { shareSummary() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_share_emi_report")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("শেয়ার করুন", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
