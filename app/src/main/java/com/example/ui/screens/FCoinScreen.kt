package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FCoinCanvasIllustration

@Composable
fun FCoinScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // A small "Coming Soon" badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFB800).copy(alpha = 0.15f),
                modifier = Modifier.testTag("fcoin_coming_soon_chip")
            ) {
                Text(
                    text = "Coming Soon",
                    color = Color(0xFFFFB800),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // A coin icon / simple coin illustration, centered
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                FCoinCanvasIllustration(
                    size = 150.dp,
                    animated = true,
                    modifier = Modifier.testTag("fcoin_canvas_illustration")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Heading: "gz ecosystem এ আপনাকে স্বাগতম"
            Text(
                text = "gz ecosystem এ আপনাকে স্বাগতম",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("text_fcoin_heading")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Body text: "অ্যাপের মাধ্যমে রিওয়ার্ড জমানোর সুযোগ খুব শীঘ্রই যুক্ত হবে। আমাদের প্রতিটি সার্ভিস ব্যবহার করে সহজেই রিওয়ার্ড জমাতে পারবেন।"
            Text(
                text = "অ্যাপের মাধ্যমে রিওয়ার্ড জমানোর সুযোগ খুব শীঘ্রই যুক্ত হবে। আমাদের প্রতিটি সার্ভিস ব্যবহার করে সহজেই রিওয়ার্ড জমাতে পারবেন।",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("text_fcoin_body")
            )
        }
    }
}
