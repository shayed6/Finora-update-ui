package com.example.util.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerPartyEntity
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

object DueLedgerPdfRenderer {

    data class PartySummary(
        val party: LedgerPartyEntity,
        val totalGavePaisa: Long,
        val totalReceivedPaisa: Long,
        val netBalancePaisa: Long, // > 0 means party owes me (পাওনা), < 0 means I owe party (দেনা)
        val lastEntryDate: Long?
    )

    fun generateOverallDueReport(
        context: Context,
        parties: List<LedgerPartyEntity>,
        entries: List<LedgerEntryEntity>,
        reportKind: String = "ALL", // "ALL", "RECEIVABLE_ONLY", "PAYABLE_ONLY"
        useBengaliDigits: Boolean = true
    ): File {
        val fonts = PdfCommon.loadFonts(context)
        val logoBitmap = PdfCommon.loadLogo(context)

        // Group entries by partyId
        val entriesByParty = entries.groupBy { it.partyId }
        val summaries = parties.map { party ->
            val pEntries = entriesByParty[party.id] ?: emptyList()
            val gave = pEntries.filter { it.type == "GAVE" }.sumOf { it.amountPaisa }
            val received = pEntries.filter { it.type == "RECEIVED" }.sumOf { it.amountPaisa }
            val lastDate = pEntries.maxOfOrNull { it.entryDate }
            PartySummary(party, gave, received, gave - received, lastDate)
        }.filter {
            when (reportKind) {
                "RECEIVABLE_ONLY" -> it.netBalancePaisa > 0
                "PAYABLE_ONLY" -> it.netBalancePaisa < 0
                else -> true
            }
        }.sortedByDescending { kotlin.math.abs(it.netBalancePaisa) }

        val totalReceivable = summaries.filter { it.netBalancePaisa > 0 }.sumOf { it.netBalancePaisa }
        val totalPayable = summaries.filter { it.netBalancePaisa < 0 }.sumOf { kotlin.math.abs(it.netBalancePaisa) }
        val netPosition = totalReceivable - totalPayable

        val pdfDocument = PdfDocument()

        val itemsPage1 = 18
        val itemsPageSubsequent = 26
        val totalPages = if (summaries.size <= itemsPage1) 1 else 1 + ((summaries.size - itemsPage1 + itemsPageSubsequent - 1) / itemsPageSubsequent)

        var currentIndex = 0
        val currentDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val dateStr = if (useBengaliDigits) BengaliFormatter.formatBengaliDate(System.currentTimeMillis()) else currentDateStr

        val reportTitle = when (reportKind) {
            "RECEIVABLE_ONLY" -> "পাওনা ও বকেয়া আদায়ের হিসাব (Receivables Ledger)"
            "PAYABLE_ONLY" -> "দেনা ও প্রদেয় হিসাব (Payables Ledger)"
            else -> "বাকির খাতা ও দেনা-পাওনা বিবরণী (Due & Credit Ledger)"
        }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            PdfCommon.drawWatermark(canvas, logoBitmap)

            var y = PdfCommon.drawHeader(
                canvas = canvas,
                logoBitmap = logoBitmap,
                fonts = fonts,
                title = reportTitle,
                subTitle = "গ্রাহক ও মহাজনের খাতা • ১০০% অন-ডিভাইস নিরাপদ হিসাব",
                dateStr = dateStr,
                currentPage = pageIndex + 1,
                totalPages = totalPages
            )

            if (pageIndex == 0) {
                y = drawDueSummaryCards(canvas, y, totalReceivable, totalPayable, netPosition, summaries.size, fonts, useBengaliDigits)
            } else {
                y += 6f
            }

            val pageSize = if (pageIndex == 0) itemsPage1 else itemsPageSubsequent
            val end = (currentIndex + pageSize).coerceAtMost(summaries.size)
            val pageSlice = summaries.subList(currentIndex, end)
            val startNumber = currentIndex
            currentIndex = end

            y = drawPartiesTable(canvas, y, pageSlice, startNumber, fonts, useBengaliDigits)

            PdfCommon.drawFooter(canvas, fonts, pageIndex + 1, totalPages, false)

            pdfDocument.finishPage(page)
        }

        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(PdfCommon.getReportsDir(context), "Finora_Due_Receivable_Payable_$dateSuffix.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    fun generatePartyStatementPdf(
        context: Context,
        party: LedgerPartyEntity,
        entries: List<LedgerEntryEntity>,
        useBengaliDigits: Boolean = true
    ): File {
        val fonts = PdfCommon.loadFonts(context)
        val logoBitmap = PdfCommon.loadLogo(context)

        val sortedEntries = entries.sortedBy { it.entryDate }
        val totalGave = sortedEntries.filter { it.type == "GAVE" }.sumOf { it.amountPaisa }
        val totalReceived = sortedEntries.filter { it.type == "RECEIVED" }.sumOf { it.amountPaisa }
        val netBalance = totalGave - totalReceived

        val pdfDocument = PdfDocument()

        val itemsPage1 = 18
        val itemsPageSubsequent = 26
        val totalPages = if (sortedEntries.size <= itemsPage1) 1 else 1 + ((sortedEntries.size - itemsPage1 + itemsPageSubsequent - 1) / itemsPageSubsequent)

        var currentIndex = 0
        var runningBalance = 0L

        val currentDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val dateStr = if (useBengaliDigits) BengaliFormatter.formatBengaliDate(System.currentTimeMillis()) else currentDateStr

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            PdfCommon.drawWatermark(canvas, logoBitmap)

            var y = PdfCommon.drawHeader(
                canvas = canvas,
                logoBitmap = logoBitmap,
                fonts = fonts,
                title = "ব্যক্তিভিত্তিক লেজার হিসাব বিবরণী (Party Statement)",
                subTitle = "${party.name} • খতিয়ান ও লেনদেন অডিট ট্রেইল",
                dateStr = dateStr,
                currentPage = pageIndex + 1,
                totalPages = totalPages
            )

            if (pageIndex == 0) {
                // Party Details Card
                y = drawPartyInfoCard(canvas, y, party, totalGave, totalReceived, netBalance, fonts, useBengaliDigits)
            } else {
                y += 6f
            }

            val pageSize = if (pageIndex == 0) itemsPage1 else itemsPageSubsequent
            val end = (currentIndex + pageSize).coerceAtMost(sortedEntries.size)
            val pageSlice = sortedEntries.subList(currentIndex, end)
            val startNumber = currentIndex
            currentIndex = end

            y = drawPartyStatementTable(canvas, y, pageSlice, startNumber, runningBalance, fonts, useBengaliDigits) { updatedBalance ->
                runningBalance = updatedBalance
            }

            PdfCommon.drawFooter(canvas, fonts, pageIndex + 1, totalPages, false)

            pdfDocument.finishPage(page)
        }

