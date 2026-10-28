package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppLanguage
import com.example.ui.LocalAppLanguage
import com.example.ui.screens.portfolio.PortfolioSummary
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import kotlin.math.abs

@Composable
fun CurrentInvestmentWidget(
    summary: PortfolioSummary,
    useBengaliDigits: Boolean = true,
    onClick: () -> Unit,
    onAddStockClick: () -> Unit = onClick,
    modifier: Modifier = Modifier
) {
    val isEnglish = LocalAppLanguage.current == AppLanguage.ENGLISH

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("home_portfolio_widget"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (summary.holdingsCount == 0) {
            // Friendly empty state (preserved layout and styling per Requirement 7)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnglish) "Portfolio Tracker" else "পোর্টফোলিও ট্র্যাকার (Portfolio)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnglish) "No investments added yet — tap to add stock" else "এখনো কোনো বিনিয়োগ যোগ করা হয়নি — পোর্টফোলিওতে যোগ করুন",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = PrimaryBlue,
                    modifier = Modifier
                        .clickable(onClick = onAddStockClick)
                        .testTag("home_portfolio_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Holding",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
            }
        } else {
            // Populated State per Requirement 7:
            // "একই কার্ডের স্টাইলে মোট পোর্টফোলিও মূল্য, লাভ/ক্ষতি ও শতাংশ (সবুজ ▲ / লাল ▼) এবং ডানে "বিস্তারিত ›"। ট্যাপে Portfolio স্ক্রিন"
            val totalGL = summary.totalUnrealizedGainLoss
            val invested = summary.totalInvested
            val glPercent = if (invested > 0.0) (totalGL / invested) * 100.0 else 0.0
            val isPositive = totalGL >= 0.0
            val glColor = if (isPositive) GrowthGreen else AlertRed
            val arrow = if (isPositive) "▲" else "▼"
            val sign = if (isPositive) "+" else "-"
            val formattedGl = BengaliFormatter.formatTaka(abs(totalGL), useBengaliDigits)
            val formattedPct = BengaliFormatter.formatNumber(abs(glPercent), 2, useBengaliDigits)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Row: Title + "বিস্তারিত ›"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Portfolio Tracker" else "পোর্টফোলিও ট্র্যাকার",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (isEnglish) "Details ›" else "বিস্তারিত ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue,
                        modifier = Modifier.testTag("home_portfolio_details_link")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Middle: Total Portfolio Value
                Text(
                    text = if (isEnglish) "Current Portfolio Value" else "মোট পোর্টফোলিও মূল্য",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = BengaliFormatter.formatTaka(summary.currentTotalValue, useBengaliDigits),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom: Profit / Loss Badge & Holdings count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = glColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$arrow $sign$formattedGl ($sign$formattedPct%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = glColor
                            )
                        }
                    }

                    Text(
                        text = if (isEnglish) "${summary.holdingsCount} Stocks" else "${BengaliFormatter.formatNumber(summary.holdingsCount.toDouble(), 0, useBengaliDigits)}টি স্টক",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
