package com.example.util.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.InvoiceStatus
import com.example.data.local.entity.InvoiceWithItems
import com.example.data.preferences.OrganizationInfo
import com.example.data.preferences.OrganizationInfoRepository
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

object InvoicePdfRenderer {

    fun generate(context: Context, invoiceWithItems: InvoiceWithItems): File {
        val invoice = invoiceWithItems.invoice
        val items = invoiceWithItems.items
        val isEnglish = invoice.language.equals("EN", ignoreCase = true)

        val fonts = PdfCommon.loadFonts(context)
        val logoBitmap = PdfCommon.loadLogo(context)

        val pdfDocument = PdfDocument()

        // Page calculation:
        // Page 1 has header (~100pt), seller/buyer blocks (~90pt), meta (~45pt), table header (~24pt), summary box (~140pt), terms & signature (~80pt), footer (~40pt)
        // Table items: ~26pt each
        val maxItemsFirstPage = 8
        val itemsPerSubsequentPage = 18

        val totalPages = if (items.size <= maxItemsFirstPage) {
            1
        } else {
            1 + ((items.size - maxItemsFirstPage + itemsPerSubsequentPage - 1) / itemsPerSubsequentPage)
        }

        val storedOrg = OrganizationInfoRepository.getInstance(context).getOrganizationInfoBlocking()
        val orgInfo = OrganizationInfo(
            name = invoice.businessName.ifBlank { storedOrg.name },
            address = invoice.businessAddress?.ifBlank { storedOrg.address } ?: storedOrg.address,
            phone = invoice.businessPhone?.ifBlank { storedOrg.phone } ?: storedOrg.phone
        )

        var currentItemIndex = 0

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // White background
            val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            // Finora Watermark
            PdfCommon.drawWatermark(canvas, logoBitmap)

            // Header
            val dateStr = SimpleDateFormat("dd MMM yyyy", if (isEnglish) Locale.ENGLISH else Locale.getDefault()).format(Date(invoice.invoiceDate))
            var y = PdfCommon.drawHeader(
                canvas = canvas,
                logoBitmap = logoBitmap,
                fonts = fonts,
                title = if (isEnglish) "INVOICE / BILL" else "ইনভয়েস / বিল",
                subTitle = if (isEnglish) "Official Commercial Invoice" else "ব্যবসায়িক চালান ও বিক্রয় হিসাব",
                dateStr = if (isEnglish) dateStr else BengaliFormatter.formatBengaliDate(invoice.invoiceDate),
                currentPage = pageIndex + 1,
                totalPages = totalPages,
                orgInfo = orgInfo
            )

            if (pageIndex == 0) {
                // Seller & Customer Cards
                y = drawSellerAndBuyerBlocks(canvas, y, invoice, fonts, isEnglish)

                // Invoice Meta row (Inv #, Date, Due Date, Status)
                y = drawInvoiceMetaRow(canvas, y, invoice, fonts, isEnglish)
            } else {
                y += 10f
            }

            // Line items for this page
            val itemsForThisPage = if (pageIndex == 0) {
                val end = (currentItemIndex + maxItemsFirstPage).coerceAtMost(items.size)
                val slice = items.subList(currentItemIndex, end)
                currentItemIndex = end
                slice
            } else {
                val end = (currentItemIndex + itemsPerSubsequentPage).coerceAtMost(items.size)
                val slice = items.subList(currentItemIndex, end)
                currentItemIndex = end
                slice
            }

            // Draw Items Table
            y = drawItemsTable(canvas, y, itemsForThisPage, currentItemIndex - itemsForThisPage.size, fonts, isEnglish)

            // On the last page, draw Summary calculation box, terms and signature
            if (pageIndex == totalPages - 1) {
                y = drawCalculationBox(canvas, y, invoice, fonts, isEnglish)
                drawTermsAndSignatures(canvas, y, invoice, fonts, isEnglish)
            }

            // Footer
            PdfCommon.drawFooter(canvas, fonts, pageIndex + 1, totalPages, isEnglish)

            pdfDocument.finishPage(page)
        }

