package com.example.ui.screens.pdf

import com.example.ads.AdManager
import com.example.ads.PdfExportAdGate
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.LedgerPartyEntity
import com.example.data.local.entity.PdfReportEntity
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.BengaliFormatter
import java.io.File

@Composable
fun PdfReportsHubScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PdfReportsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("রিপোর্ট তৈরি", "রিপোর্ট হিস্ট্রি")

    val allReports by viewModel.allReportHistory.collectAsState()
    val allParties by viewModel.allParties.collectAsState()

    val isGenerating by viewModel.isGenerating.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val activePreview by viewModel.activePreview.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_pdf_reports_hub")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Primary Tab Navigation
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_pdf_$index")
                        )
                    }
                }

                when (selectedTabIndex) {
                    0 -> GenerateReportsTabContent(
                        parties = allParties,
                        onGenerateIncomeExpense = { period, start, end ->
                            val activity = PdfExportAdGate.findActivity(context)
                            AdManager.onPdfExportRequested(activity) {
                                viewModel.generateIncomeExpenseReport(period, start, end)
                            }
                        },
                        onGenerateSavingsInvestment = {
                            val activity = PdfExportAdGate.findActivity(context)
                            AdManager.onPdfExportRequested(activity) {
                                viewModel.generateSavingsInvestmentReport()
                            }
                        },
                        onGenerateDueReport = { kind ->
                            val activity = PdfExportAdGate.findActivity(context)
                            AdManager.onPdfExportRequested(activity) {
                                viewModel.generateDueReport(kind)
                            }
                        },
                        onGeneratePartyStatement = { partyId ->
                            val activity = PdfExportAdGate.findActivity(context)
                            AdManager.onPdfExportRequested(activity) {
                                viewModel.generatePartyStatement(partyId)
                            }
                        }
                    )
                    1 -> ReportHistoryTabContent(
                        reports = allReports,
                        onOpenReport = { report ->
                            val file = File(report.filePath)
                            viewModel.openExistingPdf(file, report.title)
                        },
                        onShareReport = { report ->
                            val file = File(report.filePath)
                            if (file.exists()) {
                                viewModel.openExistingPdf(file, report.title)
                                viewModel.shareActivePdf(context)
                            }
                        },
                        onDeleteReport = { reportId ->
                            viewModel.deleteReportHistory(reportId)
                        }
                    )
                }
            }

            // Loading overlay
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                            Text(
                                text = "প্রিন্ট-রেডি PDF তৈরি হচ্ছে...",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal PDF Viewer
    activePreview?.let { preview ->
        PdfPreviewModal(
            preview = preview,
            onClose = { viewModel.closePreview() },
            onShare = { viewModel.shareActivePdf(context) },
            onPrint = { viewModel.printActivePdf(context) },
            onSaveToUri = { uri -> viewModel.saveActivePdfToUri(context, uri) }
        )
    }
}

