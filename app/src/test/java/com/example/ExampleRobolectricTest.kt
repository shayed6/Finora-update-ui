package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.model.CalculatorRepository
import com.example.util.BengaliFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `savings goals report summary calculations are accurate`() {
    val goals = listOf(
      SavingsGoalEntity(
        id = 1,
        title = "জরুরি ফান্ড (Emergency Fund)",
        targetAmount = 100000.0,
        currentAmount = 45000.0,
        targetDate = "31/12/2026",
        category = "Emergency"
      ),
      SavingsGoalEntity(
        id = 2,
        title = "ল্যাপটপ ক্রয়",
        targetAmount = 80000.0,
        currentAmount = 80000.0,
        targetDate = "15/10/2026",
        category = "Gadget"
      )
    )

    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved = goals.sumOf { it.currentAmount }
    val remaining = (totalTarget - totalSaved).coerceAtLeast(0.0)
    val completedCount = goals.count { it.currentAmount >= it.targetAmount }
    val progress = (totalSaved / totalTarget) * 100.0

    assertEquals(180000.0, totalTarget, 0.01)
    assertEquals(125000.0, totalSaved, 0.01)
    assertEquals(55000.0, remaining, 0.01)
    assertEquals(1, completedCount)
    assertEquals(69.44, progress, 0.1)

    val formattedTaka = BengaliFormatter.formatTaka(totalTarget, true)
    assertTrue(formattedTaka.contains("১৮০,০০০") || formattedTaka.contains("১,৮০,০০০"))
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Finora", appName)
  }

  @Test
  fun `theme preferences repository default and persistence`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = UserPreferencesRepository.getInstance(context)
    repo.setThemeMode(AppThemeMode.DARK)
    val mode = repo.themeMode.first()
    assertEquals(AppThemeMode.DARK, mode)
    repo.setThemeMode(AppThemeMode.SYSTEM)
    assertEquals(AppThemeMode.SYSTEM, repo.themeMode.first())
  }

  @Test
  fun `bengali formatter converts digits correctly`() {
    val bengali = BengaliFormatter.toBengaliDigits("12345.67")
    assertEquals("১২৩৪৫.৬৭", bengali)

    val normalized = BengaliFormatter.normalizeToEnglishDigits("১২৩৪৫.৬৭")
    assertEquals("12345.67", normalized)
  }

  @Test
  fun `all 37 calculators are loaded`() {
    assertEquals(37, CalculatorRepository.allCalculators.size)
  }

  @Test
  fun `loan emi calculator computes monthly installment accurately`() {
    val emiCalc = CalculatorRepository.getById("loan_emi")
    org.junit.Assert.assertNotNull(emiCalc)

    val inputs = mapOf(
      "principal" to 1000000.0,
      "interest_rate" to 12.0,
      "tenure_years" to 3.0,
      "tenure_months" to 0.0
    )

    val result = emiCalc!!.calculate(inputs, false)
    // EMI for 10 Lakh at 12% for 36 months is ~33,214.31
    assertTrue(result.primaryValueBn.contains("33,214"))
    assertEquals(false, result.isWarning)
    assertEquals(5, result.subResults.size)
  }

  @Test
  fun `loan emi calculator handles invalid inputs gracefully`() {
    val emiCalc = CalculatorRepository.getById("loan_emi")
    org.junit.Assert.assertNotNull(emiCalc)

    val inputs = mapOf(
      "principal" to 0.0,
      "interest_rate" to 10.0,
      "tenure_years" to 0.0,
      "tenure_months" to 0.0
    )

    val result = emiCalc!!.calculate(inputs, true)
    assertEquals(true, result.isWarning)
  }

  @Test
  fun `drawer destination contains contact us and order app`() {
    val destinations = com.example.ui.components.DrawerDestination.values()
    assertTrue(destinations.contains(com.example.ui.components.DrawerDestination.CONTACT_US))
    assertTrue(destinations.contains(com.example.ui.components.DrawerDestination.ORDER_APP))
  }
}

