package com.example.ui.screens.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.IncomeSourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountsSummaryViewModel(
    application: Application,
    private val repository: AccountsSummaryRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        AccountsSummaryRepository.getInstance(application)
    )

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountsSummaryViewModel(
                    application,
                    AccountsSummaryRepository.getInstance(application)
                ) as T
            }
        }
    }

    private val _currentPeriod = MutableStateFlow(defaultCurrentMonthPeriod())
    val currentPeriod: StateFlow<PeriodRange> = _currentPeriod.asStateFlow()

    private val _sourceCustomPeriods = MutableStateFlow<Map<Long, PeriodRange>>(emptyMap())

    private val _summary = MutableStateFlow(PeriodSummary(0L, 0L, 0L, 0L, 0))
    private val _sixMonthsTrend = MutableStateFlow<List<MonthlyTrendPoint>>(emptyList())
    private val _sourceCards = MutableStateFlow<List<IncomeSourceCardData>>(emptyList())
    private val _ledgerOverview = MutableStateFlow(LedgerOverview(0L, 0L))
    private val _isLoading = MutableStateFlow(true)

    val chartType: StateFlow<ChartType> = repository.chartTypePreference
        .stateIn(viewModelScope, SharingStarted.Eagerly, ChartType.PIE)

    val allIncomeSources: StateFlow<List<IncomeSourceEntity>> = repository.allIncomeSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<AccountsSummaryUiState> = combine(
        combine(_currentPeriod, _summary, chartType) { p, s, c -> Triple(p, s, c) },
        _sixMonthsTrend,
        _sourceCards,
        _ledgerOverview
    ) { (period, summary, chart), trend, cards, ledger ->
        AccountsSummaryUiState(
            period = period,
            summary = summary,
            chartType = chart,
            sixMonthsTrend = trend,
            sourceCards = cards,
            ledgerOverview = ledger,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AccountsSummaryUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            // Recompute whenever period changes or sources change
            combine(_currentPeriod, repository.allIncomeSources, _sourceCustomPeriods) { p, s, sc ->
                Triple(p, s, sc)
            }.collect { (period, sources, customCycles) ->
                refreshData(period, sources, customCycles)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.clearCache()
            refreshData(_currentPeriod.value, allIncomeSources.value, _sourceCustomPeriods.value)
        }
    }

    private suspend fun refreshData(
        period: PeriodRange,
        sources: List<IncomeSourceEntity>,
        customCycles: Map<Long, PeriodRange>
    ) {
        _isLoading.value = true
        try {
            val summary = repository.calculatePeriodSummary(period.startTimestamp, period.endTimestamp)
            _summary.value = summary

            val selectedMonthYear = period.calendarMonthYear ?: MonthYear.current()
            val trend = repository.calculateSixMonthsTrend(selectedMonthYear)
            _sixMonthsTrend.value = trend

            val cards = repository.calculateSourceCards(sources, customCycles)
            _sourceCards.value = cards

            val ledger = repository.calculateLedgerOverview()
            _ledgerOverview.value = ledger
        } finally {
            _isLoading.value = false
        }
    }

    fun setPeriod(newPeriod: PeriodRange) {
        _currentPeriod.value = newPeriod
    }

    fun stepMainPeriod(forward: Boolean) {
        val curr = _currentPeriod.value
        val my = curr.calendarMonthYear ?: MonthYear.current()
        val nextMy = if (forward) my.next() else my.previous()
        val start = nextMy.toStartTimestamp()
        val end = nextMy.toEndTimestamp()
        _currentPeriod.value = PeriodRange(
            startTimestamp = start,
            endTimestamp = end,
            label = SummaryDateFormatter.formatPeriodLabel(start, end, true),
            isCustom = false,
            calendarMonthYear = nextMy
        )
    }

    fun setChartType(type: ChartType) {
        viewModelScope.launch {
            repository.saveChartTypePreference(type)
        }
    }

    fun stepSourceCycle(sourceId: Long, forward: Boolean) {
        val src = allIncomeSources.value.find { it.id == sourceId } ?: return
        val currentPeriod = _sourceCustomPeriods.value[sourceId] ?: computeDefaultCycle(src.startDay)
        val stepped = stepCycle(currentPeriod, src.startDay, forward)
        _sourceCustomPeriods.value = _sourceCustomPeriods.value + (sourceId to stepped)
    }

    fun setSourceCustomPeriod(sourceId: Long, range: PeriodRange) {
        _sourceCustomPeriods.value = _sourceCustomPeriods.value + (sourceId to range)
    }

    fun updateIncomeSourceStartDay(sourceId: Long, startDay: Int?, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateIncomeSourceStartDay(sourceId, startDay)
            // Reset this source's cycle to its new default
            val newCycle = computeDefaultCycle(startDay)
            _sourceCustomPeriods.value = _sourceCustomPeriods.value + (sourceId to newCycle)
            repository.clearCache()
            refreshData(_currentPeriod.value, allIncomeSources.value, _sourceCustomPeriods.value)
            onComplete()
        }
    }

    // PDF Export with Rewarded Ad Gate
    fun exportSummaryPdf(context: android.content.Context, useBengaliDigits: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val period = _currentPeriod.value
                val transactions = repository.getTransactionsInRange(period.startTimestamp, period.endTimestamp)
                val sources = repository.getAllIncomeSourcesList().associate { it.id to it.name }
                val categories = repository.getAllExpenseCategoriesList().associate { it.id to it.name }
                val file = com.example.util.PdfReportGenerator.generateIncomeExpenseReport(
                    context = context,
                    transactions = transactions,
                    sourcesMap = sources,
                    categoriesMap = categories,
                    filterTitle = "হিসাব নিকাশ সারসংক্ষেপ (${period.label})",
                    useBengaliDigits = useBengaliDigits
                )
                withContext(Dispatchers.Main) {
                    com.example.util.PdfReportGenerator.sharePdf(context, file, "হিসাব নিকাশ সারসংক্ষেপ রিপোর্ট (${period.label})")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "PDF তৈরিতে সমস্যা: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
