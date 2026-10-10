package com.example.ui.screens.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.InvestmentSectorEntity
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsEntryType
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavingsMainUiState(
    val totalSavingsPaisa: Long = 0L,
    val goals: List<GoalWithStats> = emptyList(),
    val sectors: List<InvestmentSectorEntity> = emptyList(),
    val isLoading: Boolean = true
)

class SavingsGoalsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: SavingsGoalRepository = SavingsGoalRepository.getInstance(application)
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            repository.ensureDefaultData()
        }
    }

    val uiState: StateFlow<SavingsMainUiState> = combine(
        repository.allGoals,
        repository.allEntries,
        repository.allSectors
    ) { goals, entries, sectors ->
        // Group entries by goal_id
        val entriesByGoal = entries.groupBy { it.goalId }

        var overallTotal = 0L

        // Compute stats for each goal
        val goalStatsList = goals.map { goal ->
            val goalEntries = entriesByGoal[goal.id] ?: emptyList()
            var deposit = 0L
            var withdraw = 0L
            goalEntries.forEach { entry ->
                if (entry.type == SavingsEntryType.DEPOSIT.name) deposit += entry.amount
                else if (entry.type == SavingsEntryType.WITHDRAW.name) withdraw += entry.amount
            }
            val savedPaisa = deposit - withdraw
            overallTotal += savedPaisa

            val progressPercent: Float? = goal.targetAmount?.let { target ->
                if (target > 0L) (savedPaisa.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 1f
            }

            val isCompleted = goal.targetAmount != null && savedPaisa >= goal.targetAmount

            GoalWithStats(
                goal = goal,
                savedPaisa = savedPaisa,
                progressPercent = progressPercent,
                isCompleted = isCompleted
            )
        }

        // Sort so default goal "সাধারণ সঞ্চয়" is first, then rest by creation date
        val sortedGoals = goalStatsList.sortedWith(
            compareByDescending<GoalWithStats> { it.goal.isDefault }
                .thenBy { it.goal.createdAt }
        )

        SavingsMainUiState(
            totalSavingsPaisa = overallTotal,
            goals = sortedGoals,
            sectors = sectors,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsMainUiState(isLoading = true)
    )

    fun getGoalDetailState(goalId: Long, useBengaliDigits: Boolean = true): StateFlow<GoalDetailState?> {
        return combine(
            repository.getGoalById(goalId),
            repository.getEntriesForGoal(goalId),
            repository.allSectors
        ) { goal, entries, sectors ->
            if (goal == null) return@combine null

            val sectorMap = sectors.associateBy { it.id }

            var depositTotal = 0L
            var withdrawTotal = 0L
            val sectorTotals = mutableMapOf<String, Long>()

            val displayEntries = entries.map { entry ->
                val sName = sectorMap[entry.sectorId]?.name ?: "অন্যান্য"
                if (entry.type == SavingsEntryType.DEPOSIT.name) {
                    depositTotal += entry.amount
                    sectorTotals[sName] = (sectorTotals[sName] ?: 0L) + entry.amount
                } else if (entry.type == SavingsEntryType.WITHDRAW.name) {
                    withdrawTotal += entry.amount
                    sectorTotals[sName] = (sectorTotals[sName] ?: 0L) - entry.amount
                }
                SavingsEntryItem(entry = entry, sectorName = sName)
            }

            val savedPaisa = depositTotal - withdrawTotal

            val remainingPaisa = goal.targetAmount?.let { target ->
                (target - savedPaisa).coerceAtLeast(0L)
            }

            val progressPercent = goal.targetAmount?.let { target ->
                if (target > 0L) (savedPaisa.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 1f
            }

            val isCompleted = goal.targetAmount != null && savedPaisa >= goal.targetAmount

            val remainingTimeText = SavingsFormatter.formatRemainingTime(goal.targetDate, useBengaliDigits)

            val breakdown = sectorTotals.map { (name, total) ->
                SectorTotal(sectorName = name, totalPaisa = total)
            }.filter { it.totalPaisa > 0L }
                .sortedByDescending { it.totalPaisa }

            GoalDetailState(
                goal = goal,
                savedPaisa = savedPaisa,
                remainingPaisa = remainingPaisa,
                remainingTimeText = remainingTimeText,
                progressPercent = progressPercent,
                isCompleted = isCompleted,
                sectorBreakdown = breakdown,
                entries = displayEntries
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }

    // --- Goal CRUD ---
    fun addGoal(
        name: String,
        targetAmountPaisa: Long?,
        targetDate: Long?,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = repository.addGoal(name, targetAmountPaisa, targetDate)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun updateGoal(
        id: Long,
        name: String,
        targetAmountPaisa: Long?,
        targetDate: Long?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateGoal(id, name, targetAmountPaisa, targetDate)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun deleteGoal(
        goalId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.deleteGoal(goalId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    // --- Entry CRUD ---
    fun addEntry(
        goalId: Long,
        type: String,
        amountPaisa: Long,
        sectorId: Long,
        entryDate: Long,
        note: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addEntry(goalId, type, amountPaisa, sectorId, entryDate, note)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun updateEntry(
        entry: SavingsEntryEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateEntry(entry)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun deleteEntry(
        entryId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entryId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    // --- Sector CRUD ---
    fun addCustomSector(
        name: String,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = repository.addCustomSector(name)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }

    fun deleteCustomSector(
        sectorId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.deleteCustomSector(sectorId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "ত্রুটি ঘটেছে")
            }
        }
    }
}
