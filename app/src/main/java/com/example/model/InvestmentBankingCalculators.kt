package com.example.model

import com.example.util.BengaliFormatter
import kotlin.math.pow

object InvestmentBankingCalculators {
    val list: List<CalculatorDef> = listOf(
        // === CATEGORY 3: SIP ও বিনিয়োগ ক্যালকুলেটর (6 calculators) ===

        // 1. SIP Calculator
        CalculatorDef(
            id = "sip_calc",
            category = CalculatorCategory.SIP_INVESTMENT,
            titleBn = "SIP Calculator (মাসিক এসআইপি)",
            titleEn = "SIP Calculator",
            formulaSummaryBn = "FV = P × [((১ + r)^n − ১) ÷ r] × (১ + r)",
            descriptionBn = "প্রতি মাসে নির্দিষ্ট পরিমাণ অর্থ নিয়মিত বিনিয়োগ করে মেয়াদ শেষে কত টাকা হবে তা হিসাব করুন।",
            inputs = listOf(
                InputFieldDef("monthly_inv", "প্রতি মাসে বিনিয়োগের পরিমাণ", "যেমন: ৫০০০.০০", "৳"),
                InputFieldDef("annual_rate", "প্রত্যাশিত বার্ষিক রিটার্ন হার %", "যেমন: ১২.০০", "%", defaultValue = "12"),
                InputFieldDef("years", "বিনিয়োগের সময়কাল (বছর)", "যেমন: ১০", "বছর", defaultValue = "10")
            ),
            calculate = { values, useBn ->
                val p = values["monthly_inv"] ?: 0.0
                val rate = values["annual_rate"] ?: 0.0
                val years = values["years"] ?: 0.0

                val r = rate / 12.0 / 100.0
                val n = years * 12.0

                if (p <= 0.0 || n <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "পরিপক্ক মূল্য",
                        primaryValueBn = "০ ৳",
                        insightNoteBn = "বিনিয়োগের পরিমাণ ও সময়কাল শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val fv = if (r > 0.0) {
                        p * (((1.0 + r).pow(n) - 1.0) / r) * (1.0 + r)
                    } else {
                        p * n
                    }
                    val totalInvested = p * n
                    val wealthGain = fv - totalInvested

                    CalcResult(
                        primaryLabelBn = "সম্ভাব্য পরিপক্ক মূল্য (Future Value)",
                        primaryValueBn = BengaliFormatter.formatTaka(fv, useBn),
                        subResults = listOf(
                            SubResultItem("মোট বিনিয়োগকৃত অর্থ", BengaliFormatter.formatTaka(totalInvested, useBn)),
                            SubResultItem("আনুমানিক মুনাফা / প্রবৃদ্ধি", BengaliFormatter.formatTaka(wealthGain, useBn)),
                            SubResultItem("মোট কিস্তির সংখ্যা", "${BengaliFormatter.formatNumber(n, 0, useBn)} টি")
                        ),
                        insightNoteBn = "চক্রবৃদ্ধি সুদের যাদুকরী প্রভাবে আপনার বিনিয়োগ সময়ের সাথে সাথে দ্রুত গুণিতক হারে বৃদ্ধি পায়।"
                    )
                }
            }
        ),

        // 2. Lumpsum Calculator
        CalculatorDef(
            id = "lumpsum_calc",
            category = CalculatorCategory.SIP_INVESTMENT,
            titleBn = "Lumpsum (এককালীন বিনিয়োগ)",
            titleEn = "Lumpsum Calculator",
            formulaSummaryBn = "ভবিষ্যৎ মান = মূলধন × (১ + হার)^বছর",
            descriptionBn = "একবারে বা এককালীন বিনিয়োগকৃত অর্থ চক্রবৃদ্ধি হারে বেড়ে কত টাকায় পৌঁছাবে।",
            inputs = listOf(
                InputFieldDef("principal", "এককালীন বিনিয়োগ মূলধন", "যেমন: ১০০০০০", "৳"),
                InputFieldDef("annual_rate", "প্রত্যাশিত বার্ষিক রিটার্ন %", "যেমন: ১২.৫০", "%", defaultValue = "12"),
                InputFieldDef("years", "বিনিয়োগের সময়কাল (বছর)", "যেমন: ৮", "বছর", defaultValue = "5")
            ),
            calculate = { values, useBn ->
                val p = values["principal"] ?: 0.0
                val rate = values["annual_rate"] ?: 0.0
                val years = values["years"] ?: 0.0

                val fv = p * (1.0 + rate / 100.0).pow(years)
                val gain = fv - p

                CalcResult(
                    primaryLabelBn = "সম্ভাব্য ভবিষ্যৎ মূল্য",
                    primaryValueBn = BengaliFormatter.formatTaka(fv, useBn),
                    subResults = listOf(
                        SubResultItem("মূল বিনিয়োগকৃত অর্থ", BengaliFormatter.formatTaka(p, useBn)),
                        SubResultItem("মোট অর্জিত মুনাফা", BengaliFormatter.formatTaka(gain, useBn))
                    ),
                    insightNoteBn = "এককালীন বিনিয়োগে দীর্ঘ সময় ধরে চক্রবৃদ্ধি মুনাফা সঞ্চিত হয়ে মূলধনের আকার বহুগুণ বাড়ায়।"
                )
            }
        ),

