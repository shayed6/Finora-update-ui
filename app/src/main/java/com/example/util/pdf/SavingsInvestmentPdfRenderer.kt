package com.example.util.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.ui.screens.portfolio.PortfolioSummary
import com.example.util.BengaliFormatter
import com.example.util.pdf.PdfCommon.CONTENT_WIDTH
import com.example.util.pdf.PdfCommon.ColorBlueBg
import com.example.util.pdf.PdfCommon.ColorBluePrimary
import com.example.util.pdf.PdfCommon.ColorDarkText
import com.example.util.pdf.PdfCommon.ColorGrayBg
import com.example.util.pdf.PdfCommon.ColorGrayBorder
import com.example.util.pdf.PdfCommon.ColorGrayText
import com.example.util.pdf.PdfCommon.ColorGreenBg
import com.example.util.pdf.PdfCommon.ColorGreenIncome
import com.example.util.pdf.PdfCommon.ColorNavyDark
import com.example.util.pdf.PdfCommon.ColorRedBg
import com.example.util.pdf.PdfCommon.ColorRedExpense
import com.example.util.pdf.PdfCommon.MARGIN
import com.example.util.pdf.PdfCommon.PAGE_HEIGHT
import com.example.util.pdf.PdfCommon.PAGE_WIDTH
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SavingsInvestmentPdfRenderer {

    fun generate(
        context: Context,
        goals: List<SavingsGoalEntity>,
        entries: List<SavingsEntryEntity> = emptyList(),
        holdings: List<HoldingEntity> = emptyList(),
        portfolioSummary: PortfolioSummary? = null,
        useBengaliDigits: Boolean = true
    ): File {
        val fonts = PdfCommon.loadFonts(context)
        val logoBitmap = PdfCommon.loadLogo(context)

        // Calculate Savings Metrics
        val entriesByGoal = entries.groupBy { it.goalId }
        val goalBalances = goals.associate { goal ->
            val goalEntries = entriesByGoal[goal.id] ?: emptyList()
            val savedPaisa = goalEntries.sumOf {
                if (it.type == "DEPOSIT") it.amount else -it.amount
            }
            goal.id to savedPaisa.coerceAtLeast(0L)
        }

        val totalSavingsBalance = goalBalances.values.sum()
        val totalSavingsTarget = goals.sumOf { it.targetAmount ?: 0L }

        // Calculate Investment Portfolio Metrics
        val totalInvestedCostPaisa = (portfolioSummary?.totalInvested?.times(100))?.toLong()
            ?: (holdings.sumOf { it.quantity * it.averagePrice } * 100).toLong()
        val totalMarketValuePaisa = (portfolioSummary?.currentTotalValue?.times(100))?.toLong()
            ?: (holdings.sumOf { it.quantity * it.currentPrice } * 100).toLong()
        val unrealizedProfitLossPaisa = totalMarketValuePaisa - totalInvestedCostPaisa
        val returnPercentage = if (totalInvestedCostPaisa > 0) {
            (unrealizedProfitLossPaisa.toDouble() / totalInvestedCostPaisa.toDouble()) * 100.0
        } else 0.0

        val pdfDocument = PdfDocument()

        // Page 1: Overview and Savings Goals
        // Page 2: Stock Portfolio / Investment Assets (if holdings exist) or combined
        val totalPages = if (holdings.isNotEmpty()) 2 else 1

        val orgInfo = com.example.data.preferences.OrganizationInfoRepository.getInstance(context).getOrganizationInfoBlocking()

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            PdfCommon.drawWatermark(canvas, logoBitmap)

            val currentDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val dateStr = if (useBengaliDigits) BengaliFormatter.formatBengaliDate(System.currentTimeMillis()) else currentDateStr

            if (pageIndex == 0) {
                // Page 1: Savings Section
                var y = PdfCommon.drawHeader(
                    canvas = canvas,
                    logoBitmap = logoBitmap,
                    fonts = fonts,
                    title = "সঞ্চয় ও আর্থিক লক্ষ্য রিপোর্ট (Savings Goals Statement)",
                    subTitle = "লক্ষ্যভিত্তিক নগদ জমার হিসাব • ১০০% অন-ডিভাইস নিরাপদ",
                    dateStr = dateStr,
                    currentPage = 1,
                    totalPages = totalPages,
                    orgInfo = orgInfo
                )

                // Summary Cards for Savings
                y = drawSavingsSummaryCards(canvas, y, totalSavingsBalance, totalSavingsTarget, goals.size, fonts, useBengaliDigits)

                // Savings Goals Table
                y = drawGoalsTable(canvas, y, goals, goalBalances, fonts, useBengaliDigits)

                PdfCommon.drawFooter(canvas, fonts, 1, totalPages, false)
            } else {
                // Page 2: Investment Portfolio Section
                var y = PdfCommon.drawHeader(
                    canvas = canvas,
                    logoBitmap = logoBitmap,
                    fonts = fonts,
                    title = "শেয়ার বিনিয়োগ ও সম্পদ রিপোর্ট (Investment Portfolio)",
                    subTitle = "পুঁজিবাজার বিনিয়োগ ও সমাপনী বাজারমূল্য • পৃথক সম্পদ খতিয়ান",
                    dateStr = dateStr,
                    currentPage = 2,
                    totalPages = totalPages,
                    orgInfo = orgInfo
                )

                // Investment Summary Cards
                y = drawInvestmentSummaryCards(
                    canvas = canvas,
                    startY = y,
                    totalCost = totalInvestedCostPaisa,
                    marketValue = totalMarketValuePaisa,
                    profit = unrealizedProfitLossPaisa,
                    returnPct = returnPercentage,
                    fonts = fonts,
                    useBengaliDigits = useBengaliDigits
                )

                // Holdings Table
                y = drawHoldingsTable(canvas, y, holdings, fonts, useBengaliDigits)

                PdfCommon.drawFooter(canvas, fonts, 2, totalPages, false)
            }

            pdfDocument.finishPage(page)
        }

        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(PdfCommon.getReportsDir(context), "Finora_Savings_Investment_$dateSuffix.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    private fun drawSavingsSummaryCards(
        canvas: Canvas,
        startY: Float,
        totalSaved: Long,
        totalTarget: Long,
        goalsCount: Int,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        val cardWidth = (CONTENT_WIDTH - 24f) / 3f
        val cardHeight = 52f

        val overallProgress = if (totalTarget > 0) ((totalSaved.toDouble() / totalTarget.toDouble()) * 100.0).coerceAtMost(100.0) else 0.0

        drawMetricCard(
            canvas, MARGIN, startY, cardWidth, cardHeight,
            "বর্তমান মোট সঞ্চয়",
            PdfCommon.formatMoney(totalSaved, useBengaliDigits),
            "নগদ জমার পরিমাণ",
            ColorGreenBg, ColorGreenIncome, fonts
        )

        drawMetricCard(
            canvas, MARGIN + cardWidth + 12f, startY, cardWidth, cardHeight,
            "নির্ধারিত লক্ষ্য",
            PdfCommon.formatMoney(totalTarget, useBengaliDigits),
            "${if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits("$goalsCount") else "$goalsCount"}টি লক্ষ্য",
            ColorBlueBg, ColorBluePrimary, fonts
        )

        val progressStr = String.format(Locale.US, "%.1f%%", overallProgress)
        drawMetricCard(
            canvas, MARGIN + (cardWidth + 12f) * 2, startY, cardWidth, cardHeight,
            "সামগ্রিক অগ্রগতি",
            if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits(progressStr) else progressStr,
            "লক্ষ্যের দিকে সঞ্চয়",
            Color.parseColor("#FFFBEB"), Color.parseColor("#D97706"), fonts
        )

        return startY + cardHeight + 14f
    }

    private fun drawInvestmentSummaryCards(
        canvas: Canvas,
        startY: Float,
        totalCost: Long,
        marketValue: Long,
        profit: Long,
        returnPct: Double,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        val cardWidth = (CONTENT_WIDTH - 24f) / 3f
        val cardHeight = 52f

        drawMetricCard(
            canvas, MARGIN, startY, cardWidth, cardHeight,
            "মোট বিনিয়োগ (ক্রয়মূল্য)",
            PdfCommon.formatMoney(totalCost, useBengaliDigits),
            "পোর্টফোলিওতে মোট খরচ",
            ColorBlueBg, ColorBluePrimary, fonts
        )

        drawMetricCard(
            canvas, MARGIN + cardWidth + 12f, startY, cardWidth, cardHeight,
            "বর্তমান বাজারমূল্য",
            PdfCommon.formatMoney(marketValue, useBengaliDigits),
            "আজকের মোট মূল্য",
            Color.parseColor("#F3E8FF"), Color.parseColor("#9333EA"), fonts
        )

        val isProfit = profit >= 0
        val retStr = String.format(Locale.US, "%.2f%%", returnPct)
        val profitStr = (if (isProfit) "+ " else "- ") + PdfCommon.formatMoney(kotlin.math.abs(profit), useBengaliDigits)
        drawMetricCard(
            canvas, MARGIN + (cardWidth + 12f) * 2, startY, cardWidth, cardHeight,
            if (isProfit) "মোট লাভ (Profit)" else "মোট লোকসান (Loss)",
            profitStr,
            "রিটার্ন: ${if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits(retStr) else retStr}",
            if (isProfit) ColorGreenBg else ColorRedBg,
            if (isProfit) ColorGreenIncome else ColorRedExpense,
            fonts
        )

        return startY + cardHeight + 14f
    }

    private fun drawMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        subLabel: String,
        bgColor: Int,
        accentColor: Int,
        fonts: PdfCommon.Fonts
    ) {
        val rect = RectF(x, y, x + width, y + height)
        val fillPaint = Paint().apply { color = bgColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)

        val borderPaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val accentPaint = Paint().apply { color = accentColor; style = Paint.Style.FILL }
        canvas.drawRect(x + 4f, y, x + width - 4f, y + 3f, accentPaint)

        val labelPaint = Paint().apply {
            color = ColorDarkText
            textSize = 7.5f
            typeface = fonts.regular
            isAntiAlias = true
        }
        canvas.drawText(label, x + 8f, y + 16f, labelPaint)

        val valuePaint = Paint().apply {
            color = accentColor
            textSize = 10f
            typeface = fonts.bold
            isAntiAlias = true
        }
        canvas.drawText(value, x + 8f, y + 32f, valuePaint)

        val subPaint = Paint().apply {
            color = ColorGrayText
            textSize = 7f
            typeface = fonts.regular
            isAntiAlias = true
        }
        canvas.drawText(subLabel, x + 8f, y + 44f, subPaint)
    }

    private fun drawGoalsTable(
        canvas: Canvas,
        startY: Float,
        goals: List<SavingsGoalEntity>,
        balances: Map<Long, Long>,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        var y = startY
        val colWidths = floatArrayOf(28f, 150f, 115f, 115f, 115f) // Sum = 523f

        // Table Header
        val headerHeight = 22f
        val headerRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + headerHeight)
        val headerBgPaint = Paint().apply { color = ColorNavyDark; style = Paint.Style.FILL }
        canvas.drawRoundRect(headerRect, 4f, 4f, headerBgPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = fonts.bold
            isAntiAlias = true
        }

        var colX = MARGIN
        canvas.drawText("#", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[0]
        canvas.drawText("লক্ষ্যের নাম (Goal)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[1]
        canvas.drawText("টার্গেট অ্যামাউন্ট (৳)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[2]
        canvas.drawText("বর্তমান জমা (৳)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[3]
        val rightHeaderTextPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("অগ্রগতি (%)", MARGIN + CONTENT_WIDTH - 6f, y + 15f, rightHeaderTextPaint)

        y += headerHeight + 2f

        if (goals.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = ColorGrayText
                textSize = 9.5f
                typeface = fonts.regular
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("কোন সঞ্চয় লক্ষ্য পাওয়া যায়নি।", PAGE_WIDTH / 2f, y + 30f, emptyPaint)
            return y + 50f
        }

        val rowTextPaint = Paint().apply {
            color = ColorDarkText
            textSize = 8.5f
            typeface = fonts.bold
            isAntiAlias = true
        }
        val rightRowTextPaint = Paint(rowTextPaint).apply { textAlign = Paint.Align.RIGHT }
        val borderPaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }

        for (i in goals.indices) {
            val goal = goals[i]
            val savedPaisa = balances[goal.id] ?: 0L
            val targetPaisa = goal.targetAmount ?: 0L
            val pct = if (targetPaisa > 0) ((savedPaisa.toDouble() / targetPaisa.toDouble()) * 100.0).coerceAtMost(100.0) else 0.0
            val rowHeight = 24f

            if (i % 2 == 1) {
                val rowRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight)
                val rowBgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
                canvas.drawRect(rowRect, rowBgPaint)
            }

            colX = MARGIN
            val numStr = if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits("${i + 1}") else "${i + 1}"
            canvas.drawText(numStr, colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[0]

            canvas.drawText(goal.name.take(22), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[1]

            val targetStr = if (targetPaisa > 0) PdfCommon.formatMoney(targetPaisa, useBengaliDigits) else "-"
            canvas.drawText(targetStr, colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[2]

            canvas.drawText(PdfCommon.formatMoney(savedPaisa, useBengaliDigits), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[3]

            val pctStr = String.format(Locale.US, "%.1f%%", pct)
            val pctColor = if (pct >= 100.0) ColorGreenIncome else ColorBluePrimary
            val pctPaint = Paint(rightRowTextPaint).apply { color = pctColor }
            canvas.drawText(if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits(pctStr) else pctStr, MARGIN + CONTENT_WIDTH - 6f, y + 16f, pctPaint)

            canvas.drawLine(MARGIN, y + rowHeight, MARGIN + CONTENT_WIDTH, y + rowHeight, borderPaint)
            y += rowHeight
        }

        return y + 14f
    }

    private fun drawHoldingsTable(
        canvas: Canvas,
        startY: Float,
        holdings: List<HoldingEntity>,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        var y = startY
        val colWidths = floatArrayOf(28f, 130f, 65f, 100f, 100f, 100f) // Sum = 523f

        val headerHeight = 22f
        val headerRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + headerHeight)
        val headerBgPaint = Paint().apply { color = ColorNavyDark; style = Paint.Style.FILL }
        canvas.drawRoundRect(headerRect, 4f, 4f, headerBgPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = fonts.bold
            isAntiAlias = true
        }

        var colX = MARGIN
        canvas.drawText("#", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[0]
        canvas.drawText("কোম্পানি / স্টক", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[1]
        canvas.drawText("শেয়ার", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[2]
        canvas.drawText("ক্রয়মূল্য (৳)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[3]
        canvas.drawText("বর্তমান বাজার (৳)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[4]
        val rightHeaderTextPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("লাভ / লোকসান", MARGIN + CONTENT_WIDTH - 6f, y + 15f, rightHeaderTextPaint)

        y += headerHeight + 2f

        if (holdings.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = ColorGrayText
                textSize = 9.5f
                typeface = fonts.regular
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("পোর্টফোলিওতে কোন শেয়ার হোল্ডিং পাওয়া যায়নি।", PAGE_WIDTH / 2f, y + 30f, emptyPaint)
            return y + 50f
        }

        val rowTextPaint = Paint().apply {
            color = ColorDarkText
            textSize = 8.5f
            typeface = fonts.bold
            isAntiAlias = true
        }
        val rightRowTextPaint = Paint(rowTextPaint).apply { textAlign = Paint.Align.RIGHT }
        val borderPaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }

        for (i in holdings.indices) {
            val h = holdings[i]
            val costPaisa = (h.quantity * h.averagePrice * 100).toLong()
            val marketPaisa = (h.quantity * h.currentPrice * 100).toLong()
            val gainLossPaisa = marketPaisa - costPaisa
            val isGain = gainLossPaisa >= 0
            val rowHeight = 24f

            if (i % 2 == 1) {
                val rowRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight)
                val rowBgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
                canvas.drawRect(rowRect, rowBgPaint)
            }

            colX = MARGIN
            val numStr = if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits("${i + 1}") else "${i + 1}"
            canvas.drawText(numStr, colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[0]

            canvas.drawText(h.stockName.take(18), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[1]

            val qtyStr = if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits("${h.quantity}") else "${h.quantity}"
            canvas.drawText(qtyStr, colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[2]

            canvas.drawText(PdfCommon.formatMoney(costPaisa, useBengaliDigits), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[3]

            canvas.drawText(PdfCommon.formatMoney(marketPaisa, useBengaliDigits), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[4]

            val gainColor = if (isGain) ColorGreenIncome else ColorRedExpense
            val gainPaint = Paint(rightRowTextPaint).apply { color = gainColor }
            val gainStr = (if (isGain) "+" else "-") + PdfCommon.formatMoney(kotlin.math.abs(gainLossPaisa), useBengaliDigits)
            canvas.drawText(gainStr, MARGIN + CONTENT_WIDTH - 6f, y + 16f, gainPaint)

            canvas.drawLine(MARGIN, y + rowHeight, MARGIN + CONTENT_WIDTH, y + rowHeight, borderPaint)
            y += rowHeight
        }

        return y + 14f
    }
}
