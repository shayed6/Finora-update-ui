package com.example.util.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.IncomeExpenseTransactionEntity
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

object IncomeExpensePdfRenderer {

    fun generate(
        context: Context,
        transactions: List<IncomeExpenseTransactionEntity>,
        sourcesMap: Map<Long, String> = emptyMap(),
        categoriesMap: Map<Long, String> = emptyMap(),
        filterTitle: String = "সার্বিক আয়-ব্যয় হিসাব",
        useBengaliDigits: Boolean = true
    ): File {
        val fonts = PdfCommon.loadFonts(context)
        val logoBitmap = PdfCommon.loadLogo(context)

        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        val pdfDocument = PdfDocument()

        // Page calculation:
        // Page 1: Header (~75pt), Filter badge (~28pt), 3 Metric cards (~55pt), Table Header (~22pt)
        // Rows take ~22pt each
        val itemsPage1 = 18
        val itemsPageSubsequent = 26

        val totalPages = if (transactions.size <= itemsPage1) {
            1
        } else {
            1 + ((transactions.size - itemsPage1 + itemsPageSubsequent - 1) / itemsPageSubsequent)
        }

        var currentIndex = 0
        val currentDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val orgInfo = com.example.data.preferences.OrganizationInfoRepository.getInstance(context).getOrganizationInfoBlocking()

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // White Background
            val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            // Finora Watermark
            PdfCommon.drawWatermark(canvas, logoBitmap)

            // Header
            var y = PdfCommon.drawHeader(
                canvas = canvas,
                logoBitmap = logoBitmap,
                fonts = fonts,
                title = "আয় ও ব্যয়ের পূর্ণাঙ্গ আর্থিক বিবরণী (Income & Expense Report)",
                subTitle = filterTitle,
                dateStr = if (useBengaliDigits) BengaliFormatter.formatBengaliDate(System.currentTimeMillis()) else currentDateStr,
                currentPage = pageIndex + 1,
                totalPages = totalPages,
                orgInfo = orgInfo
            )

            if (pageIndex == 0) {
                // Summary Metric Cards
                y = drawSummaryCards(canvas, y, totalIncome, totalExpense, netBalance, transactions.size, fonts, useBengaliDigits)
            } else {
                y += 6f
            }

            // Slice transactions for this page
            val pageSize = if (pageIndex == 0) itemsPage1 else itemsPageSubsequent
            val end = (currentIndex + pageSize).coerceAtMost(transactions.size)
            val pageTx = transactions.subList(currentIndex, end)
            val startNumber = currentIndex
            currentIndex = end

            // Draw Transactions Table
            y = drawTransactionTable(canvas, y, pageTx, startNumber, sourcesMap, categoriesMap, fonts, useBengaliDigits)

            // Footer
            PdfCommon.drawFooter(canvas, fonts, pageIndex + 1, totalPages, false)

            pdfDocument.finishPage(page)
        }

        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(PdfCommon.getReportsDir(context), "Finora_Income_Expense_$dateSuffix.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    private fun drawSummaryCards(
        canvas: Canvas,
        startY: Float,
        totalIncome: Long,
        totalExpense: Long,
        netBalance: Long,
        txCount: Int,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        val cardWidth = (CONTENT_WIDTH - 24f) / 3f
        val cardHeight = 52f

        // 1. Total Income Card (Green)
        drawMetricCard(
            canvas = canvas,
            x = MARGIN,
            y = startY,
            width = cardWidth,
            height = cardHeight,
            label = "মোট আয় (Total Income)",
            value = PdfCommon.formatMoney(totalIncome, useBengaliDigits),
            subLabel = if (useBengaliDigits) "${BengaliFormatter.englishToBengaliDigits("$txCount")}টি এন্ট্রি" else "$txCount entries",
            bgColor = ColorGreenBg,
            accentColor = ColorGreenIncome,
            fonts = fonts
        )

        // 2. Total Expense Card (Red)
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + cardWidth + 12f,
            y = startY,
            width = cardWidth,
            height = cardHeight,
            label = "মোট ব্যয় (Total Expense)",
            value = PdfCommon.formatMoney(totalExpense, useBengaliDigits),
            subLabel = "খরচের যোগফল",
            bgColor = ColorRedBg,
            accentColor = ColorRedExpense,
            fonts = fonts
        )