        val cleanName = party.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(PdfCommon.getReportsDir(context), "Finora_Statement_${cleanName}_$dateSuffix.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    private fun drawDueSummaryCards(
        canvas: Canvas,
        startY: Float,
        totalReceivable: Long,
        totalPayable: Long,
        netPosition: Long,
        partiesCount: Int,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        val cardWidth = (CONTENT_WIDTH - 24f) / 3f
        val cardHeight = 52f

        drawMetricCard(
            canvas, MARGIN, startY, cardWidth, cardHeight,
            "মোট পাওনা (Receivable)",
            PdfCommon.formatMoney(totalReceivable, useBengaliDigits),
            "গ্রাহকদের কাছে বাকি",
            ColorGreenBg, ColorGreenIncome, fonts
        )

        drawMetricCard(
            canvas, MARGIN + cardWidth + 12f, startY, cardWidth, cardHeight,
            "মোট দেনা (Payable)",
            PdfCommon.formatMoney(totalPayable, useBengaliDigits),
            "মহাজন/অন্যদের পাওনা",
            ColorRedBg, ColorRedExpense, fonts
        )

        val isNetReceivable = netPosition >= 0
        drawMetricCard(
            canvas, MARGIN + (cardWidth + 12f) * 2, startY, cardWidth, cardHeight,
            if (isNetReceivable) "নিট পাওনা (Net Surplus)" else "নিট দেনা (Net Deficit)",
            PdfCommon.formatMoney(kotlin.math.abs(netPosition), useBengaliDigits),
            "${if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits("$partiesCount") else "$partiesCount"} জন ব্যক্তি",
            if (isNetReceivable) ColorBlueBg else ColorRedBg,
            if (isNetReceivable) ColorBluePrimary else ColorRedExpense,
            fonts
        )

        return startY + cardHeight + 14f
    }

