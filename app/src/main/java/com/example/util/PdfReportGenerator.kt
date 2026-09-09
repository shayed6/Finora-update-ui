package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.ui.screens.portfolio.PortfolioSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 36f

    fun exportSavingsGoalsPdf(
        context: Context,
        goals: List<SavingsGoalEntity>,
        useBengaliDigits: Boolean = true
    ) {
        if (goals.isEmpty()) {
            Toast.makeText(context, "কোন সঞ্চয় লক্ষ্য পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()

            // Calculate pagination: header takes ~200pt, each goal takes ~75pt, footer takes ~40pt
            val availableHeight = PAGE_HEIGHT - MARGIN * 2 - 200f
            val itemHeight = 72f
            val itemsPerPage = (availableHeight / itemHeight).toInt().coerceAtLeast(4)
            val totalPages = ((goals.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)

            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.img_finora_logo)
            } catch (e: Exception) {
                null
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val currentDateStr = dateFormat.format(Date())

            val totalTarget = goals.sumOf { it.targetAmount }
            val totalSaved = goals.sumOf { it.currentAmount }
            val remaining = (totalTarget - totalSaved).coerceAtLeast(0.0)
            val overallPercent = if (totalTarget > 0) (totalSaved / totalTarget) * 100.0 else 0.0
            val completedCount = goals.count { it.currentAmount >= it.targetAmount }

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw Background
                val bgPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

                // 2. Draw Finora Logo Watermark (Center of page, diagonal)
                drawFinoraWatermark(canvas, logoBitmap)

                // 3. Draw Header
                var currentY = drawHeader(canvas, logoBitmap, currentDateStr, pageIndex + 1, totalPages)

                // 4. Draw Summary Overview only on page 1
                if (pageIndex == 0) {
                    currentY = drawSummaryCards(
                        canvas = canvas,
                        startY = currentY,
                        totalTarget = totalTarget,
                        totalSaved = totalSaved,
                        remaining = remaining,
                        overallPercent = overallPercent,
                        completedCount = completedCount,
                        totalGoals = goals.size,
                        useBengaliDigits = useBengaliDigits
                    )
                }

                // 5. Draw Section Title for Goals
                val subheaderPaint = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("■ সঞ্চয় লক্ষ্যসমূহের বিবরণ (Savings Goals List)", MARGIN, currentY, subheaderPaint)
                currentY += 14f

                // Draw goals for this page
                val startIndex = pageIndex * itemsPerPage
                val endIndex = (startIndex + itemsPerPage).coerceAtMost(goals.size)
                val pageGoals = goals.subList(startIndex, endIndex)

                for (i in pageGoals.indices) {
                    val goal = pageGoals[i]
                    val goalIndex = startIndex + i + 1
                    currentY = drawGoalRow(
                        canvas = canvas,
                        startY = currentY,
                        goal = goal,
                        index = goalIndex,
                        useBengaliDigits = useBengaliDigits
                    )
                }

                // 6. Draw Footer
                drawFooter(canvas, pageIndex + 1, totalPages)

                pdfDocument.finishPage(page)
            }

            // Save PDF to cache directory
            val cacheDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val pdfFile = File(cacheDir, "Finora_Savings_Goals_Report.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Share PDF via FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                clipData = ClipData.newRawUri("Finora PDF", fileUri)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Finora সঞ্চয় লক্ষ্য PDF রিপোর্ট")
                putExtra(Intent.EXTRA_TEXT, "Finora অ্যাপ থেকে তৈরি আপনার ব্যক্তিগত সঞ্চয় লক্ষ্য ও অগ্রগতি PDF রিপোর্ট।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "PDF রিপোর্ট শেয়ার অথবা ওপেন করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "PDF রিপোর্ট সফলভাবে তৈরি হয়েছে", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "PDF তৈরি করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportPortfolioPdf(
        context: Context,
        holdings: List<HoldingEntity>,
        summary: PortfolioSummary,
        useBengaliDigits: Boolean = true
    ) {
        if (holdings.isEmpty()) {
            Toast.makeText(context, "কোনো শেয়ার বিনিয়োগ পাওয়া যায়নি। প্রথমে শেয়ার যোগ করুন।", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()

            // Available height for content
            val availableHeight = PAGE_HEIGHT - MARGIN * 2 - 200f
            val itemHeight = 72f
            val itemsPerPage = (availableHeight / itemHeight).toInt().coerceAtLeast(4)
            val totalPages = ((holdings.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)

            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.img_finora_logo)
            } catch (e: Exception) {
                null
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val currentDateStr = dateFormat.format(Date())

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw Background
                val bgPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

                // 2. Draw Finora Logo Watermark
                drawFinoraWatermark(canvas, logoBitmap)

                // 3. Draw Header
                var currentY = drawHeader(canvas, logoBitmap, currentDateStr, pageIndex + 1, totalPages, title = "শেয়ার পোর্টফোলিও রিপোর্ট", subTitle = "DSE ও CSE ইনভেস্টমেন্ট ও রিয়েল-টাইম পোর্টফোলিও বিবরণী")

                // 4. Draw Portfolio Summary Overview only on page 1
                if (pageIndex == 0) {
                    currentY = drawPortfolioSummaryCards(
                        canvas = canvas,
                        startY = currentY,
                        summary = summary,
                        totalHoldings = holdings.size,
                        useBengaliDigits = useBengaliDigits
                    )
                }

                // 5. Section Header for Holdings
                val subheaderPaint = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("■ শেয়ার হোল্ডিংস ও লাভ-ক্ষতির বিস্তারিত বিবরণ (Holdings List)", MARGIN, currentY, subheaderPaint)
                currentY += 14f

                // Draw holdings for this page
                val startIndex = pageIndex * itemsPerPage
                val endIndex = (startIndex + itemsPerPage).coerceAtMost(holdings.size)
                val pageHoldings = holdings.subList(startIndex, endIndex)

                for (i in pageHoldings.indices) {
                    val holding = pageHoldings[i]
                    val holdingIndex = startIndex + i + 1
                    currentY = drawHoldingRow(
                        canvas = canvas,
                        startY = currentY,
                        holding = holding,
                        index = holdingIndex,
                        useBengaliDigits = useBengaliDigits
                    )
                }

                // 6. Draw Footer
                drawFooter(canvas, pageIndex + 1, totalPages)

                pdfDocument.finishPage(page)
            }

            // Save PDF to cache directory
            val cacheDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val pdfFile = File(cacheDir, "Finora_Portfolio_Report.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Share PDF via FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                clipData = ClipData.newRawUri("Finora PDF", fileUri)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Finora শেয়ার পোর্টফোলিও PDF রিপোর্ট")
                putExtra(Intent.EXTRA_TEXT, "Finora অ্যাপ থেকে তৈরি আপনার ব্যক্তিগত শেয়ার পোর্টফোলিও ও মুনাফা বিবরণী PDF রিপোর্ট।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "PDF পোর্টফোলিও শেয়ার অথবা ওপেন করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "পোর্টফোলিও PDF রিপোর্ট সফলভাবে তৈরি হয়েছে", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "PDF তৈরি করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Draws a subtle, high-quality watermark featuring the Finora Logo and watermark branding.
     */
    private fun drawFinoraWatermark(canvas: Canvas, logoBitmap: Bitmap?) {
        canvas.save()

        val centerX = PAGE_WIDTH / 2f
        val centerY = PAGE_HEIGHT / 2f

        // Rotate canvas for diagonal watermark
        canvas.rotate(-32f, centerX, centerY)

        // 1. Draw Finora Logo Watermark in center
        if (logoBitmap != null) {
            val logoPaint = Paint().apply {
                alpha = 24 // very subtle watermark alpha (approx 9-10% opacity)
                isFilterBitmap = true
                isAntiAlias = true
            }
            val targetSize = 220
            val destRect = RectF(
                centerX - targetSize / 2f,
                centerY - targetSize / 2f - 30f,
                centerX + targetSize / 2f,
                centerY + targetSize / 2f - 30f
            )
            canvas.drawBitmap(logoBitmap, null, destRect, logoPaint)
        } else {
            // Fallback watermark shield icon
            val shieldPaint = Paint().apply {
                color = Color.parseColor("#2563EB")
                alpha = 20
                style = Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY - 30f, 90f, shieldPaint)
        }

        // 2. Watermark Text: "FINORA"
        val watermarkTextPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            alpha = 22 // Subtle opacity
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.2f
            isAntiAlias = true
        }
        canvas.drawText("FINORA", centerX, centerY + 70f, watermarkTextPaint)

        // 3. Watermark Subtitle: "SMART FINANCIAL REPORT"
        val watermarkSubPaint = Paint().apply {
            color = Color.parseColor("#2563EB")
            alpha = 20
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
            isAntiAlias = true
        }
        canvas.drawText("FINORA FINANCIAL REPORT", centerX, centerY + 94f, watermarkSubPaint)

        canvas.restore()
    }

    /**
     * Draws the top header bar with branding, app logo, and report title.
     */
    private fun drawHeader(
        canvas: Canvas,
        logoBitmap: Bitmap?,
        dateStr: String,
        currentPage: Int,
        totalPages: Int,
        title: String = "ব্যক্তিগত সঞ্চয় ও আর্থিক লক্ষ্য রিপোর্ট (Savings Goals Report)",
        subTitle: String = "১০০% অন-ডিভাইস নিরাপদ আর্থিক হিসাব"
    ): Float {
        var y = MARGIN + 10f

        // Top Accent Color Bar
        val accentBarPaint = Paint().apply {
            color = Color.parseColor("#2563EB") // Finora Primary Blue
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 4f, accentBarPaint)

        y = MARGIN + 22f

        // Small Logo Icon or Badge
        val logoSize = 38f
        if (logoBitmap != null) {
            val logoRect = RectF(MARGIN, y, MARGIN + logoSize, y + logoSize)
            val paint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
            canvas.drawBitmap(logoBitmap, null, logoRect, paint)
        } else {
            val badgePaint = Paint().apply {
                color = Color.parseColor("#2563EB")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(MARGIN, y, MARGIN + logoSize, y + logoSize, 8f, 8f, badgePaint)
        }

        // Title and App Name
        val brandPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("FINORA", MARGIN + logoSize + 10f, y + 16f, brandPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText(title, MARGIN + logoSize + 10f, y + 32f, titlePaint)

        // Right side: Date and Page Info
        val datePaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("তারিখ: $dateStr", PAGE_WIDTH - MARGIN, y + 16f, datePaint)
        canvas.drawText("পৃষ্ঠা $currentPage / $totalPages", PAGE_WIDTH - MARGIN, y + 32f, datePaint)

        // Divider Line
        y += logoSize + 12f
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)

        return y + 14f
    }

    /**
     * Draws the overview summary metric cards for the stock portfolio.
     */
    private fun drawPortfolioSummaryCards(
        canvas: Canvas,
        startY: Float,
        summary: PortfolioSummary,
        totalHoldings: Int,
        useBengaliDigits: Boolean
    ): Float {
        var y = startY

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("■ পোর্টফোলিও সামগ্রিক পরিস্থিতি (Portfolio Summary)", MARGIN, y, titlePaint)
        y += 10f

        val cardWidth = (PAGE_WIDTH - MARGIN * 2 - 24f) / 3f
        val cardHeight = 52f

        // Card 1: Total Invested
        drawMetricCard(
            canvas = canvas,
            x = MARGIN,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "মোট বিনিয়োগ",
            value = BengaliFormatter.formatTaka(summary.totalInvested, useBengaliDigits),
            accentColor = Color.parseColor("#2563EB")
        )

        // Card 2: Current Value
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + cardWidth + 12f,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "বর্তমান বাজারমূল্য",
            value = BengaliFormatter.formatTaka(summary.totalCurrentValue, useBengaliDigits),
            accentColor = Color.parseColor("#0F172A")
        )

        // Card 3: Unrealized Gain/Loss
        val isProfit = summary.unrealizedGainLoss >= 0.0
        val pnlPrefix = if (isProfit) "+" else ""
        val pnlColor = if (isProfit) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + (cardWidth + 12f) * 2f,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "নিট লাভ / ক্ষতি",
            value = "$pnlPrefix${BengaliFormatter.formatTaka(summary.unrealizedGainLoss, useBengaliDigits)}",
            accentColor = pnlColor
        )

        y += cardHeight + 10f

        // Return banner row
        val barRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f)
        val barBgPaint = Paint().apply {
            color = if (isProfit) Color.parseColor("#F0FDF4") else Color.parseColor("#FEF2F2")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(barRect, 6f, 6f, barBgPaint)

        val statTextPaint = Paint().apply {
            color = if (isProfit) Color.parseColor("#15803D") else Color.parseColor("#B91C1C")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("মোট কোম্পানি: ${BengaliFormatter.toBengaliDigits(totalHoldings.toString())}টি", MARGIN + 12f, y + 17f, statTextPaint)

        val returnText = "পোর্টফোলিও রিটার্ন: $pnlPrefix${BengaliFormatter.formatPercent(summary.unrealizedGainLossPercent, useBengaliDigits)}"
        val returnPaint = Paint().apply {
            color = if (isProfit) Color.parseColor("#15803D") else Color.parseColor("#B91C1C")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText(returnText, PAGE_WIDTH - MARGIN - 12f, y + 17f, returnPaint)

        return y + 36f
    }

    /**
     * Draws an individual holding row item.
     */
    private fun drawHoldingRow(
        canvas: Canvas,
        startY: Float,
        holding: HoldingEntity,
        index: Int,
        useBengaliDigits: Boolean
    ): Float {
        val rowHeight = 64f
        val cardRect = RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + rowHeight)

        val bgPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        // Index Badge
        val badgeX = MARGIN + 10f
        val badgeY = startY + 12f
        val badgeSize = 20f
        val badgePaint = Paint().apply {
            color = Color.parseColor("#EFF6FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize), 4f, 4f, badgePaint)

        val badgeTextPaint = Paint().apply {
            color = Color.parseColor("#2563EB")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(BengaliFormatter.toBengaliDigits(index.toString()), badgeX + badgeSize / 2f, badgeY + 14f, badgeTextPaint)

        // Stock Name & Exchange
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("${holding.stockName} [${holding.exchange}]", badgeX + badgeSize + 8f, startY + 22f, titlePaint)

        // Subtitle: Qty & Avg Buy Price
        val subPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawText("শেয়ার: ${BengaliFormatter.toBengaliDigits(holding.quantity.toString())}টি   |   গড় ক্রয়দর: ${BengaliFormatter.formatTaka(holding.averagePrice, useBengaliDigits)}", badgeX + badgeSize + 8f, startY + 36f, subPaint)

        // Current Price & Total Current Value
        val invested = holding.quantity * holding.averagePrice
        val currentVal = holding.quantity * holding.currentPrice
        val gainLoss = currentVal - invested
        val gainLossPct = if (invested > 0.0) (gainLoss / invested) * 100.0 else 0.0
        val isProfit = gainLoss >= 0.0
        val pnlPrefix = if (isProfit) "+" else ""

        val currentValPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawText("বর্তমান দর: ${BengaliFormatter.formatTaka(holding.currentPrice, useBengaliDigits)}   |   বর্তমান মান: ${BengaliFormatter.formatTaka(currentVal, useBengaliDigits)}", MARGIN + 10f, startY + 54f, currentValPaint)

        // Profit/Loss Badge on right
        val pnlTextColor = if (isProfit) Color.parseColor("#15803D") else Color.parseColor("#DC2626")
        val pnlText = "$pnlPrefix${BengaliFormatter.formatTaka(gainLoss, useBengaliDigits)} ($pnlPrefix${BengaliFormatter.formatPercent(gainLossPct, useBengaliDigits)})"
        val pnlTextPaint = Paint().apply {
            color = pnlTextColor
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText(pnlText, PAGE_WIDTH - MARGIN - 10f, startY + 24f, pnlTextPaint)

        val investPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("বিনিয়োগ: ${BengaliFormatter.formatTaka(invested, useBengaliDigits)}", PAGE_WIDTH - MARGIN - 10f, startY + 40f, investPaint)

        return startY + rowHeight + 8f
    }

    /**
     * Draws the 4 overview summary metric cards.
     */
    private fun drawSummaryCards(
        canvas: Canvas,
        startY: Float,
        totalTarget: Double,
        totalSaved: Double,
        remaining: Double,
        overallPercent: Double,
        completedCount: Int,
        totalGoals: Int,
        useBengaliDigits: Boolean
    ): Float {
        var y = startY

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("■ সামগ্রিক সঞ্চয় পরিস্থিতি (Executive Summary)", MARGIN, y, titlePaint)
        y += 10f

        val cardWidth = (PAGE_WIDTH - MARGIN * 2 - 24f) / 3f
        val cardHeight = 52f

        // Card 1: Total Target
        drawMetricCard(
            canvas = canvas,
            x = MARGIN,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "মোট লক্ষ্যমাত্রা",
            value = BengaliFormatter.formatTaka(totalTarget, useBengaliDigits),
            accentColor = Color.parseColor("#2563EB")
        )

        // Card 2: Total Saved
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + cardWidth + 12f,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "মোট সঞ্চিত অর্থ",
            value = BengaliFormatter.formatTaka(totalSaved, useBengaliDigits),
            accentColor = Color.parseColor("#16A34A")
        )

        // Card 3: Remaining
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + (cardWidth + 12f) * 2f,
            y = y,
            width = cardWidth,
            height = cardHeight,
            label = "অবশিষ্ট সঞ্চয়",
            value = BengaliFormatter.formatTaka(remaining, useBengaliDigits),
            accentColor = Color.parseColor("#D97706")
        )

        y += cardHeight + 10f

        // Progress Bar Row
        val barRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f)
        val barBgPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(barRect, 6f, 6f, barBgPaint)

        val progressFillWidth = ((barRect.width() - 8f) * (overallPercent.coerceIn(0.0, 100.0) / 100.0)).toFloat()
        if (progressFillWidth > 0) {
            val fillPaint = Paint().apply {
                color = Color.parseColor("#16A34A") // Growth Green
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(
                RectF(barRect.left + 4f, barRect.top + 4f, barRect.left + 4f + progressFillWidth, barRect.bottom - 4f),
                4f, 4f, fillPaint
            )
        }

        val progressTextPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val pctStr = "অগ্রগতি: ${BengaliFormatter.formatPercent(overallPercent, useBengaliDigits)} | মোট লক্ষ্য: ${BengaliFormatter.toBengaliDigits(totalGoals.toString())}টি (সম্পন্ন: ${BengaliFormatter.toBengaliDigits(completedCount.toString())}টি)"
        canvas.drawText(pctStr, MARGIN + 10f, y + 17f, progressTextPaint)

        return y + 36f
    }

    private fun drawMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        label: String,
        value: String,
        accentColor: Int
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        // Outline Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        // Left accent bar
        val accentPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(x, y, x + 4f, y + height), 4f, 4f, accentPaint)

        // Label
        val labelPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText(label, x + 10f, y + 18f, labelPaint)

        // Value
        val valPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(value, x + 10f, y + 38f, valPaint)
    }

    /**
     * Draws a single goal entry with title, category, metrics, progress bar, and status badge.
     */
    private fun drawGoalRow(
        canvas: Canvas,
        startY: Float,
        goal: SavingsGoalEntity,
        index: Int,
        useBengaliDigits: Boolean
    ): Float {
        val rowHeight = 64f
        val rect = RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + rowHeight)

        // Background Card
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        // Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val isCompleted = goal.currentAmount >= goal.targetAmount
        val progressPercent = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100.0 else 0.0

        // Left Index Badge
        val badgeX = MARGIN + 8f
        val badgeY = startY + 12f
        val badgeSize = 20f
        val badgePaint = Paint().apply {
            color = if (isCompleted) Color.parseColor("#DCFCE7") else Color.parseColor("#EFF6FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize), 4f, 4f, badgePaint)

        val badgeTextPaint = Paint().apply {
            color = if (isCompleted) Color.parseColor("#16A34A") else Color.parseColor("#2563EB")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(BengaliFormatter.toBengaliDigits(index.toString()), badgeX + badgeSize / 2f, badgeY + 14f, badgeTextPaint)

        // Goal Title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val safeTitle = if (goal.title.length > 30) goal.title.take(30) + "..." else goal.title
        canvas.drawText(safeTitle, badgeX + badgeSize + 8f, startY + 22f, titlePaint)

        // Category Tag
        val categoryPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawText("• ক্যাটাগরি: ${goal.category}", badgeX + badgeSize + 8f, startY + 36f, categoryPaint)

        // Status Badge (Right side)
        val statusText = if (isCompleted) "অর্জিত (Done)" else "চলমান (In Progress)"
        val statusBgColor = if (isCompleted) Color.parseColor("#DCFCE7") else Color.parseColor("#FEF3C7")
        val statusTextColor = if (isCompleted) Color.parseColor("#15803D") else Color.parseColor("#B45309")

        val statusPaint = Paint().apply {
            color = statusTextColor
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText(statusText, PAGE_WIDTH - MARGIN - 10f, startY + 22f, statusPaint)

        // Deadline / Target Date
        val datePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("টার্গেট: ${goal.targetDate}", PAGE_WIDTH - MARGIN - 10f, startY + 36f, datePaint)

        // Amounts Row (Target / Saved / Progress %)
        val amountsPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9.5f
            isAntiAlias = true
        }
        val amountStr = "টার্গেট: ${BengaliFormatter.formatTaka(goal.targetAmount, useBengaliDigits)}   |   সঞ্চিত: ${BengaliFormatter.formatTaka(goal.currentAmount, useBengaliDigits)} (${BengaliFormatter.formatPercent(progressPercent, useBengaliDigits)})"
        canvas.drawText(amountStr, MARGIN + 10f, startY + 54f, amountsPaint)

        // Mini progress bar in goal card
        val pBarLeft = PAGE_WIDTH - MARGIN - 130f
        val pBarTop = startY + 46f
        val pBarWidth = 120f
        val pBarHeight = 7f

        val trackPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(pBarLeft, pBarTop, pBarLeft + pBarWidth, pBarTop + pBarHeight), 3.5f, 3.5f, trackPaint)

        val fillW = (pBarWidth * (progressPercent.coerceIn(0.0, 100.0) / 100.0)).toFloat()
        if (fillW > 0) {
            val fillPaint = Paint().apply {
                color = if (isCompleted) Color.parseColor("#16A34A") else Color.parseColor("#2563EB")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(pBarLeft, pBarTop, pBarLeft + fillW, pBarTop + pBarHeight), 3.5f, 3.5f, fillPaint)
        }

        return startY + rowHeight + 8f
    }

    /**
     * Draws the footer section on each page.
     */
    private fun drawFooter(canvas: Canvas, currentPage: Int, totalPages: Int) {
        val y = PAGE_HEIGHT - MARGIN - 8f

        // Top line
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y - 10f, linePaint)

        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
            isAntiAlias = true
        }
        canvas.drawText("Finora — আপনার স্মার্ট ব্যক্তিগত অর্থায়ন ও সঞ্চয় সহায়ক | ১০০% অন-ডিভাইস নিরাপদ হিসাব", MARGIN, y + 2f, footerPaint)

        val pagePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("পৃষ্ঠা $currentPage / $totalPages", PAGE_WIDTH - MARGIN, y + 2f, pagePaint)
    }
}