        // 3. Net Balance Card (Blue or Red depending on sign)
        val isPositive = netBalance >= 0
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + (cardWidth + 12f) * 2,
            y = startY,
            width = cardWidth,
            height = cardHeight,
            label = if (isPositive) "নিট উদ্বৃত্ত (Net Savings)" else "নিট ঘাটতি (Deficit)",
            value = PdfCommon.formatMoney(kotlin.math.abs(netBalance), useBengaliDigits),
            subLabel = if (isPositive) "আয়ের পর অবশিষ্ট" else "ব্যয় আয়ের চেয়ে বেশি",
            bgColor = if (isPositive) ColorBlueBg else ColorRedBg,
            accentColor = if (isPositive) ColorBluePrimary else ColorRedExpense,
            fonts = fonts
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

        // Accent top line
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
            textSize = 10.5f
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

    private fun drawTransactionTable(
        canvas: Canvas,
        startY: Float,
        items: List<IncomeExpenseTransactionEntity>,
        startNumber: Int,
        sourcesMap: Map<Long, String>,
        categoriesMap: Map<Long, String>,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        var y = startY
        val colWidths = floatArrayOf(26f, 75f, 52f, 130f, 135f, 105f) // Sum = 523f

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
        canvas.drawText("তারিখ", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[1]
        canvas.drawText("ধরন", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[2]
        canvas.drawText("খাত / উৎস", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[3]
        canvas.drawText("বিবরণ ও নোট", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[4]
        val rightHeaderTextPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("পরিমাণ (৳)", MARGIN + CONTENT_WIDTH - 6f, y + 15f, rightHeaderTextPaint)

        y += headerHeight + 2f

        if (items.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = ColorGrayText
                textSize = 9.5f
                typeface = fonts.regular
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("এই ফিল্টারে কোন লেনদেন পাওয়া যায়নি।", PAGE_WIDTH / 2f, y + 30f, emptyPaint)
            return y + 50f
        }

        val rowTextPaint = Paint().apply {
            color = ColorDarkText
            textSize = 8.5f
            typeface = fonts.regular
            isAntiAlias = true
        }
        val rightRowTextPaint = Paint(rowTextPaint).apply { textAlign = Paint.Align.RIGHT }
        val borderPaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }

        for (i in items.indices) {
            val tx = items[i]
            val rowHeight = 22f

            if (i % 2 == 1) {
                val rowRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight)
                val rowBgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
                canvas.drawRect(rowRect, rowBgPaint)
            }

            colX = MARGIN
            // Serial
            val numStr = if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits("${startNumber + i + 1}") else "${startNumber + i + 1}"
            canvas.drawText(numStr, colX + 6f, y + 15f, rowTextPaint)
            colX += colWidths[0]

            // Date
            val dateStr = if (useBengaliDigits) BengaliFormatter.formatBengaliDate(tx.occurredAt) else SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(tx.occurredAt))
            canvas.drawText(dateStr.take(12), colX + 6f, y + 15f, rowTextPaint)
            colX += colWidths[1]

            // Type
            val isIncome = tx.type == "INCOME"
            val typeColor = if (isIncome) ColorGreenIncome else ColorRedExpense
            val typePaint = Paint(rowTextPaint).apply { color = typeColor; typeface = fonts.bold }
            canvas.drawText(if (isIncome) "আয়" else "ব্যয়", colX + 6f, y + 15f, typePaint)
            colX += colWidths[2]

            // Sector / Category
            val sectorName = if (isIncome) {
                sourcesMap[tx.sourceId] ?: "সাধারণ আয়"
            } else {
                categoriesMap[tx.categoryId] ?: "সাধারণ খরচ"
            }
            canvas.drawText(sectorName.take(16), colX + 6f, y + 15f, rowTextPaint)
            colX += colWidths[3]

            // Note / Tagline
            val noteText = tx.tagLine?.ifBlank { null } ?: tx.note?.ifBlank { null } ?: "-"
            canvas.drawText(noteText.take(20), colX + 6f, y + 15f, rowTextPaint)
            colX += colWidths[4]

            // Amount
            val amountPaint = Paint(rightRowTextPaint).apply { color = typeColor; typeface = fonts.bold }
            val amountStr = (if (isIncome) "+ " else "- ") + PdfCommon.formatMoney(tx.amount, useBengaliDigits)
            canvas.drawText(amountStr, MARGIN + CONTENT_WIDTH - 6f, y + 15f, amountPaint)

            canvas.drawLine(MARGIN, y + rowHeight, MARGIN + CONTENT_WIDTH, y + rowHeight, borderPaint)
            y += rowHeight
        }

        return y + 10f
    }
}
