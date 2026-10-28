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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdManager
import com.example.data.local.entity.DividendEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.TransactionEntity
import com.example.util.BengaliFormatter
import com.example.util.CsvReportGenerator
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Design System Colors per Requirement 8:
private val PortfolioNavy = Color(0xFF0B1F4B)
private val PortfolioBlue = Color(0xFF1769FF)
private val PortfolioGreen = Color(0xFF19C77A)
private val PortfolioRed = Color(0xFFEF5350)
private val CsePurple = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    useBengaliDigits: Boolean = true,
    viewModel: PortfolioViewModel = viewModel(),
    openAddSheetOnStart: Boolean = false,
    onAddSheetOpened: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val holdings by viewModel.holdings.collectAsState()
    val holdingsWithDividends by viewModel.holdingsWithDividends.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val watchlistItems by viewModel.watchlistItems.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: হোল্ডিং, 1: ওয়াচলিস্ট
    var selectedExchangeFilter by remember { mutableStateOf("সব") } // সব, DSE, CSE
    var showAddHoldingSheet by remember { mutableStateOf(false) }
    var showAddWatchlistDialog by remember { mutableStateOf(false) }

    var selectedHoldingForHistory by remember { mutableStateOf<HoldingEntity?>(null) }
    var holdingForDividend by remember { mutableStateOf<HoldingEntity?>(null) }
    var holdingToDelete by remember { mutableStateOf<HoldingEntity?>(null) }
    var watchlistStockToDelete by remember { mutableStateOf<WatchlistItem?>(null) }

    // Direct open from Home (+) button
    LaunchedEffect(openAddSheetOnStart) {
        if (openAddSheetOnStart) {
            selectedTabIndex = 0
            showAddHoldingSheet = true
            onAddSheetOpened()
        }
    }

    // Requirement: Do NOT show any ad format inside the Portfolio buy/sell transaction flow
    val isInsideTxFlow = showAddHoldingSheet || holdingForDividend != null
    LaunchedEffect(isInsideTxFlow) {
        AdManager.setInsideTransactionFlow(isInsideTxFlow)
    }

    // Lifecycle: Foreground 60s price polling. Stops cleanly on dispose.
    DisposableEffect(Unit) {
        viewModel.startLivePricePolling()
        onDispose {
            AdManager.setInsideTransactionFlow(false)
            viewModel.stopLivePricePolling()
        }
    }

    // Filter holdings list by selected chip
    val filteredHoldings = remember(holdingsWithDividends, selectedExchangeFilter) {
        when (selectedExchangeFilter) {
            "DSE" -> holdingsWithDividends.filter { it.holding.exchange.equals("DSE", ignoreCase = true) }
            "CSE" -> holdingsWithDividends.filter { it.holding.exchange.equals("CSE", ignoreCase = true) }
            else -> holdingsWithDividends
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        showAddHoldingSheet = true
                    } else {
                        showAddWatchlistDialog = true
                    }
                },
                containerColor = PortfolioBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("portfolio_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Bar: Subtitle & Quick Actions (Refresh with Ad gate, CSV, PDF)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRefreshing) "বাজার থেকে লাইভ দর সংগ্রহ করা হচ্ছে..." else "DSE ও CSE লাইভ দর ও রিয়েলটাইম হিসাব",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRefreshing) PortfolioBlue else MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("portfolio_refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = PortfolioBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Live Prices",
                                tint = PortfolioBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            exportPortfolioCsv(context, holdings, summary, useBengaliDigits, holdingsWithDividends)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("portfolio_export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export CSV",
                            tint = PortfolioBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            exportPortfolioReport(context, holdings, summary, useBengaliDigits, holdingsWithDividends)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("portfolio_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF",
                            tint = PortfolioBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Top Two Tabs: "হোল্ডিং" & "ওয়াচলিস্ট" (Requirement 4)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PortfolioBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PortfolioBlue,
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "হোল্ডিং (${BengaliFormatter.toBengaliDigits(holdings.size.toString())})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.testTag("portfolio_tab_holdings")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "ওয়াচলিস্ট (${BengaliFormatter.toBengaliDigits(watchlistItems.size.toString())})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.testTag("portfolio_tab_watchlist")
                )
            }

            // Main Tab Content with LazyColumn (Performance Requirement 10)
            if (selectedTabIndex == 0) {
                // ==================== "হোল্ডিং" TAB ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))

                        // 1. Hero Card (Dark Navy #0B1F4B) per Requirement 5
                        PortfolioHeroCard(
                            summary = summary,
                            useBengaliDigits = useBengaliDigits
                        )
                    }

                    item {
                        // 2. Two Small Cards Side-by-Side: মোট বিনিয়োগ & ডিভিডেন্ড প্রাপ্ত (মোট)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricCard(
                                title = "মোট বিনিয়োগ",
                                amount = BengaliFormatter.formatTaka(summary.totalInvested, useBengaliDigits),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "ডিভিডেন্ড প্রাপ্ত (মোট)",
                                amount = BengaliFormatter.formatTaka(summary.totalDividend, useBengaliDigits),
                                amountColor = PortfolioGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        // 3. Exchange Donut Chart & Legend (Requirement 5)
                        ExchangeDonutChartCard(
                            holdings = holdings,
                            useBengaliDigits = useBengaliDigits
                        )
                    }

                    item {
                        // 4. Filter Chips: সব / DSE / CSE (Requirement 5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ফিল্টার:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            listOf("সব", "DSE", "CSE").forEach { filter ->
                                val isSelected = selectedExchangeFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedExchangeFilter = filter },
                                    label = { Text(filter) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PortfolioBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = PortfolioBlue
                                    ),
                                    modifier = Modifier.testTag("filter_chip_$filter")
                                )
                            }
                        }
                    }

                    // 5. Holding Card list
                    if (filteredHoldings.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = PortfolioBlue.copy(alpha = 0.12f),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PieChart,
                                                contentDescription = null,
                                                tint = PortfolioBlue,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = if (holdings.isEmpty()) "আপনার পোর্টফোলিও ফাঁকা" else "এই ফিল্টারে কোনো হোল্ডিং নেই",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (holdings.isEmpty()) "নিচের '+' বাটনে ট্যাপ করে আপনার প্রথম স্টক ক্রয় এন্ট্রি করুন।" else "অন্য এক্সচেঞ্জ নির্বাচন করুন অথবা নতুন স্টক যুক্ত করুন।",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredHoldings, key = { it.holding.id }) { item ->
                            HoldingItemCard(
                                holdingWithDiv = item,
                                useBengaliDigits = useBengaliDigits,
                                onClick = { selectedHoldingForHistory = item.holding },
                                onAddDividend = { holdingForDividend = item.holding },
                                onDelete = { holdingToDelete = item.holding }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp)) // Padding for FAB
                    }
                }
            } else {
                // ==================== "ওয়াচলিস্ট" TAB (Requirement 6) ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "লাইভ ওয়াচলিস্ট",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "স্টক মুছতে সোয়াইপ বা লং-প্রেস করুন",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (watchlistItems.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp)
                                    .testTag("watchlist_empty_card"),
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
                                        color = PortfolioBlue.copy(alpha = 0.12f),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = PortfolioBlue,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Requirement 6 Exact Message:
                                    Text(
                                        text = "ওয়াচলিস্টে কোনো স্টক নেই। + চেপে যোগ করুন।",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { showAddWatchlistDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = PortfolioBlue)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ওয়াচলিস্টে স্টক যোগ করুন")
                                    }
                                }
                            }
                        }
                    } else {
                        items(watchlistItems, key = { "${it.exchange}:${it.symbol}" }) { item ->
                            WatchlistItemRow(
                                item = item,
                                useBengaliDigits = useBengaliDigits,
                                onLongClick = { watchlistStockToDelete = item },
                                onDelete = { viewModel.removeFromWatchlist(item.exchange, item.symbol) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // 6 & 7. Add Holding Bottom Sheet with Live Preview (Requirement 5)
    if (showAddHoldingSheet) {
        AddHoldingBottomSheet(
            holdings = holdings,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { showAddHoldingSheet = false },
            onSave = { exchange, stockName, buyingPrice, qty, commission ->
                viewModel.addBuyTransaction(
                    exchange = exchange,
                    stockName = stockName,
                    buyingPrice = buyingPrice,
                    quantity = qty,
                    commissionPercent = commission
                )
                showAddHoldingSheet = false
            }
        )
    }

    // Add Watchlist Dialog
    if (showAddWatchlistDialog) {
        AddWatchlistDialog(
            onDismiss = { showAddWatchlistDialog = false },
            onAdd = { exchange, symbol ->
                val added = viewModel.addToWatchlist(exchange, symbol)
                if (added) {
                    Toast.makeText(context, "$symbol ওয়াচলিস্টে যোগ হয়েছে", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "$symbol ইতিমধ্যে ওয়াচলিস্টে আছে", Toast.LENGTH_SHORT).show()
                }
                showAddWatchlistDialog = false
            }
        )
    }

    // Confirm Delete Watchlist Dialog
    watchlistStockToDelete?.let { stock ->
        AlertDialog(
            onDismissRequest = { watchlistStockToDelete = null },
            title = { Text("ওয়াচলিস্ট থেকে মুছবেন?", fontWeight = FontWeight.Bold) },
            text = { Text("${stock.exchange}: ${stock.symbol} ওয়াচলিস্ট থেকে মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFromWatchlist(stock.exchange, stock.symbol)
                        watchlistStockToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PortfolioRed)
                ) {
                    Text("মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { watchlistStockToDelete = null }) {
                    Text("বাতিল")
                }
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

    // Transaction History Dialog
    selectedHoldingForHistory?.let { holding ->
        HoldingHistoryDialog(
            holding = holding,
            viewModel = viewModel,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { selectedHoldingForHistory = null },
            onAddDividend = { holdingForDividend = holding }
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
                    text = "${holding.stockName} (${holding.exchange})-এর সকল লেনদেন ও ডিভিডেন্ড হিস্ট্রি স্থায়ীভাবে মুছে যাবে। এই কাজ পূর্বাবস্থায় ফিরিয়ে আনা যাবে না।",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHolding(holding)
                        holdingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PortfolioRed)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { holdingToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

/**
 * 1. Hero Card (Dark Navy #0B1F4B) per Requirement 5:
 * মোট পোর্টফোলিও মূল্য (বড়), নিচে মোট লাভ/ক্ষতি ও শতাংশ (সবুজ ▲ / লাল ▼)
 */
@Composable
private fun PortfolioHeroCard(
    summary: PortfolioSummary,
    useBengaliDigits: Boolean
) {
    val totalGL = summary.totalUnrealizedGainLoss
    val invested = summary.totalInvested
    val glPercent = if (invested > 0.0) (totalGL / invested) * 100.0 else 0.0
    val isPositive = totalGL >= 0.0
    val glColor = if (isPositive) PortfolioGreen else PortfolioRed
    val arrow = if (isPositive) "▲" else "▼"
    val sign = if (isPositive) "+" else "-"
    val formattedGl = BengaliFormatter.formatTaka(abs(totalGL), useBengaliDigits)
    val formattedPct = BengaliFormatter.formatNumber(abs(glPercent), 2, useBengaliDigits)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_hero_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PortfolioNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "মোট পোর্টফোলিও মূল্য",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = BengaliFormatter.formatTaka(summary.currentTotalValue, useBengaliDigits),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = glColor.copy(alpha = 0.18f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$arrow $sign$formattedGl ($sign$formattedPct%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = glColor
                    )
                }
            }
        }
    }
}

/**
 * 2. Small Metric Card for Side-by-Side row: মোট বিনিয়োগ & ডিভিডেন্ড প্রাপ্ত (মোট)
 */
@Composable
private fun MetricCard(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    amountColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

/**
 * 3. Donut Chart Card by Exchange (DSE Blue, CSE Green) & Legend (Requirement 5 & 10)
 * Lightweight custom Canvas draw, zero external charting libraries.
 */
@Composable
private fun ExchangeDonutChartCard(
    holdings: List<HoldingEntity>,
    useBengaliDigits: Boolean
) {
    val dseCount = holdings.count { it.exchange.equals("DSE", ignoreCase = true) }
    val cseCount = holdings.count { it.exchange.equals("CSE", ignoreCase = true) }
    val totalCount = dseCount + cseCount

    val dseRatio = if (totalCount > 0) dseCount.toFloat() / totalCount else 1f
    val cseRatio = if (totalCount > 0) cseCount.toFloat() / totalCount else 0f

    val dsePct = if (totalCount > 0) ((dseCount.toDouble() / totalCount) * 100).toInt() else 0
    val csePct = if (totalCount > 0) 100 - dsePct else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Donut Canvas
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    if (totalCount == 0) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth)
                        )
                    } else {
                        // DSE arc
                        val dseSweep = dseRatio * 360f
                        drawArc(
                            color = PortfolioBlue,
                            startAngle = -90f,
                            sweepAngle = dseSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        // CSE arc
                        if (cseCount > 0) {
                            val cseSweep = cseRatio * 360f
                            drawArc(
                                color = PortfolioGreen,
                                startAngle = -90f + dseSweep,
                                sweepAngle = cseSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        }
                    }
                }

                Text(
                    text = "${BengaliFormatter.toBengaliDigits(totalCount.toString())}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Legend
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "এক্সচেঞ্জ অনুপাত",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PortfolioBlue))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DSE: ${BengaliFormatter.toBengaliDigits(dsePct.toString())}% (${BengaliFormatter.toBengaliDigits(dseCount.toString())}টি)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PortfolioGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CSE: ${BengaliFormatter.toBengaliDigits(csePct.toString())}% (${BengaliFormatter.toBengaliDigits(cseCount.toString())}টি)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 5. Holding Card Item per Requirement 5:
 * - উপরে বামে: এক্সচেঞ্জ ট্যাগ + স্টকের নাম; নিচে "X শেয়ার · গড় ৳Y"
 * - উপরে ডানে: মোট খরচ; নিচে ছোট করে বর্তমান মূল্য
 * - নিচের সারিতে তিনটি তথ্য: আনরিয়ালাইজড লাভ/ক্ষতি (টাকা ও %), ডিভিডেন্ড প্রাপ্ত, শেয়ার প্রতি রিটার্ন
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
    val totalCost = holding.quantity * holding.averagePrice
    val currentVal = if (holdingWithDiv.currentPrice != null && holdingWithDiv.currentPrice > 0.0) {
        holdingWithDiv.currentPrice * holding.quantity
    } else {
        totalCost
    }

    val unrealizedGL = holdingWithDiv.unrealizedGainLoss ?: 0.0
    val glPercent = holdingWithDiv.unrealizedGainLossPercent ?: 0.0
    val isPositive = unrealizedGL >= 0.0
    val glColor = if (isPositive) PortfolioGreen else PortfolioRed
    val arrow = if (isPositive) "▲" else "▼"
    val sign = if (isPositive) "+" else "-"

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
            // Top Row:
            // Left: Exchange Tag + Stock Name. Below: "X শেয়ার · গড় ৳Y"
            // Right: Total Cost. Below: Current Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (holding.exchange.uppercase() == "DSE") PortfolioBlue else CsePurple
                        ) {
                            Text(
                                text = holding.exchange.uppercase(),
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = holding.stockName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${BengaliFormatter.toBengaliDigits(holding.quantity.toString())} শেয়ার · গড় ${BengaliFormatter.formatTaka(holding.averagePrice, useBengaliDigits)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = BengaliFormatter.formatTaka(totalCost, useBengaliDigits),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "বর্তমান: ${BengaliFormatter.formatTaka(currentVal, useBengaliDigits)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Bottom Row: 3 Metrics: আনরিয়ালাইজড লাভ/ক্ষতি, ডিভিডেন্ড প্রাপ্ত, শেয়ার প্রতি রিটার্ন
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Unrealized G/L
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        text = "লাভ/ক্ষতি",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$arrow $sign${BengaliFormatter.formatTaka(abs(unrealizedGL), useBengaliDigits)} ($sign${BengaliFormatter.formatNumber(abs(glPercent), 1, useBengaliDigits)}%)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = glColor
                    )
                }

                // Metric 2: Dividend Received
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ডিভিডেন্ড",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = BengaliFormatter.formatTaka(holdingWithDiv.totalDividend, useBengaliDigits),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PortfolioGreen
                    )
                }

                // Metric 3: Return per Share
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "প্রতি শেয়ার আয়",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = BengaliFormatter.formatTaka(holdingWithDiv.returnPerShare, useBengaliDigits),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Watchlist Item Row per Requirement 6:
 * নাম, এক্সচেঞ্জ, ছোট স্পার্কলাইন, বর্তমান দাম, দৈনিক পরিবর্তন % (সবুজ/লাল)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WatchlistItemRow(
    item: WatchlistItem,
    useBengaliDigits: Boolean,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val change = item.changePercent ?: 0.0
    val isPositive = change >= 0.0
    val changeColor = if (isPositive) PortfolioGreen else PortfolioRed
    val sign = if (isPositive) "+" else "-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .testTag("watchlist_row_${item.symbol}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Exchange Badge + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.2f)
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = if (item.exchange.uppercase() == "DSE") PortfolioBlue else CsePurple
                ) {
                    Text(
                        text = item.exchange.uppercase(),
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = item.symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Center: Sparkline Canvas (Lightweight custom draw per Requirement 6 & 10)
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 26.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                SparklineView(
                    points = item.sparkline,
                    lineColor = changeColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Current Price & Daily change %
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (item.currentPrice != null && item.currentPrice > 0.0) {
                        BengaliFormatter.formatTaka(item.currentPrice, useBengaliDigits)
                    } else {
                        "—"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$sign${BengaliFormatter.formatNumber(abs(change), 2, useBengaliDigits)}%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from Watchlist",
                    tint = PortfolioRed.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Lightweight Sparkline Custom Canvas View (Requirement 6 & 10):
 * Draws local points (max 30 points). Draws straight horizontal line if no data.
 */
@Composable
private fun SparklineView(
    points: List<Double>,
    lineColor: Color
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        if (points.size < 2) {
            // Straight line when no history or single point
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }

        val min = points.minOrNull() ?: 0.0
        val max = points.maxOrNull() ?: 1.0
        val range = if (max > min) max - min else 1.0

        val path = Path()
        val stepX = w / (points.size - 1)

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val normalizedY = (p - min) / range
            val y = (h - (normalizedY * h)).toFloat().coerceIn(0f, h)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * 6 & 7. Add Holding Bottom Sheet with Live Preview (Requirement 5)
 * Shows live preview:
 * - If stock already exists: "মার্জ হয়ে হবে X শেয়ার, গড় ৳Y"
 * - If not exists: "নতুন হোল্ডিং"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHoldingBottomSheet(
    holdings: List<HoldingEntity>,
    useBengaliDigits: Boolean,
    onDismiss: () -> Unit,
    onSave: (exchange: String, stockName: String, buyingPrice: Double, quantity: Int, commissionPercent: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var exchange by remember { mutableStateOf("DSE") }
    var stockName by remember { mutableStateOf("") }
    var buyingPriceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var commissionText by remember { mutableStateOf("0.40") }

    val parsedPrice = BengaliFormatter.parseAmount(buyingPriceText) ?: 0.0
    val parsedQty = BengaliFormatter.parseInt(quantityText) ?: 0
    val parsedComm = BengaliFormatter.parseAmount(commissionText) ?: 0.40
    val isValid = stockName.isNotBlank() && parsedPrice > 0.0 && parsedQty > 0

    // Live preview calculation:
    val trimmedStock = stockName.trim().uppercase()
    val existingHolding = remember(exchange, trimmedStock, holdings) {
        if (trimmedStock.isNotBlank()) {
            holdings.find {
                it.exchange.equals(exchange, ignoreCase = true) &&
                        it.stockName.trim().equals(trimmedStock, ignoreCase = true)
            }
        } else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "নতুন শেয়ার ক্রয় এন্ট্রি (Add Holding)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Exchange Selection Chips (DSE / CSE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = exchange == "DSE",
                    onClick = { exchange = "DSE" },
                    label = { Text("ঢাকা স্টক এক্সচেঞ্জ (DSE)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PortfolioBlue.copy(alpha = 0.15f),
                        selectedLabelColor = PortfolioBlue
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = exchange == "CSE",
                    onClick = { exchange = "CSE" },
                    label = { Text("চট্টগ্রাম (CSE)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CsePurple.copy(alpha = 0.15f),
                        selectedLabelColor = CsePurple
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Stock Name (plain text input per Requirement 1)
            OutlinedTextField(
                value = stockName,
                onValueChange = { stockName = it },
                label = { Text("স্টক নাম / কোড (যেমন: GP, CITYBANK)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PortfolioBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = buyingPriceText,
                    onValueChange = { buyingPriceText = it },
                    label = { Text("ক্রয় মূল্য প্রতি শেয়ার (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PortfolioBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("শেয়ার সংখ্যা") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PortfolioBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            OutlinedTextField(
                value = commissionText,
                onValueChange = { commissionText = it },
                label = { Text("ব্রোকারেজ কমিশন (%) [ডিফল্ট ০.৪০%]") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PortfolioBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // 7. Live Preview Card before saving (Requirement 5)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_holding_live_preview"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (existingHolding != null) PortfolioBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (existingHolding != null) PortfolioBlue.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "লাইভ প্রিভিউ (Live Preview):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isValid) {
                        val effectivePrice = parsedPrice * (1.0 + (parsedComm / 100.0))
                        if (existingHolding != null) {
                            val oldQty = existingHolding.quantity
                            val oldAvgPrice = existingHolding.averagePrice
                            val newTotalQty = oldQty + parsedQty
                            val newAvgPrice = ((oldQty * oldAvgPrice) + (parsedQty * effectivePrice)) / newTotalQty

                            Text(
                                text = "মার্জ হয়ে হবে ${BengaliFormatter.toBengaliDigits(newTotalQty.toString())} শেয়ার, গড় ${BengaliFormatter.formatTaka(newAvgPrice, useBengaliDigits)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PortfolioBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "পূর্বের রেকর্ড: ${BengaliFormatter.toBengaliDigits(oldQty.toString())} শেয়ার @ ${BengaliFormatter.formatTaka(oldAvgPrice, useBengaliDigits)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "নতুন হোল্ডিং",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PortfolioGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${BengaliFormatter.toBengaliDigits(parsedQty.toString())} শেয়ার, কার্যকর গড় ক্রয়মূল্য ${BengaliFormatter.formatTaka(effectivePrice, useBengaliDigits)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = if (stockName.isNotBlank() && existingHolding != null) {
                                "স্টকটি আগে থেকেই আছে — দর ও পরিমাণ লিখলে মার্জ ফলাফল দেখতে পাবেন"
                            } else {
                                "স্টকের নাম, দর ও পরিমাণ লিখলে প্রিভিউ দেখতে পাবেন"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (isValid) {
                        onSave(exchange, stockName, parsedPrice, parsedQty, parsedComm)
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_holding_save_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PortfolioBlue)
            ) {
                Text(
                    text = "ক্রয় এন্ট্রি সংরক্ষণ করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Add Stock to Watchlist Dialog
 */
@Composable
private fun AddWatchlistDialog(
    onDismiss: () -> Unit,
    onAdd: (exchange: String, symbol: String) -> Unit
) {
    var exchange by remember { mutableStateOf("DSE") }
    var symbol by remember { mutableStateOf("") }
    val isValid = symbol.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ওয়াচলিস্টে স্টক যোগ করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ক্রয় ব্যতীত স্টক ট্র্যাক করতে ওয়াচলিস্টে যোগ করুন।",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = exchange == "DSE",
                        onClick = { exchange = "DSE" },
                        label = { Text("DSE") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = exchange == "CSE",
                        onClick = { exchange = "CSE" },
                        label = { Text("CSE") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("স্টক কোড (যেমন: GP, BEXIMCO)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PortfolioBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onAdd(exchange, symbol.trim().uppercase())
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = PortfolioBlue)
            ) {
                Text("যোগ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
            Text(
                text = "ডিভিডেন্ড আয় যুক্ত করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${holding.stockName} (${holding.exchange})-এর প্রাপ্ত মোট ডিভিডেন্ড টাকা লিখুন।",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("মোট প্রাপ্ত ডিভিডেন্ড (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PortfolioBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSave(parsedAmount)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = PortfolioGreen)
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

/**
 * Dialog showing transaction history and dividend records for a holding.
 */
@Composable
private fun HoldingHistoryDialog(
    holding: HoldingEntity,
    viewModel: PortfolioViewModel,
    useBengaliDigits: Boolean,
    onDismiss: () -> Unit,
    onAddDividend: () -> Unit
) {
    var transactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    var dividends by remember { mutableStateOf<List<DividendEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(holding.id) {
        scope.launch {
            transactions = viewModel.getTransactionHistory(holding.id)
            dividends = viewModel.getDividendHistory(holding.id)
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${holding.stockName} (${holding.exchange}) বিবরণ",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "মোট শেয়ার: ${BengaliFormatter.toBengaliDigits(holding.quantity.toString())}টি",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "গড় দর: ${BengaliFormatter.formatTaka(holding.averagePrice, useBengaliDigits)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "ক্রয় লেনদেনের ইতিহাস (${BengaliFormatter.toBengaliDigits(transactions.size.toString())}টি):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                transactions.forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${BengaliFormatter.toBengaliDigits(tx.quantity.toString())} শেয়ার @ ${BengaliFormatter.formatTaka(tx.buyingPrice, useBengaliDigits)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = dateFormat.format(Date(tx.dateTimestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "কমিশন ${BengaliFormatter.formatNumber(tx.commissionPercent, 2, useBengaliDigits)}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ডিভিডেন্ড আয় (${BengaliFormatter.toBengaliDigits(dividends.size.toString())}টি):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddDividend) {
                        Text("+ ডিভিডেন্ড", fontSize = 12.sp, color = PortfolioGreen)
                    }
                }

                dividends.forEach { div ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = BengaliFormatter.formatTaka(div.amount, useBengaliDigits),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PortfolioGreen
                        )
                        IconButton(
                            onClick = {
                                viewModel.deleteDividend(div.id)
                                scope.launch {
                                    dividends = viewModel.getDividendHistory(holding.id)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Dividend",
                                tint = PortfolioRed.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

// Helpers for CSV and PDF export (Preserved per Requirement 1 & 9)
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