@Composable
private fun GenerateReportsTabContent(
    parties: List<LedgerPartyEntity>,
    onGenerateIncomeExpense: (IncomeExpenseFilterPeriod, Long?, Long?) -> Unit,
    onGenerateSavingsInvestment: () -> Unit,
    onGenerateDueReport: (String) -> Unit,
    onGeneratePartyStatement: (Long) -> Unit
) {
    var selectedPartyId by remember { mutableStateOf<Long?>(parties.firstOrNull()?.id) }
    var incomePeriod by remember { mutableStateOf(IncomeExpenseFilterPeriod.THIS_MONTH) }
    var dueFilterKind by remember { mutableStateOf("ALL") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module 1: আয়-ব্যয় রিপোর্ট
        item {
            ReportModuleCard(
                title = "আয়-ব্যয় রিপোর্ট (Income & Expense Statement)",
                subtitle = "তারিখভিত্তিক সম্পূর্ণ আয়, খরচ, নিট লাভ-লোকসান এবং ক্যাটাগরি সারসংক্ষেপ",
                icon = Icons.Default.ReceiptLong,
                accentColor = GrowthGreen
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("রিপোর্টের সময়কাল নির্বাচন করুন:", style = MaterialTheme.typography.bodySmall)

                    val periods = listOf(
                        Pair(IncomeExpenseFilterPeriod.TODAY, "আজ"),
                        Pair(IncomeExpenseFilterPeriod.THIS_WEEK, "এই সপ্তাহ"),
                        Pair(IncomeExpenseFilterPeriod.THIS_MONTH, "এই মাস"),
                        Pair(IncomeExpenseFilterPeriod.THIS_YEAR, "চলতি বছর"),
                        Pair(IncomeExpenseFilterPeriod.ALL, "সার্বিক")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(periods) { (p, label) ->
                            FilterChip(
                                selected = incomePeriod == p,
                                onClick = { incomePeriod = p },
                                label = { Text(label) }
                            )
                        }
                    }

                    Button(
                        onClick = { onGenerateIncomeExpense(incomePeriod, null, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = GrowthGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_generate_income_expense_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("আয়-ব্যয় PDF রিপোর্ট তৈরি করুন")
                    }
                }
            }
        }

        // Module 2: সঞ্চয় ও বিনিয়োগ রিপোর্ট
        item {
            ReportModuleCard(
                title = "সঞ্চয় ও বিনিয়োগ রিপোর্ট (Savings & Portfolio)",
                subtitle = "লক্ষ্যভিত্তিক নগদ জমা এবং স্টক পোর্টফোলিও সমাপনী বাজারমূল্য ও মুনাফা বিবরণী",
                icon = Icons.Default.Savings,
                accentColor = Color(0xFF1769FF)
            ) {
                Button(
                    onClick = onGenerateSavingsInvestment,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1769FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_generate_savings_pdf")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("সঞ্চয় ও বিনিয়োগ PDF রিপোর্ট তৈরি করুন")
                }
            }
        }

        // Module 3: দেনা-পাওনা ও বাকির রিপোর্ট
        item {
            ReportModuleCard(
                title = "দেনা-পাওনা ও বাকির রিপোর্ট (Due & Credit Ledger)",
                subtitle = "সকল কাস্টমার ও ব্যক্তির কাছে বাকি টাকা (পাওনা) এবং প্রদেয় (দেনা) বিবরণী",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = PrimaryBlue
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val kinds = listOf(
                        Pair("ALL", "সকল বাকি খাতা"),
                        Pair("RECEIVABLE_ONLY", "শুধু পাওনা (পাবো)"),
                        Pair("PAYABLE_ONLY", "শুধু দেনা (দিতে হবে)")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(kinds) { (k, label) ->
                            FilterChip(
                                selected = dueFilterKind == k,
                                onClick = { dueFilterKind = k },
                                label = { Text(label) }
                            )
                        }
                    }

                    Button(
                        onClick = { onGenerateDueReport(dueFilterKind) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_generate_due_report_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("বাকির খাতা PDF রিপোর্ট তৈরি করুন")
                    }
                }
            }
        }

        // Module 4: ব্যক্তিভিত্তিক লেজার স্টেটমেন্ট
        item {
            ReportModuleCard(
                title = "ব্যক্তিভিত্তিক লেজার স্টেটমেন্ট (Party Statement)",
                subtitle = "নির্দিষ্ট একজন গ্রাহক বা মহাজনের বিস্তারিত দেনা-পাওনা খতিয়ান অডিট ট্রেইল",
                icon = Icons.Default.Person,
                accentColor = Color(0xFF8E24AA)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (parties.isEmpty()) {
                        Text(
                            text = "বাকির খাতায় কোন ব্যক্তি যুক্ত নেই। প্রথমে বাকির খাতায় ব্যক্তি যোগ করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("ব্যক্তি নির্বাচন করুন:", style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(parties) { p ->
                                FilterChip(
                                    selected = selectedPartyId == p.id,
                                    onClick = { selectedPartyId = p.id },
                                    label = { Text(p.name) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                selectedPartyId?.let { onGeneratePartyStatement(it) }
                            },
                            enabled = selectedPartyId != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_generate_party_statement_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ব্যক্তির স্টেটমেন্ট PDF তৈরি করুন")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportModuleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            content()
        }
    }
}

@Composable
private fun ReportHistoryTabContent(
    reports: List<PdfReportEntity>,
    onOpenReport: (PdfReportEntity) -> Unit,
    onShareReport: (PdfReportEntity) -> Unit,
    onDeleteReport: (Long) -> Unit
) {
    if (reports.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "কোন সংরক্ষিত রিপোর্ট নেই",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "নতুন রিপোর্ট তৈরি করলে তা এখানে সংরক্ষিত থাকবে।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reports, key = { it.id }) { report ->
                Card(
                    onClick = { onOpenReport(report) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_history_${report.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryBlue)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            report.subtitle?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = BengaliFormatter.formatBengaliDate(report.createdAt) + " • ${report.fileSizeBytes / 1024} KB",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        Row {
                            IconButton(onClick = { onShareReport(report) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = PrimaryBlue)
                            }
                            IconButton(onClick = { onDeleteReport(report.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
