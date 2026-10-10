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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    val uiState: StateFlow<IncomeExpenseUiState> = combine(
        repository.allTransactions,
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
    ) { allTransactions, sources, categories, tagLines, filterTuple ->
        val sourcesMap = sources.associateBy { it.id }
        val categoriesMap = categories.associateBy { it.id }

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

        // 1. Calculate Monthly Summary (based on selectedMonthYear)
        val monthStart = filterTuple.monthYear.toStartTimestamp()
        val monthEnd = filterTuple.monthYear.toEndTimestamp()

        val monthTransactions = allTransactions.filter {
            it.occurredAt in monthStart..monthEnd
        }

        var totalIncome = 0L
        var totalExpense = 0L
        monthTransactions.forEach { tx ->
            if (tx.type == CashTransactionType.INCOME.name) {
                totalIncome += tx.amount
            } else if (tx.type == CashTransactionType.EXPENSE.name) {
                totalExpense += tx.amount
            }
        }
        val balance = totalIncome - totalExpense

        // 2. Filter displayed items based on activeTab and source/category filter
        val tabTransactions = monthTransactions.filter {
            it.type == filterTuple.tab.name
        }

        val filteredTransactions = tabTransactions.filter { tx ->
            if (filterTuple.tab == CashTransactionType.INCOME) {
                filterTuple.sourceFilter == null || tx.sourceId == filterTuple.sourceFilter
            } else {
                filterTuple.catFilter == null || tx.categoryId == filterTuple.catFilter
            }
        }

        val displayItems = filteredTransactions.map { tx ->
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

        IncomeExpenseUiState(
            activeTab = filterTuple.tab,
            selectedMonthYear = filterTuple.monthYear,
            summary = IncomeExpenseSummary(
                totalIncomePaisa = totalIncome,
                totalExpensePaisa = totalExpense,
                balancePaisa = balance
            ),
            displayedTransactions = displayItems,
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
        repository.allTransactions,
        repository.allIncomeSources,
        repository.allExpenseCategories
    ) { allTransactions, sources, categories ->
        val sourcesMap = sources.associateBy { it.id }
        val categoriesMap = categories.associateBy { it.id }
        allTransactions.take(5).map { tx ->
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
}

private data class FilterTuple(
    val tab: CashTransactionType,
    val monthYear: MonthYear,
    val sourceFilter: Long?,
    val catFilter: Long?,
    val selectedProfiles: Set<String> = emptySet()
)
