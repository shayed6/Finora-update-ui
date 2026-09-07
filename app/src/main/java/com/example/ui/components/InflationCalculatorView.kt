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
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
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
import kotlin.math.pow
import kotlin.math.roundToInt

data class InflationMilestone(
    val year: Int,
    val futureCost: Double,
    val purchasingPower: Double,
    val costIncreasePercent: Double,
    val powerLostPercent: Double
)

enum class InflationMode(val titleBn: String) {
    FUTURE_COST("ভবিষ্যতের খরচ বৃদ্ধি"),
    PURCHASING_POWER("টাকার মান হ্রাস"),
    REAL_RETURN("প্রকৃত বিনিয়োগ লাভ")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InflationCalculatorView(
    useBengaliDigits: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(InflationMode.FUTURE_COST) }

    // Core interactive state
    var currentAmount by remember { mutableDoubleStateOf(10000.0) }
    var inflationRate by remember { mutableDoubleStateOf(8.5) }
    var tenureYears by remember { mutableIntStateOf(10) }
    var investmentReturnRate by remember { mutableDoubleStateOf(12.0) }

    // Text field buffers
    var amountInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("10000") else "10000")
    }
    var inflationInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("8.5") else "8.5")
    }
    var yearsInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("10") else "10")
    }
    var returnInputText by remember {
        mutableStateOf(if (useBengaliDigits) BengaliFormatter.toBengaliDigits("12") else "12")
    }

    fun updateAmount(value: Double) {
        currentAmount = value
        amountInputText = if (useBengaliDigits) {
            BengaliFormatter.toBengaliDigits(value.roundToInt().toString())
        } else {
            value.roundToInt().toString()
        }
    }

    fun updateInflation(value: Double) {
        inflationRate = value
        val formatted = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
        inflationInputText = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    fun updateYears(value: Int) {
        tenureYears = value
        yearsInputText = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(value.toString()) else value.toString()
    }

    fun updateReturnRate(value: Double) {
        investmentReturnRate = value
        val formatted = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
        returnInputText = if (useBengaliDigits) BengaliFormatter.toBengaliDigits(formatted) else formatted
    }

    // Calculations
    val futureCost by remember(currentAmount, inflationRate, tenureYears) {
        derivedStateOf {
            val i = inflationRate / 100.0
            currentAmount * (1.0 + i).pow(tenureYears.toDouble())
        }
    }

    val priceIncrease by remember(currentAmount, futureCost) {
        derivedStateOf { (futureCost - currentAmount).coerceAtLeast(0.0) }
    }

    val realPurchasingPower by remember(currentAmount, inflationRate, tenureYears) {
        derivedStateOf {
            val i = inflationRate / 100.0
            val factor = (1.0 + i).pow(tenureYears.toDouble())
            if (factor > 0) currentAmount / factor else 0.0
        }
    }

    val purchasingPowerLossPercent by remember(currentAmount, realPurchasingPower) {
        derivedStateOf {
            if (currentAmount > 0) {
                ((currentAmount - realPurchasingPower) / currentAmount * 100.0).coerceIn(0.0, 100.0)
            } else 0.0
        }
    }

    // Fisher equation: (1 + r_nominal) = (1 + r_real) * (1 + i) => r_real = [(1 + r_nominal) / (1 + i)] - 1
    val realReturnRate by remember(investmentReturnRate, inflationRate) {
        derivedStateOf {
            val nom = investmentReturnRate / 100.0
            val inf = inflationRate / 100.0
            ((1.0 + nom) / (1.0 + inf) - 1.0) * 100.0
        }
    }

    // Year-by-year erosion milestones
    val milestones by remember(currentAmount, inflationRate, tenureYears) {
        derivedStateOf {
            val i = inflationRate / 100.0
            val milestoneYears = listOf(1, 3, 5, 10, 15, 20, 25, 30, tenureYears)
                .distinct()
                .filter { it <= tenureYears }
                .sorted()

            milestoneYears.map { y ->
                val factor = (1.0 + i).pow(y.toDouble())
                val fCost = currentAmount * factor
                val pPower = if (factor > 0) currentAmount / factor else 0.0
                val incPct = if (currentAmount > 0) ((fCost - currentAmount) / currentAmount * 100.0) else 0.0
                val lostPct = if (currentAmount > 0) ((currentAmount - pPower) / currentAmount * 100.0) else 0.0
                InflationMilestone(y, fCost, pPower, incPct, lostPct)
            }
        }
    }

    val warningColor = Color(0xFFEF4444)
    val accentOrange = Color(0xFFF59E0B)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // Mode Switcher Tabs
        // ==========================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                InflationMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) PrimaryBlue else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMode = mode }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = mode.titleBn,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else FinoraNavy,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 1. Hero Impact Summary Card
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("inflation_summary_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FinoraNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                when (selectedMode) {
                    InflationMode.FUTURE_COST -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = accentOrange.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = accentOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${BengaliFormatter.formatNumber(tenureYears.toDouble(), 0, useBengaliDigits)} বছর পর ভবিষ্যৎ খরচ",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            val costMultiplier = if (currentAmount > 0) futureCost / currentAmount else 1.0
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentOrange.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "+${BengaliFormatter.formatNumber(((costMultiplier - 1) * 100), 0, useBengaliDigits)}% বৃদ্ধি",
                                    color = accentOrange,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = BengaliFormatter.formatTaka(futureCost, useBengaliDigits),
                            color = accentOrange,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "আজকের ${BengaliFormatter.formatTaka(currentAmount, useBengaliDigits)} মূল্যের জীবনযাত্রা চালাতে এ অর্থের দরকার হবে",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Comparison Progress Bar
                        val origRatio = if (futureCost > 0) (currentAmount / futureCost).toFloat().coerceIn(0f, 1f) else 0.5f
                        val incRatio = (1f - origRatio).coerceIn(0f, 1f)

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
                                        .weight(origRatio.coerceAtLeast(0.01f))
                                        .height(10.dp)
                                        .background(PrimaryBlue)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(incRatio.coerceAtLeast(0.01f))
                                        .height(10.dp)
                                        .background(accentOrange)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "আজকের খরচ: ${(origRatio * 100).roundToInt()}%",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.5.sp
                                )
                                Text(
                                    text = "মূল্যস্ফীতিজনিত অতিরিক্ত ব্যয়: ${(incRatio * 100).roundToInt()}%",
                                    color = accentOrange,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "খরচ বৃদ্ধির পরিমাণ",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "+${BengaliFormatter.formatTaka(priceIncrease, useBengaliDigits)}",
                                    color = accentOrange,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "খরচ বৃদ্ধির গুণক",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${BengaliFormatter.formatNumber(futureCost / currentAmount, 2, useBengaliDigits)} গুণ",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    InflationMode.PURCHASING_POWER -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = warningColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = warningColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${BengaliFormatter.formatNumber(tenureYears.toDouble(), 0, useBengaliDigits)} বছর পর টাকার প্রকৃত ক্রয়ক্ষমতা",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = warningColor.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "-${BengaliFormatter.formatNumber(purchasingPowerLossPercent, 1, useBengaliDigits)}% মানহ্রাস",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = BengaliFormatter.formatTaka(realPurchasingPower, useBengaliDigits),
                            color = Color(0xFFFCA5A5),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "আজকের ${BengaliFormatter.formatTaka(currentAmount, useBengaliDigits)} ক্যাশ ঘরে রাখলে ভবিষ্যৎ বাজারে এর সমান জিনিস কেনা যাবে",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Purchasing Power Retained vs Lost Bar
                        val retainedRatio = (1f - (purchasingPowerLossPercent / 100.0).toFloat()).coerceIn(0f, 1f)
                        val lostRatio = (1f - retainedRatio).coerceIn(0f, 1f)

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
                                        .weight(retainedRatio.coerceAtLeast(0.01f))
                                        .height(10.dp)
                                        .background(GrowthGreen)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(lostRatio.coerceAtLeast(0.01f))
                                        .height(10.dp)
                                        .background(warningColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "অবশিষ্ট ক্রয়ক্ষমতা: ${(retainedRatio * 100).roundToInt()}%",
                                    color = GrowthGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "হারানো মান: ${(lostRatio * 100).roundToInt()}%",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "অলস টাকায় মোট আর্থিক ক্ষতি",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "-${BengaliFormatter.formatTaka(currentAmount - realPurchasingPower, useBengaliDigits)}",
                                    color = warningColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "ক্রয়ক্ষমতা টিকে থাকবে",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = BengaliFormatter.formatPercent(100.0 - purchasingPowerLossPercent, useBengaliDigits),
                                    color = GrowthGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    InflationMode.REAL_RETURN -> {
                        val isPositive = realReturnRate >= 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPositive) GrowthGreen.copy(alpha = 0.2f) else warningColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isPositive) GrowthGreen else warningColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "মূল্যস্ফীতি বাদ দিয়ে প্রকৃত বার্ষিক রিটার্ন (Real CAGR)",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPositive) GrowthGreen.copy(alpha = 0.25f) else warningColor.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (isPositive) "সম্পদ বৃদ্ধি" else "সম্পদ ক্ষয়",
                                    color = if (isPositive) GrowthGreen else Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = BengaliFormatter.formatPercent(realReturnRate, useBengaliDigits),
                            color = if (isPositive) GrowthGreen else Color(0xFFFCA5A5),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = if (isPositive)
                                "আপনার বিনিয়োগ বার্ষিক মূল্যস্ফীতিকে অতিক্রম করে প্রকৃত সম্পদ তৈরি করছে।"
                            else
                                "সতর্কতা: রিটার্ন হার মূল্যস্ফীতির চেয়ে কম হওয়ায় দিনশেষে আপনার প্রকৃত ক্রয়ক্ষমতা কমে যাচ্ছে!",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "নামমাত্র রিটার্ন (Nominal)",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = BengaliFormatter.formatPercent(investmentReturnRate, useBengaliDigits),
                                    color = PrimaryBlue,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "মূল্যস্ফীতির টান",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "-${BengaliFormatter.formatPercent(inflationRate, useBengaliDigits)}",
                                    color = warningColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "প্রকৃত প্রবৃদ্ধি",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = BengaliFormatter.formatPercent(realReturnRate, useBengaliDigits),
                                    color = if (isPositive) GrowthGreen else warningColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. Interactive Input Controls Card
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
                // Section Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriceChange,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "মূল্যস্ফীতির পরিমাপ নির্ধারণ করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = "টাকার পরিমাণ, বাৎসরিক হার ও সময়কাল পরিবর্তন করুন",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.7f))

                // --- 1. Current Amount / Expense ---
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedMode == InflationMode.PURCHASING_POWER) "বর্তমান জমানো নগদ অর্থ" else "আজকের মাসিক ব্যয় বা পণ্যের মূল্য",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = BengaliFormatter.formatTaka(currentAmount, useBengaliDigits),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = PrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountInputText,
                        onValueChange = { input ->
                            amountInputText = input
                            val clean = BengaliFormatter.normalizeToEnglishDigits(input)
                            clean.toDoubleOrNull()?.let { currentAmount = it.coerceIn(500.0, 10000000.0) }
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
                                text = "টাকা",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inflation_amount_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Slider(
                        value = currentAmount.toFloat().coerceIn(1000f, 500000f),
                        onValueChange = { updateAmount(it.toDouble()) },
                        valueRange = 1000f..500000f,
                        steps = 499,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    // Quick Preset Chips for Amount
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(5000.0, 10000.0, 25000.0, 50000.0, 100000.0, 500000.0).forEach { amt ->
                            val isSelected = (currentAmount == amt)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryBlue else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else BorderSubtle
                                ),
                                modifier = Modifier.clickable { updateAmount(amt) }
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

                // --- 2. Expected Annual Inflation Rate % ---
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বার্ষিক মূল্যস্ফীতি হার",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = BengaliFormatter.formatPercent(inflationRate, useBengaliDigits),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = accentOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inflationInputText,
                        onValueChange = { input ->
                            inflationInputText = input
                            val clean = BengaliFormatter.normalizeToEnglishDigits(input)
                            clean.toDoubleOrNull()?.let { inflationRate = it.coerceIn(1.0, 30.0) }
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
                            .testTag("inflation_rate_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentOrange,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Slider(
                        value = inflationRate.toFloat().coerceIn(1f, 25f),
                        onValueChange = { updateInflation(it.toDouble()) },
                        valueRange = 1f..25f,
                        steps = 47,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = accentOrange,
                            activeTrackColor = accentOrange,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    // Quick Preset Chips for Inflation Rate
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            6.0 to "৬% (লক্ষ্যমাত্রা)",
                            8.0 to "৮% (সাধারণ)",
                            9.5 to "৯.৫% (সাম্প্রতিক গড়)",
                            11.0 to "১১% (উচ্চ)",
                            14.0 to "১৪% (চরম)"
                        ).forEach { (rt, label) ->
                            val isSelected = (inflationRate == rt)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) accentOrange else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) accentOrange else BorderSubtle
                                ),
                                modifier = Modifier.clickable { updateInflation(rt) }
                            ) {
                                Text(
                                    text = if (useBengaliDigits) label else "${rt}%",
                                    color = if (isSelected) Color.White else FinoraNavy,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // --- 3. Time Horizon (Years) ---
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সময়কাল (বছর)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = "${BengaliFormatter.formatNumber(tenureYears.toDouble(), 0, useBengaliDigits)} বছর পর",
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
                            .testTag("inflation_years_input"),
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
                        steps = 33,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    // Quick Preset Chips for Years
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

                // --- 4. Nominal Investment Return % (Visible in Mode 3) ---
                AnimatedVisibility(
                    visible = selectedMode == InflationMode.REAL_RETURN,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "বিনিয়োগের বার্ষিক রিটার্ন হার (Nominal Return)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = FinoraNavy
                            )
                            Text(
                                text = BengaliFormatter.formatPercent(investmentReturnRate, useBengaliDigits),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = GrowthGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = returnInputText,
                            onValueChange = { input ->
                                returnInputText = input
                                val clean = BengaliFormatter.normalizeToEnglishDigits(input)
                                clean.toDoubleOrNull()?.let { investmentReturnRate = it.coerceIn(1.0, 35.0) }
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GrowthGreen,
                                unfocusedBorderColor = BorderSubtle,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        Slider(
                            value = investmentReturnRate.toFloat().coerceIn(1f, 30f),
                            onValueChange = { updateReturnRate(it.toDouble()) },
                            valueRange = 1f..30f,
                            steps = 57,
                            colors = SliderDefaults.colors(
                                thumbColor = GrowthGreen,
                                activeTrackColor = GrowthGreen,
                                inactiveTrackColor = BorderSubtle
                            )
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. Year-by-Year Inflation Erosion Schedule
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
                            .background(accentOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = accentOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "সময়ভিত্তিক মূল্যস্ফীতি সময়রেখা (Timeline)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FinoraNavy
                        )
                        Text(
                            text = "বছরের পর বছর খরচ বৃদ্ধি এবং টাকার ক্রয়ক্ষমতা ক্ষয়",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                milestones.forEachIndexed { index, m ->
                    val isFinal = (m.year == tenureYears)
                    val maxCost = milestones.lastOrNull()?.futureCost ?: 1.0
                    val barFill = if (maxCost > 0) (m.futureCost / maxCost).toFloat().coerceIn(0f, 1f) else 0f

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFinal) Color(0xFFFFFBEB) else if (index % 2 == 0) Color(0xFFFAFCFF) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isFinal) accentOrange.copy(alpha = 0.4f) else BorderSubtle.copy(alpha = 0.6f)
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
                                        color = if (isFinal) accentOrange else PrimaryBlue.copy(alpha = 0.12f)
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
                                        text = "ক্রয়ক্ষমতা: ${BengaliFormatter.formatTaka(m.purchasingPower, useBengaliDigits)}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = BengaliFormatter.formatTaka(m.futureCost, useBengaliDigits),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFinal) accentOrange else FinoraNavy
                                    )
                                    Text(
                                        text = "+${BengaliFormatter.formatNumber(m.costIncreasePercent, 0, useBengaliDigits)}% ব্যয়",
                                        fontSize = 10.5.sp,
                                        color = accentOrange,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

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
                                        .background(if (isFinal) accentOrange else PrimaryBlue)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. Rule of 70 & Compounding Insights Card
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GrowthGreenLight.copy(alpha = 0.45f)),
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
                        text = "মূল্যস্ফীতি পরাস্ত করার আর্থিক কৌশল (Beat Inflation)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FinoraNavy
                    )
                }

                val halfLifeYears = if (inflationRate > 0) (70.0 / inflationRate).roundToInt() else 0

                Text(
                    text = "• ৭০-এর নিয়ম (Rule of 70): ${BengaliFormatter.formatPercent(inflationRate, useBengaliDigits)} মূল্যস্ফীতি হারে আজ আপনার হাতে থাকা টাকার ক্রয়ক্ষমতা ঠিক প্রায় ${BengaliFormatter.formatNumber(halfLifeYears.toDouble(), 0, useBengaliDigits)} বছর পর অর্ধেকে (৫০%) নেমে যাবে।",
                    fontSize = 12.sp,
                    color = FinoraNavy,
                    lineHeight = 18.sp
                )

                Text(
                    text = "• নগদ বা সেভিংস একাউন্টে অলস টাকা রাখা সবচেয়ে ঝুঁকিপূর্ণ: সেভিংসে ৩-৪% মুনাফা পাওয়া গেলেও মূল্যস্ফীতি যদি ৮-১০% হয়, তবে প্রতি বছর আপনার প্রকৃত মূলধন হারিয়ে যাচ্ছে।",
                    fontSize = 12.sp,
                    color = FinoraNavy,
                    lineHeight = 18.sp
                )

                Text(
                    text = "• সঠিক সম্পদ বিন্যাস (Asset Allocation): মূল্যস্ফীতিকে হারানোর জন্য মিউচুয়াল ফান্ড, স্টক বাজার, রিয়েল এস্টেট বা দীর্ঘমেয়াদী এসআইপি-তে বিনিয়োগ করা সবচেয়ে কার্যকর কৌশল।",
                    fontSize = 12.sp,
                    color = FinoraNavy,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
