package com.example.model

import com.example.util.BengaliFormatter
import kotlin.math.floor

object StockAverageCalculators {
    val list: List<CalculatorDef> = listOf(
        // 1. Stock P/L Calculator
        CalculatorDef(
            id = "stock_pl",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Stock P/L (শেয়ার লাভ/ক্ষতি)",
            titleEn = "Stock Profit & Loss",
            formulaSummaryBn = "নিট লাভ = (বিক্রয় মূল্য − ক্রয় মূল্য) × সংখ্যা − মোট কমিশন",
            descriptionBn = "ব্রোকারেজ কমিশন সহ শেয়ারে আপনার প্রকৃত নিট লাভ বা লোকসানের হিসাব।",
            inputs = listOf(
                InputFieldDef("buy_price", "ক্রয় মূল্য (Buy Price)", "যেমন: ১২০.০০", "৳"),
                InputFieldDef("sell_price", "বিক্রয় মূল্য (Sell Price)", "যেমন: ১৪২.৫০", "৳"),
                InputFieldDef("quantity", "শেয়ার সংখ্যা (Quantity)", "যেমন: ৫০০", "টি"),
                InputFieldDef("commission", "কমিশন % (উভয় পাশ)", "যেমন: ০.৪০", "%", defaultValue = "0.40")
            ),
            calculate = { values, useBn ->
                val buyPrice = values["buy_price"] ?: 0.0
                val sellPrice = values["sell_price"] ?: 0.0
                val qty = values["quantity"] ?: 0.0
                val commRate = (values["commission"] ?: 0.40) / 100.0

                val buyValue = buyPrice * qty
                val buyComm = buyValue * commRate
                val totalBuyCost = buyValue + buyComm

                val sellValue = sellPrice * qty
                val sellComm = sellValue * commRate
                val totalSellNet = sellValue - sellComm

                val netPL = totalSellNet - totalBuyCost
                val netPLPercent = if (totalBuyCost > 0) (netPL / totalBuyCost) * 100.0 else 0.0
                val isLoss = netPL < 0.0

                val note = if (isLoss) {
                    "সতর্কতা: এই লেনদেনে আপনার লোকসান হয়েছে। ক্ষতির কারণ ও বাজার প্রবণতা পর্যালোচনা করুন।"
                } else {
                    "অভিনন্দন! এই ট্রেডে আপনি ব্রোকার কমিশন বাদে সফলভাবে নিট লাভ করেছেন।"
                }

                CalcResult(
                    primaryLabelBn = if (isLoss) "নিট লোকসান" else "নিট লাভ",
                    primaryValueBn = BengaliFormatter.formatTaka(netPL, useBn),
                    subResults = listOf(
                        SubResultItem("লাভ/ক্ষতির হার", BengaliFormatter.formatPercent(netPLPercent, useBn)),
                        SubResultItem("মোট বিনিয়োগ খরচ", BengaliFormatter.formatTaka(totalBuyCost, useBn)),
                        SubResultItem("মোট বিক্রয় প্রাপ্তি", BengaliFormatter.formatTaka(totalSellNet, useBn)),
                        SubResultItem("মোট ব্রোকার কমিশন", BengaliFormatter.formatTaka(buyComm + sellComm, useBn))
                    ),
                    insightNoteBn = note,
                    isWarning = isLoss,
                    formulaDisplayBn = "P/L = ($sellPrice − $buyPrice) × $qty − কমিশন"
                )
            }
        ),

        // 2. Stock Avg Calculator
        CalculatorDef(
            id = "stock_avg",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Stock Avg (শেয়ার গড় ক্রয়মূল্য)",
            titleEn = "Stock Average Calculator",
            formulaSummaryBn = "নতুন গড় = (পূর্বের খরচ + নতুন খরচ) ÷ মোট শেয়ার সংখ্যা",
            descriptionBn = "নতুন শেয়ার কেনার পর পোর্টফোলিওতে শেয়ারের নতুন গড় ক্রয়মূল্য কত হবে তা বের করুন।",
            inputs = listOf(
                InputFieldDef("old_qty", "পূর্বের শেয়ার সংখ্যা", "যেমন: ২০০", "টি"),
                InputFieldDef("old_price", "পূর্বের গড় ক্রয়দর", "যেমন: ৮০.০০", "৳"),
                InputFieldDef("new_qty", "নতুন শেয়ার সংখ্যা", "যেমন: ৩০০", "টি"),
                InputFieldDef("new_price", "নতুন ক্রয়দর", "যেমন: ৬৫.০০", "৳"),
                InputFieldDef("commission", "নতুন কেনায় কমিশন %", "০.৪০", "%", defaultValue = "0.40")
            ),
            calculate = { values, useBn ->
                val oldQty = values["old_qty"] ?: 0.0
                val oldPrice = values["old_price"] ?: 0.0
                val newQty = values["new_qty"] ?: 0.0
                val newPrice = values["new_price"] ?: 0.0
                val comm = (values["commission"] ?: 0.40) / 100.0

                val totalQty = oldQty + newQty
                if (totalQty <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "নতুন গড় মূল্য",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "শেয়ার সংখ্যা শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val oldTotalCost = oldQty * oldPrice
                    val newCostWithComm = newQty * newPrice * (1.0 + comm)
                    val newAvgPrice = (oldTotalCost + newCostWithComm) / totalQty

                    val isAvgDown = newAvgPrice < oldPrice
                    val note = if (isAvgDown) {
                        "নতুন এভারেজে আপনার গড় ক্রয়মূল্য ৳${BengaliFormatter.formatNumber(oldPrice - newAvgPrice, 2, useBn)} কমেছে (Average Down)।"
                    } else {
                        "নতুন ক্রয়ের ফলে আপনার পোর্টফোলিওতে শেয়ারের গড় মূল্য বৃদ্ধি পেয়েছে (Average Up)।"
                    }

                    CalcResult(
                        primaryLabelBn = "নতুন গড় ক্রয়দর",
                        primaryValueBn = BengaliFormatter.formatTaka(newAvgPrice, useBn),
                        subResults = listOf(
                            SubResultItem("মোট নতুন শেয়ার", BengaliFormatter.formatNumber(totalQty, 0, useBn) + " টি"),
                            SubResultItem("সর্বমোট বিনিয়োগ", BengaliFormatter.formatTaka(oldTotalCost + newCostWithComm, useBn)),
                            SubResultItem("নতুন ক্রয়ের খরচ", BengaliFormatter.formatTaka(newCostWithComm, useBn))
                        ),
                        insightNoteBn = note,
                        isWarning = false
                    )
                }
            }
        ),

        // 3. Loss Recovery Calculator
        CalculatorDef(
            id = "loss_recovery",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Loss Recovery (লোকসান রিকভারি)",
            titleEn = "Loss Recovery Calculator",
            formulaSummaryBn = "প্রয়োজনীয় লাভ % = লোকসান % ÷ (১০০ − লোকসান %) × ১০০",
            descriptionBn = "শেয়ারে লোকসান হলে মূল টাকা ফেরত পেতে কত শতাংশ লাভের বৃদ্ধি প্রয়োজন তা হিসাব করুন।",
            inputs = listOf(
                InputFieldDef("buy_price", "ক্রয় মূল্য (Buy Price)", "যেমন: ১০০.০০", "৳"),
                InputFieldDef("current_price", "বর্তমান বাজার মূল্য", "যেমন: ৮০.০০", "৳")
            ),
            calculate = { values, useBn ->
                val buyPrice = values["buy_price"] ?: 0.0
                val currentPrice = values["current_price"] ?: 0.0

                if (buyPrice <= 0.0 || currentPrice <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "রিকভারি রিটার্ন",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "দাম শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else if (currentPrice >= buyPrice) {
                    val gain = ((currentPrice - buyPrice) / buyPrice) * 100.0
                    CalcResult(
                        primaryLabelBn = "বর্তমান অবস্থা",
                        primaryValueBn = "লাভজনক (${BengaliFormatter.formatPercent(gain, useBn)})",
                        insightNoteBn = "বর্তমান মূল্য ক্রয়মূল্যের সমান বা বেশি। কোনো ক্ষতি নেই!",
                        isWarning = false
                    )
                } else {
                    val lossPercent = ((buyPrice - currentPrice) / buyPrice) * 100.0
                    val recoveryPercent = (lossPercent / (100.0 - lossPercent)) * 100.0

                    val note = "মনোযোগ দিন: ২০% ক্ষতির পর মূল পুঁজি ফিরে পেতে ২৫% বৃদ্ধি দরকার; ৫০% ক্ষতি হলে ১০০% লাভ প্রয়োজন!"

                    CalcResult(
                        primaryLabelBn = "প্রয়োজনীয় রিকভারি লাভ",
                        primaryValueBn = BengaliFormatter.formatPercent(recoveryPercent, useBn),
                        subResults = listOf(
                            SubResultItem("বর্তমান ক্ষতি (Loss)", BengaliFormatter.formatPercent(lossPercent, useBn)),
                            SubResultItem("শেয়ার প্রতি ক্ষতি", BengaliFormatter.formatTaka(buyPrice - currentPrice, useBn))
                        ),
                        insightNoteBn = note,
                        isWarning = true
                    )
                }
            }
        ),

        // 4. Risk-Reward Calculator
        CalculatorDef(
            id = "risk_reward",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Risk-Reward (রিস্ক-রিওয়ার্ড অনুপাত)",
            titleEn = "Risk to Reward Ratio",
            formulaSummaryBn = "অনুপাত = রিওয়ার্ড (টার্গেট − এন্ট্রি) ÷ রিস্ক (এন্ট্রি − স্টপ-লস)",
            descriptionBn = "ট্রেডে সম্ভাব্য ক্ষতির বিপরীতে সম্ভাব্য লাভের অনুপাত নির্ধারণ করে ঝুঁকি নিয়ন্ত্রণ করুন।",
            inputs = listOf(
                InputFieldDef("entry_price", "এন্ট্রি প্রাইস (ক্রয়দর)", "যেমন: ১৫০.০০", "৳"),
                InputFieldDef("stoploss_price", "স্টপ-লস প্রাইস (Stop-Loss)", "যেমন: ১৪২.০০", "৳"),
                InputFieldDef("target_price", "টার্গেট প্রাইস (Target)", "যেমন: ১৭০.০০", "৳")
            ),
            calculate = { values, useBn ->
                val entry = values["entry_price"] ?: 0.0
                val sl = values["stoploss_price"] ?: 0.0
                val target = values["target_price"] ?: 0.0

                val risk = entry - sl
                val reward = target - entry

                if (risk <= 0.0 || reward <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "রিস্ক-রিওয়ার্ড",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "স্টপ-লস ক্রয়মূল্যের নিচে এবং টার্গেট মূল্য ক্রয়মূল্যের উপরে হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val ratio = reward / risk
                    val isGood = ratio >= 2.0
                    val note = if (isGood) {
                        "চমৎকার ট্রেড সেটআপ! রিস্ক-রিওয়ার্ড অনুপাত ১:${BengaliFormatter.formatNumber(ratio, 2, useBn)}, যা ন্যূনতম ১:২ মানদণ্ড পূরণ করেছে।"
                    } else {
                        "সতর্কতা: রিস্ক-রিওয়ার্ড অনুপাত ১:${BengaliFormatter.formatNumber(ratio, 2, useBn)}। পেশাদার ট্রেডিংয়ে ১:২ এর কম রেশিও এড়ানো ভালো।"
                    }

                    CalcResult(
                        primaryLabelBn = "রিস্ক-রিওয়ার্ড অনুপাত",
                        primaryValueBn = "১ : ${BengaliFormatter.formatNumber(ratio, 2, useBn)}",
                        subResults = listOf(
                            SubResultItem("সম্ভাব্য ঝুঁকি (রিস্ক)", BengaliFormatter.formatTaka(risk, useBn)),
                            SubResultItem("সম্ভাব্য প্রাপ্তি (রিওয়ার্ড)", BengaliFormatter.formatTaka(reward, useBn))
                        ),
                        insightNoteBn = note,
                        isWarning = !isGood
                    )
                }
            }
        ),

        // 5. Multi Stock Averaging
        CalculatorDef(
            id = "multi_avg",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Multi Stock Avg (৩ লট এভারেজিং)",
            titleEn = "Multi-Lot Stock Averaging",
            formulaSummaryBn = "ভারযুক্ত গড় = মোট বিনিয়োগ ÷ মোট শেয়ার সংখ্যা",
            descriptionBn = "৩টি ভিন্ন ভিন্ন লটে কেনা শেয়ারের সামগ্রিক একক গড় ক্রয়মূল্য হিসাব করুন।",
            inputs = listOf(
                InputFieldDef("qty1", "১ম লট শেয়ার সংখ্যা", "যেমন: ১০০", "টি"),
                InputFieldDef("price1", "১ম লট ক্রয়দর", "যেমন: ৫০.০০", "৳"),
                InputFieldDef("qty2", "২য় লট শেয়ার সংখ্যা", "যেমন: ১৫০", "টি"),
                InputFieldDef("price2", "২য় লট ক্রয়দর", "যেমন: ৪২.০০", "৳"),
                InputFieldDef("qty3", "৩য় লট শেয়ার সংখ্যা (ঐচ্ছিক)", "০", "টি", defaultValue = "0"),
                InputFieldDef("price3", "৩য় লট ক্রয়দর (ঐচ্ছিক)", "০", "৳", defaultValue = "0")
            ),
            calculate = { values, useBn ->
                val q1 = values["qty1"] ?: 0.0
                val p1 = values["price1"] ?: 0.0
                val q2 = values["qty2"] ?: 0.0
                val p2 = values["price2"] ?: 0.0
                val q3 = values["qty3"] ?: 0.0
                val p3 = values["price3"] ?: 0.0

                val totalQty = q1 + q2 + q3
                val totalCost = (q1 * p1) + (q2 * p2) + (q3 * p3)

                if (totalQty <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "গড় মূল্য",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "কমপক্ষে একটি লটের শেয়ার সংখ্যা ও দাম প্রদান করুন।",
                        isWarning = true
                    )
                } else {
                    val weightedAvg = totalCost / totalQty
                    CalcResult(
                        primaryLabelBn = "ভারযুক্ত গড় ক্রয়দর",
                        primaryValueBn = BengaliFormatter.formatTaka(weightedAvg, useBn),
                        subResults = listOf(
                            SubResultItem("সর্বমোট শেয়ার", BengaliFormatter.formatNumber(totalQty, 0, useBn) + " টি"),
                            SubResultItem("সর্বমোট বিনিয়োগ", BengaliFormatter.formatTaka(totalCost, useBn))
                        ),
                        insightNoteBn = "এভারেজিং এর মাধ্যমে আপনার সামগ্রিক ক্রয়মূল্য একটি একক ভারসাম্যপূর্ণ স্তরে চলে আসে।"
                    )
                }
            }
        ),

