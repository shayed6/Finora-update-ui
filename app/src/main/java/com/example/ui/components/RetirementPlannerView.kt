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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

data class RetirementMilestone(
    val age: Int,
    val phase: String,
    val projectedCorpus: Double,
    val monthlyExpense: Double,
    val totalInvested: Double
)

enum class RetirementViewTab(val titleBn: String) {
    PLANNER("পরিকল্পনা ও রেজাল্ট"),
    TIMELINE("বয়সভিত্তিক সময়রেখা"),
    INSIGHTS("অবসর কৌশল ও পরামর্শ")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RetirementPlannerView(
    useBengaliDigits: Boolean = true,
    modifier: Modifier = Modifier
) {
    // State variables
    var currentAge by remember { mutableIntStateOf(30) }
    var retirementAge by remember { mutableIntStateOf(60) }
    var lifeExpectancy by remember { mutableIntStateOf(80) }

    var monthlyExpenseText by remember { mutableStateOf("35000") }
    var existingSavingsText by remember { mutableStateOf("0") }

    var inflationRate by remember { mutableDoubleStateOf(7.0) }
    var preRetirementReturn by remember { mutableDoubleStateOf(12.0) }
    var postRetirementReturn by remember { mutableDoubleStateOf(8.0) }

    var selectedTab by remember { mutableStateOf(RetirementViewTab.PLANNER) }

    val monthlyExpense = monthlyExpenseText.toDoubleOrNull() ?: 0.0
    val existingSavings = existingSavingsText.toDoubleOrNull() ?: 0.0

    // Computations
    val yearsToRetire = remember(currentAge, retirementAge) {
        max(1, retirementAge - currentAge)
    }
    val retirementDuration = remember(retirementAge, lifeExpectancy) {
        max(1, lifeExpectancy - retirementAge)
    }

    val isValid = remember(currentAge, retirementAge, lifeExpectancy, monthlyExpense) {
        currentAge < retirementAge && retirementAge < lifeExpectancy && monthlyExpense > 0.0
    }

    val calculationResult by remember(
        currentAge,
        retirementAge,
        lifeExpectancy,
        monthlyExpense,
        existingSavings,
        inflationRate,
        preRetirementReturn,
        postRetirementReturn
    ) {
        derivedStateOf {
            if (!isValid) return@derivedStateOf null

            val infDec = inflationRate / 100.0
            val monthlyExpAtRet = monthlyExpense * (1.0 + infDec).pow(yearsToRetire.toDouble())
            val annualExpAtRet = monthlyExpAtRet * 12.0

            val postDec = postRetirementReturn / 100.0
            val realRate = ((1.0 + postDec) / (1.0 + infDec)) - 1.0

            // Gross corpus required to support annualExpAtRet growing with inflation
            val grossCorpus = if (abs(realRate) < 0.0001) {
                annualExpAtRet * retirementDuration
            } else {
                annualExpAtRet * ((1.0 - (1.0 + realRate).pow(-retirementDuration.toDouble())) / realRate) * (1.0 + realRate)
            }

            // Future value of existing savings
            val preDecAnnual = preRetirementReturn / 100.0
            val existingSavingsFv = existingSavings * (1.0 + preDecAnnual).pow(yearsToRetire.toDouble())

            val netCorpusRequired = max(0.0, grossCorpus - existingSavingsFv)

            // Monthly SIP calculation
            val preDecMonthly = preRetirementReturn / 100.0 / 12.0
            val totalMonths = yearsToRetire * 12.0
            val requiredMonthlySip = if (netCorpusRequired <= 0.0) {
                0.0
            } else if (preDecMonthly > 0.0) {
                (netCorpusRequired * preDecMonthly) /
                        (((1.0 + preDecMonthly).pow(totalMonths) - 1.0) * (1.0 + preDecMonthly))
            } else {
                netCorpusRequired / totalMonths
            }

            val totalSipDeposits = requiredMonthlySip * totalMonths
            val totalSipGains = max(0.0, netCorpusRequired - totalSipDeposits)

            // 5-year step milestones
            val milestones = mutableListOf<RetirementMilestone>()
            val stepYears = 5
            var age = currentAge
            while (age <= lifeExpectancy) {
                if (age <= retirementAge) {
                    val y = age - currentAge
                    val m = y * 12.0
                    val sipAccum = if (y == 0) 0.0 else {
                        if (preDecMonthly > 0.0) {
                            requiredMonthlySip * (((1.0 + preDecMonthly).pow(m) - 1.0) / preDecMonthly) * (1.0 + preDecMonthly)
                        } else requiredMonthlySip * m
                    }
                    val existingAccum = existingSavings * (1.0 + preDecAnnual).pow(y.toDouble())
                    val curExp = monthlyExpense * (1.0 + infDec).pow(y.toDouble())
                    milestones.add(
                        RetirementMilestone(
                            age = age,
                            phase = if (age == retirementAge) "অবসর শুরুর বছর" else "সঞ্চয় পর্ব",
                            projectedCorpus = sipAccum + existingAccum,
                            monthlyExpense = curExp,
                            totalInvested = (requiredMonthlySip * m) + existingSavings
                        )
                    )
                } else {
                    val yPost = age - retirementAge
                    val curExp = monthlyExpAtRet * (1.0 + infDec).pow(yPost.toDouble())
                    // remaining corpus after withdrawals
                    val postMonthlyR = postRetirementReturn / 100.0 / 12.0
                    val monthsInRet = yPost * 12.0
                    // approximate depletion
                    val remaining = max(0.0, grossCorpus * (1.0 + postDec).pow(yPost.toDouble()) - (annualExpAtRet * yPost * 1.3))
                    milestones.add(
                        RetirementMilestone(
                            age = age,
                            phase = if (age == lifeExpectancy) "পরিকল্পিত শেষ বছর" else "অবসর জীবনযাপন",
                            projectedCorpus = remaining,
                            monthlyExpense = curExp,
                            totalInvested = (requiredMonthlySip * yearsToRetire * 12.0) + existingSavings
                        )
                    )
                }
                age += stepYears
                if (age > lifeExpectancy && milestones.lastOrNull()?.age != lifeExpectancy) {
                    age = lifeExpectancy
                }
            }

            RetirementCalculationOutput(
                grossCorpus = grossCorpus,
                existingSavingsFv = existingSavingsFv,
                netCorpusRequired = netCorpusRequired,
                requiredMonthlySip = requiredMonthlySip,
                monthlyExpAtRet = monthlyExpAtRet,
                totalSipDeposits = totalSipDeposits,
                totalSipGains = totalSipGains,
                milestones = milestones
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Tabs
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = FinoraNavy,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = GrowthGreen,
                    height = 3.dp
                )
            },
            divider = { HorizontalDivider(color = BorderSubtle) }
        ) {
            RetirementViewTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.titleBn,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == tab) FinoraNavy else Color(0xFF64748B)
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            RetirementViewTab.PLANNER -> {
                // Top Result Hero Card
                calculationResult?.let { res ->
                    RetirementHeroResultCard(
                        result = res,
                        yearsToRetire = yearsToRetire,
                        retirementDuration = retirementDuration,
                        useBengaliDigits = useBengaliDigits
                    )
                } ?: run {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "দয়া করে বয়স ও ব্যয়ের তথ্য সঠিকভাবে ইনপুট দিন (বর্তমান বয়স < অবসরের বয়স < আয়ুষ্কাল)।",
                                fontSize = 13.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }

                // Input Controls Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "বয়স ও ব্যয়ের তথ্য",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FinoraNavy
                        )

                        // Current Age Slider
                        AgeSliderRow(
                            labelBn = "বর্তমান বয়স",
                            value = currentAge,
                            min = 18,
                            max = 65,
                            useBn = useBengaliDigits,
                            onValueChange = { currentAge = it }
                        )

                        // Retirement Age Slider
                        AgeSliderRow(
                            labelBn = "পরিকল্পিত অবসরের বয়স",
                            value = retirementAge,
                            min = max(currentAge + 1, 40),
                            max = 75,
                            useBn = useBengaliDigits,
                            onValueChange = { retirementAge = it }
                        )

                        // Life Expectancy Slider
                        AgeSliderRow(
                            labelBn = "প্রত্যাশিত আয়ুষ্কাল (Life Expectancy)",
                            value = lifeExpectancy,
                            min = max(retirementAge + 1, 60),
                            max = 100,
                            useBn = useBengaliDigits,
                            onValueChange = { lifeExpectancy = it }
                        )

                        HorizontalDivider(color = BorderSubtle)

                        // Monthly Expense Input
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "বর্তমান মাসিক পারিবারিক ব্যয় (Living Expenses)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FinoraNavy
                            )
                            OutlinedTextField(
                                value = monthlyExpenseText,
                                onValueChange = { monthlyExpenseText = it.filter { char -> char.isDigit() } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ret_monthly_expense_input"),
                                trailingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderSubtle
                                ),
                                singleLine = true
                            )

