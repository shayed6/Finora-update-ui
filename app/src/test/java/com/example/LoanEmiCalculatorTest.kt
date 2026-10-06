package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.CalculatorRepository
import com.example.ui.components.LoanEmiCalculatorView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.pow

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LoanEmiCalculatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLoanEmiMathematicalFormulaAccuracy() {
        val emiCalc = CalculatorRepository.getById("loan_emi")
        assertNotNull(emiCalc)

        // 10,00,000 Taka at 12.0% for 3 years (36 months)
        val inputs = mapOf(
            "principal" to 1000000.0,
            "interest_rate" to 12.0,
            "tenure_years" to 3.0,
            "tenure_months" to 0.0
        )

        val result = emiCalc!!.calculate(inputs, false)
        assertEquals(false, result.isWarning)
        assertTrue(result.primaryValueBn.contains("33,214"))

        val totalInterestSub = result.subResults.find { it.labelBn == "মোট সুদ" }
        assertNotNull(totalInterestSub)
        assertTrue(totalInterestSub!!.valueBn.contains("1,95,715") || totalInterestSub.valueBn.contains("195,715"))

        val totalPaymentSub = result.subResults.find { it.labelBn == "মোট পরিশোধ" }
        assertNotNull(totalPaymentSub)
        assertTrue(totalPaymentSub!!.valueBn.contains("1,195,715") || totalPaymentSub.valueBn.contains("11,95,715") || totalPaymentSub.valueBn.contains("1195715"))
    }

    @Test
    fun testLoanEmiCalculatorViewRenders() {
        composeTestRule.setContent {
            LoanEmiCalculatorView(useBengaliDigits = false)
        }

        composeTestRule.onNodeWithTag("loan_emi_calculator_view").assertExists()
        composeTestRule.onNodeWithTag("emi_hero_result_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_monthly_emi_value").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_total_interest_value").assertIsDisplayed()

        composeTestRule.onNodeWithTag("input_loan_amount").assertExists()
        composeTestRule.onNodeWithTag("slider_loan_amount").assertExists()
        composeTestRule.onNodeWithTag("input_interest_rate").assertExists()
        composeTestRule.onNodeWithTag("slider_interest_rate").assertExists()
        composeTestRule.onNodeWithTag("input_tenure").assertExists()
        composeTestRule.onNodeWithTag("slider_tenure").assertExists()

        composeTestRule.onNodeWithTag("btn_copy_emi_summary").assertExists()
        composeTestRule.onNodeWithTag("btn_share_emi_report").assertExists()
    }
}
