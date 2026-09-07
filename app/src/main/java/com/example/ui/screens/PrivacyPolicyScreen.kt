package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.FinoraNavy
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun PrivacyPolicyScreen(
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
        // Guarantee Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GrowthGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = GrowthGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "১০০% অন-ডিভাইস প্রাইভেসি নিশ্চয়তা",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FinoraNavy
                    )
                    Text(
                        text = "আপনার আর্থিক হিসাব সর্বদা আপনার নিজের ডিভাইসেই সুরক্ষিত",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Full Policy Text Card
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
                    text = "গোপনীয়তা নীতি (Privacy Policy)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinoraNavy
                )

                Text(
                    text = "Finora ব্যবহারকারীদের ব্যক্তিগত তথ্যের সুরক্ষায় সর্বোচ্চ প্রতিশ্রুতিবদ্ধ। এই নীতিমালায় অ্যাপ্লিকেশনটি কীভাবে কাজ করে তা স্পষ্টভাবে ব্যাখ্যা করা হয়েছে:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                PolicySection(
                    title = "১. কোনো ব্যক্তিগত বা আর্থিক ডাটা সংরক্ষণ নয়",
                    content = "Finora অ্যাপে আপনি যে পরিমাণ অর্থ, শেয়ারের দর বা বিনিয়োগের সংখ্যা ইনপুট দেন, তা সম্পূর্ণ আপনার ডিভাইসের মেমরিতে সাময়িকভাবে হিসাব হয়। কোনো ধরনের আর্থিক ডাটা, ব্যাংকিং তথ্য বা ব্যক্তিগত লেনদেনের রেকর্ড আমাদের কোনো সার্ভার বা তৃতীয় পক্ষের কাছে পাঠানো বা সংরক্ষণ করা হয় না।"
                )

                PolicySection(
                    title = "২. অন-ডিভাইস ক্যালকুলেশন",
                    content = "অ্যাপের সমস্ত ফর্মুলা (P/E, SIP, এভারেজিং, লোন ইত্যাদি) ডিভাইসের অভ্যন্তরীণ প্রসেসরের মাধ্যমে স্বাধীনভাবে কাজ করে। এর জন্য কোনো ইন্টারনেট সংযোগ বা অ্যাকাউন্টে লগইন করার প্রয়োজন নেই।"
                )

                PolicySection(
                    title = "৩. তৃতীয় পক্ষের বিজ্ঞাপন সেবা (AdMob)",
                    content = "অ্যাপটি বিনামূল্যে পরিচালনা ও রক্ষণাবেক্ষণ করার উদ্দেশ্যে এতে গুগল অ্যাডমব (Google AdMob) বা অনুরূপ নির্ভরযোগ্য তৃতীয় পক্ষের বিজ্ঞাপন প্রদর্শন করা হতে পারে। গুগল তাদের নিজস্ব গোপনীয়তা নীতি মেনে ব্যবহারকারীর অভিজ্ঞতা উন্নত করার উদ্দেশ্যে বেনামী ডিভাইস আইডেন্টিফায়ার বা নন-পার্সোনাল ডেটা সংগ্রহ করতে পারে। এ বিষয়ে বিস্তারিত জানতে গুগলের অফিসিয়াল গোপনীয়তা নীতি দেখুন।"
                )

                PolicySection(
                    title = "৪. বাহ্যিক ওয়েব লিংক",
                    content = "অ্যাপটিতে শিক্ষামূলক উদ্দেশ্যে Investaloy, Learn with Kabir, Learn with Rafiq এর মতো বাহ্যিক ওয়েবসাইটের লিংক প্রদান করা হয়েছে। এই লিংকগুলোতে ক্লিক করলে ব্যবহারকারী তাদের নিজস্ব প্ল্যাটফর্মে প্রবেশ করবেন, যার গোপনীয়তা নীতি তাদের নিজস্ব নিয়ন্ত্রণাধীন।"
                )

                PolicySection(
                    title = "৫. যোগাযোগের তথ্য",
                    content = "গোপনীয়তা নীতি বা অ্যাপ সম্পর্কিত কোনো মতামত বা প্রশ্ন থাকলে ডেভেলপার টিমের সাথে নির্দ্বিধায় যোগাযোগ করুন।"
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "সর্বশেষ আপডেট: সেপ্টেম্বর ২০২৬",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = FinoraNavy
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