                            // Quick expense chips
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(25000, 35000, 50000, 75000, 100000).forEach { amount ->
                                    val isSelected = monthlyExpenseText == amount.toString()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) GrowthGreenLight else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) GrowthGreen else Color.Transparent
                                        ),
                                        modifier = Modifier.clickable {
                                            monthlyExpenseText = amount.toString()
                                        }
                                    ) {
                                        Text(
                                            text = "${BengaliFormatter.formatNumber(amount.toDouble(), 0, useBengaliDigits)} ৳",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) GrowthGreen else FinoraNavy
                                        )
                                    }
                                }
                            }
                        }

                        // Existing Savings Input
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "বর্তমান জমাকৃত অবসর সঞ্চয় (ঐচ্ছিক)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FinoraNavy
                            )
                            OutlinedTextField(
                                value = existingSavingsText,
                                onValueChange = { existingSavingsText = it.filter { char -> char.isDigit() } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ret_existing_savings_input"),
                                trailingIcon = { Text("৳", fontWeight = FontWeight.Bold, color = PrimaryBlue) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderSubtle
                                ),
                                singleLine = true
                            )
                        }

                        HorizontalDivider(color = BorderSubtle)

                        // Rates Sliders
                        Text(
                            text = "অর্থনৈতিক ও বিনিয়োগ পূর্বাভাষ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FinoraNavy
                        )

                        // Inflation Rate
                        RateSliderRow(
                            labelBn = "প্রত্যাশিত গড় মূল্যস্ফীতি (Inflation)",
                            value = inflationRate,
                            min = 4.0,
                            max = 14.0,
                            unit = "%",
                            useBn = useBengaliDigits,
                            onValueChange = { inflationRate = it }
                        )

                        // Pre-Retirement Return
                        RateSliderRow(
                            labelBn = "অবসরপূর্ব বার্ষিক বিনিয়োগ রিটার্ন (SIP)",
                            value = preRetirementReturn,
                            min = 6.0,
                            max = 18.0,
                            unit = "%",
                            useBn = useBengaliDigits,
                            onValueChange = { preRetirementReturn = it }
                        )

                        // Post-Retirement Return
                        RateSliderRow(
                            labelBn = "অবসরকালীন নিরাপদ বিনিয়োগ রিটার্ন (SWP/সঞ্চয়পত্র)",
                            value = postRetirementReturn,
                            min = 4.0,
                            max = 12.0,
                            unit = "%",
                            useBn = useBengaliDigits,
                            onValueChange = { postRetirementReturn = it }
                        )
                    }
                }
            }

            RetirementViewTab.TIMELINE -> {
                calculationResult?.let { res ->
                    RetirementTimelineCard(
                        milestones = res.milestones,
                        useBengaliDigits = useBengaliDigits
                    )
                }
            }

            RetirementViewTab.INSIGHTS -> {
                RetirementInsightsCard(
                    yearsToRetire = yearsToRetire,
                    retirementDuration = retirementDuration,
                    useBengaliDigits = useBengaliDigits
                )
            }
        }
    }
}

