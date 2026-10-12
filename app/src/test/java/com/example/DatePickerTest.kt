package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.FinoraDatePickerDialog
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatePickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val originalLocale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale("bn", "BD"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun testFinoraDatePickerDialogWithBengaliLocale() {
        var dateSelected = false
        composeTestRule.setContent {
            FinoraDatePickerDialog(
                initialDateMillis = System.currentTimeMillis(),
                allowFutureDates = false,
                onDismissRequest = {},
                onDateSelected = { _, _, _ -> dateSelected = true }
            )
        }
        composeTestRule.onNodeWithTag("btn_confirm_date_picker").performClick()
        assertTrue(dateSelected)
    }
}
