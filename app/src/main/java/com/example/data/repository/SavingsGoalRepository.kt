package com.example.data.repository

import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

class SavingsGoalRepository(private val savingsGoalDao: SavingsGoalDao) {

    val allGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()

    suspend fun insertGoal(goal: SavingsGoalEntity): Long =
        savingsGoalDao.insertGoal(goal)

    suspend fun updateGoal(goal: SavingsGoalEntity) =
        savingsGoalDao.updateGoal(goal)

    suspend fun deleteGoal(id: Long) =
        savingsGoalDao.deleteGoalById(id)

    suspend fun addContribution(id: Long, amount: Double) =
        savingsGoalDao.addContribution(id, amount)
}