@Composable
private fun RetirementHeroResultCard(
    result: RetirementCalculationOutput,
    yearsToRetire: Int,
    retirementDuration: Int,
    useBengaliDigits: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("retirement_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FinoraNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = GrowthGreen.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "অবসর তহবিল পরিকল্পনা",
                        color = GrowthGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${BengaliFormatter.formatNumber(yearsToRetire.toDouble(), 0, useBengaliDigits)} বছর সময় বাকি",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Primary Output: Required Corpus
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "প্রয়োজনীয় অবসরকালীন মোট করপাস (Corpus)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = BengaliFormatter.formatCompactTaka(result.grossCorpus, useBengaliDigits),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "সম্পূর্ণ পরিমাণ: ৳ ${BengaliFormatter.formatNumber(result.grossCorpus, 0, useBengaliDigits)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            // Action Output: Required Monthly SIP
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GrowthGreen.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, GrowthGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "লক্ষ্যপূরণে প্রয়োজনীয় মাসিক এসআইপি (SIP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = BengaliFormatter.formatTaka(result.requiredMonthlySip, useBengaliDigits),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GrowthGreen
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GrowthGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = FinoraNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Secondary Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Future monthly expense
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "অবসরে মাসিক খরচ",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = BengaliFormatter.formatTaka(result.monthlyExpAtRet, useBengaliDigits),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Retirement Duration
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "অবসর জীবনকাল",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${BengaliFormatter.formatNumber(retirementDuration.toDouble(), 0, useBengaliDigits)} বছর",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Proportional Distribution Bar (Invested vs Wealth Gain)
            val totalWealth = result.totalSipDeposits + result.totalSipGains
            val depositRatio = if (totalWealth > 0) (result.totalSipDeposits / totalWealth).toFloat().coerceIn(0.05f, 0.95f) else 0.5f
            val gainRatio = 1f - depositRatio

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "তহবিল গঠনের অনুপাত (আসল জমা বনাম মুনাফা)",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(depositRatio)
                            .fillMaxWidth()
                            .background(PrimaryBlue)
                    )
                    Box(
                        modifier = Modifier
                            .weight(gainRatio)
                            .fillMaxWidth()
                            .background(GrowthGreen)
                    )
                }

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
                            text = "মোট জমা: ${BengaliFormatter.formatCompactTaka(result.totalSipDeposits, useBengaliDigits)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
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
                            text = "চক্রবৃদ্ধি লাভ: ${BengaliFormatter.formatCompactTaka(result.totalSipGains, useBengaliDigits)}",
                            fontSize = 11.sp,
                            color = GrowthGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RetirementTimelineCard(
    milestones: List<RetirementMilestone>,
    useBengaliDigits: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "বয়সভিত্তিক সঞ্চয় ও ব্যয়ের রূপরেখা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinoraNavy
                )
            }

            Text(
                text = "বিভিন্ন বয়সে আপনার তহবিলের প্রবৃদ্ধি এবং মূল্যস্ফীতির কারণে মাসিক খরচের পরিবর্তন নিচে দেখানো হলো:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            milestones.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (index % 2 == 0) Color(0xFFF8FAFC) else Color.White)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "${BengaliFormatter.formatNumber(item.age.toDouble(), 0, useBengaliDigits)} বছর বয়স",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = item.phase,
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1.8f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "তহবিল: ${BengaliFormatter.formatCompactTaka(item.projectedCorpus, useBengaliDigits)}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = GrowthGreen
                        )
                        Text(
                            text = "মাসিক খরচ: ${BengaliFormatter.formatTaka(item.monthlyExpense, useBengaliDigits)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RetirementInsightsCard(
    yearsToRetire: Int,
    retirementDuration: Int,
    useBengaliDigits: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "অবসরকালীন অর্থব্যবস্থাপনার মূল সূত্র",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinoraNavy
                )
            }

            // Insight 1: Rule of 4%
            RetirementRuleItem(
                titleBn = "৪% নিরাপদ উত্তোলন নিয়ম (4% Rule)",
                descriptionBn = "অবসরের প্রথম বছরে সঞ্চিত তহবিলের সর্বোচ্চ ৪% উত্তোলন করে এবং পরবর্তীতে মূল্যস্ফীতি সমন্বয় করে ব্যয় করলে ২০-২৫ বছর তহবিল নিঃশেষ হওয়ার ঝুঁকি থাকে না।"
            )

            // Insight 2: Inflation Risk
            RetirementRuleItem(
                titleBn = "মূল্যস্ফীতি কেন সবচেয়ে বড় নীরব শত্রু?",
                descriptionBn = "৭% মূল্যস্ফীতিতে প্রতি ১০ বছরে যেকোনো পণ্যের দাম দ্বিগুণ হয়। আজকের ৩৫,০০০ টাকার জীবনযাত্রার মান ৩০ বছর পর বজায় রাখতে প্রতি মাসে প্রায় ২,৬৫,০০০ টাকা ব্যয় হবে!"
            )

            // Insight 3: Cost of Delay
            RetirementRuleItem(
                titleBn = "দেরি করার চড়া মূল্য (Cost of Waiting)",
                descriptionBn = "মাত্র ৫ বছর দেরিতে এসআইপি শুরু করলে একই অবসর তহবিল অর্জনের জন্য মাসিক জমার পরিমাণ প্রায় দ্বিগুণ (৮০%-১০০% বেশি) হতে পারে।"
            )

            // Insight 4: Asset Allocation
            RetirementRuleItem(
                titleBn = "অবসর তহবিলের পোর্টফোলিও বিন্যাস",
                descriptionBn = "অবসরের পূর্বে ৭০% ইকুইটি ও মিউচুয়াল ফান্ড এবং ৩০% নিরাপদ ফিক্সড ইনকামে বিনিয়োগ রাখুন। অবসরের পর মূলধন বাঁচাতে ৬০% সঞ্চয়পত্র/ডিপিএস/এফডি এবং ৪০% ডিভিডেন্ড বা ইনডেক্স ফান্ডে রাখুন।"
            )
        }
    }
}

