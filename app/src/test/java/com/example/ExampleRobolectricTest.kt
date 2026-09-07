package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CalculatorRepository
import com.example.util.BengaliFormatter
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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Finora", appName)
  }

  @Test
  fun `bengali formatter converts digits correctly`() {
    val bengali = BengaliFormatter.toBengaliDigits("12345.67")
    assertEquals("১২৩৪৫.৬৭", bengali)

    val normalized = BengaliFormatter.normalizeToEnglishDigits("১২৩৪৫.৬৭")
    assertEquals("12345.67", normalized)
  }

  @Test
  fun `all 36 calculators are loaded`() {
    assertEquals(36, CalculatorRepository.allCalculators.size)
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
}