        // 3. Investment to Reach (Goal SIP)
        CalculatorDef(
            id = "goal_sip",
            category = CalculatorCategory.SIP_INVESTMENT,
            titleBn = "Goal SIP (লক্ষ্য অর্জনের এসআইপি)",
            titleEn = "Investment to Reach Goal",
            formulaSummaryBn = "প্রয়োজনীয় মাসিক SIP = লক্ষ্য × r ÷ [((১+r)^n − ১) × (১+r)]",
            descriptionBn = "নির্দিষ্ট আর্থিক লক্ষ্য (যেমন ১ কোটি টাকা) অর্জনের জন্য প্রতি মাসে কত টাকা সঞ্চয় করতে হবে।",
            inputs = listOf(
                InputFieldDef("target_amount", "কাঙ্ক্ষিত আর্থিক লক্ষ্য (টাকা)", "যেমন: ১০০০,০০০০", "৳"),
                InputFieldDef("annual_rate", "প্রত্যাশিত বার্ষিক রিটার্ন %", "যেমন: ১৪.০০", "%", defaultValue = "14"),
                InputFieldDef("years", "লক্ষ্য অর্জনের সময়কাল (বছর)", "যেমন: ১৫", "বছর", defaultValue = "10")
            ),
            calculate = { values, useBn ->
                val target = values["target_amount"] ?: 0.0
                val rate = values["annual_rate"] ?: 0.0
                val years = values["years"] ?: 0.0

                val r = rate / 12.0 / 100.0
                val n = years * 12.0

                if (target <= 0.0 || n <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "মাসিক কিস্তি",
                        primaryValueBn = "০ ৳",
                        insightNoteBn = "সঠিক লক্ষ্য ও সময়কাল প্রদান করুন।",
                        isWarning = true
                    )
                } else {
                    val monthlySip = if (r > 0.0) {
                        (target * r) / (((1.0 + r).pow(n) - 1.0) * (1.0 + r))
                    } else {
                        target / n
                    }
                    val totalInvested = monthlySip * n

                    CalcResult(
                        primaryLabelBn = "প্রয়োজনীয় মাসিক এসআইপি",
                        primaryValueBn = BengaliFormatter.formatTaka(monthlySip, useBn),
                        subResults = listOf(
                            SubResultItem("আপনার কাঙ্ক্ষিত লক্ষ্য", BengaliFormatter.formatTaka(target, useBn)),
                            SubResultItem("মোট নিজস্ব বিনিয়োগ", BengaliFormatter.formatTaka(totalInvested, useBn)),
                            SubResultItem("রিটার্ন থেকে আসবে", BengaliFormatter.formatTaka(target - totalInvested, useBn))
                        ),
                        insightNoteBn = "নিয়মিত সুশৃঙ্খল বিনিয়োগের মাধ্যমে আপনার আর্থিক লক্ষ্য সহজেই অর্জন করা সম্ভব।"
                    )
                }
            }
        ),

        // 4. SWP Calculator
        CalculatorDef(
            id = "swp_calc",
            category = CalculatorCategory.SIP_INVESTMENT,
            titleBn = "SWP (সিস্টেমেটিক উইথড্রয়াল প্ল্যান)",
            titleEn = "Systematic Withdrawal Plan",
            formulaSummaryBn = "অবশিষ্ট ব্যালেন্স = তহবিল × (১+r)^n − উত্তোলন কম্পাউন্ড",
            descriptionBn = "সঞ্চিত তহবিল থেকে প্রতি মাসে নিয়মিত নির্দিষ্ট টাকা উত্তোলনের পর অবশিষ্ট ব্যালেন্স কত থাকবে।",
            inputs = listOf(
                InputFieldDef("initial_corpus", "প্রারম্ভিক সঞ্চিত তহবিল", "যেমন: ২৫০০০০০", "৳"),
                InputFieldDef("monthly_withdrawal", "প্রতি মাসে উত্তোলনের পরিমাণ", "যেমন: ২০০০০", "৳"),
                InputFieldDef("annual_rate", "তহবিলের বার্ষিক রিটার্ন %", "যেমন: ৯.০০", "%", defaultValue = "9"),
                InputFieldDef("years", "উত্তোলনের সময়কাল (বছর)", "যেমন: ১০", "বছর", defaultValue = "10")
            ),
            calculate = { values, useBn ->
                val corpus = values["initial_corpus"] ?: 0.0
                val withdrawal = values["monthly_withdrawal"] ?: 0.0
                val rate = values["annual_rate"] ?: 0.0
                val years = values["years"] ?: 0.0

                val r = rate / 12.0 / 100.0
                val n = years * 12.0

                val compGrowth = (1.0 + r).pow(n)
                val remainingBalance = if (r > 0.0) {
                    corpus * compGrowth - withdrawal * ((compGrowth - 1.0) / r)
                } else {
                    corpus - (withdrawal * n)
                }
                val totalWithdrawn = withdrawal * n
                val isDepleted = remainingBalance <= 0.0

                val note = if (isDepleted) {
                    "উত্তোলনের হার বেশি — করপাস মেয়াদ শেষের আগেই ফুরিয়ে যেতে পারে"
                } else {
                    "তহবিলটি সফলভাবে নিয়মিত মাসিক আয় প্রদান করবে এবং মেয়াদ শেষেও অর্থ অবশিষ্ট থাকবে।"
                }

                CalcResult(
                    primaryLabelBn = if (isDepleted) "করপাস ফুরিয়ে যাবে" else "মেয়াদ শেষে অবশিষ্ট ব্যালেন্স",
                    primaryValueBn = BengaliFormatter.formatTaka(if (isDepleted) 0.0 else remainingBalance, useBn),
                    subResults = listOf(
                        SubResultItem("মোট উত্তোলিত অর্থ", BengaliFormatter.formatTaka(totalWithdrawn, useBn)),
                        SubResultItem("প্রারম্ভিক তহবিল", BengaliFormatter.formatTaka(corpus, useBn))
                    ),
                    insightNoteBn = note,
                    isWarning = isDepleted
                )
            }
        ),

        // 5. STP Calculator
        CalculatorDef(
            id = "stp_calc",
            category = CalculatorCategory.SIP_INVESTMENT,
            titleBn = "STP (সিস্টেমেটিক ট্রান্সফার প্ল্যান)",
            titleEn = "Systematic Transfer Plan",
            formulaSummaryBn = "টার্গেট ফান্ড = মাসিক ট্রান্সফারের এসআইপি কম্পাউন্ড মান",
            descriptionBn = "একটি কম ঝুঁকিপূর্ণ ফান্ড থেকে নিয়মিতভাবে ইকুইটি ফান্ডে অর্থ স্থানান্তরের ফলাফল হিসাব।",
            inputs = listOf(
                InputFieldDef("source_corpus", "সোর্স ফান্ডের মূলধন", "যেমন: ১০,০০,০০০", "৳"),
                InputFieldDef("monthly_transfer", "প্রতি মাসের ট্রান্সফার পরিমাণ", "যেমন: ১৫,০০০", "৳"),
                InputFieldDef("target_return", "টার্গেট ফান্ডের প্রত্যাশিত রিটার্ন %", "যেমন: ১৩.০০", "%", defaultValue = "13"),
                InputFieldDef("years", "ট্রান্সফারের সময়কাল (বছর)", "যেমন: ৫", "বছর", defaultValue = "5")
            ),
            calculate = { values, useBn ->
                val source = values["source_corpus"] ?: 0.0
                val transfer = values["monthly_transfer"] ?: 0.0
                val rate = values["target_return"] ?: 0.0
                val years = values["years"] ?: 0.0

                val r = rate / 12.0 / 100.0
                val n = years * 12.0

                val targetFv = if (r > 0.0) {
                    transfer * (((1.0 + r).pow(n) - 1.0) / r) * (1.0 + r)
                } else {
                    transfer * n
                }
                val totalTransferred = transfer * n
                val remainingSource = source - totalTransferred
                val isOverTransferred = remainingSource < 0.0

                CalcResult(
                    primaryLabelBn = "টার্গেট ফান্ডের ভ্যালু",
                    primaryValueBn = BengaliFormatter.formatTaka(targetFv, useBn),
                    subResults = listOf(
                        SubResultItem("সোর্স ফান্ডে অবশিষ্ট", BengaliFormatter.formatTaka(if (remainingSource < 0) 0.0 else remainingSource, useBn)),
                        SubResultItem("মোট স্থানান্তরিত অর্থ", BengaliFormatter.formatTaka(totalTransferred, useBn))
                    ),
                    insightNoteBn = if (isOverTransferred) "সতর্কতা: ট্রান্সফারের মোট পরিমাণ সোর্স ফান্ডের মোট মূলধনের চেয়ে বেশি।"
                    else "এসটিপি এককালীন ঝুঁকি হ্রাস করে ধাপে ধাপে শেয়ার বাজারে প্রবেশ করতে সাহায্য করে।",
                    isWarning = isOverTransferred
                )
            }
        ),

        // 6. Mutual Fund Return
        CalculatorDef(
            id = "mf_return",
            category = CalculatorCategory.SIP_INVESTMENT,
            titleBn = "MF Return (মিউচুয়াল ফান্ড রিটার্ন)",
            titleEn = "Mutual Fund Return Calculator",
            formulaSummaryBn = "পরম লাভ % এবং সিএজিআর (CAGR) বার্ষিক চক্রবৃদ্ধি হার",
            descriptionBn = "মিউচুয়াল ফান্ডে বিনিয়োগকৃত অর্থের মোট রিটার্ন এবং বাৎসরিক চক্রবৃদ্ধি প্রবৃদ্ধি হার বের করুন।",
            inputs = listOf(
                InputFieldDef("invested", "বিনিয়োগকৃত মোট অর্থ", "যেমন: ২০০০০০", "৳"),
                InputFieldDef("current_val", "বর্তমান বাজার মূল্য", "যেমন: ৩২৫০০০", "৳"),
                InputFieldDef("years", "বিনিয়োগের সময়কাল (বছর)", "যেমন: ৩.৫", "বছর", defaultValue = "3")
            ),
            calculate = { values, useBn ->
                val invested = values["invested"] ?: 0.0
                val current = values["current_val"] ?: 0.0
                val years = values["years"] ?: 0.0

                if (invested <= 0.0 || years <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "রিটার্ন",
                        primaryValueBn = "০%",
                        insightNoteBn = "বিনিয়োগকৃত অর্থ ও সময়কাল শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val absReturn = ((current - invested) / invested) * 100.0
                    val cagr = ((current / invested).pow(1.0 / years) - 1.0) * 100.0
                    val isLoss = current < invested

                    CalcResult(
                        primaryLabelBn = "বার্ষিক চক্রবৃদ্ধি রিটার্ন (CAGR)",
                        primaryValueBn = BengaliFormatter.formatPercent(cagr, useBn),
                        subResults = listOf(
                            SubResultItem("মোট পরম রিটার্ন (Absolute)", BengaliFormatter.formatPercent(absReturn, useBn)),
                            SubResultItem("মোট লাভ/ক্ষতির পরিমাণ", BengaliFormatter.formatTaka(current - invested, useBn))
                        ),
                        insightNoteBn = if (isLoss) "বর্তমান মান বিনিয়োগের চেয়ে কম; বাজার সংশোধন পর্যালোচনা করুন।"
                        else "সিএজিআর (CAGR) দীর্ঘমেয়াদে বিনিয়োগের প্রকৃত বার্ষিক দক্ষতা প্রকাশের সেরা নির্দেশক।",
                        isWarning = isLoss
                    )
                }
            }
        ),

        // 7. Inflation Calculator (মূল্যস্ফীতি ক্যালকুলেটর)
        CalculatorDef(
            id = "inflation_calc",
            category = CalculatorCategory.SIP_INVESTMENT,
            titleBn = "Inflation Calculator (মূল্যস্ফীতি ক্যালকুলেটর)",
            titleEn = "Inflation Calculator",
            formulaSummaryBn = "ভবিষ্যতের ব্যয় = বর্তমান ব্যয় × (১ + মূল্যস্ফীতি)^বছর",
            descriptionBn = "মূল্যস্ফীতির কারণে ভবিষ্যৎ খরচ বৃদ্ধি এবং নগদ টাকার প্রকৃত ক্রয়ক্ষমতা হ্রাস পরিমাপ করুন।",
            inputs = listOf(
                InputFieldDef("current_amount", "বর্তমান টাকার পরিমাণ / খরচ", "যেমন: ১০০০০", "৳", defaultValue = "10000"),
                InputFieldDef("inflation_rate", "বার্ষিক মূল্যস্ফীতি হার %", "যেমন: ৮.০০", "%", defaultValue = "8"),
                InputFieldDef("years", "সময়কাল (বছর)", "যেমন: ১০", "বছর", defaultValue = "10")
            ),
            calculate = { values, useBn ->
                val amount = values["current_amount"] ?: 0.0
                val rate = values["inflation_rate"] ?: 0.0
                val years = values["years"] ?: 0.0

                if (amount <= 0.0 || years <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "ভবিষ্যতের আনুমানিক মূল্য",
                        primaryValueBn = "০ ৳",
                        insightNoteBn = "টাকার পরিমাণ ও সময়কাল শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val rateDec = rate / 100.0
                    val factor = (1.0 + rateDec).pow(years)
                    val futureCost = amount * factor
                    val priceIncrease = futureCost - amount
                    val realPurchasingPower = amount / factor
                    val powerLoss = amount - realPurchasingPower
                    val lossPercent = (1.0 - (1.0 / factor)) * 100.0

                    CalcResult(
                        primaryLabelBn = "ভবিষ্যতে এই জিনিসের জন্য প্রয়োজন হবে",
                        primaryValueBn = BengaliFormatter.formatTaka(futureCost, useBn),
                        subResults = listOf(
                            SubResultItem("খরচ বৃদ্ধির পরিমাণ", "+${BengaliFormatter.formatTaka(priceIncrease, useBn)}"),
                            SubResultItem("আজকের এই টাকার ভবিষ্যৎ ক্রয়ক্ষমতা", BengaliFormatter.formatTaka(realPurchasingPower, useBn)),
                            SubResultItem("ক্রয়ক্ষমতা ক্ষয়ের হার", "-${BengaliFormatter.formatPercent(lossPercent, useBn)}")
                        ),
                        insightNoteBn = "মূল্যস্ফীতিকে বিবেচনায় না রেখে বিনিয়োগ পরিকল্পনা করলে ভবিষ্যৎ আর্থিক লক্ষ্য অর্জনে ঘাটতি দেখা দিতে পারে।"
                    )
                }
            }
        ),

        // 8. Retirement Planner (অবসর পরিকল্পনা ক্যালকুলেটর)
        CalculatorDef(
            id = "retirement_calc",
            category = CalculatorCategory.SIP_INVESTMENT,
            titleBn = "Retirement Planner (অবসর পরিকল্পনা ক্যালকুলেটর)",
            titleEn = "Retirement Planning Calculator",
            formulaSummaryBn = "প্রয়োজনীয় করপাস = অবসরে বার্ষিক খরচ × [১ − (১ + r_real)^(-n)] ÷ r_real",
            descriptionBn = "অবসরের পর নিশ্চিন্তে জীবনযাপনের জন্য কত টাকা করপাস সঞ্চয় করতে হবে এবং প্রতি মাসে কত টাকা এসআইপি করতে হবে তা নিরূপণ করুন।",
            inputs = listOf(
                InputFieldDef("current_age", "বর্তমান বয়স", "যেমন: ৩০", "বছর", defaultValue = "30"),
                InputFieldDef("retirement_age", "অবসরের বয়স", "যেমন: ৬০", "বছর", defaultValue = "60"),
                InputFieldDef("life_expectancy", "প্রত্যাশিত আয়ুষ্কাল", "যেমন: ৮০", "বছর", defaultValue = "80"),
                InputFieldDef("monthly_expense", "বর্তমান মাসিক পারিবারিক ব্যয়", "যেমন: ৩৫০০০", "৳", defaultValue = "35000"),
                InputFieldDef("inflation_rate", "প্রত্যাশিত বার্ষিক মূল্যস্ফীতি %", "যেমন: ৭.০০", "%", defaultValue = "7"),
                InputFieldDef("pre_ret_return", "অবসরপূর্ব বিনিয়োগ রিটার্ন (SIP) %", "যেমন: ১২.০০", "%", defaultValue = "12"),
                InputFieldDef("post_ret_return", "অবসরকালীন বার্ষিক রিটার্ন %", "যেমন: ৮.০০", "%", defaultValue = "8")
            ),
            calculate = { values, useBn ->
                val currentAge = values["current_age"] ?: 30.0
                val retAge = values["retirement_age"] ?: 60.0
                val lifeExp = values["life_expectancy"] ?: 80.0
                val expense = values["monthly_expense"] ?: 35000.0
                val inflation = values["inflation_rate"] ?: 7.0
                val preReturn = values["pre_ret_return"] ?: 12.0
                val postReturn = values["post_ret_return"] ?: 8.0

                val yearsToRetire = retAge - currentAge
                val retYears = lifeExp - retAge

                if (yearsToRetire <= 0.0 || retYears <= 0.0 || expense <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "প্রয়োজনীয় অবসর করপাস",
                        primaryValueBn = "০ ৳",
                        insightNoteBn = "বয়স এবং ব্যয়ের মান সঠিকভাবে প্রদান করুন (বর্তমান বয়স < অবসরের বয়স < আয়ুষ্কাল)।",
                        isWarning = true
                    )
                } else {
                    val infDec = inflation / 100.0
                    val monthlyExpAtRet = expense * (1.0 + infDec).pow(yearsToRetire)
                    val annualExpAtRet = monthlyExpAtRet * 12.0

                    val postDec = postReturn / 100.0
                    val realRate = ((1.0 + postDec) / (1.0 + infDec)) - 1.0

                    val corpus = if (kotlin.math.abs(realRate) < 0.0001) {
                        annualExpAtRet * retYears
                    } else {
                        annualExpAtRet * ((1.0 - (1.0 + realRate).pow(-retYears)) / realRate) * (1.0 + realRate)
                    }

                    val preDecMonth = preReturn / 100.0 / 12.0
                    val monthsToRetire = yearsToRetire * 12.0
                    val requiredSip = if (preDecMonth > 0.0) {
                        (corpus * preDecMonth) / (((1.0 + preDecMonth).pow(monthsToRetire) - 1.0) * (1.0 + preDecMonth))
                    } else {
                        corpus / monthsToRetire
                    }

                    CalcResult(
                        primaryLabelBn = "প্রয়োজনীয় অবসরকালীন তহবিল (Corpus)",
                        primaryValueBn = BengaliFormatter.formatCompactTaka(corpus, useBn),
                        subResults = listOf(
                            SubResultItem("প্রয়োজনীয় মাসিক বিনিয়োগ (SIP)", BengaliFormatter.formatTaka(requiredSip, useBn)),
                            SubResultItem("অবসরে ১ম বছরের মাসিক ব্যয়", BengaliFormatter.formatTaka(monthlyExpAtRet, useBn)),
                            SubResultItem("অবসরপূর্ব সঞ্চয়ের সময়", "${BengaliFormatter.formatNumber(yearsToRetire, 0, useBn)} বছর"),
                            SubResultItem("অবসরের সময়কাল", "${BengaliFormatter.formatNumber(retYears, 0, useBn)} বছর"),
                            SubResultItem("পূর্ণ করপাসের পরিমাণ", BengaliFormatter.formatTaka(corpus, useBn))
                        ),
                        insightNoteBn = "সময়মতো এসআইপি (SIP) শুরু করলে চক্রবৃদ্ধি মুনাফার মাধ্যমে প্রয়োজনীয় বিশাল করপাস সহজে অর্জন করা সম্ভব।"
                    )
                }
            }
        ),

        // === CATEGORY 4: ব্যাংকিং ক্যালকুলেটর (4 calculators) ===

        // 1. FD Calculator
        CalculatorDef(
            id = "fd_calc",
            category = CalculatorCategory.BANKING,
            titleBn = "FD Calculator (ফিক্সড ডিপোজিট / এফডি)",
            titleEn = "Fixed Deposit Calculator",
            formulaSummaryBn = "পরিপক্ক মূল্য = মূলধন × [১ + (হার ÷ ফ্রিকোয়েন্সি)]^(ফ্রিকোয়েন্সি × বছর)",
            descriptionBn = "ব্যাংক বা আর্থিক প্রতিষ্ঠানে ফিক্সড ডিপোজিটের মেয়াদপূর্তিতে প্রাপ্ত মোট অর্থ ও মুনাফা।",
            inputs = listOf(
                InputFieldDef("principal", "জমা মূলধন (Principal)", "যেমন: ৫০০০০০", "৳"),
                InputFieldDef("interest_rate", "বার্ষিক সুদের হার %", "যেমন: ৯.৫০", "%", defaultValue = "9"),
                InputFieldDef("years", "মেয়াদ (বছর)", "যেমন: ৩", "বছর", defaultValue = "3"),
                InputFieldDef("comp_freq", "চক্রবৃদ্ধির ফ্রিকোয়েন্সি বছরে", "যেমন: ৪ (ত্রৈমাসিক)", "বার", defaultValue = "4")
            ),
            calculate = { values, useBn ->
                val p = values["principal"] ?: 0.0
                val rate = values["interest_rate"] ?: 0.0
                val years = values["years"] ?: 0.0
                val freq = (values["comp_freq"] ?: 4.0).let { if (it <= 0) 4.0 else it }

                val maturity = p * (1.0 + (rate / 100.0) / freq).pow(freq * years)
                val interest = maturity - p

                CalcResult(
                    primaryLabelBn = "মেয়াদপূর্তি মূল্য (Maturity Value)",
                    primaryValueBn = BengaliFormatter.formatTaka(maturity, useBn),
                    subResults = listOf(
                        SubResultItem("মূল জমা", BengaliFormatter.formatTaka(p, useBn)),
                        SubResultItem("মোট সুদ", BengaliFormatter.formatTaka(interest, useBn)),
                        SubResultItem("চক্রবৃদ্ধি ফ্রিকোয়েন্সি", "বছরে ${BengaliFormatter.formatNumber(freq, 0, useBn)} বার")
                    ),
                    insightNoteBn = "ব্যাংক এফডি নিশ্চিত ও নিরাপদ আয়ের একটি প্রধান মাধ্যম।"
                )
            }
        ),

        // 2. RD / DPS Calculator
        CalculatorDef(
            id = "rd_calc",
            category = CalculatorCategory.BANKING,
            titleBn = "RD / DPS (ডিপিএস ক্যালকুলেটর)",
            titleEn = "Recurring Deposit / DPS Calculator",
            formulaSummaryBn = "পরিপক্ক মান = মাসিক জমা ও চক্রবৃদ্ধি সুদের সমষ্টি",
            descriptionBn = "প্রতি মাসে নির্দিষ্ট কিস্তির ডিপিএস জমা দিয়ে মেয়াদ শেষে মোট কত টাকা ফেরত পাওয়া যাবে।",
            inputs = listOf(
                InputFieldDef("monthly_dep", "প্রতি মাসের ডিপিএস কিস্তি", "যেমন: ৫০০০", "৳"),
                InputFieldDef("interest_rate", "বার্ষিক সুদের/মুনাফার হার %", "যেমন: ৮.৫০", "%", defaultValue = "8.5"),
                InputFieldDef("months", "মোট মেয়াদ (মাস)", "যেমন: ৬০ (৫ বছর)", "মাস", defaultValue = "60")
            ),
            calculate = { values, useBn ->
                val monthly = values["monthly_dep"] ?: 0.0
                val rate = values["interest_rate"] ?: 0.0
                val months = values["months"] ?: 0.0

                val r = rate / 12.0 / 100.0
                val maturity = if (r > 0.0) {
                    monthly * (((1.0 + r).pow(months) - 1.0) / r) * (1.0 + r)
                } else {
                    monthly * months
                }
                val totalDeposited = monthly * months
                val interestGained = maturity - totalDeposited

                CalcResult(
                    primaryLabelBn = "ডিপিএস মেয়াদপূর্তি মূল্য",
                    primaryValueBn = BengaliFormatter.formatTaka(maturity, useBn),
                    subResults = listOf(
                        SubResultItem("মূল জমা", BengaliFormatter.formatTaka(totalDeposited, useBn)),
                        SubResultItem("মোট সুদ", BengaliFormatter.formatTaka(interestGained, useBn)),
                        SubResultItem("মোট কিস্তির সময়কাল", "${BengaliFormatter.formatNumber(months, 0, useBn)} মাস")
                    ),
                    insightNoteBn = "ডিপিএস বা আরডি ছোট ছোট সঞ্চয় দিয়ে বড় পুঁজি গঠনের অন্যতম সেরা উপায়।"
                )
            }
        ),

        // 3. Loan EMI Calculator
        CalculatorDef(
            id = "loan_emi",
            category = CalculatorCategory.BANKING,
            titleBn = "Loan EMI (ঋণ কিস্তি ক্যালকুলেটর)",
            titleEn = "Loan EMI Calculator",
            formulaSummaryBn = "EMI = P × r × (১+r)^n ÷ [((১+r)^n) − ১]",
            descriptionBn = "ঋণের পরিমাণ (Loan Amount), বার্ষিক সুদের হার (Interest Rate), এবং মেয়াদ (Tenure) দিয়ে প্রতি মাসের কিস্তি (Monthly Installment / EMI) ও মোট প্রদেয় সুদ হিসাব করুন।",
            inputs = listOf(
                InputFieldDef("principal", "ঋণের মূল পরিমাণ (Loan Amount)", "যেমন: ১০০০০০০", "৳", defaultValue = "1000000"),
                InputFieldDef("interest_rate", "বার্ষিক সুদের হার % (Interest Rate)", "যেমন: ৯.৫০", "%", defaultValue = "9.5"),
                InputFieldDef("tenure_years", "ঋণের মেয়াদ - বছর (Tenure in Years)", "যেমন: ৩ বা ৫", "বছর", defaultValue = "3"),
                InputFieldDef("tenure_months", "অতিরিক্ত বা একক মেয়াদ - মাস (Optional Months)", "যেমন: ০ বা ৩৬", "মাস", defaultValue = "0", isRequired = false)
            ),
            calculate = { values, useBn ->
                val p = values["principal"] ?: 0.0
                val rate = values["interest_rate"] ?: 0.0
                val years = values["tenure_years"] ?: 0.0
                val months = values["tenure_months"] ?: 0.0
                val n = (years * 12.0) + months

                val r = rate / 12.0 / 100.0
                if (p <= 0.0 || n <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "মাসিক কিস্তি (EMI)",
                        primaryValueBn = "০ ৳",
                        insightNoteBn = "সঠিক ঋণের পরিমাণ ও মেয়াদ (বছর বা মাস) প্রদান করুন।",
                        isWarning = true
                    )
                } else {
                    val emi = if (r > 0.0) {
                        val comp = (1.0 + r).pow(n)
                        (p * r * comp) / (comp - 1.0)
                    } else {
                        p / n
                    }
                    val totalPayment = emi * n
                    val totalInterest = totalPayment - p
                    val interestPercentage = if (totalPayment > 0.0) (totalInterest / totalPayment) * 100.0 else 0.0

                    val tenureTextBn = when {
                        years > 0.0 && months > 0.0 ->
                            "${BengaliFormatter.formatNumber(years, 0, useBn)} বছর ${BengaliFormatter.formatNumber(months, 0, useBn)} মাস (মোট ${BengaliFormatter.formatNumber(n, 0, useBn)} মাস)"
                        years > 0.0 ->
                            "${BengaliFormatter.formatNumber(years, 0, useBn)} বছর (${BengaliFormatter.formatNumber(n, 0, useBn)} মাস)"
                        else ->
                            "${BengaliFormatter.formatNumber(n, 0, useBn)} মাস"
                    }

                    CalcResult(
                        primaryLabelBn = "মাসিক কিস্তি (EMI)",
                        primaryValueBn = BengaliFormatter.formatTaka(emi, useBn),
                        subResults = listOf(
                            SubResultItem("মোট পরিশোধ", BengaliFormatter.formatTaka(totalPayment, useBn)),
                            SubResultItem("মোট সুদ", BengaliFormatter.formatTaka(totalInterest, useBn)),
                            SubResultItem("আসল ঋণ (Principal)", BengaliFormatter.formatTaka(p, useBn)),
                            SubResultItem("মোট পরিশোধের সময়কাল", tenureTextBn),
                            SubResultItem("সুদের আনুপাতিক অংশ", BengaliFormatter.formatPercent(interestPercentage, useBn))
                        ),
                        insightNoteBn = "প্রতি মাসে ৳ ${BengaliFormatter.formatNumber(emi, 0, useBn)} কিস্তি পরিশোধে সর্বমোট ৳ ${BengaliFormatter.formatNumber(totalInterest, 0, useBn)} সুদ হবে। মেয়াদ কমিয়ে আনলে মোট সুদের খরচ উল্লেখযোগ্যভাবে সাশ্রয় হয়।"
                    )
                }
            }
        ),

        // 4. Interest Rate to ROI (Effective Annual Rate)
        CalculatorDef(
            id = "interest_to_roi",
            category = CalculatorCategory.BANKING,
            titleBn = "Interest to ROI (কার্যকরী সুদের হার)",
            titleEn = "Effective Annual Rate (EAR)",
            formulaSummaryBn = "কার্যকরী বার্ষিক হার = [(১ + নামমাত্র হার ÷ ফ্রিকোয়েন্সি)^ফ্রিকোয়েন্সি − ১] × ১০০",
            descriptionBn = "বছরে একাধিকবার চক্রবৃদ্ধি হলে বার্ষিক নামমাত্র সুদের হারের প্রকৃত কার্যকরী হার (EAR)।",
            inputs = listOf(
                InputFieldDef("nominal_rate", "নামমাত্র বার্ষিক সুদের হার %", "যেমন: ১০.০০", "%"),
                InputFieldDef("comp_freq", "বছরে চক্রবৃদ্ধির সংখ্যা", "যেমন: ১২ (মাসিক) বা ৪ (ত্রৈমাসিক)", "বার", defaultValue = "12")
            ),
            calculate = { values, useBn ->
                val nominal = values["nominal_rate"] ?: 0.0
                val freq = (values["comp_freq"] ?: 12.0).let { if (it <= 0) 12.0 else it }

                val ear = ((1.0 + (nominal / freq / 100.0)).pow(freq) - 1.0) * 100.0

                CalcResult(
                    primaryLabelBn = "কার্যকরী বার্ষিক সুদের হার (EAR)",
                    primaryValueBn = BengaliFormatter.formatPercent(ear, useBn),
                    subResults = listOf(
                        SubResultItem("নামমাত্র বা ঘোষিত সুদের হার", BengaliFormatter.formatPercent(nominal, useBn)),
                        SubResultItem("চক্রবৃদ্ধির মাত্রা", "বছরে ${BengaliFormatter.formatNumber(freq, 0, useBn)} বার")
                    ),
                    insightNoteBn = "চক্রবৃদ্ধি যত ঘন ঘন হবে, প্রকৃত কার্যকরী সুদের হার নামমাত্র হারের চেয়ে তত বৃদ্ধি পাবে।"
                )
            }
        ),

        // === CATEGORY 5: অন্যান্য ক্যালকুলেটর (1 calculator) ===

        // 1. GST/VAT Calculator
        CalculatorDef(
            id = "gst_vat",
            category = CalculatorCategory.OTHERS,
            titleBn = "GST/VAT (ভ্যাট ও ট্যাক্স ক্যালকুলেটর)",
            titleEn = "GST / VAT Calculator",
            formulaSummaryBn = "ভ্যাট এর পরিমাণ = ভিত্তি মূল্য × (ভ্যাট হার ÷ ১০০)",
            descriptionBn = "পণ্যের মূল দামের উপর প্রযোজ্য ভ্যাট (যেমন বাংলাদেশে ১৫% স্ট্যান্ডার্ড ভ্যাট) ও মোট বিল হিসাব।",
            inputs = listOf(
                InputFieldDef("base_amount", "ভিত্তি মূল্য / পণ্যের প্রকৃত দর", "যেমন: ৫০০০", "৳"),
                InputFieldDef("vat_rate", "ভ্যাট বা ট্যাক্স এর হার %", "যেমন: ১৫.০০", "%", defaultValue = "15")
            ),
            calculate = { values, useBn ->
                val base = values["base_amount"] ?: 0.0
                val rate = values["vat_rate"] ?: 15.0

                val taxAmount = base * (rate / 100.0)
                val totalAmount = base + taxAmount

                CalcResult(
                    primaryLabelBn = "সর্বমোট দাম",
                    primaryValueBn = BengaliFormatter.formatTaka(totalAmount, useBn),
                    subResults = listOf(
                        SubResultItem("GST/VAT পরিমাণ", BengaliFormatter.formatTaka(taxAmount, useBn)),
                        SubResultItem("ভ্যাট পূর্ববর্তী আসল মূল্য", BengaliFormatter.formatTaka(base, useBn)),
                        SubResultItem("প্রযুক্ত ভ্যাট হার", BengaliFormatter.formatPercent(rate, useBn))
                    ),
                    insightNoteBn = "বাংলাদেশে সাধারণ বাণিজ্যিক পণ্যে ১৫% আদর্শ মূসক (ভ্যাট) প্রযোজ্য।"
                )
            }
        )
    )
}