        // Save file
        val cleanNumber = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(PdfCommon.getReportsDir(context), "Finora_Invoice_${cleanNumber}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    private fun drawSellerAndBuyerBlocks(
        canvas: Canvas,
        startY: Float,
        invoice: com.example.data.local.entity.InvoiceEntity,
        fonts: PdfCommon.Fonts,
        isEnglish: Boolean
    ): Float {
        val blockWidth = (CONTENT_WIDTH - 14f) / 2f
        val blockHeight = 72f

        // 1. Seller Box (Left)
        val sellerRect = RectF(MARGIN, startY, MARGIN + blockWidth, startY + blockHeight)
        val boxBgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
        canvas.drawRoundRect(sellerRect, 6f, 6f, boxBgPaint)

        val borderPaint = Paint().apply {
            color = ColorGrayBorder
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(sellerRect, 6f, 6f, borderPaint)

        val labelPaint = Paint().apply {
            color = ColorBluePrimary
            textSize = 8.5f
            typeface = fonts.bold
            isAntiAlias = true
        }
        canvas.drawText(if (isEnglish) "FROM / SELLER" else "বিক্রেতা / প্রতিষ্ঠান", MARGIN + 10f, startY + 15f, labelPaint)

        val namePaint = Paint().apply {
            color = ColorNavyDark
            textSize = 10.5f
            typeface = fonts.bold
            isAntiAlias = true
        }
        canvas.drawText(invoice.businessName.ifBlank { "Finora Business" }, MARGIN + 10f, startY + 30f, namePaint)

        val detailPaint = Paint().apply {
            color = ColorDarkText
            textSize = 8.5f
            typeface = fonts.regular
            isAntiAlias = true
        }
        val sellerPhone = invoice.businessPhone?.let { "মোবাইল: $it" } ?: ""
        canvas.drawText(sellerPhone, MARGIN + 10f, startY + 44f, detailPaint)
        val sellerAddr = invoice.businessAddress?.take(36) ?: ""
        canvas.drawText(sellerAddr, MARGIN + 10f, startY + 58f, detailPaint)

        // 2. Buyer Box (Right)
        val buyerX = MARGIN + blockWidth + 14f
        val buyerRect = RectF(buyerX, startY, buyerX + blockWidth, startY + blockHeight)
        canvas.drawRoundRect(buyerRect, 6f, 6f, boxBgPaint)
        canvas.drawRoundRect(buyerRect, 6f, 6f, borderPaint)

        canvas.drawText(if (isEnglish) "BILL TO / CUSTOMER" else "ক্রেতা / গ্রাহক", buyerX + 10f, startY + 15f, labelPaint)
        canvas.drawText(invoice.customerName.ifBlank { if (isEnglish) "Valued Customer" else "সম্মানিত গ্রাহক" }, buyerX + 10f, startY + 30f, namePaint)
        val buyerPhone = invoice.customerPhone?.let { "ফোন: $it" } ?: ""
        canvas.drawText(buyerPhone, buyerX + 10f, startY + 44f, detailPaint)
        val buyerAddr = invoice.customerAddress?.take(36) ?: ""
        canvas.drawText(buyerAddr, buyerX + 10f, startY + 58f, detailPaint)

        return startY + blockHeight + 10f
    }

    private fun drawInvoiceMetaRow(
        canvas: Canvas,
        startY: Float,
        invoice: com.example.data.local.entity.InvoiceEntity,
        fonts: PdfCommon.Fonts,
        isEnglish: Boolean
    ): Float {
        val rowHeight = 36f
        val rect = RectF(MARGIN, startY, MARGIN + CONTENT_WIDTH, startY + rowHeight)
        val fillPaint = Paint().apply { color = ColorBlueBg; style = Paint.Style.FILL }
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)

        val borderPaint = Paint().apply {
            color = ColorGrayBorder
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val colWidth = CONTENT_WIDTH / 4f

        // Column 1: Invoice No
        val subLabelPaint = Paint().apply {
            color = ColorGrayText
            textSize = 7.5f
            typeface = fonts.regular
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = ColorNavyDark
            textSize = 9.5f
            typeface = fonts.bold
            isAntiAlias = true
        }

        canvas.drawText(if (isEnglish) "INVOICE NO" else "ইনভয়েস নং", MARGIN + 8f, startY + 13f, subLabelPaint)
        canvas.drawText(invoice.invoiceNumber, MARGIN + 8f, startY + 27f, valPaint)

        // Column 2: Date
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(invoice.invoiceDate))
        canvas.drawText(if (isEnglish) "INVOICE DATE" else "ইস্যু তারিখ", MARGIN + colWidth + 8f, startY + 13f, subLabelPaint)
        canvas.drawText(if (isEnglish) dateStr else BengaliFormatter.formatBengaliDate(invoice.invoiceDate), MARGIN + colWidth + 8f, startY + 27f, valPaint)

        // Column 3: Due Date
        val dueDateStr = if (invoice.dueDate != null) {
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(invoice.dueDate))
        } else {
            "-"
        }
        canvas.drawText(if (isEnglish) "DUE DATE" else "পরিশোধের মেয়াদ", MARGIN + colWidth * 2 + 8f, startY + 13f, subLabelPaint)
        canvas.drawText(if (invoice.dueDate != null && !isEnglish) BengaliFormatter.formatBengaliDate(invoice.dueDate) else dueDateStr, MARGIN + colWidth * 2 + 8f, startY + 27f, valPaint)

