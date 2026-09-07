package com.example.ui.screens.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsGoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SavingsGoalRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = SavingsGoalRepository(db.savingsGoalDao())
    }

    val goals: StateFlow<List<SavingsGoalEntity>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double,
        targetDate: String,
        category: String = "সাধারণ"
    ) {
        viewModelScope.launch {
            repository.insertGoal(
                SavingsGoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = initialAmount,
                    targetDate = targetDate,
                    category = category
                )
            )
        }
    }

    fun addContribution(id: Long, amount: Double) {
        viewModelScope.launch {
            repository.addContribution(id, amount)
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }
}
