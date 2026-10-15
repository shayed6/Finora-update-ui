package com.example.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.util.BengaliFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Native PDF generation and table rendering engine for Finora.
 * Built using native [android.graphics.pdf.PdfDocument] and [Canvas]
 * with first-class bundled Noto Sans Bengali font support.
 */
class PdfEngine(
    private val context: Context,
    val pageWidth: Int = 595,  // A4 standard portrait width in points
    val pageHeight: Int = 842, // A4 standard portrait height in points
    val margin: Float = 36f    // 0.5 inch margins
) : AutoCloseable {

    val contentWidth: Float = pageWidth - (margin * 2)

    val pdfDocument: PdfDocument = PdfDocument()

    // Bundled Bengali Fonts
    val regularFont: Typeface = try {
        ResourcesCompat.getFont(context, R.font.noto_sans_bengali_regular) ?: Typeface.DEFAULT
    } catch (_: Exception) {
        Typeface.DEFAULT
    }

    val boldFont: Typeface = try {
        ResourcesCompat.getFont(context, R.font.noto_sans_bengali_semibold) ?: Typeface.DEFAULT_BOLD
    } catch (_: Exception) {
        Typeface.DEFAULT_BOLD
    }

    // Finora Corporate Palette
    val colorNavyDark = Color.parseColor("#0F172A")
    val colorNavyAccent = Color.parseColor("#1E3A8A")
    val colorBluePrimary = Color.parseColor("#2563EB")
    val colorBlueBg = Color.parseColor("#EFF6FF")
    val colorGreenIncome = Color.parseColor("#16A34A")
    val colorGreenBg = Color.parseColor("#F0FDF4")
    val colorRedExpense = Color.parseColor("#DC2626")
    val colorRedBg = Color.parseColor("#FEF2F2")
    val colorGrayBorder = Color.parseColor("#E2E8F0")
    val colorGrayBg = Color.parseColor("#F8FAFC")
    val colorGrayText = Color.parseColor("#64748B")
    val colorDarkText = Color.parseColor("#1E293B")

    val logoBitmap: Bitmap? = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.img_finora_logo)
    } catch (_: Exception) {
        null
    }

    data class TableColumn(
        val title: String,
        val weight: Float = 1f,
        val alignment: Paint.Align = Paint.Align.LEFT
    )

    data class TableCell(
        val text: String,
        val alignment: Paint.Align? = null,
        val textColor: Int? = null,
        val isBold: Boolean = false
    )

    data class TableRow(
        val cells: List<TableCell>,
        val isHeader: Boolean = false,
        val backgroundColor: Int? = null,
        val height: Float = 24f
    )

    fun startPage(pageNumber: Int): Pair<PdfDocument.Page, Canvas> {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Clean white page background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        return Pair(page, canvas)
    }

    fun finishPage(page: PdfDocument.Page) {
        pdfDocument.finishPage(page)
    }

    fun drawWatermark(canvas: Canvas) {
        canvas.save()
        val centerX = pageWidth / 2f
        val centerY = pageHeight / 2f
        canvas.rotate(-30f, centerX, centerY)

        if (logoBitmap != null) {
            val watermarkLogoSize = 130f
            val logoRect = RectF(
                centerX - watermarkLogoSize / 2,
                centerY - watermarkLogoSize / 2 - 25f,
                centerX + watermarkLogoSize / 2,
                centerY + watermarkLogoSize / 2 - 25f
            )
            val logoPaint = Paint().apply {
                alpha = 14
                isFilterBitmap = true
            }
            canvas.drawBitmap(logoBitmap, null, logoRect, logoPaint)
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            alpha = 18
            textSize = 34f
            typeface = boldFont
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("FINORA FINANCE", centerX, centerY + 65f, textPaint)
        canvas.restore()
    }

    fun drawHeader(
        canvas: Canvas,
        title: String,
        subtitle: String? = null,
        dateText: String? = null,
        pageNumber: Int = 1,
        totalPages: Int = 1
    ): Float {
        var y = margin

        // Finora Emblem / Logo
        if (logoBitmap != null) {
            val logoRect = RectF(margin, y, margin + 40f, y + 40f)
            canvas.drawBitmap(logoBitmap, null, logoRect, Paint().apply { isFilterBitmap = true })
        }

        // Title with Noto Sans Bengali font
        val titlePaint = Paint().apply {
            color = colorNavyDark
            textSize = 15f
            typeface = boldFont
            isAntiAlias = true
        }
        canvas.drawText(title, margin + 48f, y + 16f, titlePaint)

        // Subtitle
        if (subtitle != null) {
            val subtitlePaint = Paint().apply {
                color = colorGrayText
                textSize = 8.5f
                typeface = regularFont
                isAntiAlias = true
            }
            canvas.drawText(subtitle, margin + 48f, y + 28f, subtitlePaint)
        }

        // Top right metadata (Date and Page indicator)
        val metaPaint = Paint().apply {
            color = colorGrayText
            textSize = 7.5f
            typeface = regularFont
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        if (dateText != null) {
            canvas.drawText("তারিখ: $dateText", margin + contentWidth, y + 14f, metaPaint)
        }
        canvas.drawText("পৃষ্ঠা $pageNumber / $totalPages", margin + contentWidth, y + 26f, metaPaint)

        y += 44f

        // Top Header Divider Line
        val linePaint = Paint().apply {
            color = colorBluePrimary
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)

        return y + 14f
    }

    fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        totalPages: Int,
        isEnglish: Boolean = false
    ) {
        val y = pageHeight - margin + 8f

        val borderPaint = Paint().apply {
            color = colorGrayBorder
            strokeWidth = 0.8f
        }
        canvas.drawLine(margin, y - 10f, margin + contentWidth, y - 10f, borderPaint)

        val footerTextPaint = Paint().apply {
            color = colorGrayText
            textSize = 7.5f
            typeface = regularFont
            isAntiAlias = true
        }

        val brandText = if (isEnglish) {
            "Generated securely by Finora App • 100% On-Device Financial Manager"
        } else {
            "Finora অ্যাপ দ্বারা অন-ডিভাইস শতভাগ নিরাপদ ও নির্ভুলভাবে প্রস্তুতকৃত"
        }
        canvas.drawText(brandText, margin, y + 4f, footerTextPaint)

        val pageStr = if (isEnglish) {
            "Page $pageNumber of $totalPages"
        } else {
            "পৃষ্ঠা ${BengaliFormatter.englishToBengaliDigits("$pageNumber")} এর ${BengaliFormatter.englishToBengaliDigits("$totalPages")}"
        }
        val rightTextPaint = Paint().apply {
            color = colorGrayText
            textSize = 7.5f
            typeface = regularFont
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText(pageStr, margin + contentWidth, y + 4f, rightTextPaint)
    }

    /**
     * Renders a structured data table using native Canvas drawing
     * with full Noto Sans Bengali font shaping support.
     */
    fun renderTable(
        canvas: Canvas,
        startY: Float,
        columns: List<TableColumn>,
        rows: List<TableRow>,
        customContentWidth: Float = contentWidth,
        leftMargin: Float = margin
    ): Float {
        var y = startY
        val totalWeight = columns.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
        val colWidths = columns.map { (it.weight / totalWeight) * customContentWidth }

        // Header Paints
        val headerBgPaint = Paint().apply {
            color = colorNavyDark
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = boldFont
            isAntiAlias = true
        }

        // Draw Table Header
        val headerHeight = 22f
        val headerRect = RectF(leftMargin, y, leftMargin + customContentWidth, y + headerHeight)
        canvas.drawRoundRect(headerRect, 4f, 4f, headerBgPaint)

        var colX = leftMargin
        for (i in columns.indices) {
            val col = columns[i]
            val w = colWidths[i]

            val textX = when (col.alignment) {
                Paint.Align.RIGHT -> colX + w - 6f
                Paint.Align.CENTER -> colX + (w / 2f)
                else -> colX + 6f
            }
            headerTextPaint.textAlign = col.alignment
            canvas.drawText(col.title, textX, y + 15f, headerTextPaint)
            colX += w
        }

        y += headerHeight

        // Row Paints
        val regularTextPaint = Paint().apply {
            color = colorDarkText
            textSize = 8.5f
            typeface = regularFont
            isAntiAlias = true
        }
        val boldRowTextPaint = Paint().apply {
            color = colorDarkText
            textSize = 8.5f
            typeface = boldFont
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = colorGrayBorder
            strokeWidth = 0.6f
        }
        val zebraBgPaint = Paint().apply {
            color = colorGrayBg
            style = Paint.Style.FILL
        }

        // Draw Table Rows
        for (rowIndex in rows.indices) {
            val row = rows[rowIndex]
            val rowHeight = row.height

            // Zebra background striping
            if (row.backgroundColor != null) {
                val rowRect = RectF(leftMargin, y, leftMargin + customContentWidth, y + rowHeight)
                canvas.drawRect(rowRect, Paint().apply { color = row.backgroundColor; style = Paint.Style.FILL })
            } else if (rowIndex % 2 == 1) {
                val rowRect = RectF(leftMargin, y, leftMargin + customContentWidth, y + rowHeight)
                canvas.drawRect(rowRect, zebraBgPaint)
            }

            colX = leftMargin
            for (colIndex in columns.indices) {
                val w = colWidths[colIndex]
                val defaultAlign = columns[colIndex].alignment
                val cell = row.cells.getOrNull(colIndex)

                if (cell != null) {
                    val align = cell.alignment ?: defaultAlign
                    val textPaint = if (cell.isBold) boldRowTextPaint else regularTextPaint
                    textPaint.textAlign = align
                    textPaint.color = cell.textColor ?: colorDarkText

                    val textX = when (align) {
                        Paint.Align.RIGHT -> colX + w - 6f
                        Paint.Align.CENTER -> colX + (w / 2f)
                        else -> colX + 6f
                    }

                    canvas.drawText(cell.text, textX, y + (rowHeight / 2f) + 3.5f, textPaint)
                }

                colX += w
            }

            // Bottom border line for each row
            canvas.drawLine(leftMargin, y + rowHeight, leftMargin + customContentWidth, y + rowHeight, borderPaint)
            y += rowHeight
        }

        return y + 8f
    }

    /**
     * Convenience table helper for 2D list of strings with Bengali typography.
     */
    fun renderSimpleTable(
        canvas: Canvas,
        startY: Float,
        headers: List<String>,
        rows: List<List<String>>,
        weights: List<Float>? = null,
        alignments: List<Paint.Align>? = null
    ): Float {
        val count = headers.size
        val columns = headers.mapIndexed { index, header ->
            TableColumn(
                title = header,
                weight = weights?.getOrNull(index) ?: 1f,
                alignment = alignments?.getOrNull(index) ?: Paint.Align.LEFT
            )
        }

        val tableRows = rows.map { rowStrings ->
            TableRow(
                cells = rowStrings.mapIndexed { colIndex, str ->
                    TableCell(
                        text = str,
                        alignment = alignments?.getOrNull(colIndex)
                    )
                }
            )
        }

        return renderTable(canvas, startY, columns, tableRows)
    }

    fun save(outputFile: File): File {
        val parent = outputFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val outputStream = FileOutputStream(outputFile)
        writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        return outputFile
    }

    fun writeTo(outputStream: OutputStream) {
        pdfDocument.writeTo(outputStream)
    }

    override fun close() {
        try {
            pdfDocument.close()
        } catch (_: Exception) {
        }
    }
}
