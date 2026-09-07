package com.example.ui.screens.portfolio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val holdings by viewModel.holdings.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scope = rememberCoroutineScope()

    // Manage lifecycle of auto-refresh timer (start when visible, stop when leaving)
    DisposableEffect(Unit) {
        viewModel.startAutoRefresh()
        onDispose {
            viewModel.stopAutoRefresh()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedHoldingForHistory by remember { mutableStateOf<HoldingEntity?>(null) }
    var holdingToDelete by remember { mutableStateOf<HoldingEntity?>(null) }
    var holdingToEditPrice by remember { mutableStateOf<HoldingEntity?>(null) }

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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshPricesManually() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Header Bar with Title & Manual Refresh Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "আমার পোর্টফোলিও",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "DSE ও CSE শেয়ারের অন-ডিভাইস হিসাব",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshPricesManually() },
                            modifier = Modifier.testTag("portfolio_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Prices",
                                tint = PrimaryBlue
                            )
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

                if (holdings.isEmpty()) {
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
                                text = "হোল্ডিংস তালিকা (${BengaliFormatter.toBengaliDigits(holdings.size.toString())}টি স্টক)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "হিস্ট্রি দেখতে ট্যাপ করুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(holdings, key = { it.id }) { holding ->
                        HoldingItemCard(
                            holding = holding,
                            useBengaliDigits = useBengaliDigits,
                            onClick = { selectedHoldingForHistory = holding },
                            onEditPrice = { holdingToEditPrice = holding },
                            onDelete = { holdingToDelete = holding }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp)) // Space for FAB
                    }
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

    // Transaction History Dialog
    selectedHoldingForHistory?.let { holding ->
        TransactionHistoryDialog(
            holding = holding,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { selectedHoldingForHistory = null },
            fetchTransactions = { viewModel.getTransactionHistory(holding.id) }
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
                    text = "আপনি কি নিশ্চিতভাবে ${holding.exchange} এর \"${holding.stockName}\" হোল্ডিং এবং এর পূর্ববর্তী সমস্ত ক্রয়ের রেকর্ড মুছে ফেলতে চান?",
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

    // Edit Current Price Dialog
    holdingToEditPrice?.let { holding ->
        EditCurrentPriceDialog(
            holding = holding,
            useBengaliDigits = useBengaliDigits,
            onDismiss = { holdingToEditPrice = null },
            onSave = { newPrice ->
                viewModel.updateHoldingCurrentPrice(holding.id, newPrice)
                holdingToEditPrice = null
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
                    text = "Current Investment",
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

            // Main Metrics: Invested & Current Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "মোট বিনিয়োগ (Total Invested)",
                        fontSize = 11.5.sp,
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
                        text = "বর্তমান বাজার মূল্য (Current Value)",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = BengaliFormatter.formatTaka(summary.totalCurrentValue, useBengaliDigits),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlue
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Unrealized Gain/Loss Banner
            val isProfit = summary.unrealizedGainLoss >= 0
            val gainLossColor = if (isProfit) GrowthGreen else AlertRed
            val bgColor = if (isProfit) GrowthGreen.copy(alpha = 0.12f) else AlertRed.copy(alpha = 0.12f)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                border = BorderStroke(1.dp, gainLossColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = gainLossColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "অবাস্তবায়িত লাভ / ক্ষতি:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val prefix = if (isProfit) "+" else ""
                    Text(
                        text = "$prefix${BengaliFormatter.formatTaka(summary.unrealizedGainLoss, useBengaliDigits)} ($prefix${BengaliFormatter.formatPercent(summary.unrealizedGainLossPercent, useBengaliDigits)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = gainLossColor
                    )
                }
            }
        }
    }
}

/**
 * Holding Card as per exact layout in section 4 of the specification:
 * - Row 1: Exchange badge (DSE/CSE) + Stock Name + Quantity (e.g. "DSE · City Bank · ২০টি শেয়ার")
 * - Row 2 (primary, large): Total Price (Quantity × Average Buying Price)
 *   Directly below it, in small muted text: the average buying price per share (e.g. "গড় দাম: ৳২৫.১০/শেয়ার")
 * - Row 3: Current Price field — tappable/editable inline (shows a small edit icon), defaults to the average buying price
 * - Row 4: Unrealized Gain/Loss — (Current Price − Average Buying Price) × Quantity, shown as an amount (৳) and percentage
 */
@Composable
private fun HoldingItemCard(
    holding: HoldingEntity,
    useBengaliDigits: Boolean,
    onClick: () -> Unit,
    onEditPrice: () -> Unit,
    onDelete: () -> Unit
) {
    val totalPrice = holding.quantity * holding.averagePrice
    val currentHoldingValue = holding.quantity * holding.currentPrice
    val unrealizedGainLoss = currentHoldingValue - totalPrice
    val unrealizedPercent = if (totalPrice > 0) (unrealizedGainLoss / totalPrice) * 100.0 else 0.0
    val isProfit = unrealizedGainLoss >= 0
    val gainLossColor = if (isProfit) GrowthGreen else AlertRed

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
            // Row 1: Exchange badge (DSE/CSE) + Stock Name + Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2 (primary, large): Total Price (Quantity × Average Buying Price)
            // Directly below it, in small muted text: average buying price per share
            Column {
                Text(
                    text = "মোট ক্রয় মূল্য (Total Price)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = BengaliFormatter.formatTaka(totalPrice, useBengaliDigits),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "গড় দাম: ${BengaliFormatter.formatTaka(holding.averagePrice, useBengaliDigits)}/শেয়ার",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Current Price field — tappable/editable inline (shows a small edit icon)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEditPrice),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "বর্তমান বাজার দর:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${BengaliFormatter.formatTaka(holding.currentPrice, useBengaliDigits)}/শেয়ার",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "আপডেট",
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Current Price",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Unrealized Gain/Loss
            val prefix = if (isProfit) "+" else ""
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "অবাস্তবায়িত লাভ / ক্ষতি:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$prefix${BengaliFormatter.formatTaka(unrealizedGainLoss, useBengaliDigits)} ($prefix${BengaliFormatter.formatPercent(unrealizedPercent, useBengaliDigits)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = gainLossColor
                )
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
    var isTickerDropdownExpanded by remember { mutableStateOf(false) }

    val filteredTickers = remember(stockName) {
        if (stockName.isBlank()) {
            PortfolioRepository.COMMON_TICKERS.take(8)
        } else {
            PortfolioRepository.COMMON_TICKERS.filter {
                it.contains(stockName.trim(), ignoreCase = true)
            }.take(8)
        }
    }

    val isValid = stockName.isNotBlank() &&
            (buyingPriceText.toDoubleOrNull() ?: 0.0) > 0.0 &&
            (quantityText.toIntOrNull() ?: 0) > 0

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

                // Stock Name with Autocomplete dropdown + free text
                ExposedDropdownMenuBox(
                    expanded = isTickerDropdownExpanded,
                    onExpandedChange = { isTickerDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = stockName,
                        onValueChange = {
                            stockName = it
                            isTickerDropdownExpanded = true
                        },
                        label = { Text("স্টক নাম / কোড (যেমন: GP, CITYBANK)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTickerDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    if (filteredTickers.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = isTickerDropdownExpanded,
                            onDismissRequest = { isTickerDropdownExpanded = false }
                        ) {
                            filteredTickers.forEach { ticker ->
                                DropdownMenuItem(
                                    text = { Text(ticker) },
                                    onClick = {
                                        stockName = ticker
                                        isTickerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

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
                    val price = buyingPriceText.toDoubleOrNull() ?: 0.0
                    val qty = quantityText.toIntOrNull() ?: 0
                    val comm = commissionText.toDoubleOrNull() ?: 0.40
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
 * Transaction History Dialog
 */
@Composable
private fun TransactionHistoryDialog(
    holding: HoldingEntity,
    useBengaliDigits: Boolean,
    onDismiss: () -> Unit,
    fetchTransactions: suspend () -> List<TransactionEntity>
) {
    var history by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(holding.id) {
        history = fetchTransactions()
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
                        text = "ক্রয় লেনদেনের ইতিহাস (Buy History)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("লোড হচ্ছে...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (history.isEmpty()) {
                Text(
                    text = "কোনো লেনদেনের তথ্য পাওয়া যায়নি।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history) { tx ->
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
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = BengaliFormatter.formatTaka(tx.totalCost, useBengaliDigits),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryBlue
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "কমিশন: ${BengaliFormatter.formatPercent(tx.commissionPercent, useBengaliDigits)} (কার্যকর: ${BengaliFormatter.formatTaka(tx.effectivePricePerShare, useBengaliDigits)}/টি)",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateFormat.format(Date(tx.dateTimestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
 * Edit Current Price Dialog
 */
@Composable
private fun EditCurrentPriceDialog(
    holding: HoldingEntity,
    useBengaliDigits: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var priceText by remember { mutableStateOf(holding.currentPrice.toString()) }
    val newPrice = priceText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${holding.stockName} এর বাজার দর আপডেট",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Text(
                    text = "বর্তমান দাম ম্যানুয়ালি আপডেট করুন (গড় ক্রয়মূল্য: ${BengaliFormatter.formatTaka(holding.averagePrice, useBengaliDigits)}):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("নতুন বাজার দর (৳)") },
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
                onClick = { if (newPrice > 0) onSave(newPrice) },
                enabled = newPrice > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("আপডেট করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
