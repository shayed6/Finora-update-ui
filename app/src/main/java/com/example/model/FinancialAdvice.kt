package com.example.model

data class FinancialAdvice(
    val id: Int,
    val category: String,
    val title: String,
    val quote: String,
    val takeaway: String,
    val authorOrTag: String
)

object FinancialAdviceProvider {
    val ADVICE_LIST = listOf(
        FinancialAdvice(
            id = 1,
            category = "শেয়ার বাজার (Stock Market)",
            title = "বৈচিত্র্যকরণ নীতি (Diversification)",
            quote = "সব ডিম কখনও এক ঝুড়িতে রাখবেন না।",
            takeaway = "শেয়ার বাজারে লোকসানের ঝুঁকি কমাতে ফার্মা, ব্যাংক, টেলিকম ও আইটি সহ একাধিক শক্তিশালী খাতের নির্ভরযোগ্য কোম্পানিতে মূলধন বণ্টন করুন।",
            authorOrTag = "ওয়ারেন বাফেট"
        ),
        FinancialAdvice(
            id = 2,
            category = "ব্যক্তিগত অর্থায়ন (Personal Finance)",
            title = "জরুরী তহবিল গঠন (Emergency Fund)",
            quote = "বিনিয়োগের প্রথম স্তম্ভ হলো আর্থিক নিরাপত্তা।",
            takeaway = "শেয়ার বা ঝুঁকিপূর্ণ খাতে নামার আগে অন্তত ৩ থেকে ৬ মাসের পারিবারিক জীবনযাত্রার ব্যয়ের সমান টাকা সহজে নগদায়নযোগ্য সঞ্চয়ে রাখুন।",
            authorOrTag = "আর্থিক নীতি"
        ),
        FinancialAdvice(
            id = 3,
            category = "মূল্যস্ফীতি পরাস্ত (Beat Inflation)",
            title = "চক্রবৃদ্ধি মুনাফার শক্তি (Compound Interest)",
            quote = "চক্রবৃদ্ধি সুদ পৃথিবীর অষ্টম আশ্চর্য।",
            takeaway = "সেভিংস একাউন্টে অলস টাকা মূল্যস্ফীতির কারণে ক্রয়ক্ষমতা হারায়। দীর্ঘমেয়াদী SIP বা মানসম্পন্ন ব্লু-চিপ স্টকে নিয়মিত বিনিয়োগ করুন।",
            authorOrTag = "আলবার্ট আইনস্টাইন"
        ),
        FinancialAdvice(
            id = 4,
            category = "ট্রেডিং ডিসিপ্লিন (Trading Discipline)",
            title = "স্টপ-লস ও আবেগ নিয়ন্ত্রণ (Stop-Loss)",
            quote = "বাজারে আবেগের কোনো স্থান নেই, নিয়মের মূল্য সর্বোচ্চ।",
            takeaway = "শেয়ার কেনার আগেই আপনার লোকসান সহনশীলতা (Stop-Loss) নির্ধারণ করুন। গুজবে কান না দিয়ে কোম্পানির আর্থিক বিবরণী ও পি/ই রেশিও দেখে সিদ্ধান্ত নিন।",
            authorOrTag = "বেঞ্জামিন গ্রাহাম"
        ),
        FinancialAdvice(
            id = 5,
            category = "শেয়ার মূল্যায়ন (Valuation)",
            title = "P/E রেশিও ও ফান্ডামেন্টাল অ্যানালাইসিস",
            quote = "মূল্য হলো যা আপনি দেন, মান হলো যা আপনি পান।",
            takeaway = "কোম্পানির অতীত মুনাফা প্রবৃদ্ধি, ডিভিডেন্ড ইতিহাস এবং সেক্টর গড় পি/ই যাচাই না করে শুধু দাম বাড়ছে দেখে হুজুগে শেয়ার কিনবেন না।",
            authorOrTag = "স্মার্ট ইনভেস্টর"
        ),
        FinancialAdvice(
            id = 6,
            category = "সঞ্চয় লক্ষ্য (Savings Goals)",
            title = "৫০/৩০/২০ বাজেটিং নিয়ম",
            quote = "খরচের পর যা বাঁচে তা নয়, বরং সঞ্চয়ের পর যা বাঁচে তা খরচ করুন।",
            takeaway = "আয়ের ৫০% প্রয়োজনীয় ব্যয়ে, ৩০% শখ বা ইচ্ছায় এবং অন্তত ২০% ভবিষ্যৎ সঞ্চয় ও বিনিয়োগ তহবিলে সরাসরি বরাদ্দ দিন।",
            authorOrTag = "আর্থিক শৃঙ্খলা"
        )
    )

    fun getRandomAdvice(): FinancialAdvice {
        return ADVICE_LIST.random()
    }

    fun getDailyAdvice(): FinancialAdvice {
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return ADVICE_LIST[dayOfYear % ADVICE_LIST.size]
    }
}