        // 6. Selling Target Price
        CalculatorDef(
            id = "target_sell_price",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Target Price (বিক্রয় টার্গেট মূল্য)",
            titleEn = "Selling Target Price Calculator",
            formulaSummaryBn = "টার্গেট দর = ক্রয়দর × [১ + (কাঙ্ক্ষিত লাভ% + কমিশন%) ÷ ১০০]",
            descriptionBn = "নির্দিষ্ট শতাংশ নিট মুনাফা অর্জনের জন্য শেয়ারটি কত দামে বিক্রি করতে হবে তা জানুন।",
            inputs = listOf(
                InputFieldDef("buy_price", "ক্রয় মূল্য (Buy Price)", "যেমন: ১২০.০০", "৳"),
                InputFieldDef("desired_profit", "কাঙ্ক্ষিত নিট লাভ %", "যেমন: ১৫.০০", "%"),
                InputFieldDef("commission", "কমিশন %", "০.৪০", "%", defaultValue = "0.40")
            ),
            calculate = { values, useBn ->
                val buyPrice = values["buy_price"] ?: 0.0
                val desiredProfit = values["desired_profit"] ?: 0.0
                val comm = values["commission"] ?: 0.40

                val targetPrice = buyPrice * (1.0 + (desiredProfit + comm) / 100.0)
                val perShareProfit = targetPrice - buyPrice

                CalcResult(
                    primaryLabelBn = "টার্গেট বিক্রয় মূল্য",
                    primaryValueBn = BengaliFormatter.formatTaka(targetPrice, useBn),
                    subResults = listOf(
                        SubResultItem("শেয়ার প্রতি প্রত্যাশিত লাভ", BengaliFormatter.formatTaka(perShareProfit, useBn)),
                        SubResultItem("প্রত্যাশিত লাভের হার", BengaliFormatter.formatPercent(desiredProfit, useBn)),
                        SubResultItem("সমন্বিত কমিশন হার", BengaliFormatter.formatPercent(comm, useBn))
                    ),
                    insightNoteBn = "এই দামে শেয়ার বিক্রি করলে ব্রোকার কমিশন পরিশোধের পর আপনার কাঙ্ক্ষিত নিট লাভ থাকবে।"
                )
            }
        ),

