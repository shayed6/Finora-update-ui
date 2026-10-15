package com.example.ui.screens.pdf

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.InvoiceWithItems
import com.example.data.local.entity.PdfReportEntity
import com.example.data.local.entity.ReportType
import com.example.data.preferences.OrganizationInfo
import com.example.data.preferences.OrganizationInfoRepository
import com.example.data.repository.InvoiceRepository
import com.example.ui.screens.portfolio.PortfolioSummary
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

data class ActivePdfPreview(
    val file: File,
    val title: String,
    val pageBitmaps: List<Bitmap>
)

enum class IncomeExpenseFilterPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL,
    CUSTOM
}

class PdfReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val invoiceRepo = InvoiceRepository.getInstance(application)
    private val orgInfoRepo = OrganizationInfoRepository.getInstance(application)

    val orgInfo: StateFlow<OrganizationInfo> = orgInfoRepo.organizationInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrganizationInfo())

    val allParties = db.ledgerDao().getAllParties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow("ALL") // "ALL", "PAID", "PARTIAL", "DUE"

    val allInvoices: StateFlow<List<InvoiceWithItems>> = invoiceRepo.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReportHistory: StateFlow<List<PdfReportEntity>> = invoiceRepo.allPdfReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredInvoices: StateFlow<List<InvoiceWithItems>> = combine(
        allInvoices,
        searchQuery,
        statusFilter
    ) { invoices, query, status ->
        invoices.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.invoice.invoiceNumber.contains(query, ignoreCase = true) ||
                item.invoice.customerName.contains(query, ignoreCase = true) ||
                (item.invoice.customerPhone?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == "ALL" || item.invoice.status.equals(status, ignoreCase = true)
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePreview = MutableStateFlow<ActivePdfPreview?>(null)
    val activePreview: StateFlow<ActivePdfPreview?> = _activePreview.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    suspend fun getNextInvoiceNumber(): String {
        return invoiceRepo.generateNextInvoiceNumber()
    }

    fun closePreview() {
        _activePreview.value?.pageBitmaps?.forEach {
            if (!it.isRecycled) it.recycle()
        }
        _activePreview.value = null
    }

    private suspend fun renderPdfToBitmaps(file: File): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                // 1.5x resolution for sharp text preview on mobile screens
                val width = (page.width * 1.5f).toInt()
                val height = (page.height * 1.5f).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }

    fun openExistingPdf(file: File, title: String) {
        viewModelScope.launch {
            if (!file.exists()) {
                _userMessage.value = "ফাইলটি পাওয়া যায়নি।"
                return@launch
            }
            _isGenerating.value = true
            val bitmaps = renderPdfToBitmaps(file)
            _isGenerating.value = false
            if (bitmaps.isNotEmpty()) {
                _activePreview.value = ActivePdfPreview(file, title, bitmaps)
            } else {
                _userMessage.value = "PDF প্রিভিউ লোড করা যায়নি।"
            }
        }
    }

    fun generateInvoicePdf(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val context = getApplication<Application>()
                val file = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateInvoice(context, invoiceWithItems)
                }

                // Update invoice with PDF file path
                invoiceRepo.saveInvoice(
                    invoice = invoiceWithItems.invoice.copy(pdfFilePath = file.absolutePath),
                    items = invoiceWithItems.items
                )

                // Save report history
                invoiceRepo.savePdfReport(
                    PdfReportEntity(
                        reportType = ReportType.INVOICE,
                        title = "ইনভয়েস ${invoiceWithItems.invoice.invoiceNumber}",
                        subtitle = "${invoiceWithItems.invoice.customerName} • ৳ ${invoiceWithItems.invoice.grandTotalPaisa / 100.0}",
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileUri = file.toURI().toString(),
                        fileSizeBytes = file.length(),
                        createdAt = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis()
                    )
                )

                val bitmaps = renderPdfToBitmaps(file)
                _activePreview.value = ActivePdfPreview(
                    file = file,
                    title = "ইনভয়েস ${invoiceWithItems.invoice.invoiceNumber}",
                    pageBitmaps = bitmaps
                )
            } catch (e: Exception) {
                _userMessage.value = "ইনভয়েস PDF তৈরিতে ত্রুটি: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun saveInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        generatePdfImmediately: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                val invoiceId = invoiceRepo.saveInvoice(invoice, items)
                val fullInvoice = invoiceRepo.getInvoiceByIdSync(invoiceId)
                if (fullInvoice != null) {
                    if (generatePdfImmediately) {
                        generateInvoicePdf(fullInvoice)
                    } else {
                        _userMessage.value = "ইনভয়েস সফলভাবে সংরক্ষিত হয়েছে"
                    }
                }
            } catch (e: Exception) {
                _userMessage.value = "ইনভয়েস সংরক্ষণ করা যায়নি: ${e.message}"
            }
        }
    }

    fun deleteInvoice(invoiceId: Long) {
        viewModelScope.launch {
            try {
                invoiceRepo.deleteInvoice(invoiceId)
                _userMessage.value = "ইনভয়েস ডিলিট করা হয়েছে"
            } catch (e: Exception) {
                _userMessage.value = "ডিলিট করতে সমস্যা হয়েছে: ${e.message}"
            }
        }
    }

    fun generateIncomeExpenseReport(
        period: IncomeExpenseFilterPeriod,
        customStart: Long? = null,
        customEnd: Long? = null
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val context = getApplication<Application>()
                val (startTime, endTime, filterTitle) = calculateDateRange(period, customStart, customEnd)

                val transactions = withContext(Dispatchers.IO) {
                    if (startTime != null && endTime != null) {
                        db.incomeExpenseDao().getTransactionsInRange(startTime, endTime).first()
                    } else {
                        db.incomeExpenseDao().getAllTransactions().first()
                    }
                }

                val sources = withContext(Dispatchers.IO) { db.incomeExpenseDao().getAllIncomeSourcesList() }
                val categories = withContext(Dispatchers.IO) { db.incomeExpenseDao().getAllExpenseCategoriesList() }
                val sourcesMap = sources.associate { it.id to it.name }
                val categoriesMap = categories.associate { it.id to it.name }

                val file = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateIncomeExpenseReport(
                        context = context,
                        transactions = transactions,
                        sourcesMap = sourcesMap,
                        categoriesMap = categoriesMap,
                        filterTitle = filterTitle,
                        useBengaliDigits = true
                    )
                }

                invoiceRepo.savePdfReport(
                    PdfReportEntity(
                        reportType = ReportType.INCOME_EXPENSE,
                        title = "আয়-ব্যয় রিপোর্ট ($filterTitle)",
                        subtitle = "${transactions.size}টি লেনদেন",
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileUri = file.toURI().toString(),
                        fileSizeBytes = file.length(),
                        dateRangeStart = startTime,
                        dateRangeEnd = endTime,
                        createdAt = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis()
                    )
                )

                val bitmaps = renderPdfToBitmaps(file)
                _activePreview.value = ActivePdfPreview(file, "আয়-ব্যয় রিপোর্ট ($filterTitle)", bitmaps)
            } catch (e: Exception) {
                _userMessage.value = "আয়-ব্যয় PDF তৈরিতে সমস্যা: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateSavingsInvestmentReport() {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val context = getApplication<Application>()
                val goals = withContext(Dispatchers.IO) { db.savingsGoalDao().getAllGoalsList() }
                val entries = withContext(Dispatchers.IO) { db.savingsGoalDao().getAllEntries().first() }
                val holdings = withContext(Dispatchers.IO) { db.portfolioDao().getAllHoldingsList() }

                val totalInvested = holdings.sumOf { it.quantity * it.averagePrice }
                val currentTotalValue = holdings.sumOf { it.quantity * it.currentPrice }
                val summary = PortfolioSummary(
                    totalInvested = totalInvested,
                    currentTotalValue = currentTotalValue,
                    totalUnrealizedGainLoss = currentTotalValue - totalInvested,
                    holdingsCount = holdings.size
                )

                val file = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateSavingsInvestmentReport(
                        context = context,
                        goals = goals,
                        entries = entries,
                        holdings = holdings,
                        portfolioSummary = summary,
                        useBengaliDigits = true
                    )
                }

                invoiceRepo.savePdfReport(
                    PdfReportEntity(
                        reportType = ReportType.SAVINGS_INVESTMENT,
                        title = "সঞ্চয় ও বিনিয়োগ রিপোর্ট",
                        subtitle = "${goals.size}টি সঞ্চয় লক্ষ্য • ${holdings.size}টি স্টক হোল্ডিং",
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileUri = file.toURI().toString(),
                        fileSizeBytes = file.length(),
                        createdAt = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis()
                    )
                )

                val bitmaps = renderPdfToBitmaps(file)
                _activePreview.value = ActivePdfPreview(file, "সঞ্চয় ও বিনিয়োগ রিপোর্ট", bitmaps)
            } catch (e: Exception) {
                _userMessage.value = "সঞ্চয় রিপোর্ট তৈরিতে সমস্যা: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateDueReport(reportKind: String = "ALL") {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val context = getApplication<Application>()
                val parties = withContext(Dispatchers.IO) { db.ledgerDao().getAllPartiesList() }
                val entries = withContext(Dispatchers.IO) { db.ledgerDao().getAllEntriesList() }

                val file = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateDueReport(
                        context = context,
                        parties = parties,
                        entries = entries,
                        reportKind = reportKind,
                        useBengaliDigits = true
                    )
                }

                val title = when (reportKind) {
                    "RECEIVABLE_ONLY" -> "পাওনা ও বকেয়া রিপোর্ট"
                    "PAYABLE_ONLY" -> "দেনা ও প্রদেয় রিপোর্ট"
                    else -> "বাকির খাতা ও দেনা-পাওনা বিবরণী"
                }

                invoiceRepo.savePdfReport(
                    PdfReportEntity(
                        reportType = ReportType.DUE_RECEIVABLE_PAYABLE,
                        title = title,
                        subtitle = "${parties.size} জন ব্যক্তি",
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileUri = file.toURI().toString(),
                        fileSizeBytes = file.length(),
                        createdAt = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis()
                    )
                )

                val bitmaps = renderPdfToBitmaps(file)
                _activePreview.value = ActivePdfPreview(file, title, bitmaps)
            } catch (e: Exception) {
                _userMessage.value = "দেনা-পাওনা PDF তৈরিতে সমস্যা: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generatePartyStatement(partyId: Long) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val context = getApplication<Application>()
                val party = withContext(Dispatchers.IO) { db.ledgerDao().getPartyByIdDirect(partyId) }
                if (party == null) {
                    _userMessage.value = "ব্যক্তি খুঁজে পাওয়া যায়নি"
                    _isGenerating.value = false
                    return@launch
                }
                val entries = withContext(Dispatchers.IO) { db.ledgerDao().getEntriesForParty(partyId).first() }

                val file = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generatePartyStatementReport(
                        context = context,
                        party = party,
                        entries = entries,
                        useBengaliDigits = true
                    )
                }

                invoiceRepo.savePdfReport(
                    PdfReportEntity(
                        reportType = ReportType.PARTY_STATEMENT,
                        title = "${party.name} - খতিয়ান স্টেটমেন্ট",
                        subtitle = "${entries.size}টি লেনদেন",
                        fileName = file.name,
                        filePath = file.absolutePath,
                        fileUri = file.toURI().toString(),
                        fileSizeBytes = file.length(),
                        createdAt = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis()
                    )
                )

                val bitmaps = renderPdfToBitmaps(file)
                _activePreview.value = ActivePdfPreview(file, "${party.name} - খতিয়ান স্টেটমেন্ট", bitmaps)
            } catch (e: Exception) {
                _userMessage.value = "স্টেটমেন্ট তৈরিতে সমস্যা: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteReportHistory(reportId: Long) {
        viewModelScope.launch {
            try {
                invoiceRepo.deletePdfReport(reportId)
                _userMessage.value = "রিপোর্ট হিস্ট্রি ডিলিট করা হয়েছে"
            } catch (e: Exception) {
                _userMessage.value = "ডিলিট করতে সমস্যা হয়েছে: ${e.message}"
            }
        }
    }

    fun shareActivePdf(context: Context) {
        val preview = _activePreview.value ?: return
        PdfReportGenerator.sharePdf(context, preview.file, preview.title)
    }

    fun printActivePdf(context: Context) {
        val preview = _activePreview.value ?: return
        PdfReportGenerator.printPdf(context, preview.file, preview.title)
    }

    fun saveActivePdfToUri(context: Context, destinationUri: Uri) {
        val preview = _activePreview.value ?: return
        val success = PdfReportGenerator.copyFileToUri(context, preview.file, destinationUri)
        _userMessage.value = if (success) "PDF ফাইলটি আপনার ডিভাইসে সেভ হয়েছে" else "ফাইল সেভ করতে সমস্যা হয়েছে"
    }

    private fun calculateDateRange(
        period: IncomeExpenseFilterPeriod,
        customStart: Long?,
        customEnd: Long?
    ): Triple<Long?, Long?, String> {
        val cal = Calendar.getInstance()
        return when (period) {
            IncomeExpenseFilterPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Triple(start, cal.timeInMillis, "আজকের হিসাব")
            }
            IncomeExpenseFilterPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                Triple(start, cal.timeInMillis, "চলতি সপ্তাহের হিসাব")
            }
            IncomeExpenseFilterPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                Triple(start, cal.timeInMillis, "চলতি মাসের হিসাব")
            }
            IncomeExpenseFilterPeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                Triple(start, cal.timeInMillis, "চলতি বছরের হিসাব")
            }
            IncomeExpenseFilterPeriod.CUSTOM -> {
                Triple(customStart, customEnd, "নির্দিষ্ট সময়কাল")
            }
            IncomeExpenseFilterPeriod.ALL -> {
                Triple(null, null, "সার্বিক হিসাব")
            }
        }
    }
}
