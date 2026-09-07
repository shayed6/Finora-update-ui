package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.AppConfig

data class LearningPlatform(
    val name: String,
    val titleBn: String,
    val descriptionBn: String,
    val url: String,
    val badgeBn: String
)

@Composable
fun LearnStockScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val platforms = listOf(
        LearningPlatform(
            name = "Investaloy",
            titleBn = "ইনভেস্টালয় (Investaloy)",
            descriptionBn = "বাংলাদেশি শেয়ার বাজারের ফান্ডামেন্টাল বিশ্লেষণ, কোম্পানি রিপোর্ট এবং আর্থিক রেশিও বিশ্লেষণের শিক্ষামূলক রিসোর্স।",
            url = AppConfig.URL_INVESTALOY,
            badgeBn = "ফান্ডামেন্টাল"
        ),
        LearningPlatform(
            name = "Learn with Kabir",
            titleBn = "লার্ন উইথ কবির (Learn with Kabir)",
            descriptionBn = "চার্ট প্যাটার্ন, টেকনিক্যাল ইন্ডিকেটর, সুইং ট্রেডিং কৌশল ও ঝুঁকি ব্যবস্থাপনা সম্পর্কিত বিস্তারিত ভিডিও গাইড।",
            url = AppConfig.URL_LEARN_WITH_KABIR,
            badgeBn = "টেকনিক্যাল"
        ),
        LearningPlatform(
            name = "Learn with Rafiq",
            titleBn = "লার্ন উইথ রফিক (Learn with Rafiq)",
            descriptionBn = "নতুন বিনিয়োগকারীদের জন্য ডিএসই গাইডলাইন, স্টক পিকিং স্ট্র্যাটেজি ও পোর্টফোলিও ম্যানেজমেন্ট।",
            url = AppConfig.URL_LEARN_WITH_RAFIQ,
            badgeBn = "শিক্ষানবিস গাইড"
        )
    )

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ওয়েব লিংক খুলতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FinoraNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = GrowthGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Learn Stock",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Top ranked learning platform (শীর্ষস্থানীয় প্ল্যাটফর্ম)",
                            color = GrowthGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "শেয়ার বাজারে বিনিয়োগের পূর্বে সঠিক জ্ঞান অর্জন অপরিহার্য। নিচে শীর্ষস্থানীয় তিনটি প্ল্যাটফর্মের মাধ্যমে বিনামূল্যে শিখুন:",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        }

        // Platform Cards
        platforms.forEach { platform ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { openUrl(platform.url) }
                    .testTag("learning_link_${platform.name.lowercase().replace(" ", "_")}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = platform.titleBn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FinoraNavy
                        )
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = platform.badgeBn,
                                color = PrimaryBlue,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = platform.descriptionBn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { openUrl(platform.url) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue.copy(alpha = 0.1f),
                            contentColor = PrimaryBlue
                        )
                    ) {
                        Text(
                            text = "প্ল্যাটফর্মটি ভিজিট করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Golden Rules for BD Stock Investors Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "স্মার্ট বিনিয়োগের মূলনীতি",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FinoraNavy
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val tips = listOf(
                    "কখনোই গুজবে বা ধার করা অর্থে শেয়ার কেনাবেচা করবেন না।",
                    "কোম্পানির P/E, EPS ও ডিভিডেন্ডের ধারাবাহিকতা যাচাই করুন।",
                    "সব টাকা একটি মাত্র শেয়ারে বিনিয়োগ না করে পোর্টফোলিও বৈচিত্র্যময় রাখুন।",
                    "ট্রেডিং করার সময় সবসময় স্টপ-লস এবং রিস্ক-রিওয়ার্ড রেশিও মেনে চলুন।"
                )

                tips.forEach { tip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "•", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