        // 7. Quantity Calculator
        CalculatorDef(
            id = "quantity_calc",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Quantity Calc (বাজেটে শেয়ার সংখ্যা)",
            titleEn = "Quantity Calculator",
            formulaSummaryBn = "সংখ্যা = বাজেট ÷ [শেয়ার দর × (১ + কমিশন%)]",
            descriptionBn = "নির্দিষ্ট অর্থ বা বাজেট দিয়ে কমিশন সমন্বয় করে সর্বোচ্চ কতটি শেয়ার কেনা যাবে।",
            inputs = listOf(
                InputFieldDef("budget", "মোট বিনিয়োগ বাজেট", "যেমন: ৫০০০০.০০", "৳"),
                InputFieldDef("price", "শেয়ার প্রতি বাজার দর", "যেমন: ১২৫.০০", "৳"),
                InputFieldDef("commission", "কমিশন %", "০.৪০", "%", defaultValue = "0.40")
            ),
            calculate = { values, useBn ->
                val budget = values["budget"] ?: 0.0
                val price = values["price"] ?: 0.0
                val comm = (values["commission"] ?: 0.40) / 100.0

                val costPerShare = price * (1.0 + comm)
                if (costPerShare <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "শেয়ার সংখ্যা",
                        primaryValueBn = "০ টি",
                        insightNoteBn = "শেয়ার দর শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val qty = floor(budget / costPerShare)
                    val totalCost = qty * costPerShare
                    val remaining = budget - totalCost

                    CalcResult(
                        primaryLabelBn = "ক্রয়যোগ্য শেয়ার সংখ্যা",
                        primaryValueBn = "${BengaliFormatter.formatNumber(qty, 0, useBn)} টি",
                        subResults = listOf(
                            SubResultItem("মোট প্রয়োজনীয় খরচ", BengaliFormatter.formatTaka(totalCost, useBn)),
                            SubResultItem("অবশিষ্ট ব্যালেন্স", BengaliFormatter.formatTaka(remaining, useBn)),
                            SubResultItem("প্রতি শেয়ারে কমিশন সহ খরচ", BengaliFormatter.formatTaka(costPerShare, useBn))
                        ),
                        insightNoteBn = "কমিশন হিসাব করে আপনার বাজেট সর্বোচ্চ কার্যকরভাবে শেয়ার সংখ্যায় রূপান্তর করা হয়েছে।"
                    )
                }
            }
        ),

