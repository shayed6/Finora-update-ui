package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FinoraLogo
import com.example.ui.components.FinoraWordmark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue
import com.example.util.AppConfig

@Composable
fun AboutScreen(
    onShowSplash: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FinoraNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FinoraLogo(size = 72.dp)
                Spacer(modifier = Modifier.height(14.dp))
                FinoraWordmark(
                    titleSize = 26.sp,
                    textColor = Color.White,
                    taglineColor = GrowthGreen,
                    showTagline = true,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = AppConfig.TAGLINE_BN,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "সংস্করণ: ${AppConfig.APP_VERSION}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.clickable(onClick = onShowSplash)
                ) {
                    Text(
                        text = "স্প্ল্যাশ স্ক্রিন দেখুন ›",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // About Paragraph Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Finora পরিচিতি",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinoraNavy
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Finora হলো একটি সম্পূর্ণ দেশীয় ব্যক্তিগত অর্থায়ন ও শেয়ার বাজার ক্যালকুলেটর অ্যাপ, যা বিশেষভাবে বাংলাদেশের বিনিয়োগকারী, ব্যবসায়ী ও সাধারণ ব্যবহারকারীদের দৈনন্দিন আর্থিক হিসাবের জন্য তৈরি।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ঢাকা স্টক এক্সচেঞ্জ (DSE) ও সিএসই-র শেয়ার লেনদেন, পি/ই ও ইপিএস অনুপাত, নতুন কেনায় এভারেজিং, ব্রেক-ইভেন দর, এসআইপি এবং ব্যাংক ডিপিএস/এফডি এর মতো ৩৫টি বাস্তবমুখী ক্যালকুলেটর এখন এক অ্যাপে সম্পূর্ণ বাংলায়।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        // Key Features Highlights Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "কেন Finora সেরা?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinoraNavy
                )

                FeatureRow(
                    icon = Icons.Default.Security,
                    title = "১০০% নিরাপদ ও অন-ডিভাইস",
                    desc = "আপনার কোনো আর্থিক ডাটা বাইরে যায় না বা কোনো সার্ভারে সংরক্ষিত হয় না।"
                )

                FeatureRow(
                    icon = Icons.Default.Speed,
                    title = "তাত্ক্ষণিক ও নির্ভুল হিসাব",
                    desc = "ইনপুট দেওয়ার সাথে সাথেই সঠিক রেজাল্ট ও প্রাসঙ্গিক আর্থিক পরামর্শ।"
                )

                FeatureRow(
                    icon = Icons.Default.Star,
                    title = "সম্পূর্ণ বাংলা ইন্টারফেস ও ৳ প্রতীক",
                    desc = "সহজে বোঝার মতো পরিচ্ছন্ন ফিনটেক ডিজাইন ও দেশীয় মুদ্রা প্রতীক।"
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GrowthGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GrowthGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = FinoraNavy
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
