package com.example.ui.screens.portfolio

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import com.example.util.CsvReportGenerator
import com.example.util.PdfReportGenerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdManager
import com.example.data.local.entity.DividendEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.PortfolioRepository
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    useBengaliDigits: Boolean = true,
    viewModel: PortfolioViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val holdings by viewModel.holdings.collectAsState()
    val holdingsWithDividends by viewModel.holdingsWithDividends.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedHoldingForHistory by remember { mutableStateOf<HoldingEntity?>(null) }
    var holdingForDividend by remember { mutableStateOf<HoldingEntity?>(null) }
    var holdingToDelete by remember { mutableStateOf<HoldingEntity?>(null) }

    // Requirement: Do NOT show any ad format inside the Portfolio buy/sell transaction flow
    val isInsideTxFlow = showAddDialog || holdingForDividend != null
    androidx.compose.runtime.LaunchedEffect(isInsideTxFlow) {
        AdManager.setInsideTransactionFlow(isInsideTxFlow)
    }

    // Schedule: Refresh live_prices every 50-60 seconds while Portfolio screen is active/foreground.
    // Do NOT run background scrape when app or screen is closed.
    DisposableEffect(Unit) {
        viewModel.startLivePricePolling()
        onDispose {
            AdManager.setInsideTransactionFlow(false)
            viewModel.stopLivePricePolling()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("portfolio_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Holding")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                // Header Bar with Title, Manual Refresh Button & PDF Export Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আমার পোর্টফোলিও",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isRefreshing) "বাজার থেকে লাইভ দর সংগ্রহ করা হচ্ছে..." else "DSE ও CSE লাইভ দর ও ডিভিডেন্ড আয় ট্র্যাকিং",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRefreshing) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val act = findActivity(context)
                                AdManager.onManualRefreshRequested(act) {
                                    viewModel.manualRefreshLivePrices()
                                }
                            },
                            modifier = Modifier.testTag("portfolio_refresh_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryBlue
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Live Prices",
                                    tint = PrimaryBlue
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                exportPortfolioCsv(context, holdings, summary, useBengaliDigits, holdingsWithDividends)
                            },
                            modifier = Modifier.testTag("portfolio_export_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = "Export Portfolio CSV",
                                tint = PrimaryBlue
                            )
                        }

                        IconButton(
                            onClick = {
                                exportPortfolioReport(context, holdings, summary, useBengaliDigits, holdingsWithDividends)
                            },
                            modifier = Modifier.testTag("portfolio_export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export Portfolio PDF Report",
                                tint = PrimaryBlue
                            )
                        }
                    }
                }
            }

                // Summary Card: "Current Investment" Header
                item {
                    PortfolioSummaryCard(
                        summary = summary,
                        useBengaliDigits = useBengaliDigits
                    )
                }

                if (holdingsWithDividends.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "এখনো কোনো বিনিয়োগ যোগ করা হয়নি",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "নিচের '+' বাটনে ট্যাপ করে আপনার প্রথম স্টক ক্রয় এন্ট্রি করুন। একই স্টক একাধিকবার কিনলে স্বয়ংক্রিয়ভাবে গড় ক্রয়মূল্য হিসাব হবে।",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("স্টক যোগ করুন")
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "হোল্ডিংস তালিকা (${BengaliFormatter.toBengaliDigits(holdingsWithDividends.size.toString())}টি স্টক)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ডিভিডেন্ড ও হিস্ট্রি বিস্তারিত",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(holdingsWithDividends, key = { it.holding.id }) { item ->
                        HoldingItemCard(
                            holdingWithDiv = item,
                            useBengaliDigits = useBengaliDigits,
                            onClick = { selectedHoldingForHistory = item.holding },
                            onAddDividend = { holdingForDividend = item.holding },
                            onDelete = { holdingToDelete = item.holding }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp)) // Space for FAB
                    }
                }
            }
    }

    // Add Buy Transaction Dialog
    if (showAddDialog) {
        AddBuyTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { exchange, stockName, buyingPrice, qty, commission ->
                viewModel.addBuyTransaction(
                    exchange = exchange,
                    stockName = stockName,
                    buyingPrice = buyingPrice,
                    quantity = qty,
                    commissionPercent = commission
                )
                showAddDialog = false
            }
        )
    }

    // Add Dividend Dialog
    holdingForDividend?.let { holding ->
        AddDividendDialog(
            holding = holding,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { holdingForDividend = null },
            onSave = { amount ->
                viewModel.addDividend(holding.id, amount)
                holdingForDividend = null
            }
        )
    }

    // Transaction & Dividend History Dialog
    selectedHoldingForHistory?.let { holding ->
        TransactionAndDividendHistoryDialog(
            holding = holding,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { selectedHoldingForHistory = null },
            fetchTransactions = { viewModel.getTransactionHistory(holding.id) },
            fetchDividends = { viewModel.getDividendHistory(holding.id) },
            onDeleteDividend = { divId -> viewModel.deleteDividend(divId) }
        )
    }

    // Delete Holding Dialog
    holdingToDelete?.let { holding ->
        AlertDialog(
            onDismissRequest = { holdingToDelete = null },
            title = {
                Text(
                    text = "হোল্ডিং মুছে ফেলবেন?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে ${holding.exchange} এর \"${holding.stockName}\" হোল্ডিং এবং এর পূর্ববর্তী সমস্ত ক্রয়ের ও ডিভিডেন্ডের রেকর্ড মুছে ফেলতে চান?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHolding(holding)
                        holdingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { holdingToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun PortfolioSummaryCard(
    summary: PortfolioSummary,
    useBengaliDigits: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Investment (বিনিয়োগ বিবরণী)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = PrimaryBlue
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "মোট স্টক: ${BengaliFormatter.formatNumber(summary.holdingsCount.toDouble(), 0, useBengaliDigits)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Metrics: Total Invested & Current Portfolio Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "মোট ক্রয়মূল্য (Total Invested)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = BengaliFormatter.formatTaka(summary.totalInvested, useBengaliDigits),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "বর্তমান বাজারমূল্য (Current Value)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (summary.hasAnyLivePrice) BengaliFormatter.formatTaka(summary.currentTotalValue, useBengaliDigits) else "—",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Metrics: Unrealized Gain/Loss & Total Dividend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "অবাস্তবায়িত লাভ/ক্ষতি (Unrealized G/L)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (summary.hasAnyLivePrice) {
                        val isGain = summary.totalUnrealizedGainLoss >= 0
                        val color = if (isGain) GrowthGreen else AlertRed
                        val sign = if (isGain) "+" else ""
                        val glPct = if (summary.totalInvested > 0) (summary.totalUnrealizedGainLoss / summary.totalInvested) * 100.0 else 0.0
                        Text(
                            text = "$sign${BengaliFormatter.formatTaka(summary.totalUnrealizedGainLoss, useBengaliDigits)} ($sign${BengaliFormatter.formatPercent(glPct, useBengaliDigits)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = color
                        )
                    } else {
                        Text(
                            text = "—",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "মোট ডিভিডেন্ড আয় (Dividends)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = BengaliFormatter.formatTaka(summary.totalDividend, useBengaliDigits),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GrowthGreen
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Dividend Yield Banner
            val divYieldPct = if (summary.totalInvested > 0) (summary.totalDividend / summary.totalInvested) * 100.0 else 0.0

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = GrowthGreen.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, GrowthGreen.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = GrowthGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ডিভিডেন্ড রিটার্ন (Dividend Yield):",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = BengaliFormatter.formatPercent(divYieldPct, useBengaliDigits),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = GrowthGreen
                    )
                }
            }
        }
    }
}

/**
 * Holding Item Card showing investment details, live price, unrealized G/L, dividend earnings, and return per share.
 * Layout strictly follows:
 * Exchange - Stock name - Quantity - Total Price (avg price/share shown smaller below) - Current Price - Unrealized G/L - Dividend Received - Per-Share Dividend Return
 */
@Composable
private fun HoldingItemCard(
    holdingWithDiv: HoldingWithDividends,
    useBengaliDigits: Boolean,
    onClick: () -> Unit,
    onAddDividend: () -> Unit,
    onDelete: () -> Unit
) {
    val holding = holdingWithDiv.holding
    val totalPrice = holding.quantity * holding.averagePrice

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("holding_card_${holding.stockName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Exchange badge (DSE/CSE) + Stock Name + Quantity + Outdated indicator + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (holding.exchange.uppercase() == "DSE") PrimaryBlue else Color(0xFF8B5CF6)
                    ) {
                        Text(
                            text = holding.exchange.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = holding.stockName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "· ${BengaliFormatter.formatNumber(holding.quantity.toDouble(), 0, useBengaliDigits)}টি শেয়ার",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (holdingWithDiv.isPriceOutdated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFF3CD),
                            border = BorderStroke(0.5.dp, Color(0xFFFFC107))
                        ) {
                            Text(
                                text = "পুরোনো",
                                fontSize = 9.5.sp,
                                color = Color(0xFF856404),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAddDividend,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Dividend",
                            tint = GrowthGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AlertRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Total Price (avg price/share shown smaller below) - Current Price - Unrealized G/L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Total Price + avg price below
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "মোট ক্রয়মূল্য",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = BengaliFormatter.formatTaka(totalPrice, useBengaliDigits),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "গড়: ${BengaliFormatter.formatTaka(holding.averagePrice, useBengaliDigits)}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Current Price
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "বর্তমান দর",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val cp = holdingWithDiv.currentPrice
                    if (cp != null && cp > 0.0) {
                        Text(
                            text = BengaliFormatter.formatTaka(cp, useBengaliDigits),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        val changePct = holdingWithDiv.livePrice?.changePercent
                        if (changePct != null && changePct != 0.0) {
                            val sign = if (changePct > 0) "+" else ""
                            Text(
                                text = "$sign${BengaliFormatter.formatPercent(changePct, useBengaliDigits)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (changePct > 0) GrowthGreen else AlertRed
                            )
                        } else {
                            Text(
                                text = "০.০০%",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "—",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "পাওয়া যায়নি",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Unrealized G/L
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "অবাস্তবায়িত লাভ/ক্ষতি",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val ugl = holdingWithDiv.unrealizedGainLoss
                    val uglPct = holdingWithDiv.unrealizedGainLossPercent
                    if (ugl != null) {
                        val isGain = ugl >= 0
                        val color = if (isGain) GrowthGreen else AlertRed
                        val sign = if (isGain) "+" else ""
                        Text(
                            text = "$sign${BengaliFormatter.formatTaka(ugl, useBengaliDigits)}",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "($sign${BengaliFormatter.formatPercent(uglPct ?: 0.0, useBengaliDigits)})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                    } else {
                        Text(
                            text = "—",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "—",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Dividend Received (ডিভিডেন্ড প্রাপ্ত) & Per-Share Dividend Return (শেয়ার প্রতি রিটার্ন)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ডিভিডেন্ড প্রাপ্ত:",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${BengaliFormatter.formatTaka(holdingWithDiv.totalDividend, useBengaliDigits)} (${BengaliFormatter.toBengaliDigits(holdingWithDiv.dividendCount.toString())} বার)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrowthGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "শেয়ার প্রতি রিটার্ন:",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${BengaliFormatter.formatTaka(holdingWithDiv.returnPerShare, useBengaliDigits)}/শেয়ার",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrowthGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    TextButton(
                        onClick = onAddDividend,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "+ ডিভিডেন্ড",
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add Buy Transaction Form Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBuyTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (exchange: String, stockName: String, buyingPrice: Double, quantity: Int, commissionPercent: Double) -> Unit
) {
    var exchange by remember { mutableStateOf("DSE") }
    var stockName by remember { mutableStateOf("") }
    var buyingPriceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var commissionText by remember { mutableStateOf("0.40") }

    val parsedPrice = BengaliFormatter.parseAmount(buyingPriceText) ?: 0.0
    val parsedQty = BengaliFormatter.parseInt(quantityText) ?: 0
    val isValid = stockName.isNotBlank() && parsedPrice > 0.0 && parsedQty > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "নতুন শেয়ার ক্রয় এন্ট্রি (Buy)",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exchange selector: DSE / CSE
                Column {
                    Text(
                        text = "এক্সচেঞ্জ (Exchange)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = exchange == "DSE",
                            onClick = { exchange = "DSE" },
                            label = { Text("ঢাকা স্টক এক্সচেঞ্জ (DSE)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryBlue
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = exchange == "CSE",
                            onClick = { exchange = "CSE" },
                            label = { Text("চট্টগ্রাম (CSE)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFF8B5CF6)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Stock Name (plain text input)
                OutlinedTextField(
                    value = stockName,
                    onValueChange = { stockName = it },
                    label = { Text("স্টক নাম / কোড (যেমন: GP, CITYBANK)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Buying Price
                OutlinedTextField(
                    value = buyingPriceText,
                    onValueChange = { buyingPriceText = it },
                    label = { Text("ক্রয় মূল্য প্রতি শেয়ার (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Quantity
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("শেয়ার সংখ্যা (Quantity)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Commission %
                OutlinedTextField(
                    value = commissionText,
                    onValueChange = { commissionText = it },
                    label = { Text("ব্রোকারেজ কমিশন (%) [ডিফল্ট ০.৪০%]") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = BengaliFormatter.parseAmount(buyingPriceText) ?: 0.0
                    val qty = BengaliFormatter.parseInt(quantityText) ?: 0
                    val comm = BengaliFormatter.parseAmount(commissionText) ?: 0.40
                    if (isValid) {
                        onSave(exchange, stockName, price, qty, comm)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("সেভ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

/**
 * Add Dividend Dialog
 */
@Composable
private fun AddDividendDialog(
    holding: HoldingEntity,
    useBengaliDigits: Boolean,
    onDismiss: () -> Unit,
    onSave: (amount: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val parsedAmount = BengaliFormatter.parseAmount(amountText) ?: 0.0
    val isValid = parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = GrowthGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ডিভিডেন্ড যোগ করুন",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${holding.stockName} (${holding.exchange})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "প্রাপ্ত নগদ ডিভিডেন্ডের মোট পরিমাণ লিখুন:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("ডিভিডেন্ড পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrowthGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (parsedAmount > 0.0 && holding.quantity > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val perShare = parsedAmount / holding.quantity
                    Text(
                        text = "প্রতি শেয়ারে আয়: ${BengaliFormatter.formatTaka(perShare, useBengaliDigits)}",
                        fontSize = 11.5.sp,
                        color = GrowthGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onSave(parsedAmount) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen)
            ) {
                Text("যোগ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

/**
 * Combined Transaction and Dividend History Dialog with Tabs
 */
@Composable
private fun TransactionAndDividendHistoryDialog(
    holding: HoldingEntity,
    useBengaliDigits: Boolean,
    onDismiss: () -> Unit,
    fetchTransactions: suspend () -> List<TransactionEntity>,
    fetchDividends: suspend () -> List<DividendEntity>,
    onDeleteDividend: (Long) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Transactions, 1: Dividends
    var txList by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    var divList by remember { mutableStateOf<List<DividendEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(holding.id) {
        txList = fetchTransactions()
        divList = fetchDividends()
        isLoading = false
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${holding.stockName} (${holding.exchange})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "লেনদেন ও ডিভিডেন্ড হিস্ট্রি",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryBlue,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "ক্রয় লেনদেন (${BengaliFormatter.toBengaliDigits(txList.size.toString())})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "ডিভিডেন্ড (${BengaliFormatter.toBengaliDigits(divList.size.toString())})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("লোড হচ্ছে...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (selectedTab == 0) {
                    // Buy Transactions List
                    if (txList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "কোনো লেনদেনের তথ্য পাওয়া যায়নি।",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(txList, key = { it.id }) { tx ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${BengaliFormatter.formatNumber(tx.quantity.toDouble(), 0, useBengaliDigits)}টি শেয়ার @ ${BengaliFormatter.formatTaka(tx.buyingPrice, useBengaliDigits)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = BengaliFormatter.formatTaka(tx.totalCost, useBengaliDigits),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = PrimaryBlue
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "কমিশন: ${BengaliFormatter.formatPercent(tx.commissionPercent, useBengaliDigits)} (কার্যকর: ${BengaliFormatter.formatTaka(tx.effectivePricePerShare, useBengaliDigits)}/টি)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = dateFormat.format(Date(tx.dateTimestamp)),
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Dividends List
                    if (divList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "এখনো কোনো ডিভিডেন্ড যোগ করা হয়নি।",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(divList, key = { it.id }) { div ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = GrowthGreen.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, GrowthGreen.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = BengaliFormatter.formatTaka(div.amount, useBengaliDigits),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = GrowthGreen
                                            )
                                            val perShare = if (holding.quantity > 0) div.amount / holding.quantity else 0.0
                                            Text(
                                                text = "প্রতি শেয়ার: ${BengaliFormatter.formatTaka(perShare, useBengaliDigits)} · ${dateFormat.format(Date(div.dateTimestamp))}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                onDeleteDividend(div.id)
                                                divList = divList.filter { it.id != div.id }
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Dividend",
                                                tint = AlertRed.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                Text("ঠিক আছে")
            }
        }
    )
}

/**
 * Exports the tracked stock portfolio data as a beautifully formatted PDF report with watermark.
 */
fun exportPortfolioReport(
    context: Context,
    holdings: List<HoldingEntity>,
    summary: PortfolioSummary,
    useBengaliDigits: Boolean,
    holdingsWithDividends: List<HoldingWithDividends> = emptyList()
) {
    PdfReportGenerator.exportPortfolioPdf(
        context = context,
        holdings = holdings,
        summary = summary,
        useBengaliDigits = useBengaliDigits,
        holdingsWithDividends = holdingsWithDividends
    )
}

/**
 * Exports the current portfolio status and unrealized gain/loss data to a local CSV file.
 */
fun exportPortfolioCsv(
    context: Context,
    holdings: List<HoldingEntity>,
    summary: PortfolioSummary,
    useBengaliDigits: Boolean,
    holdingsWithDividends: List<HoldingWithDividends> = emptyList()
) {
    CsvReportGenerator.exportPortfolioCsv(
        context = context,
        holdings = holdings,
        summary = summary,
        useBengaliDigits = useBengaliDigits,
        holdingsWithDividends = holdingsWithDividends
    )
}

private fun findActivity(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}


