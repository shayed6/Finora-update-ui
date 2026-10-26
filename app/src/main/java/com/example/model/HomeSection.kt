package com.example.model

/**
 * Represents customizable sections on the Finora Home Screen.
 * Users can reorder these sections up or down according to their preference.
 */
enum class HomeSection(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val subtitleBn: String,
    val subtitleEn: String
) {
    PORTFOLIO_SUMMARY(
        id = "portfolio_summary",
        titleBn = "আমার পোর্টফোলিও",
        titleEn = "My Portfolio",
        subtitleBn = "শেয়ার বিনিয়োগ ও লাভ-লোকসান সারসংক্ষেপ",
        subtitleEn = "Stock investment and P&L summary"
    ),
    FINANCIAL_OVERVIEW(
        id = "financial_overview_card",
        titleBn = "আর্থিক বিবরণী",
        titleEn = "Financial Overview",
        subtitleBn = "আয়, ব্যয়, নিট ব্যালেন্স ও মিনি চার্ট",
        subtitleEn = "Income, expense, net balance & mini chart"
    ),
    QUICK_ACTIONS(
        id = "quick_actions_row",
        titleBn = "দ্রুত অ্যাকশন",
        titleEn = "Quick Actions",
        subtitleBn = "আয় যোগ, ব্যয় যোগ ও হিসাব পাতি বোতাম",
        subtitleEn = "Add income, add expense & hisab pati buttons"
    ),
    FAVORITE_CALCULATORS(
        id = "favorite_calculators",
        titleBn = "প্রিয় ক্যালকুলেটর",
        titleEn = "Favorite Calculators",
        subtitleBn = "আপনার পিন করা পছন্দের ক্যালকুলেটরগুলো",
        subtitleEn = "Your pinned favorite calculators"
    ),
    PRIMARY_NAV_CARDS(
        id = "primary_nav_cards",
        titleBn = "ক্যালকুলেটর ও হিসাব পাতি",
        titleEn = "Calculators & Hisab Pati",
        subtitleBn = "৩৭টি ক্যালকুলেটর ও হিসাবের খাতা কার্ড",
        subtitleEn = "37 calculators & hisab pati cards"
    ),
    RECENT_TRANSACTIONS(
        id = "recent_transactions_card",
        titleBn = "সাম্প্রতিক লেনদেন",
        titleEn = "Recent Transactions",
        subtitleBn = "সর্বশেষ আয় ও ব্যয়ের তালিকা",
        subtitleEn = "Latest income and expense records"
    ),
    PRIVACY_INDICATOR(
        id = "privacy_indicator_footer",
        titleBn = "নিরাপত্তা ও গোপনীয়তা",
        titleEn = "Security & Privacy",
        subtitleBn = "সম্পূর্ণ অন-ডিভাইস লোকাল ডেটা সুরক্ষা",
        subtitleEn = "Complete on-device local data privacy"
    );

    companion object {
        val DEFAULT_ORDER: List<HomeSection> = listOf(
            PORTFOLIO_SUMMARY,
            FINANCIAL_OVERVIEW,
            QUICK_ACTIONS,
            FAVORITE_CALCULATORS,
            PRIMARY_NAV_CARDS,
            RECENT_TRANSACTIONS,
            PRIVACY_INDICATOR
        )

        fun parseOrder(orderString: String?): List<HomeSection> {
            if (orderString.isNullOrBlank()) return DEFAULT_ORDER
            val parsed = orderString.split(",")
                .mapNotNull { key ->
                    val cleanKey = key.trim()
                    entries.firstOrNull { it.id == cleanKey }
                }
                .distinct()
            val missing = DEFAULT_ORDER.filter { it !in parsed }
            return parsed + missing
        }

        fun serialize(sections: List<HomeSection>): String {
            return sections.joinToString(",") { it.id }
        }
    }
}