        // 8. Break-even Calculator
        CalculatorDef(
            id = "breakeven_price",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Break-even (ব্রেক-ইভেন দর)",
            titleEn = "Break-Even Price Calculator",
            formulaSummaryBn = "ব্রেক-ইভেন দর = ক্রয়দর × (১ + মোট কমিশন%)",
            descriptionBn = "লাভও না ক্ষতিও না (নো প্রফিট-নো লস) এমন ন্যূনতম বিক্রয় দর নির্ধারণ করুন।",
            inputs = listOf(
                InputFieldDef("buy_price", "ক্রয় মূল্য (Buy Price)", "যেমন: ৮৫.০০", "৳"),
                InputFieldDef("commission", "মোট কমিশন % (কেনা ও বেচা)", "যেমন: ০.৮০", "%", defaultValue = "0.80")
            ),
            calculate = { values, useBn ->
                val buyPrice = values["buy_price"] ?: 0.0
                val comm = (values["commission"] ?: 0.80) / 100.0

                val breakEven = buyPrice * (1.0 + comm)
                val commAmount = breakEven - buyPrice

                CalcResult(
                    primaryLabelBn = "ব্রেক-ইভেন বিক্রয় দর",
                    primaryValueBn = BengaliFormatter.formatTaka(breakEven, useBn),
                    subResults = listOf(
                        SubResultItem("ক্রয় মূল্য", BengaliFormatter.formatTaka(buyPrice, useBn)),
                        SubResultItem("মোট কমিশন সমন্বয়", BengaliFormatter.formatTaka(commAmount, useBn))
                    ),
                    insightNoteBn = "এই দরে বিক্রি করলে আপনার কোনো আর্থিক ক্ষতি হবে না (কমিশন সম্পূর্ণ সমন্বিত)।"
                )
            }
        ),

