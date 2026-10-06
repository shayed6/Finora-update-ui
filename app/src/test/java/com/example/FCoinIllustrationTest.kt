package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.components.FCoinCanvasIllustration
import com.example.ui.screens.FCoinScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FCoinIllustrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFCoinCanvasIllustrationRenders() {
        composeTestRule.setContent {
            FCoinCanvasIllustration(animated = false)
        }
        composeTestRule.onNodeWithTag("fcoin_canvas_illustration").assertIsDisplayed()
    }

    @Test
    fun testFCoinScreenRendersComingSoonContent() {
        composeTestRule.setContent {
            FCoinScreen(onNavigateBack = {})
        }
        composeTestRule.onNodeWithTag("fcoin_coming_soon_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("fcoin_canvas_illustration").assertIsDisplayed()
        composeTestRule.onNodeWithTag("text_fcoin_heading")
            .assertIsDisplayed()
            .assertTextEquals("gz ecosystem এ আপনাকে স্বাগতম")
        composeTestRule.onNodeWithTag("text_fcoin_body")
            .assertIsDisplayed()
            .assertTextEquals("অ্যাপের মাধ্যমে রিওয়ার্ড জমানোর সুযোগ খুব শীঘ্রই যুক্ত হবে। আমাদের প্রতিটি সার্ভিস ব্যবহার করে সহজেই রিওয়ার্ড জমাতে পারবেন।")
    }
}
