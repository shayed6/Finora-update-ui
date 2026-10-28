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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppLanguage
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.GrowthGreen
import com.example.ui.theme.PrimaryBlue

/**
 * In-app Privacy Policy Screen.
 * Displays the complete, official, verbatim Bengali or English Privacy Policy text
 * based on the active app language setting.
 */
@Composable
fun PrivacyPolicyScreen(
    modifier: Modifier = Modifier,
    appLanguage: AppLanguage = LocalAppLanguage.current
) {
    val isEnglish = appLanguage == AppLanguage.ENGLISH
    val sections = if (isEnglish) englishPolicySections else bengaliPolicySections

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Banner with Security Icon & Title
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("privacy_policy_header_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            text = if (isEnglish) "Privacy Policy" else "গোপনীয়তা নীতি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEnglish) "Finora – Personal Finance & Information Security" else "Finora — আপনার আর্থিক হিসাব ও তথ্যের সুরক্ষা",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                // Effective Date, Last Updated & Developer Info
                Text(
                    text = if (isEnglish) "Effective date: 26 September 2026" else "কার্যকর হওয়ার তারিখ: ২৬ সেপ্টেম্বর ২০২৬",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Text(
                    text = if (isEnglish) "Last updated: 27 October 2026" else "সর্বশেষ হালনাগাদ: ২৭ অক্টোবর ২০২৬",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue,
                    lineHeight = 18.sp
                )
                Text(
                    text = if (isEnglish) "Developer: Gazi Holding Limited" else "ডেভেলপার: গাজী হোল্ডিং লিমিটেড",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Full Verbatim Policy Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("privacy_policy_content_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                sections.forEachIndexed { index, section ->
                    PolicySection(
                        title = section.title,
                        paragraphs = section.paragraphs
                    )
                    if (index < sections.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Reusable section composable for each numbered policy article.
 * Handles bold titles, regular body paragraphs, and indented bullet items.
 */
@Composable
private fun PolicySection(
    title: String,
    paragraphs: List<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        for (p in paragraphs) {
            val isBullet = p.startsWith("•")
            if (isBullet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = p.removePrefix("•").trim(),
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            } else {
                Text(
                    text = p,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private data class PolicyItem(
    val title: String,
    val paragraphs: List<String>
)

private val bengaliPolicySections = listOf(
    PolicyItem(
        title = "১. ভূমিকা",
        paragraphs = listOf(
            "Finora (\"আমরা\", \"অ্যাপ\") বাংলাদেশি বিনিয়োগকারী ও সাধারণ মানুষের জন্য তৈরি একটি আর্থিক হিসাবের অ্যাপ। এতে আছে পোর্টফোলিও ট্র্যাকার, ৩৭টি স্টক ও ব্যাংকিং ক্যালকুলেটর, হিসাব পাতি (বাকির খাতা, আয়-ব্যয়, সঞ্চয় ও বিনিয়োগ, হিসাব নিকাশ) এবং PDF রিপোর্ট। অ্যাপ ব্যবহার করলে আপনি এই নীতিতে সম্মত হচ্ছেন বলে ধরা হবে।"
        )
    ),
    PolicyItem(
        title = "২. আমরা কোন তথ্য সংগ্রহ করি",
        paragraphs = listOf(
            "• আপনার নিজের দেওয়া আর্থিক তথ্য (পোর্টফোলিওর শেয়ার ও দর, আয়-ব্যয়, বাকি-বকেয়া, সঞ্চয় ও লক্ষ্য): শুধু আপনার ডিভাইসে থাকে।",
            "• অ্যাপ সেটিংস (ডার্ক মোড, প্রিয় ক্যালকুলেটর): শুধু আপনার ডিভাইসে থাকে।",
            "• বিজ্ঞাপন-সংক্রান্ত তথ্য (অ্যাডভার্টাইজিং আইডি, ডিভাইসের ধরন, IP থেকে আনুমানিক অবস্থান, বিজ্ঞাপনে ইন্টারঅ্যাকশন): বিজ্ঞাপন নেটওয়ার্কের মাধ্যমে (ধারা ৫)।",
            "• প্রযুক্তিগত তথ্য (অপারেটিং সিস্টেম, নেটওয়ার্কের ধরন, বিজ্ঞাপন SDK-এর ক্র্যাশ ডায়াগনস্টিক): বিজ্ঞাপন SDK-গুলোর মাধ্যমে।",
            "আমরা আপনার নাম, ফোন নম্বর, ইমেইল, ব্যাংক বা কার্ডের তথ্য চাই না এবং সংগ্রহ করি না। অ্যাপে বর্তমানে কোনো লগইন বা অ্যাকাউন্ট নেই।"
        )
    ),
    PolicyItem(
        title = "৩. আর্থিক তথ্য আপনার ডিভাইসেই থাকে",
        paragraphs = listOf(
            "আপনার সব হিসাব অ্যাপের ভেতরে আপনার ডিভাইসে সংরক্ষিত থাকে। এই তথ্য আমাদের সার্ভারে পাঠানো বা সংরক্ষণ করা হয় না। অ্যাপ আনইনস্টল করলে বা অ্যাপের ডেটা মুছলে এই তথ্য স্থায়ীভাবে মুছে যেতে পারে, তাই গুরুত্বপূর্ণ তথ্যের PDF এক্সপোর্ট বা নিজস্ব ব্যাকআপ রাখুন।"
        )
    ),
    PolicyItem(
        title = "৪. শেয়ারবাজারের তথ্য",
        paragraphs = listOf(
            "DSE ও CSE-র শেয়ারের দর দেখাতে অ্যাপ ইন্টারনেট থেকে প্রকাশ্য বাজার-তথ্য ডাউনলোড করে (dse.com.bd, dsebd.org ও cse.com.bd থেকে)। এটি শুধু পোর্টফোলিও খুললে বা ম্যানুয়াল রিফ্রেশ করলে ঘটে। এতে শুধু শেয়ারের নাম ও সর্বশেষ দর নেওয়া হয়; আপনার পোর্টফোলিও, পরিমাণ, ক্রয়মূল্য বা লাভ-ক্ষতি পাঠানো হয় না। তবে যেকোনো ওয়েবসাইটের মতো ওই সাইটগুলো আপনার IP ঠিকানা দেখতে পারে। লাভ-ক্ষতির হিসাব আপনার ডিভাইসেই হয়।"
        )
    ),
    PolicyItem(
        title = "৫. বিজ্ঞাপন",
        paragraphs = listOf(
            "অ্যাপে বিজ্ঞাপন দেখানো হয়। এর জন্য আমরা Unity LevelPlay (মিডিয়েশন প্ল্যাটফর্ম) এবং তার মাধ্যমে InMobi ও Mintegral বিজ্ঞাপন নেটওয়ার্ক ব্যবহার করি। এই SDK-গুলো বিজ্ঞাপন দেখানো, পরিমাপ ও ডায়াগনস্টিকের জন্য নিচের তথ্য সংগ্রহ করতে পারে:",
            "• Unity LevelPlay: অ্যাডভার্টাইজিং আইডি (GAID), GAID না থাকলে অ্যান্ড্রয়েড আইডি, ডিভাইসের মডেল, OS ভার্সন, ক্যারিয়ার, নেটওয়ার্কের ধরন, IP থেকে আনুমানিক অবস্থান (দেশ/অঞ্চল/শহর), SDK চালুর সময় ও ক্র্যাশ/ANR ডায়াগনস্টিক, বিজ্ঞাপনের ইম্প্রেশন, ক্লিক ও রিওয়ার্ড।",
            "• InMobi: GAID, ডিভাইসের স্পেসিফিকেশন, স্ক্রিন রেজোলিউশন, ভাষা, ডিভাইসের ওরিয়েন্টেশন, IP থেকে আনুমানিক অবস্থান, বিজ্ঞাপন প্রদর্শন ও ক্লিকের তথ্য।",
            "• Mintegral: GAID, সিস্টেম ভার্সন, ভাষা, স্ক্রিন dpi, ডিস্কের খালি জায়গা, IP থেকে আনুমানিক অঞ্চল, ইম্প্রেশন ও রিওয়ার্ডেড ভিডিও সম্পন্ন হওয়ার তথ্য।",
            "এই SDK-গুলো সুনির্দিষ্ট GPS অবস্থান সংগ্রহ করে না। অ্যাপে দুই ধরনের বিজ্ঞাপন থাকতে পারে: নিচে ব্যানার বিজ্ঞাপন এবং কিছু ফিচার ব্যবহারের আগে রিওয়ার্ডেড ভিডিও বিজ্ঞাপন (যেমন দিনের প্রথম PDF এক্সপোর্ট)। আপনি ফোনের সেটিংস (Settings → Google → Ads) থেকে অ্যাডভার্টাইজিং আইডি রিসেট বা ব্যক্তিগতকৃত বিজ্ঞাপন বন্ধ করতে পারেন। আপনার আর্থিক হিসাবের তথ্য বিজ্ঞাপনদাতাদের সাথে শেয়ার করা হয় না।",
            "নেটওয়ার্কগুলোর নীতি: Unity [লিংক], InMobi [লিংক], Mintegral [লিংক]।"
        )
    ),
    PolicyItem(
        title = "৬. PDF রিপোর্ট (PRO)",
        paragraphs = listOf(
            "PDF রিপোর্ট আপনার ডিভাইসেই তৈরি হয়। তৈরি ফাইলটি আপনি কোথায় সেভ করবেন বা কার সাথে শেয়ার করবেন (WhatsApp, ইমেইল ইত্যাদি) তা সম্পূর্ণ আপনার সিদ্ধান্ত। শেয়ার করার পর সেই ফাইলের ওপর আমাদের নিয়ন্ত্রণ থাকে না।"
        )
    ),
    PolicyItem(
        title = "৭. PRO সাবস্ক্রিপশন ও পেমেন্ট",
        paragraphs = listOf(
            "PRO ফিচারের জন্য কোনো মূল্য পরিশোধ করতে হলে তা [Google Play Billing / অন্য মাধ্যম] দিয়ে হবে। আমরা আপনার কার্ড বা পেমেন্ট তথ্য দেখি না বা সংরক্ষণ করি না।"
        )
    ),
    PolicyItem(
        title = "৮. ডিভাইস অনুমতি",
        paragraphs = listOf(
            "• ইন্টারনেট: শেয়ারের দর আনা ও বিজ্ঞাপন দেখানো।",
            "• নেটওয়ার্ক অবস্থা: ইন্টারনেট সংযোগ আছে কি না দেখা।",
            "অ্যাপ লোকেশন, স্টোরেজ, ক্যামেরা বা ফোন-পরিচয়ের অনুমতি চায় না।"
        )
    ),
    PolicyItem(
        title = "৯. তৃতীয় পক্ষের লিংক",
        paragraphs = listOf(
            "\"শেয়ার বাজার শিখুন\" অংশে Investaloy, Investment mentor Kabir ও Finance with Rafyq-এর মতো বাইরের ওয়েবসাইট বা প্ল্যাটফর্মের লিংক আছে। লিংকে ক্লিক করলে আপনি অ্যাপের বাইরে সেই সাইটে যাবেন। তাদের তথ্য সংগ্রহ ও গোপনীয়তা নীতির জন্য আমরা দায়ী নই।"
        )
    ),
    PolicyItem(
        title = "১০. F-Coin ও ভবিষ্যৎ ফিচার",
        paragraphs = listOf(
            "F-Coin এখনো চালু হয়নি। চালু হলে অ্যাকাউন্ট, কয়েন ব্যালেন্স বা পুরস্কার সংক্রান্ত নতুন তথ্য সংগ্রহ করা হতে পারে। তখন আমরা এই নীতি হালনাগাদ করব এবং প্রয়োজনে আপনার সম্মতি নেব।"
        )
    ),
    PolicyItem(
        title = "১১. তথ্যের নিরাপত্তা",
        paragraphs = listOf(
            "আপনার তথ্য ডিভাইসে থাকায় তার নিরাপত্তা অনেকটাই আপনার ফোনের নিরাপত্তার ওপর নির্ভর করে। স্ক্রিন লক ব্যবহার করুন এবং অ্যাপ শুধু Google Play বা বিশ্বস্ত উৎস থেকে ইনস্টল করুন। কোনো পদ্ধতিই শতভাগ নিরাপদ নয়, তবে আমরা তথ্য সুরক্ষায় যুক্তিসংগত ব্যবস্থা নিই।"
        )
    ),
    PolicyItem(
        title = "১২. আপনার নিয়ন্ত্রণ ও অধিকার",
        paragraphs = listOf(
            "• অ্যাপের ভেতরে যেকোনো এন্ট্রি দেখতে, সম্পাদনা করতে বা মুছতে পারেন।",
            "• সব তথ্য মুছতে ফোনের সেটিংস থেকে অ্যাপের ডেটা ক্লিয়ার করুন বা অ্যাপ আনইনস্টল করুন।",
            "• বিজ্ঞাপন সংক্রান্ত তথ্যের জন্য ফোনের অ্যাড সেটিংস ব্যবহার করুন।",
            "• আমাদের কাছে আপনার কোনো ব্যক্তিগত তথ্য থাকলে তা দেখতে বা মুছতে ধারা ১৬-র ঠিকানায় যোগাযোগ করুন।"
        )
    ),
    PolicyItem(
        title = "১৩. শিশুদের গোপনীয়তা",
        paragraphs = listOf(
            "Finora ১৮ বছরের কম বয়সীদের জন্য তৈরি নয়। আমরা জেনেশুনে শিশুদের ব্যক্তিগত তথ্য সংগ্রহ করি না।"
        )
    ),
    PolicyItem(
        title = "১৪. আর্থিক পরামর্শ নয়",
        paragraphs = listOf(
            "অ্যাপের ক্যালকুলেটর, শেয়ারের দর ও হিসাব শুধু তথ্য ও সহায়তার জন্য। এটি বিনিয়োগ, কর বা আইনি পরামর্শ নয়। বিনিয়োগের সিদ্ধান্তের দায়িত্ব ব্যবহারকারীর নিজের। শেয়ারের দর কিছুটা বিলম্বিত বা ভুল হতে পারে।"
        )
    ),
    PolicyItem(
        title = "১৫. নীতি পরিবর্তন",
        paragraphs = listOf(
            "অ্যাপে নতুন ফিচার যোগ হলে এই নীতি হালনাগাদ হতে পারে। পরিবর্তনের পর অ্যাপ ব্যবহার চালিয়ে গেলে নতুন নীতিতে সম্মতি দেওয়া হয়েছে বলে ধরা হবে। বড় পরিবর্তন হলে অ্যাপে জানানো হবে। এই পাতার ওপরের \"সর্বশেষ হালনাগাদ\" তারিখ দেখে নিন।"
        )
    ),
    PolicyItem(
        title = "১৬. যোগাযোগ",
        paragraphs = listOf(
            "এই নীতি নিয়ে প্রশ্ন থাকলে যোগাযোগ করুন:",
            "ইমেইল: shayedafride@gmail.com"
        )
    )
)

private val englishPolicySections = listOf(
    PolicyItem(
        title = "1. Introduction",
        paragraphs = listOf(
            "Finora (\"we\", \"our\", \"the app\") is a personal finance app built for Bangladeshi investors and everyday users. It includes a portfolio tracker, 37 stock and banking calculators, a Hisab Pati section (due ledger, income and expense, savings and investment, account summary) and PDF reports. By using the app, you agree to this policy."
        )
    ),
    PolicyItem(
        title = "2. Information we collect",
        paragraphs = listOf(
            "• Financial information you enter yourself (portfolio shares and prices, income and expenses, dues, savings and goals): stored only on your device.",
            "• App settings (dark mode, favorite calculators): stored only on your device.",
            "• Advertising-related information (advertising ID, device type, approximate location derived from IP address, interactions with ads): collected through ad networks (see Section 5).",
            "• Technical information (operating system, network type, crash diagnostics of the ad SDKs): collected through the ad SDKs.",
            "We do not ask for or collect your name, phone number, email address, or bank or card details. The app currently has no login or account."
        )
    ),
    PolicyItem(
        title = "3. Your financial data stays on your device",
        paragraphs = listOf(
            "All your records are stored inside the app on your device. This data is not sent to or stored on our servers. If you uninstall the app or clear its data, this information may be permanently deleted, so please export important records as PDF or keep your own backup."
        )
    ),
    PolicyItem(
        title = "4. Stock market data",
        paragraphs = listOf(
            "To show current share prices for DSE and CSE, the app downloads public market data from the internet (from dse.com.bd, dsebd.org and cse.com.bd). This happens only when you open the Portfolio or refresh it manually. Only share names and latest prices are retrieved; your portfolio, quantities, purchase prices or profit and loss are never sent. As with any website, those sites can see your IP address. Profit and loss calculations are done on your device."
        )
    ),
    PolicyItem(
        title = "5. Advertising",
        paragraphs = listOf(
            "The app shows ads. For this we use Unity LevelPlay (a mediation platform) and, through it, the InMobi and Mintegral ad networks. To show and measure ads and for diagnostics, these SDKs may collect the following:",
            "• Unity LevelPlay: advertising ID (GAID), Android ID when GAID is unavailable, device model, OS version, carrier, network type, approximate location from IP address (country, region, city), SDK start-up time and crash/ANR diagnostics, ad impressions, clicks and rewards.",
            "• InMobi: GAID, device specifications, screen resolution, language, device orientation, approximate location from IP address, ad display and click data.",
            "• Mintegral: GAID, system version, language, screen dpi, free disk space, approximate region from IP address, impressions and rewarded video completion data.",
            "These SDKs do not collect precise GPS location. The app may show two kinds of ads: a banner ad at the bottom, and rewarded video ads before some features (for example, the first PDF export of the day). You can reset your advertising ID or turn off personalized ads in your phone settings (Settings → Google → Ads). Your financial data is never shared with advertisers.",
            "Network policies: Unity [link], InMobi [link], Mintegral [link]."
        )
    ),
    PolicyItem(
        title = "6. PDF reports (PRO)",
        paragraphs = listOf(
            "PDF reports are generated on your device. Where you save the file and whom you share it with (WhatsApp, email, etc.) is entirely your decision. Once shared, we have no control over that file."
        )
    ),
    PolicyItem(
        title = "7. PRO subscription and payments",
        paragraphs = listOf(
            "If a payment is required for PRO features, it will be made through [Google Play Billing / other method]. We do not see or store your card or payment details."
        )
    ),
    PolicyItem(
        title = "8. Device permissions",
        paragraphs = listOf(
            "• Internet: to fetch share prices and show ads.",
            "• Network state: to check whether an internet connection is available.",
            "The app does not request location, storage, camera or phone identity permissions."
        )
    ),
    PolicyItem(
        title = "9. Third-party links",
        paragraphs = listOf(
            "The \"Learn the Share Market\" section contains links to external websites and platforms such as Investaloy, Investment mentor Kabir and Finance with Rafyq. When you tap a link you leave the app and visit that site. We are not responsible for their data collection or privacy policies."
        )
    ),
    PolicyItem(
        title = "10. F-Coin and future features",
        paragraphs = listOf(
            "F-Coin has not launched yet. When it does, we may collect new information related to accounts, coin balances or rewards. We will then update this policy and, where needed, ask for your consent."
        )
    ),
    PolicyItem(
        title = "11. Data security",
        paragraphs = listOf(
            "Because your data is stored on your device, its security depends largely on your phone's security. Use a screen lock and install apps only from Google Play or trusted sources. No method is 100% secure, but we take reasonable steps to protect information."
        )
    ),
    PolicyItem(
        title = "12. Your control and rights",
        paragraphs = listOf(
            "• You can view, edit or delete any entry inside the app.",
            "• To delete all data, clear the app's data in your phone settings or uninstall the app.",
            "• For advertising data, use your phone's ad settings.",
            "• If we hold any personal information about you and you want to view or delete it, contact us at the address in Section 16."
        )
    ),
    PolicyItem(
        title = "13. Children's privacy",
        paragraphs = listOf(
            "Finora is not intended for children under 18. We do not knowingly collect personal information from children."
        )
    ),
    PolicyItem(
        title = "14. Not financial advice",
        paragraphs = listOf(
            "The app's calculators, share prices and records are for information and convenience only. They are not investment, tax or legal advice. You are responsible for your own investment decisions. Share prices may be delayed or inaccurate."
        )
    ),
    PolicyItem(
        title = "15. Changes to this policy",
        paragraphs = listOf(
            "We may update this policy when new features are added. Continuing to use the app after a change means you accept the updated policy. Major changes will be announced in the app. Please check the \"Last updated\" date above."
        )
    ),
    PolicyItem(
        title = "16. Contact",
        paragraphs = listOf(
            "For questions about this policy, contact us:",
            "t.me/gz_holdings"
        )
    )
)
