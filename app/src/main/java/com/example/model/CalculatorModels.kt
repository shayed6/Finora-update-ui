package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class CalculatorCategory(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val descriptionBn: String,
    val icon: ImageVector,
    val count: Int
) {
    STOCK_RATIOS(
        id = "stock_ratios",
        titleBn = "স্টক অ্যানালাইসিস রেশিও",
        titleEn = "Stock Analysis Ratios",
        descriptionBn = "P/E, P/B, EPS, ROE সহ মৌলিক বিশ্লেষণের ১৪টি গুরুত্বপূর্ণ অনুপাত",
        icon = Icons.Default.Assessment,
        count = 14
    ),
    STOCK_AVG_PL(
        id = "stock_avg_pl",
        titleBn = "স্টক এভারেজ ও লাভ/ক্ষতি",
        titleEn = "Stock Average & Profit/Loss",
        descriptionBn = "লাভ/ক্ষতি, নতুন এভারেজ, ব্রেক-ইভেন ও টার্গেট প্রাইস হিসাব",
        icon = Icons.Default.TrendingUp,
        count = 10
    ),
    SIP_INVESTMENT(
        id = "sip_investment",
        titleBn = "SIP ও বিনিয়োগ ক্যালকুলেটর",
        titleEn = "SIP & Investment",
        descriptionBn = "এসআইপি, অবসর পরিকল্পনা, মূল্যস্ফীতি ও ভবিষ্যৎ সম্পদ প্রক্ষেপণ",
        icon = Icons.Default.Calculate,
        count = 8
    ),
    BANKING(
        id = "banking",
        titleBn = "ব্যাংকিং ক্যালকুলেটর",
        titleEn = "Banking Calculators",
        descriptionBn = "এফডি, ডিপিএস, লোন ইএমআই ও কার্যকরী সুদের হারের হিসাব",
        icon = Icons.Default.AccountBalance,
        count = 4
    ),
    OTHERS(
        id = "others",
        titleBn = "অন্যান্য ক্যালকুলেটর",
        titleEn = "Other Calculators",
        descriptionBn = "ভ্যাট, জিএসটি ও ব্যবসায়িক ট্যাক্স পরিমাপের সুবিধা",
        icon = Icons.Default.ReceiptLong,
        count = 1
    )
}

data class InputFieldDef(
    val id: String,
    val labelBn: String,
    val placeholderBn: String,
    val unit: String = "",
    val defaultValue: String = "",
    val isRequired: Boolean = true,
    val helpTextBn: String = ""
)

data class SubResultItem(
    val labelBn: String,
    val valueBn: String
)

data class CalcResult(
    val primaryLabelBn: String,
    val primaryValueBn: String,
    val subResults: List<SubResultItem> = emptyList(),
    val insightNoteBn: String,
    val isWarning: Boolean = false,
    val formulaDisplayBn: String = ""
)

data class CalculatorDef(
    val id: String,
    val category: CalculatorCategory,
    val titleBn: String,
    val titleEn: String,
    val formulaSummaryBn: String,
    val descriptionBn: String,
    val inputs: List<InputFieldDef>,
    val calculate: (Map<String, Double>, Boolean) -> CalcResult
)
