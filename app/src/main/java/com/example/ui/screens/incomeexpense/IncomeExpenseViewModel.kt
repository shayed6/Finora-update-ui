package com.example.ui.screens.incomeexpense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CashTransactionType
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.IncomeExpenseTransactionEntity
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.repository.IncomeExpenseRepository
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IncomeExpenseUiState(
    val activeTab: CashTransactionType = CashTransactionType.INCOME,
    val selectedMonthYear: MonthYear = MonthYear.current(),
    val summary: IncomeExpenseSummary = IncomeExpenseSummary(0L, 0L, 0L),
    val displayedTransactions: List<TransactionDisplayItem> = emptyList(),
    val incomeSources: List<IncomeSourceEntity> = emptyList(),
    val expenseCategories: List<ExpenseCategoryEntity> = emptyList(),
    val selectedSourceFilterId: Long? = null,
    val selectedCategoryFilterId: Long? = null,
    val tagLineSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = false
)

class IncomeExpenseViewModel(
    application: Application,
    private val repository: IncomeExpenseRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        IncomeExpenseRepository.getInstance(application)
    )

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IncomeExpenseViewModel(
                    application,
                    IncomeExpenseRepository.getInstance(application)
                ) as T
            }
        }
    }

    private val _activeTab = MutableStateFlow(CashTransactionType.INCOME)
    val activeTab: StateFlow<CashTransactionType> = _activeTab.asStateFlow()

    private val _selectedMonthYear = MutableStateFlow(MonthYear.current())
    val selectedMonthYear: StateFlow<MonthYear> = _selectedMonthYear.asStateFlow()

    private val _selectedSourceFilterId = MutableStateFlow<Long?>(null)
    val selectedSourceFilterId: StateFlow<Long?> = _selectedSourceFilterId.asStateFlow()

    private val _selectedCategoryFilterId = MutableStateFlow<Long?>(null)
    val selectedCategoryFilterId: StateFlow<Long?> = _selectedCategoryFilterId.asStateFlow()

    private val orgRepo = com.example.data.preferences.OrganizationInfoRepository.getInstance(application)

    init {
        viewModelScope.launch {
            repository.ensurePresets()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val summaryFlow: Flow<com.example.data.local.dao.IncomeExpenseAggregate> = _selectedMonthYear.flatMapLatest { monthYear ->
        repository.getAggregateInRangeFlow(
            startTime = monthYear.toStartTimestamp(),
            endTime = monthYear.toEndTimestamp()
        )
    }

    private val nameLookupsFlow = combine(
        repository.allIncomeSources,
        repository.allExpenseCategories
    ) { sources, categories ->
        Pair(sources.associateBy { it.id }, categories.associateBy { it.id })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedTransactions: Flow<PagingData<TransactionDisplayItem>> = combine(
        _activeTab,
        _selectedMonthYear,
        _selectedSourceFilterId,
        _selectedCategoryFilterId
    ) { tab, monthYear, sourceFilter, catFilter ->
        val type = tab.name
        val startTime = monthYear.toStartTimestamp()
        val endTime = monthYear.toEndTimestamp()
        val sourceId = if (tab == CashTransactionType.INCOME) sourceFilter else null
        val categoryId = if (tab == CashTransactionType.EXPENSE) catFilter else null
        FilterParams(type, startTime, endTime, sourceId, categoryId)
    }.flatMapLatest { params ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                repository.getTransactionsPaged(
                    type = params.type,
                    startTime = params.startTime,
                    endTime = params.endTime,
                    sourceId = params.sourceId,
                    categoryId = params.categoryId
                )
            }
        ).flow
    }.combine(nameLookupsFlow) { pagingData, (sourcesMap, categoriesMap) ->
        pagingData.map { tx ->
            val name = if (tx.type == CashTransactionType.INCOME.name) {
                sourcesMap[tx.sourceId]?.name ?: "অজানা উৎস"
            } else {
                categoriesMap[tx.categoryId]?.name ?: "অজানা খাত"
            }
            val linkedName = tx.linkedIncomeSourceId?.let { sourcesMap[it]?.name }
            TransactionDisplayItem(
                transaction = tx,
                categoryOrSourceName = name,
                linkedSourceName = linkedName
            )
        }
    }.cachedIn(viewModelScope)

    val uiState: StateFlow<IncomeExpenseUiState> = combine(
        summaryFlow,
        repository.allIncomeSources,
        repository.allExpenseCategories,
        repository.allDistinctTagLines,
        combine(
            _activeTab,
            _selectedMonthYear,
            _selectedSourceFilterId,
            _selectedCategoryFilterId,
            orgRepo.selectedWorkProfiles
        ) { tab, monthYear, sourceFilter, catFilter, profiles ->
            FilterTuple(tab, monthYear, sourceFilter, catFilter, profiles)
        }
    ) { aggregate, sources, categories, tagLines, filterTuple ->
        // Sort items so selected profiles appear first
        val activeProfiles = filterTuple.selectedProfiles
        val sortedSources = sources.sortedWith(
            compareByDescending<IncomeSourceEntity> { it.profile != null && it.profile in activeProfiles }
                .thenByDescending { it.isPreset }
                .thenBy { it.id }
        )
        val sortedCategories = categories.sortedWith(
            compareByDescending<ExpenseCategoryEntity> { it.profile != null && it.profile in activeProfiles }
                .thenByDescending { it.isPreset }
                .thenBy { it.id }
        )

        IncomeExpenseUiState(
            activeTab = filterTuple.tab,
            selectedMonthYear = filterTuple.monthYear,
            summary = IncomeExpenseSummary(
                totalIncomePaisa = aggregate.totalIncome,
                totalExpensePaisa = aggregate.totalExpense,
                balancePaisa = aggregate.totalIncome - aggregate.totalExpense
            ),
            displayedTransactions = emptyList(),
            incomeSources = sortedSources,
            expenseCategories = sortedCategories,
            selectedSourceFilterId = filterTuple.sourceFilter,
            selectedCategoryFilterId = filterTuple.catFilter,
            tagLineSuggestions = tagLines,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IncomeExpenseUiState(isLoading = true)
    )

    val recentTransactions: StateFlow<List<TransactionDisplayItem>> = combine(
        repository.getRecentTransactions(5),
        repository.allIncomeSources,
        repository.allExpenseCategories
    ) { recentList, sources, categories ->
        val sourcesMap = sources.associateBy { it.id }
        val categoriesMap = categories.associateBy { it.id }
        recentList.map { tx ->
            val name = if (tx.type == CashTransactionType.INCOME.name) {
                sourcesMap[tx.sourceId]?.name ?: "অজানা উৎস"
            } else {
                categoriesMap[tx.categoryId]?.name ?: "অজানা খাত"
            }
            val linkedName = tx.linkedIncomeSourceId?.let { sourcesMap[it]?.name }
            TransactionDisplayItem(
                transaction = tx,
                categoryOrSourceName = name,
                linkedSourceName = linkedName
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setTab(tab: CashTransactionType) {
        _activeTab.value = tab
    }

    fun setMonthYear(monthYear: MonthYear) {
        _selectedMonthYear.value = monthYear
    }

    fun setSourceFilter(sourceId: Long?) {
        _selectedSourceFilterId.value = sourceId
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryFilterId.value = categoryId
    }

    // --- Transaction CRUD ---
    fun addIncome(
        amountPaisa: Long,
        sourceId: Long,
        occurredAt: Long,
        tagLine: String?,
        note: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addTransaction(
                    type = CashTransactionType.INCOME.name,
                    amount = amountPaisa,
                    occurredAt = occurredAt,
                    sourceId = sourceId,
                    tagLine = tagLine,
                    note = note
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun addExpense(
        amountPaisa: Long,
        categoryId: Long,
        occurredAt: Long,
        tagLine: String?,
        note: String?,
        linkedIncomeSourceId: Long?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addTransaction(
                    type = CashTransactionType.EXPENSE.name,
                    amount = amountPaisa,
                    occurredAt = occurredAt,
                    categoryId = categoryId,
                    tagLine = tagLine,
                    note = note,
                    linkedIncomeSourceId = linkedIncomeSourceId
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun updateTransaction(
        transaction: IncomeExpenseTransactionEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateTransaction(transaction)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun deleteTransaction(
        transactionId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transactionId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    // --- Category & Source Management ---
    fun addCustomIncomeSource(name: String, onSuccess: (Long) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repository.addIncomeSource(name)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun renameIncomeSource(id: Long, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.renameIncomeSource(id, newName)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun deleteIncomeSource(id: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteIncomeSource(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun addCustomExpenseCategory(name: String, onSuccess: (Long) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repository.addExpenseCategory(name)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun renameExpenseCategory(id: Long, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.renameExpenseCategory(id, newName)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun deleteExpenseCategory(id: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteExpenseCategory(id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    // PDF Export with Rewarded Ad Gate
    fun exportMonthlyPdf(context: android.content.Context, useBengaliDigits: Boolean = true) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val monthYear = _selectedMonthYear.value
                val startTime = monthYear.toStartTimestamp()
                val endTime = monthYear.toEndTimestamp()
                val transactions = repository.getTransactionsInRange(startTime, endTime).kotlinx.coroutines.flow.first()
                val sources = repository.allIncomeSources.kotlinx.coroutines.flow.first().associate { it.id to it.name }
                val categories = repository.allExpenseCategories.kotlinx.coroutines.flow.first().associate { it.id to it.name }
                val file = com.example.util.PdfReportGenerator.generateIncomeExpenseReport(
                    context = context,
                    transactions = transactions,
                    sourcesMap = sources,
                    categoriesMap = categories,
                    filterTitle = "${monthYear.toDisplayBengali()} আয়-ব্যয় হিসাব",
                    useBengaliDigits = useBengaliDigits
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    com.example.util.PdfReportGenerator.sharePdf(context, file, "${monthYear.toDisplayBengali()} আয়-ব্যয় রিপোর্ট")
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "PDF তৈরিতে সমস্যা: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

private data class FilterTuple(
    val tab: CashTransactionType,
    val monthYear: MonthYear,
    val sourceFilter: Long?,
    val catFilter: Long?,
    val selectedProfiles: Set<String> = emptySet()
)

private data class FilterParams(
    val type: String,
    val startTime: Long,
    val endTime: Long,
    val sourceId: Long?,
    val categoryId: Long?
)