        // 9. Average Exit Price
        CalculatorDef(
            id = "avg_exit_price",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Avg Exit Price (গড় বিক্রয় মূল্য)",
            titleEn = "Average Exit Price",
            formulaSummaryBn = "গড় বিক্রয় দর = মোট বিক্রয়লব্ধ অর্থ ÷ মোট বিক্রিত শেয়ার",
            descriptionBn = "ভিন্ন ভিন্ন ধাপে (২টি লট) শেয়ার বিক্রি করলে গড় প্রস্থান মূল্য বের করুন।",
            inputs = listOf(
                InputFieldDef("qty1", "১ম লট বিক্রিত শেয়ার", "যেমন: ২০০", "টি"),
                InputFieldDef("price1", "১ম লট বিক্রয় দর", "যেমন: ১৩৫.০০", "৳"),
                InputFieldDef("qty2", "২য় লট বিক্রিত শেয়ার", "যেমন: ৩০০", "টি"),
                InputFieldDef("price2", "২য় লট বিক্রয় দর", "যেমন: ১৪৮.০০", "৳")
            ),
            calculate = { values, useBn ->
                val q1 = values["qty1"] ?: 0.0
                val p1 = values["price1"] ?: 0.0
                val q2 = values["qty2"] ?: 0.0
                val p2 = values["price2"] ?: 0.0

                val totalQty = q1 + q2
                val totalRevenue = (q1 * p1) + (q2 * p2)

                if (totalQty <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "গড় বিক্রয় দর",
                        primaryValueBn = "অপ্রযোজ্য",
                        insightNoteBn = "বিক্রিত শেয়ার সংখ্যা শূন্যের বেশি হতে হবে।",
                        isWarning = true
                    )
                } else {
                    val avgExit = totalRevenue / totalQty
                    CalcResult(
                        primaryLabelBn = "গড় প্রস্থান (Exit) দর",
                        primaryValueBn = BengaliFormatter.formatTaka(avgExit, useBn),
                        subResults = listOf(
                            SubResultItem("মোট বিক্রিত শেয়ার", BengaliFormatter.formatNumber(totalQty, 0, useBn) + " টি"),
                            SubResultItem("মোট বিক্রয় মূল্য", BengaliFormatter.formatTaka(totalRevenue, useBn))
                        ),
                        insightNoteBn = "একাধিক ধাপে মুনাফা উত্তোলনের ক্ষেত্রে ভারযুক্ত গড় বিক্রয় মূল্য অত্যন্ত কার্যকর।"
                    )
                }
            }
        ),

        // 10. Buy Stock
        CalculatorDef(
            id = "buy_stock",
            category = CalculatorCategory.STOCK_AVG_PL,
            titleBn = "Buy Stock (শেয়ার ক্রয় প্ল্যানার)",
            titleEn = "Buy Stock Planner",
            formulaSummaryBn = "সর্বোচ্চ শেয়ার = ফ্লোর[বাজেট ÷ (দর × ১.০১)]",
            descriptionBn = "আপনার নির্দিষ্ট ক্যাপিটাল ও বর্তমান দরে কমিশন বাদে কত টাকার শেয়ার কেনা সম্ভব।",
            inputs = listOf(
                InputFieldDef("budget", "উপলব্ধ ক্যাপিটাল / ফান্ড", "যেমন: ১০০০০০০", "৳"),
                InputFieldDef("price", "শেয়ার প্রতি দর", "যেমন: ২২০.০০", "৳"),
                InputFieldDef("commission", "কমিশন %", "০.৪০", "%", defaultValue = "0.40")
            ),
            calculate = { values, useBn ->
                val budget = values["budget"] ?: 0.0
                val price = values["price"] ?: 0.0
                val comm = (values["commission"] ?: 0.40) / 100.0

                val costPerShare = price * (1.0 + comm)
                if (costPerShare <= 0.0) {
                    CalcResult(
                        primaryLabelBn = "সর্বোচ্চ শেয়ার",
                        primaryValueBn = "০ টি",
                        insightNoteBn = "সঠিক দর প্রদান করুন।",
                        isWarning = true
                    )
                } else {
                    val maxQty = floor(budget / costPerShare)
                    val totalInvestment = maxQty * costPerShare
                    val rawValue = maxQty * price
                    val commCharge = totalInvestment - rawValue

                    CalcResult(
                        primaryLabelBn = "কেনা যাবে এমন শেয়ার",
                        primaryValueBn = "${BengaliFormatter.formatNumber(maxQty, 0, useBn)} টি",
                        subResults = listOf(
                            SubResultItem("মোট প্রয়োজনীয় তহবিল", BengaliFormatter.formatTaka(totalInvestment, useBn)),
                            SubResultItem("শেয়ারের আসল মূল্য", BengaliFormatter.formatTaka(rawValue, useBn)),
                            SubResultItem("সম্ভাব্য কমিশন খরচ", BengaliFormatter.formatTaka(commCharge, useBn))
                        ),
                        insightNoteBn = "এই ক্রয় পরিকল্পনা আপনার ক্যাপিটালের সুরক্ষা ও কমিশন সীমাবদ্ধতা মেনে প্রস্তুত।"
                    )
                }
            }
        )
    )
}