    private fun drawPartyInfoCard(
        canvas: Canvas,
        startY: Float,
        party: LedgerPartyEntity,
        totalGave: Long,
        totalReceived: Long,
        netBalance: Long,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        val cardHeight = 60f
        val rect = RectF(MARGIN, startY, MARGIN + CONTENT_WIDTH, startY + cardHeight)
        val bgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Left: Party Name & Phone
        val labelPaint = Paint().apply {
            color = ColorNavyDark
            textSize = 12f
            typeface = fonts.bold
            isAntiAlias = true
        }
        canvas.drawText("ব্যক্তির নাম: ${party.name}", MARGIN + 12f, startY + 22f, labelPaint)

        val phonePaint = Paint().apply {
            color = ColorDarkText
            textSize = 9f
            typeface = fonts.regular
            isAntiAlias = true
        }
        val phoneStr = party.phone?.let { "মোবাইল: $it" } ?: "মোবাইল: যুক্ত নেই"
        canvas.drawText(phoneStr, MARGIN + 12f, startY + 42f, phonePaint)

        // Right: Balance Status
        val isOwesMe = netBalance >= 0
        val balanceTitle = if (isOwesMe) "বর্তমান পাওনা (পাবো)" else "বর্তমান দেনা (দিতে হবে)"
        val balanceColor = if (isOwesMe) ColorGreenIncome else ColorRedExpense

        val balLabelPaint = Paint().apply {
            color = ColorGrayText
            textSize = 8.5f
            typeface = fonts.regular
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText(balanceTitle, MARGIN + CONTENT_WIDTH - 12f, startY + 20f, balLabelPaint)

        val balValuePaint = Paint().apply {
            color = balanceColor
            textSize = 14f
            typeface = fonts.bold
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText(PdfCommon.formatMoney(kotlin.math.abs(netBalance), useBengaliDigits), MARGIN + CONTENT_WIDTH - 12f, startY + 44f, balValuePaint)

        return startY + cardHeight + 12f
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

    private fun drawPartiesTable(
        canvas: Canvas,
        startY: Float,
        summaries: List<PartySummary>,
        startNumber: Int,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean
    ): Float {
        var y = startY
        val colWidths = floatArrayOf(28f, 150f, 110f, 110f, 125f) // Sum = 523f

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
        canvas.drawText("ব্যক্তি / কাস্টমার", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[1]
        canvas.drawText("মোট দিলাম (৳)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[2]
        canvas.drawText("মোট পেলাম (৳)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[3]
        val rightHeaderTextPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("অবশিষ্ট স্থিতি (বাকি ৳)", MARGIN + CONTENT_WIDTH - 6f, y + 15f, rightHeaderTextPaint)

        y += headerHeight + 2f

        if (summaries.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = ColorGrayText
                textSize = 9.5f
                typeface = fonts.regular
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("কোন ব্যক্তির বাকি হিসাব পাওয়া যায়নি।", PAGE_WIDTH / 2f, y + 30f, emptyPaint)
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

        for (i in summaries.indices) {
            val s = summaries[i]
            val rowHeight = 24f

            if (i % 2 == 1) {
                val rowRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight)
                val rowBgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
                canvas.drawRect(rowRect, rowBgPaint)
            }

            colX = MARGIN
            val numStr = if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits("${startNumber + i + 1}") else "${startNumber + i + 1}"
            canvas.drawText(numStr, colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[0]

            val nameStr = s.party.name + (s.party.phone?.let { " ($it)" } ?: "")
            canvas.drawText(nameStr.take(24), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[1]

            canvas.drawText(PdfCommon.formatMoney(s.totalGavePaisa, useBengaliDigits), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[2]

            canvas.drawText(PdfCommon.formatMoney(s.totalReceivedPaisa, useBengaliDigits), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[3]

            val isReceivable = s.netBalancePaisa >= 0
            val balanceColor = if (s.netBalancePaisa == 0L) ColorDarkText else if (isReceivable) ColorGreenIncome else ColorRedExpense
            val balancePaint = Paint(rightRowTextPaint).apply { color = balanceColor }
            val statusLabel = if (s.netBalancePaisa == 0L) "পরিশোধ" else if (isReceivable) "(পাবো)" else "(দেনা)"
            val balanceStr = "${PdfCommon.formatMoney(kotlin.math.abs(s.netBalancePaisa), useBengaliDigits)} $statusLabel"
            canvas.drawText(balanceStr, MARGIN + CONTENT_WIDTH - 6f, y + 16f, balancePaint)

            canvas.drawLine(MARGIN, y + rowHeight, MARGIN + CONTENT_WIDTH, y + rowHeight, borderPaint)
            y += rowHeight
        }

        return y + 14f
    }

    private fun drawPartyStatementTable(
        canvas: Canvas,
        startY: Float,
        entries: List<LedgerEntryEntity>,
        startNumber: Int,
        initialRunningBalance: Long,
        fonts: PdfCommon.Fonts,
        useBengaliDigits: Boolean,
        onBalanceUpdated: (Long) -> Unit
    ): Float {
        var y = startY
        var currentRunning = initialRunningBalance
        val colWidths = floatArrayOf(26f, 75f, 60f, 132f, 115f, 115f) // Sum = 523f

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
        canvas.drawText("লেনদেন", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[2]
        canvas.drawText("বিবরণ ও রেফারেন্স", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[3]
        canvas.drawText("টাকা (৳)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[4]
        val rightHeaderTextPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("জের / স্থিতি (৳)", MARGIN + CONTENT_WIDTH - 6f, y + 15f, rightHeaderTextPaint)

        y += headerHeight + 2f

        if (entries.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = ColorGrayText
                textSize = 9.5f
                typeface = fonts.regular
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("এই ব্যক্তির কোন লেনদেন এন্ট্রি নেই।", PAGE_WIDTH / 2f, y + 30f, emptyPaint)
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

        for (i in entries.indices) {
            val e = entries[i]
            val isGave = e.type == "GAVE"
            if (isGave) currentRunning += e.amountPaisa else currentRunning -= e.amountPaisa
            val rowHeight = 22f

            if (i % 2 == 1) {
                val rowRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight)
                val rowBgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
                canvas.drawRect(rowRect, rowBgPaint)
            }

            colX = MARGIN
            val numStr = if (useBengaliDigits) BengaliFormatter.englishToBengaliDigits("${startNumber + i + 1}") else "${startNumber + i + 1}"
            canvas.drawText(numStr, colX + 6f, y + 15f, rowTextPaint)
            colX += colWidths[0]

            val dateStr = if (useBengaliDigits) BengaliFormatter.formatBengaliDate(e.entryDate) else SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(e.entryDate))
            canvas.drawText(dateStr.take(12), colX + 6f, y + 15f, rowTextPaint)
            colX += colWidths[1]

            val typeColor = if (isGave) ColorRedExpense else ColorGreenIncome
            val typeText = if (isGave) "দিলাম" else "পেলাম"
            val typePaint = Paint(rowTextPaint).apply { color = typeColor }
            canvas.drawText(typeText, colX + 6f, y + 15f, typePaint)
            colX += colWidths[2]

            val noteStr = e.note?.take(20) ?: "-"
            canvas.drawText(noteStr, colX + 6f, y + 15f, rowTextPaint)
            colX += colWidths[3]

            val amtStr = (if (isGave) "+ " else "- ") + PdfCommon.formatMoney(e.amountPaisa, useBengaliDigits)
            val amtPaint = Paint(rowTextPaint).apply { color = typeColor }
            canvas.drawText(amtStr, colX + 6f, y + 15f, amtPaint)
            colX += colWidths[4]

            val runColor = if (currentRunning >= 0) ColorGreenIncome else ColorRedExpense
            val runPaint = Paint(rightRowTextPaint).apply { color = runColor }
            val runSign = if (currentRunning >= 0) "পাবো " else "দেনা "
            canvas.drawText("$runSign${PdfCommon.formatMoney(kotlin.math.abs(currentRunning), useBengaliDigits)}", MARGIN + CONTENT_WIDTH - 6f, y + 15f, runPaint)

            canvas.drawLine(MARGIN, y + rowHeight, MARGIN + CONTENT_WIDTH, y + rowHeight, borderPaint)
            y += rowHeight
        }

        onBalanceUpdated(currentRunning)
        return y + 10f
    }
}
