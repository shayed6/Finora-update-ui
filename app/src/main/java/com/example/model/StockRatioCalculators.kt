package com.example.model

import com.example.util.BengaliFormatter

object StockRatioCalculators {
    val list: List<CalculatorDef> = listOf(
        // 1. P/E Ratio
        CalculatorDef(
            id = "pe_ratio",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "P/E Ratio (মূল্য-আয় অনুপাত)",
            titleEn = "Price to Earnings Ratio",
            formulaSummaryBn = "P/E = শেয়ারের বাজার দর ÷ ইপিএস (EPS)",
            descriptionBn = "শেয়ার প্রতি ১ টাকা আয়ের জন্য বিনিয়োগকারী কত গুণ মূল্য দিতে রাজি আছেন তা পরিমাপ করে।",
            inputs = listOf(
                InputFieldDef("price", "শেয়ারের বর্তমান দর (Price)", "যেমন: ১২৫.৫০", "৳"),
                InputFieldDef("eps", "শেয়ার প্রতি আয় বা ইপিএস (EPS)", "যেমন: ৮.২০", "৳")
            ),
            calculate = { values, useBn ->
                val price = values["price"] ?: 0.0
                val eps = values["eps"] ?: 0.0
                if (eps <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "P/E রেশিও",
                        primaryValueBn = "অপ্রযোজ্য (N/A)",
                        insightNoteBn = "কোম্পানিটি লোকসানে বা শূন্য ইপিএস-এ রয়েছে। তাই ইতিবাচক পি/ই রেশিও পাওয়া সম্ভব নয়।",
                        isWarning = true,
                        formulaDisplayBn = "P/E = $price ÷ $eps"
                    )
                } else {
                    val pe = price / eps
                    val isHigh = pe > 25.0
                    val isGood = pe in 10.0..20.0
                    val note = when {
                        pe < 10.0 -> "P/E ১০-এর নিচে: শেয়ারটি অবমূল্যায়িত (Undervalued) হতে পারে, তবে আয়ের ধারাবাহিকতা যাচাই করুন।"
                        pe <= 20.0 -> "P/E ১০ থেকে ২০-এর মধ্যে: সাধারণ শেয়ারের ক্ষেত্রে অত্যন্ত যুক্তিসঙ্গত ও নিরাপদ মূল্যায়ন।"
                        pe <= 25.0 -> "P/E ২০ থেকে ২৫-এর মধ্যে: শেয়ারটির মূল্যায়ন গড়ে কিছুটা বেশি, বৃদ্ধির সম্ভাবনা থাকতে পারে।"
                        else -> "P/E ২৫-এর বেশি: শেয়ারটি অতিমূল্যায়িত (Overvalued) হওয়ার সম্ভাবনা রয়েছে।"
                    }
                    CalcResult(
                        primaryLabelBn = "P/E রেশিও",
                        primaryValueBn = BengaliFormatter.formatRatio(pe, useBn),
                        subResults = listOf(
                            SubResultItem("শেয়ারের বাজার দর", BengaliFormatter.formatTaka(price, useBn)),
                            SubResultItem("শেয়ার প্রতি আয় (ইপিএস)", BengaliFormatter.formatTaka(eps, useBn))
                        ),
                        insightNoteBn = note,
                        isWarning = isHigh,
                        formulaDisplayBn = "P/E = $price ÷ $eps"
                    )
                }
            }
        ),

