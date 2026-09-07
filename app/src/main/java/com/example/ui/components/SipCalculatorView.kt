package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.GrowthGreenLight
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import kotlin.math.pow
import kotlin.math.roundToInt

data class SipMilestone(
    val year: Int,
    val invested: Double,
    val wealth: Double,
    val gain: Double,
    val multiplier: Double
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SipCalculatorView(
    useBengaliDigits: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Interactive State
    var monthlyAmount by remember { mutableDoubleStateOf(5000.0) }
    var annualReturnRate by remember { mutableDoubleStateOf(12.0) }
    var tenureYears by remember { mutableIntStateOf(10) }
    var isStepUpEnabled by remember { mutableStateOf(false) }
    var stepUpRate by remember { mutableDoubleStateOf(10.0) } // 10% annual increase

    // String representations for text fields
    var monthlyInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("5000") else "5000")
    }
    var rateInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("12") else "12")
    }
    var yearsInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("10") else "10")
    }

    // Synchronize inputs when slider or chips change
    fun updateMonthly(value: Double) {
        monthlyAmount = value
        monthlyInputText = if (useBengaliDigits) {
            BengaliFormatter.toBengaliDigits(value.roundToInt().toString())
        } else {
            value.roundToInt().toString()
        }
    }

    fun updateRate(value: Double) {
        annualReturnRate = value
        val formatted = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
        rateInputText = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun updateYears(value: Int) {
        tenureYears = value
        yearsInputText = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(value.toString()) else value.toString()
    }

    // Calculation Engine
    val sipResult by remember(monthlyAmount, annualReturnRate, tenureYears, isStepUpEnabled, stepUpRate) {
        derivedStateOf {
            val r = annualReturnRate / 12.0 / 100.0
            val totalMonths = tenureYears * 12

            if (monthlyAmount <= 0.0 || tenureYears <= 0) {
                Triple(0.0, 0.0, 0.0)
            } else if (!isStepUpEnabled) {
                // Standard SIP Formula: FV = P * [((1 + r)^n - 1) / r] * (1 + r)
                val fv = if (r > 0.0) {
                    monthlyAmount * (((1.0 + r).pow(totalMonths.toDouble()) - 1.0) / r) * (1.0 + r)
                } else {
                    monthlyAmount * totalMonths
                }
                val totalInvested = monthlyAmount * totalMonths
                val wealthGain = fv - totalInvested
                Triple(totalInvested, wealthGain, fv)
            } else {
                // Step-up SIP: Monthly amount increases annually by stepUpRate%
                var currentMonthly = monthlyAmount
                var fv = 0.0
                var totalInvested = 0.0

                for (m in 1..totalMonths) {
                    if (m > 1 && (m - 1) % 12 == 0) {
                        currentMonthly *= (1.0 + stepUpRate / 100.0)
                    }
                    totalInvested += currentMonthly
                    fv = (fv + currentMonthly) * (1.0 + r)
                }
                val wealthGain = fv - totalInvested
                Triple(totalInvested, wealthGain, fv)
            }
        }
    }

    val (totalInvested, wealthGain, totalWealth) = sipResult

    // Year-by-year milestones
    val milestones by remember(monthlyAmount, annualReturnRate, tenureYears, isStepUpEnabled, stepUpRate) {
        derivedStateOf {
            val r = annualReturnRate / 12.0 / 100.0
            val milestoneYears = listOf(1, 3, 5, 10, 15, 20, 25, 30, tenureYears)
                .distinct()
                .filter { it <= tenureYears }
                .sorted()

            milestoneYears.map { y ->
                val mCount = y * 12
                var inv = 0.0
                var fv = 0.0
                var currMonthly = monthlyAmount

                if (!isStepUpEnabled) {
                    inv = monthlyAmount * mCount
                    fv = if (r > 0.0) {
                        monthlyAmount * (((1.0 + r).pow(mCount.toDouble()) - 1.0) / r) * (1.0 + r)
                    } else {
                        inv
                    }
                } else {
                    for (m in 1..mCount) {
                        if (m > 1 && (m - 1) % 12 == 0) {
                            currMonthly *= (1.0 + stepUpRate / 100.0)
                        }
                        inv += currMonthly
                        fv = (fv + currMonthly) * (1.0 + r)
                    }
                }
                val gain = fv - inv
                val mult = if (inv > 0.0) fv / inv else 1.0
                SipMilestone(y, inv, fv, gain, mult)
            }
        }
    }

    val investedRatio = if (totalWealth > 0.0) (totalInvested / totalWealth).toFloat().coerceIn(0f, 1f) else 0.5f
    val gainRatio = (1f - investedRatio).coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. Hero Wealth Accumulation Overview Card
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("sip_wealth_summary_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FinoraNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GrowthGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = GrowthGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সম্ভাব্য মোট সম্পদ (Future Value)",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (totalInvested > 0.0 && totalWealth > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GrowthGreen.copy(alpha = 0.25f)
                        ) {
                            val mult = totalWealth / totalInvested
                            Text(
                                text = "${BengaliFormatter.formatNumber(mult, 1, useBengaliDigits)}x প্রবৃদ্ধি",
                                color = GrowthGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Future Wealth Value
                Text(
                    text = BengaliFormatter.formatTaka(totalWealth, useBengaliDigits),
                    color = GrowthGreen,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = BengaliFormatter.formatCompactTaka(totalWealth, useBengaliDigits),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Visual Ratio Bar: Invested vs Wealth Gained
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(investedRatio.coerceAtLeast(0.01f))
                                .height(10.dp)
                                .background(PrimaryBlue)
                        )
                        Box(
                            modifier = Modifier
                                .weight(gainRatio.coerceAtLeast(0.01f))
                                .height(10.dp)
                                .background(GrowthGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "মূলধন: ${(investedRatio * 100).roundToInt()}%",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.5.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GrowthGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "মুনাফা: ${(gainRatio * 100).roundToInt()}%",
                                color = GrowthGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(14.dp))

                // Sub Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "মোট বিনিয়োগকৃত অর্থ",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = BengaliFormatter.formatTaka(totalInvested, useBengaliDigits),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "আনুমানিক অর্জিত মুনাফা",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = BengaliFormatter.formatTaka(wealthGain, useBengaliDigits),
                            color = GrowthGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. Interactive Inputs Card
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "বিনিয়োগের তথ্য নির্ধারণ করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = "স্লাইডার অথবা দ্রুত অপশন বেছে নিন",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f))

                // --- 1. Monthly Investment ---
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "প্রতি মাসে বিনিয়োগ",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = BengaliFormatter.formatTaka(monthlyAmount, useBengaliDigits),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = monthlyInputText,
                        onValueChange = { input ->
                            monthlyInputText = input
                            val clean = BengaliFormatter.normalizeToEnglishDigits(input)
                            clean.toDoubleOrNull()?.let { monthlyAmount = it.coerceIn(500.0, 500000.0) }
                        },
                        leadingIcon = {
                            Text(
                                text = "৳",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                fontSize = 16.sp
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "টাকা/মাস",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sip_monthly_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Slider(
                        value = monthlyAmount.toFloat().coerceIn(500f, 100000f),
                        onValueChange = { updateMonthly(it.toDouble()) },
                        valueRange = 500f..100000f,
                        steps = 198, // step of 500
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    // Quick Chips for Monthly Amount
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1000.0, 2500.0, 5000.0, 10000.0, 25000.0, 50000.0).forEach { amt ->
                            val isSelected = (monthlyAmount == amt)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryBlue else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier.clickable { updateMonthly(amt) }
                            ) {
                                Text(
                                    text = BengaliFormatter.formatTaka(amt, useBengaliDigits).replace(".00", ""),
                                    color = if (isSelected) Color.White else FinoraNavy,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // --- 2. Expected Annual Return % ---
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "প্রত্যাশিত বার্ষিক রিটার্ন হার",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = BengaliFormatter.formatPercent(annualReturnRate, useBengaliDigits),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = GrowthGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rateInputText,
                        onValueChange = { input ->
                            rateInputText = input
                            val clean = BengaliFormatter.normalizeToEnglishDigits(input)
                            clean.toDoubleOrNull()?.let { annualReturnRate = it.coerceIn(1.0, 30.0) }
                        },
                        trailingIcon = {
                            Text(
                                text = "% বার্ষিক",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sip_rate_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Slider(
                        value = annualReturnRate.toFloat().coerceIn(1f, 30f),
                        onValueChange = { updateRate(it.toDouble()) },
                        valueRange = 1f..30f,
                        steps = 57, // step of 0.5%
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = GrowthGreen,
                            activeTrackColor = GrowthGreen,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    // Quick Chips for Return Rates
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(8.0 to "৮% (সঞ্চয়)", 10.0 to "১০% (বন্ড)", 12.0 to "১২% (ব্যালেন্সড)", 15.0 to "১৫% (ইকুইটি)", 18.0 to "১৮% (উচ্চ)").forEach { (rt, label) ->
                            val isSelected = (annualReturnRate == rt)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GrowthGreen else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GrowthGreen else BorderSubtle
                                ),
                                modifier = Modifier.clickable { updateRate(rt) }
                            ) {
                                Text(
                                    text = if (useBengaliDigits) label else "${rt.toInt()}%",
                                    color = if (isSelected) Color.White else FinoraNavy,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // --- 3. Investment Period (Years) ---
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বিনিয়োগের সময়কাল",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = "${BengaliFormatter.formatNumber(tenureYears.toDouble(), 0, useBengaliDigits)} বছর (${BengaliFormatter.formatNumber((tenureYears * 12).toDouble(), 0, useBengaliDigits)} কিস্তি)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = yearsInputText,
                        onValueChange = { input ->
                            yearsInputText = input
                            val clean = BengaliFormatter.normalizeToEnglishDigits(input)
                            clean.toIntOrNull()?.let { tenureYears = it.coerceIn(1, 35) }
                        },
                        trailingIcon = {
                            Text(
                                text = "বছর",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sip_tenure_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Slider(
                        value = tenureYears.toFloat().coerceIn(1f, 35f),
                        onValueChange = { updateYears(it.roundToInt()) },
                        valueRange = 1f..35f,
                        steps = 33, // 1 to 35
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    // Quick Chips for Years
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(3, 5, 10, 15, 20, 25, 30).forEach { yr ->
                            val isSelected = (tenureYears == yr)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryBlue else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier.clickable { updateYears(yr) }
                            ) {
                                Text(
                                    text = "${BengaliFormatter.formatNumber(yr.toDouble(), 0, useBengaliDigits)} বছর",
                                    color = if (isSelected) Color.White else FinoraNavy,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // --- 4. Optional Annual Step-up SIP Toggle ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "বার্ষিক কিস্তি বৃদ্ধি (Step-up SIP)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = FinoraNavy
                                )
                                Text(
                                    text = "আয় বৃদ্ধির সাথে প্রতি বছর কিস্তির পরিমাণ বাড়ান",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isStepUpEnabled,
                                onCheckedChange = { isStepUpEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = GrowthGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = BorderSubtle
                                ),
                                modifier = Modifier.testTag("sip_stepup_switch")
                            )
                        }

                        AnimatedVisibility(
                            visible = isStepUpEnabled,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "প্রতি বছর বৃদ্ধির হার",
                                        fontSize = 12.sp,
                                        color = FinoraNavy
                                    )
                                    Text(
                                        text = BengaliFormatter.formatPercent(stepUpRate, useBengaliDigits),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = GrowthGreen
                                    )
                                }
                                Slider(
                                    value = stepUpRate.toFloat().coerceIn(5f, 25f),
                                    onValueChange = { stepUpRate = it.toDouble() },
                                    valueRange = 5f..25f,
                                    steps = 19,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GrowthGreen,
                                        activeTrackColor = GrowthGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. Wealth Accumulation Milestone Timeline
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GrowthGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = GrowthGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "সময়ভিত্তিক সম্পদ বৃদ্ধি প্রক্ষেপণ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = "চক্রবৃদ্ধি মুনাফায় সময়ের সাথে কীভাবে তহবিল বৃদ্ধি পায়",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Milestones Table
                milestones.forEachIndexed { index, m ->
                    val isFinal = (m.year == tenureYears)
                    val maxWealth = milestones.lastOrNull()?.wealth ?: 1.0
                    val barFill = if (maxWealth > 0) (m.wealth / maxWealth).toFloat().coerceIn(0f, 1f) else 0f

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFinal) GrowthGreenLight.copy(alpha = 0.45f) else if (index % 2 == 0) Color(0xFFFAFCFF) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isFinal) GrowthGreen.copy(alpha = 0.4f) else BorderSubtle.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isFinal) GrowthGreen else PrimaryBlue.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${BengaliFormatter.formatNumber(m.year.toDouble(), 0, useBengaliDigits)} বছর",
                                            color = if (isFinal) Color.White else PrimaryBlue,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "বিনিয়োগ: ${BengaliFormatter.formatTaka(m.invested, useBengaliDigits)}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = BengaliFormatter.formatTaka(m.wealth, useBengaliDigits),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFinal) GrowthGreen else FinoraNavy
                                    )
                                    Text(
                                        text = "+${BengaliFormatter.formatTaka(m.gain, useBengaliDigits)} লাভ",
                                        fontSize = 10.5.sp,
                                        color = GrowthGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Mini Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(barFill)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isFinal) GrowthGreen else PrimaryBlue)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. Compounding Magic Insight Cards
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GrowthGreenLight.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, GrowthGreen.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = GrowthGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "চক্রবৃদ্ধি মুনাফার (Compounding) মহাশক্তি",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FinoraNavy
                    )
                }

                val doublingYears = if (annualReturnRate > 0) (72.0 / annualReturnRate).roundToInt() else 0

                Text(
                    text = "• ৭২-এর নিয়ম (Rule of 72): ${BengaliFormatter.formatPercent(annualReturnRate, useBengaliDigits)} মুনাফা হারে আপনার বিনিয়োগ করা অর্থ প্রতি প্রায় ${BengaliFormatter.formatNumber(doublingYears.toDouble(), 0, useBengaliDigits)} বছরে দ্বিগুণ হবে।",
                    fontSize = 12.sp,
                    color = FinoraNavy,
                    lineHeight = 18.sp
                )

                Text(
                    text = "• দীর্ঘমেয়াদে ধৈর্য: লক্ষ্য করুন, প্রথম কয়েক বছরে মোট বিনিয়োগ এবং সম্পদের ব্যবধান কম থাকে, কিন্তু ১০-১৫ বছর পর মুনাফার অংশ মূল বিনিয়োগকে বহু গুণে ছাড়িয়ে যায়।",
                    fontSize = 12.sp,
                    color = FinoraNavy,
                    lineHeight = 18.sp
                )

                Text(
                    text = "• ধারাবাহিকতা বজায় রাখা: বাজারের সাময়িক পতন সত্ত্বেও প্রতি মাসে নির্ধারিত দিনে বিনিয়োগ চালিয়ে গেলে নিম্ন মূল্যে বেশি ইউনিট কেনার সুবিধা পাওয়া যায়।",
                    fontSize = 12.sp,
                    color = FinoraNavy,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