@Composable
private fun RetirementRuleItem(
    titleBn: String,
    descriptionBn: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = titleBn,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FinoraNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = descriptionBn,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun AgeSliderRow(
    labelBn: String,
    value: Int,
    min: Int,
    max: Int,
    useBn: Boolean,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labelBn,
                style = MaterialTheme.typography.labelMedium,
                color = FinoraNavy
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${BengaliFormatter.formatNumber(value.toDouble(), 0, useBn)} বছর",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = PrimaryBlue,
                activeTrackColor = PrimaryBlue,
                inactiveTrackColor = BorderSubtle
            )
        )
    }
}

@Composable
private fun RateSliderRow(
    labelBn: String,
    value: Double,
    min: Double,
    max: Double,
    unit: String,
    useBn: Boolean,
    onValueChange: (Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labelBn,
                style = MaterialTheme.typography.labelMedium,
                color = FinoraNavy
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = GrowthGreenLight
            ) {
                Text(
                    text = "${BengaliFormatter.formatNumber(value, 1, useBn)} $unit",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GrowthGreen
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange((it * 10).roundToInt() / 10.0) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = GrowthGreen,
                activeTrackColor = GrowthGreen,
                inactiveTrackColor = BorderSubtle
            )
        )
    }
}

data class RetirementCalculationOutput(
    val grossCorpus: Double,
    val existingSavingsFv: Double,
    val netCorpusRequired: Double,
    val requiredMonthlySip: Double,
    val monthlyExpAtRet: Double,
    val totalSipDeposits: Double,
    val totalSipGains: Double,
    val milestones: List<RetirementMilestone>
)