        // 2. P/B Ratio
        CalculatorDef(
            id = "pb_ratio",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "P/B Ratio (মূল্য-বুক ভ্যালু অনুপাত)",
            titleEn = "Price to Book Value Ratio",
            formulaSummaryBn = "P/B = শেয়ারের বাজার দর ÷ বিভিপিএস (BVPS)",
            descriptionBn = "কোম্পানির নিট বুক ভ্যালুর তুলনায় বর্তমান শেয়ার বাজার দর কত গুণ তা প্রকাশ করে।",
            inputs = listOf(
                InputFieldDef("price", "শেয়ারের বর্তমান দর (Price)", "যেমন: ২০০", "৳"),
                InputFieldDef("bvps", "শেয়ার প্রতি বুক ভ্যালু (BVPS)", "যেমন: ১২০", "৳")
            ),
            calculate = { values, useBn ->
                val price = values["price"] ?: 0.0
                val bvps = values["bvps"] ?: 0.0
                if (bvps <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "P/B রেশিও",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "কোম্পানির বুক ভ্যালু শূন্য বা ঋণাত্মক, যা উচ্চ আর্থিক ঝুঁকির ইঙ্গিত দেয়।",
                        isWarning = true
                    )
                } else {
                    val pb = price / bvps
                    val note = when {
                        pb < 1.0 -> "P/B ১.০-এর নিচে: শেয়ারটি তার নিট সম্পত্তির মূল্যের চেয়ে কমে ট্রেড হচ্ছে।"
                        pb <= 3.0 -> "P/B ১.০ থেকে ৩.০-এর মধ্যে: সুস্থ ও গ্রহণযোগ্য মূল্যায়ন।"
                        else -> "P/B ৩.০-এর বেশি: প্রিমিয়াম মূল্যায়ন। উচ্চ বৃদ্ধির সম্ভাবনা না থাকলে ঝুঁকিপূর্ণ হতে পারে।"
                    }
                    CalcResult(
                        primaryLabelBn = "P/B রেশিও",
                        primaryValueBn = BengaliFormatter.formatRatio(pb, useBn),
                        subResults = listOf(
                            SubResultItem("শেয়ার দর", BengaliFormatter.formatTaka(price, useBn)),
                            SubResultItem("বুক ভ্যালু (BVPS)", BengaliFormatter.formatTaka(bvps, useBn))
                        ),
                        insightNoteBn = note,
                        isWarning = pb > 3.0
                    )
                }
            }
        ),

        // 3. P/S Ratio
        CalculatorDef(
            id = "ps_ratio",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "P/S Ratio (মূল্য-বিক্রয় অনুপাত)",
            titleEn = "Price to Sales Ratio",
            formulaSummaryBn = "P/S = বাজার মূলধন ÷ বার্ষিক মোট রাজস্ব",
            descriptionBn = "কোম্পানির মোট আয়ের বিপরীতে বাজার কত মূল্য দিচ্ছে তা যাচাই করতে ব্যবহৃত হয়।",
            inputs = listOf(
                InputFieldDef("market_cap", "বাজার মূলধন / মার্কেট ক্যাপ", "যেমন: ৫০০০০০০০০", "৳"),
                InputFieldDef("revenue", "বার্ষিক মোট রাজস্ব / বিক্রয়", "যেমন: ২৫০০০০০০০", "৳")
            ),
            calculate = { values, useBn ->
                val marketCap = values["market_cap"] ?: 0.0
                val revenue = values["revenue"] ?: 0.0
                if (revenue <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "P/S রেশিও",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "বার্ষিক বিক্রয় বা রাজস্ব অবশ্যই শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val ps = marketCap / revenue
                    val note = if (ps <= 2.0) {
                        "P/S ২-এর নিচে থাকলে সাধারণত ভালো বিক্রয় সমৃদ্ধ বিনিয়োগ সুযোগ ধরা হয়।"
                    } else {
                        "উচ্চ P/S রেশিও; কোম্পানির ভবিষ্যতের বিক্রয় প্রবৃদ্ধি ভালো হওয়া দরকার।"
                    }
                    CalcResult(
                        primaryLabelBn = "P/S রেশিও",
                        primaryValueBn = BengaliFormatter.formatRatio(ps, useBn),
                        insightNoteBn = note,
                        isWarning = ps > 4.0
                    )
                }
            }
        ),

        // 4. EPS
        CalculatorDef(
            id = "eps",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "EPS (শেয়ার প্রতি আয়)",
            titleEn = "Earnings Per Share",
            formulaSummaryBn = "EPS = (নিট মুনাফা − প্রেফার্ড ডিভিডেন্ড) ÷ সাধারণ শেয়ার সংখ্যা",
            descriptionBn = "কোম্পানির প্রতিটি সাধারণ শেয়ারের বিপরীতে অর্জিত মুনাফার পরিমাণ।",
            inputs = listOf(
                InputFieldDef("net_income", "মোট নিট মুনাফা (Net Income)", "যেমন: ১০০০০০০০০", "৳"),
                InputFieldDef("pref_div", "প্রেফার্ড ডিভিডেন্ড (যদি থাকে)", "০", "৳", defaultValue = "0"),
                InputFieldDef("shares", "মোট সাধারণ শেয়ার সংখ্যা", "যেমন: ১২৫০০০০০", "টি")
            ),
            calculate = { values, useBn ->
                val netIncome = values["net_income"] ?: 0.0
                val prefDiv = values["pref_div"] ?: 0.0
                val shares = values["shares"] ?: 0.0
                if (shares <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "ইপিএস (EPS)",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "শেয়ার সংখ্যা শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val eps = (netIncome - prefDiv) / shares
                    val isNegative = eps < 0.0
                    val note = if (isNegative) {
                        "সতর্কতা: কোম্পানিটি লোকসানে রয়েছে (ঋণাত্মক ইপিএস)।"
                    } else {
                        "ক্রমবর্ধমান ও স্থিতিশীল ইপিএস কোম্পানির সুস্থ প্রবৃদ্ধির শক্তিশালী প্রমাণ।"
                    }
                    CalcResult(
                        primaryLabelBn = "শেয়ার প্রতি আয় (ইপিএস)",
                        primaryValueBn = BengaliFormatter.formatTaka(eps, useBn),
                        subResults = listOf(
                            SubResultItem("বন্টনযোগ্য মুনাফা", BengaliFormatter.formatTaka(netIncome - prefDiv, useBn)),
                            SubResultItem("মোট শেয়ার", BengaliFormatter.formatNumber(shares, 0, useBn) + " টি")
                        ),
                        insightNoteBn = note,
                        isWarning = isNegative
                    )
                }
            }
        ),

        // 5. Book Value
        CalculatorDef(
            id = "book_value",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "Book Value (মোট বুক ভ্যালু)",
            titleEn = "Total Book Value",
            formulaSummaryBn = "বুক ভ্যালু = মোট সম্পদ − মোট দায়",
            descriptionBn = "কোম্পানির সমস্ত দায়দেনা পরিশোধের পর শেয়ারহোল্ডারদের জন্য অবশিষ্ট নিট সম্পত্তির মূল্য।",
            inputs = listOf(
                InputFieldDef("assets", "মোট সম্পদ (Total Assets)", "যেমন: ৮০০০০০০০০", "৳"),
                InputFieldDef("liabilities", "মোট দায় (Total Liabilities)", "যেমন: ৩৫০০০০০০০", "৳")
            ),
            calculate = { values, useBn ->
                val assets = values["assets"] ?: 0.0
                val liabilities = values["liabilities"] ?: 0.0
                val bv = assets - liabilities
                val isNegative = bv < 0.0
                CalcResult(
                    primaryLabelBn = "মোট বুক ভ্যালু",
                    primaryValueBn = BengaliFormatter.formatTaka(bv, useBn),
                    subResults = listOf(
                        SubResultItem("মোট সম্পদ", BengaliFormatter.formatTaka(assets, useBn)),
                        SubResultItem("মোট দায়", BengaliFormatter.formatTaka(liabilities, useBn))
                    ),
                    insightNoteBn = if (isNegative) "সতর্কতা: কোম্পানির দায় সম্পদের চেয়ে বেশি (নেগেটিভ ইক্যুইটি)।"
                    else "ইতিবাচক বুক ভ্যালু কোম্পানির ভারসাম্যপূর্ণ সম্পদ ভিত্তির পরিচয় দেয়।",
                    isWarning = isNegative
                )
            }
        ),

        // 6. BVPS
        CalculatorDef(
            id = "bvps",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "BVPS (শেয়ার প্রতি বুক ভ্যালু)",
            titleEn = "Book Value Per Share",
            formulaSummaryBn = "BVPS = মোট বুক ভ্যালু ÷ মোট শেয়ার সংখ্যা",
            descriptionBn = "প্রতিটি শেয়ারের বিপরীতে কোম্পানির নিট সম্পদের আর্থিক ভিত্তি পরিমাপ করে।",
            inputs = listOf(
                InputFieldDef("book_value", "মোট বুক ভ্যালু (Book Value)", "যেমন: ৪৫০,০০,০০০০", "৳"),
                InputFieldDef("shares", "মোট সাধারণ শেয়ার সংখ্যা", "যেমন: ১০০০০০০০", "টি")
            ),
            calculate = { values, useBn ->
                val bv = values["book_value"] ?: 0.0
                val shares = values["shares"] ?: 0.0
                if (shares <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "বিভিপিএস (BVPS)",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "শেয়ার সংখ্যা শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val bvps = bv / shares
                    CalcResult(
                        primaryLabelBn = "শেয়ার প্রতি বুক ভ্যালু",
                        primaryValueBn = BengaliFormatter.formatTaka(bvps, useBn),
                        insightNoteBn = "বর্তমান শেয়ারের বাজার দর যদি BVPS-এর নিচে থাকে, তবে এটি ভ্যালু ইনভেস্টিং সুযোগ হতে পারে।",
                        isWarning = bvps <= 0.0
                    )
                }
            }
        ),

        // 7. D/E Ratio
        CalculatorDef(
            id = "de_ratio",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "D/E Ratio (ঋণ-ইক্যুইটি অনুপাত)",
            titleEn = "Debt to Equity Ratio",
            formulaSummaryBn = "D/E = মোট ঋণ ÷ শেয়ারহোল্ডার ইক্যুইটি",
            descriptionBn = "কোম্পানির নিজের পুঁজির তুলনায় কতটুকু ঋণ গ্রহণ করা হয়েছে তার ঝুঁকি পরিমাপ।",
            inputs = listOf(
                InputFieldDef("debt", "মোট ঋণ (Total Debt)", "যেমন: ১৫০০০০০০০", "৳"),
                InputFieldDef("equity", "শেয়ারহোল্ডার ইক্যুইটি", "যেমন: ২০০০০০০০০", "৳")
            ),
            calculate = { values, useBn ->
                val debt = values["debt"] ?: 0.0
                val equity = values["equity"] ?: 0.0
                if (equity <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "D/E রেশিও",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "ঋণাত্মক বা শূন্য ইক্যুইটি কোম্পানির মারাত্মক দেউলিয়া ঝুঁকি নির্দেশ করে।",
                        isWarning = true
                    )
                } else {
                    val de = debt / equity
                    val note = when {
                        de < 1.0 -> "D/E ১.০-এর কম: কোম্পানিটির ঋণঝুঁকি কম ও নিরাপদ ব্যালেন্স শিট রয়েছে।"
                        de <= 2.0 -> "D/E ১.০ থেকে ২.০-এর মধ্যে: মাঝারি ঋণ নির্ভরতা, সাধারণত গ্রহণযোগ্য।"
                        else -> "সতর্কতা: D/E ২.০-এর বেশি! কোম্পানি অতিরিক্ত ঋণের ঝুঁকিতে রয়েছে।"
                    }
                    CalcResult(
                        primaryLabelBn = "D/E রেশিও",
                        primaryValueBn = BengaliFormatter.formatRatio(de, useBn),
                        insightNoteBn = note,
                        isWarning = de > 2.0
                    )
                }
            }
        ),

        // 8. D/A Ratio
        CalculatorDef(
            id = "da_ratio",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "D/A Ratio (ঋণ-সম্পদ অনুপাত)",
            titleEn = "Debt to Assets Ratio",
            formulaSummaryBn = "D/A = মোট ঋণ ÷ মোট সম্পদ",
            descriptionBn = "কোম্পানির মোট সম্পদের কত শতাংশ ঋণের টাকায় কেনা হয়েছে তা পরিমাপ করে।",
            inputs = listOf(
                InputFieldDef("debt", "মোট ঋণ (Total Debt)", "যেমন: ৪০০০,০০০০", "৳"),
                InputFieldDef("assets", "মোট সম্পদ (Total Assets)", "যেমন: ১০০০,০০০০০", "৳")
            ),
            calculate = { values, useBn ->
                val debt = values["debt"] ?: 0.0
                val assets = values["assets"] ?: 0.0
                if (assets <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "D/A রেশিও",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "মোট সম্পদ শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val da = (debt / assets) * 100.0
                    val note = if (da <= 50.0) {
                        "ঋণ অনুপাত ৫০%-এর নিচে থাকলে আর্থিক সক্ষমতা নিরাপদ ধরা হয়।"
                    } else {
                        "সতর্কতা: মোট সম্পদের অর্ধেকের বেশি ঋণ দ্বারা অর্থায়িত।"
                    }
                    CalcResult(
                        primaryLabelBn = "ঋণ-সম্পদ অনুপাত",
                        primaryValueBn = BengaliFormatter.formatPercent(da, useBn),
                        insightNoteBn = note,
                        isWarning = da > 60.0
                    )
                }
            }
        ),

        // 9. ROE
        CalculatorDef(
            id = "roe",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "ROE (ইক্যুইটির উপর রিটার্ন)",
            titleEn = "Return on Equity",
            formulaSummaryBn = "ROE = (নিট মুনাফা ÷ শেয়ারহোল্ডার ইক্যুইটি) × ১০০%",
            descriptionBn = "শেয়ারহোল্ডারদের বিনিয়োগ করা অর্থ ব্যবহার করে কোম্পানি কত দক্ষতায় মুনাফা করছে।",
            inputs = listOf(
                InputFieldDef("income", "নিট মুনাফা (Net Income)", "যেমন: ৩৫০,০০০০", "৳"),
                InputFieldDef("equity", "শেয়ারহোল্ডার ইক্যুইটি", "যেমন: ২০০০,০০০০", "৳")
            ),
            calculate = { values, useBn ->
                val income = values["income"] ?: 0.0
                val equity = values["equity"] ?: 0.0
                if (equity <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "ROE",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "ইক্যুইটি অবশ্যই শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val roe = (income / equity) * 100.0
                    val isGood = roe >= 15.0
                    val note = when {
                        roe >= 20.0 -> "ROE ২০%-এর বেশি: অসামান্য মুনাফা সৃষ্টির ক্ষমতা!"
                        roe >= 15.0 -> "১৫%-এর বেশি ROE ভালো মানের কোম্পানি ধরা হয়।"
                        roe > 0.0 -> "১৫%-এর নিচে ROE: সন্তোষজনক তবে আরও উন্নতির সুযোগ রয়েছে।"
                        else -> "সতর্কতা: ঋণাত্মক ROE নির্দেশ করে কোম্পানি লোকসানে রয়েছে।"
                    }
                    CalcResult(
                        primaryLabelBn = "রিটার্ন অন ইক্যুইটি (ROE)",
                        primaryValueBn = BengaliFormatter.formatPercent(roe, useBn),
                        insightNoteBn = note,
                        isWarning = roe < 10.0
                    )
                }
            }
        ),

        // 10. ROA
        CalculatorDef(
            id = "roa",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "ROA (সম্পদের উপর রিটার্ন)",
            titleEn = "Return on Assets",
            formulaSummaryBn = "ROA = (নিট মুনাফা ÷ মোট সম্পদ) × ১০০%",
            descriptionBn = "কোম্পানির সমস্ত সম্পদ কাজে লাগিয়ে কতটা কার্যকরভাবে লাভ সৃষ্টি হচ্ছে তা দেখায়।",
            inputs = listOf(
                InputFieldDef("income", "নিট মুনাফা (Net Income)", "যেমন: ১২০,০০০০", "৳"),
                InputFieldDef("assets", "মোট সম্পদ (Total Assets)", "যেমন: ১৫০০,০০০০", "৳")
            ),
            calculate = { values, useBn ->
                val income = values["income"] ?: 0.0
                val assets = values["assets"] ?: 0.0
                if (assets <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "ROA",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "মোট সম্পদ শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val roa = (income / assets) * 100.0
                    val note = if (roa >= 5.0) {
                        "৫%-এর বেশি ROA দক্ষ সম্পদ ব্যবহারের স্পষ্ট প্রমাণ।"
                    } else {
                        "৫%-এর নিচে ROA কোম্পানির সম্পদ ব্যবহারের দক্ষতা আরও বাড়ানো প্রয়োজন।"
                    }
                    CalcResult(
                        primaryLabelBn = "রিটার্ন অন অ্যাসেট (ROA)",
                        primaryValueBn = BengaliFormatter.formatPercent(roa, useBn),
                        insightNoteBn = note,
                        isWarning = roa < 5.0
                    )
                }
            }
        ),

        // 11. ROCE
        CalculatorDef(
            id = "roce",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "ROCE (বিনিয়োগকৃত মূলধনের উপর রিটার্ন)",
            titleEn = "Return on Capital Employed",
            formulaSummaryBn = "ROCE = [EBIT ÷ (মোট সম্পদ − চলতি দায়)] × ১০০%",
            descriptionBn = "ব্যবসায় খাটানো মোট মূলধনের বিপরীতে কর ও সুদ পূর্ব মুনাফার দক্ষতা।",
            inputs = listOf(
                InputFieldDef("ebit", "পরিচালন মুনাফা / EBIT", "যেমন: ২৫০,০০০০", "৳"),
                InputFieldDef("assets", "মোট সম্পদ (Total Assets)", "যেমন: ১৮০০,০০০০", "৳"),
                InputFieldDef("liabilities", "চলতি দায় (Current Liabilities)", "যেমন: ৩০০,০০০০", "৳")
            ),
            calculate = { values, useBn ->
                val ebit = values["ebit"] ?: 0.0
                val assets = values["assets"] ?: 0.0
                val liabilities = values["liabilities"] ?: 0.0
                val capitalEmployed = assets - liabilities
                if (capitalEmployed <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "ROCE",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "বিনিয়োগকৃত মূলধন অবশ্যই ধনাত্মক হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val roce = (ebit / capitalEmployed) * 100.0
                    val note = if (roce >= 15.0) {
                        "১৫%-এর বেশি ROCE দীর্ঘমেয়াদী টেকসই ও সফল ব্যবসার শক্তিশালী লক্ষণ।"
                    } else {
                        "মূলধন খরচের চেয়ে ROCE বেশি হওয়া বাঞ্ছনীয় (১৫%+ আদর্শ)।"
                    }
                    CalcResult(
                        primaryLabelBn = "ROCE অনুপাত",
                        primaryValueBn = BengaliFormatter.formatPercent(roce, useBn),
                        subResults = listOf(
                            SubResultItem("বিনিয়োগকৃত মূলধন", BengaliFormatter.formatTaka(capitalEmployed, useBn))
                        ),
                        insightNoteBn = note,
                        isWarning = roce < 12.0
                    )
                }
            }
        ),

        // 12. Market Cap
        CalculatorDef(
            id = "market_cap",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "Market Cap (বাজার মূলধন)",
            titleEn = "Market Capitalization",
            formulaSummaryBn = "মার্কেট ক্যাপ = শেয়ারের বর্তমান দর × মোট শেয়ার সংখ্যা",
            descriptionBn = "স্টক এক্সচেঞ্জে কোম্পানির তালিকাভুক্ত সমস্ত শেয়ারের মোট বাজার মূল্য।",
            inputs = listOf(
                InputFieldDef("price", "শেয়ারের বর্তমান বাজার দর", "যেমন: ৭৮.৫০", "৳"),
                InputFieldDef("shares", "মোট শেয়ার সংখ্যা", "যেমন: ৫০০০০০০০", "টি")
            ),
            calculate = { values, useBn ->
                val price = values["price"] ?: 0.0
                val shares = values["shares"] ?: 0.0
                val mcap = price * shares
                val note = "কোম্পানির আকার অনুযায়ী লার্জ ক্যাপ, মিড ক্যাপ বা স্মল ক্যাপ শ্রেণিবদ্ধ করা হয়।"
                CalcResult(
                    primaryLabelBn = "মোট বাজার মূলধন (Market Cap)",
                    primaryValueBn = BengaliFormatter.formatTaka(mcap, useBn),
                    subResults = listOf(
                        SubResultItem("শেয়ার দর", BengaliFormatter.formatTaka(price, useBn)),
                        SubResultItem("মোট শেয়ার", BengaliFormatter.formatNumber(shares, 0, useBn) + " টি")
                    ),
                    insightNoteBn = note
                )
            }
        ),

        // 13. Dividend Yield
        CalculatorDef(
            id = "dividend_yield",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "Dividend Yield (লভ্যাংশ ফলন)",
            titleEn = "Dividend Yield %",
            formulaSummaryBn = "ডিভিডেন্ড ইল্ড = (শেয়ার প্রতি বার্ষিক লভ্যাংশ ÷ শেয়ার দর) × ১০০%",
            descriptionBn = "বর্তমান বাজার দরের তুলনায় বার্ষিক কত শতাংশ নগদ লভ্যাংশ বিনিয়োগকারী পাচ্ছেন।",
            inputs = listOf(
                InputFieldDef("dividend", "শেয়ার প্রতি বার্ষিক লভ্যাংশ", "যেমন: ১২.০০", "৳"),
                InputFieldDef("price", "শেয়ারের বর্তমান বাজার দর", "যেমন: ১৫০.০০", "৳")
            ),
            calculate = { values, useBn ->
                val dividend = values["dividend"] ?: 0.0
                val price = values["price"] ?: 0.0
                if (price <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "ডিভিডেন্ড ইল্ড",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "শেয়ারের দর শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val dy = (dividend / price) * 100.0
                    val note = if (dy >= 5.0) {
                        "৫%-এর বেশি ডিভিডেন্ড ইল্ড নিয়মিত আয়ের জন্য অত্যন্ত আকর্ষণীয়!"
                    } else {
                        "ডিভিডেন্ড ইল্ডের পাশাপাশি মূলধনী লাভের সম্ভাবনা বিবেচনা করুন।"
                    }
                    CalcResult(
                        primaryLabelBn = "লভ্যাংশ ফলন (Dividend Yield)",
                        primaryValueBn = BengaliFormatter.formatPercent(dy, useBn),
                        insightNoteBn = note,
                        isWarning = false
                    )
                }
            }
        ),

        // 14. PEG Ratio
        CalculatorDef(
            id = "peg_ratio",
            category = CalculatorCategory.STOCK_RATIOS,
            titleBn = "PEG Ratio (মূল্য-আয়-প্রবৃদ্ধি অনুপাত)",
            titleEn = "Price/Earnings to Growth Ratio",
            formulaSummaryBn = "PEG = P/E রেশিও ÷ প্রত্যাশিত ইপিএস প্রবৃদ্ধি হার (%)",
            descriptionBn = "কোম্পানির ভবিষ্যৎ আয়ের বৃদ্ধির হারের সাপেক্ষে শেয়ারের মূল্যায়ন সস্তা নাকি দামী।",
            inputs = listOf(
                InputFieldDef("pe", "P/E রেশিও", "যেমন: ১৮.৫০", "x"),
                InputFieldDef("growth", "প্রত্যাশিত বার্ষিক প্রবৃদ্ধি হার (%)", "যেমন: ২০", "%")
            ),
            calculate = { values, useBn ->
                val pe = values["pe"] ?: 0.0
                val growth = values["growth"] ?: 0.0
                if (growth <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "PEG রেশিও",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "প্রত্যাশিত আয়ের প্রবৃদ্ধি ধনাত্মক হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val peg = pe / growth
                    val note = when {
                        peg < 1.0 -> "PEG ১.০-এর কম: শেয়ারটি তার প্রবৃদ্ধির তুলনায় অবমূল্যায়িত (সস্তা)!"
                        peg <= 1.5 -> "PEG ১.০ থেকে ১.৫-এর মধ্যে: উপযুক্ত ও ন্যায্য মূল্যায়ন।"
                        else -> "সতর্কতা: PEG ২-এর বেশি হলে প্রবৃদ্ধির তুলনায় শেয়ারের দাম বেশি।"
                    }
                    CalcResult(
                        primaryLabelBn = "PEG রেশিও",
                        primaryValueBn = BengaliFormatter.formatRatio(peg, useBn),
                        insightNoteBn = note,
                        isWarning = peg > 2.0
                    )
                }
            }
        )
    )
}