        // Column 4: Status Badge
        val statusX = MARGIN + colWidth * 3 + 8f
        val (statusText, statusBg, statusColor) = when (invoice.status) {
            InvoiceStatus.PAID.name -> Triple(if (isEnglish) "PAID" else "পরিশোধিত", ColorGreenBg, ColorGreenIncome)
            InvoiceStatus.PARTIAL.name -> Triple(if (isEnglish) "PARTIAL" else "আংশিক বাকি", Color.parseColor("#FFFBEB"), Color.parseColor("#D97706"))
            else -> Triple(if (isEnglish) "DUE" else "বকেয়া", ColorRedBg, ColorRedExpense)
        }

        val badgeRect = RectF(statusX, startY + 8f, statusX + colWidth - 16f, startY + 28f)
        val badgePaint = Paint().apply { color = statusBg; style = Paint.Style.FILL }
        canvas.drawRoundRect(badgeRect, 4f, 4f, badgePaint)

        val badgeTextPaint = Paint().apply {
            color = statusColor
            textSize = 9f
            typeface = fonts.bold
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(statusText, badgeRect.centerX(), startY + 22f, badgeTextPaint)

        return startY + rowHeight + 12f
    }

    private fun drawItemsTable(
        canvas: Canvas,
        startY: Float,
        items: List<InvoiceItemEntity>,
        startIndex: Int,
        fonts: PdfCommon.Fonts,
        isEnglish: Boolean
    ): Float {
        var y = startY
        val colWidths = floatArrayOf(28f, 225f, 65f, 95f, 110f) // Sum = 523f

        // Table Header
        val headerHeight = 22f
        val headerRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + headerHeight)
        val headerBgPaint = Paint().apply { color = ColorNavyDark; style = Paint.Style.FILL }
        canvas.drawRoundRect(headerRect, 4f, 4f, headerBgPaint)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = fonts.bold
            isAntiAlias = true
        }

        var colX = MARGIN
        canvas.drawText("#", colX + 8f, y + 15f, headerTextPaint)
        colX += colWidths[0]
        canvas.drawText(if (isEnglish) "Item & Description" else "আইটেম ও বিবরণ", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[1]
        canvas.drawText(if (isEnglish) "Qty" else "পরিমাণ", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[2]
        canvas.drawText(if (isEnglish) "Unit Price (৳)" else "একক দর (৳)", colX + 6f, y + 15f, headerTextPaint)
        colX += colWidths[3]
        val rightHeaderTextPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(if (isEnglish) "Total (৳)" else "মোট (৳)", MARGIN + CONTENT_WIDTH - 8f, y + 15f, rightHeaderTextPaint)

        y += headerHeight + 2f

        if (items.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = ColorGrayText
                textSize = 9f
                typeface = fonts.regular
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(if (isEnglish) "No items recorded in invoice." else "ইনভয়েসে কোন আইটেম যুক্ত করা হয়নি।", PAGE_WIDTH / 2f, y + 25f, emptyPaint)
            return y + 40f
        }

        val rowTextPaint = Paint().apply {
            color = ColorDarkText
            textSize = 9f
            typeface = fonts.bold
            isAntiAlias = true
        }
        val descPaint = Paint().apply {
            color = ColorGrayText
            textSize = 7.5f
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
            val item = items[i]
            val hasDesc = !item.description.isNullOrBlank()
            val rowHeight = if (hasDesc) 32f else 24f

            // Alternate row background
            if (i % 2 == 1) {
                val rowRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight)
                val rowBgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
                canvas.drawRect(rowRect, rowBgPaint)
            }

            colX = MARGIN
            val numStr = if (isEnglish) "${startIndex + i + 1}" else BengaliFormatter.englishToBengaliDigits("${startIndex + i + 1}")
            canvas.drawText(numStr, colX + 8f, y + 16f, rowTextPaint)
            colX += colWidths[0]

            // Item Name & optional description
            canvas.drawText(item.itemName.take(32), colX + 6f, y + 16f, rowTextPaint)
            if (hasDesc) {
                canvas.drawText(item.description!!.take(40), colX + 6f, y + 28f, descPaint)
            }
            colX += colWidths[1]

            // Qty
            val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else String.format(Locale.US, "%.2f", item.quantity)
            canvas.drawText(if (isEnglish) qtyStr else BengaliFormatter.englishToBengaliDigits(qtyStr), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[2]

            // Unit Price
            canvas.drawText(PdfCommon.formatMoney(item.unitPricePaisa, !isEnglish), colX + 6f, y + 16f, rowTextPaint)
            colX += colWidths[3]

            // Total
            canvas.drawText(PdfCommon.formatMoney(item.totalPricePaisa, !isEnglish), MARGIN + CONTENT_WIDTH - 8f, y + 16f, rightRowTextPaint)

            canvas.drawLine(MARGIN, y + rowHeight, MARGIN + CONTENT_WIDTH, y + rowHeight, borderPaint)
            y += rowHeight
        }

        return y + 10f
    }

    private fun drawCalculationBox(
        canvas: Canvas,
        startY: Float,
        invoice: com.example.data.local.entity.InvoiceEntity,
        fonts: PdfCommon.Fonts,
        isEnglish: Boolean
    ): Float {
        var y = startY
        val boxWidth = 220f
        val boxX = MARGIN + CONTENT_WIDTH - boxWidth

        val rows = listOf(
            Pair(if (isEnglish) "Subtotal:" else "উপ-মোট:", invoice.subtotalPaisa),
            Pair(if (isEnglish) "Discount (-):" else "ছাড় (-):", -invoice.discountPaisa),
            Pair(if (isEnglish) "Additional Charges (+):" else "ভ্যাট / অন্যান্য খরচ (+):", invoice.additionalChargesPaisa),
            Pair(if (isEnglish) "Grand Total:" else "সর্বমোট প্রদেয়:", invoice.grandTotalPaisa),
            Pair(if (isEnglish) "Amount Paid:" else "পরিশোধিত টাকা:", invoice.paidAmountPaisa),
            Pair(if (isEnglish) "Balance Due:" else "অবশিষ্ট বকেয়া:", invoice.dueAmountPaisa)
        )

        val boxHeight = rows.size * 20f + 6f
        val boxRect = RectF(boxX, y, boxX + boxWidth, y + boxHeight)

        val bgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
        canvas.drawRoundRect(boxRect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(boxRect, 6f, 6f, borderPaint)

        var rowY = y + 16f
        val labelPaint = Paint().apply {
            color = ColorDarkText
            textSize = 8.5f
            typeface = fonts.regular
            isAntiAlias = true
        }
        val boldLabelPaint = Paint().apply {
            color = ColorNavyDark
            textSize = 9.5f
            typeface = fonts.bold
            isAntiAlias = true
        }
        val rightValPaint = Paint().apply {
            color = ColorDarkText
            textSize = 8.5f
            typeface = fonts.bold
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val grandTotalPaint = Paint().apply {
            color = ColorBluePrimary
            textSize = 10f
            typeface = fonts.bold
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val duePaint = Paint().apply {
            color = if (invoice.dueAmountPaisa > 0) ColorRedExpense else ColorGreenIncome
            textSize = 9.5f
            typeface = fonts.bold
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        for (i in rows.indices) {
            val (label, amount) = rows[i]
            val isGrandTotal = (i == 3)
            val isDue = (i == 5)

            if (isGrandTotal) {
                canvas.drawRect(boxX, rowY - 12f, boxX + boxWidth, rowY + 6f, Paint().apply { color = ColorBlueBg })
                canvas.drawText(label, boxX + 8f, rowY, boldLabelPaint)
                canvas.drawText(PdfCommon.formatMoney(amount, !isEnglish), boxX + boxWidth - 8f, rowY, grandTotalPaint)
            } else if (isDue) {
                canvas.drawText(label, boxX + 8f, rowY, boldLabelPaint)
                canvas.drawText(PdfCommon.formatMoney(amount, !isEnglish), boxX + boxWidth - 8f, rowY, duePaint)
            } else {
                canvas.drawText(label, boxX + 8f, rowY, labelPaint)
                val sign = if (amount < 0) "- " else ""
                val absMoney = PdfCommon.formatMoney(kotlin.math.abs(amount), !isEnglish)
                canvas.drawText("$sign$absMoney", boxX + boxWidth - 8f, rowY, rightValPaint)
            }
            rowY += 20f
        }

        return y + boxHeight + 14f
    }

    private fun drawTermsAndSignatures(
        canvas: Canvas,
        startY: Float,
        invoice: com.example.data.local.entity.InvoiceEntity,
        fonts: PdfCommon.Fonts,
        isEnglish: Boolean
    ) {
        var y = startY

        // Notes and payment method (Left)
        val leftWidth = CONTENT_WIDTH - 240f
        val notesRect = RectF(MARGIN, y, MARGIN + leftWidth, y + 60f)
        val noteBgPaint = Paint().apply { color = ColorGrayBg; style = Paint.Style.FILL }
        canvas.drawRoundRect(notesRect, 6f, 6f, noteBgPaint)

        val noteTitlePaint = Paint().apply {
            color = ColorNavyDark
            textSize = 8.5f
            typeface = fonts.bold
            isAntiAlias = true
        }
        canvas.drawText(if (isEnglish) "Notes & Payment Terms:" else "পেমেন্ট মাধ্যম ও শর্তাবলী:", MARGIN + 8f, y + 14f, noteTitlePaint)

        val noteBodyPaint = Paint().apply {
            color = ColorDarkText
            textSize = 7.5f
            typeface = fonts.regular
            isAntiAlias = true
        }
        val methodStr = invoice.paymentMethod?.let { "মাধ্যম: $it" } ?: ""
        canvas.drawText(methodStr, MARGIN + 8f, y + 26f, noteBodyPaint)

        val userNotes = invoice.notes?.take(50) ?: if (isEnglish) "Thank you for your business!" else "আমাদের সাথে লেনদেন করার জন্য ধন্যবাদ!"
        canvas.drawText(userNotes, MARGIN + 8f, y + 38f, noteBodyPaint)

        val terms = invoice.terms?.take(50) ?: if (isEnglish) "Standard goods return policy within 7 days." else "মাল পৌঁছানোর ৭ দিনের মধ্যে কোনো আপত্তি থাকলে জানাতে হবে।"
        canvas.drawText(terms, MARGIN + 8f, y + 50f, noteBodyPaint)

        // Signatures (Right & Left Bottom)
        val sigY = PAGE_HEIGHT - MARGIN - 38f
        val linePaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val sigTextPaint = Paint().apply {
            color = ColorDarkText
            textSize = 8f
            typeface = fonts.bold
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Left: Customer Signature
        val customerSigX = MARGIN + 80f
        canvas.drawLine(customerSigX - 60f, sigY, customerSigX + 60f, sigY, linePaint)
        canvas.drawText(if (isEnglish) "Customer Signature" else "গ্রাহকের স্বাক্ষর", customerSigX, sigY + 12f, sigTextPaint)

        // Right: Authorized Signature
        val authSigX = MARGIN + CONTENT_WIDTH - 80f
        canvas.drawLine(authSigX - 60f, sigY, authSigX + 60f, sigY, linePaint)
        canvas.drawText(if (isEnglish) "Authorized Signature" else "অনুমোদিত স্বাক্ষর", authSigX, sigY + 12f, sigTextPaint)
    }
}
