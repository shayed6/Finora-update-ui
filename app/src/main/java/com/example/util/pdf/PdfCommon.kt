package com.example.util.pdf

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.preferences.OrganizationInfo
import com.example.data.preferences.OrganizationInfoRepository
import com.example.util.BengaliFormatter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfCommon {

    const val PAGE_WIDTH = 595 // A4 standard portrait width in points
    const val PAGE_HEIGHT = 842 // A4 standard portrait height in points
    const val MARGIN = 36f // 0.5 inch margins
    const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2 // 523f

    // Color Palette
    val ColorNavyDark = Color.parseColor("#0F172A")
    val ColorNavyAccent = Color.parseColor("#1E3A8A")
    val ColorBluePrimary = Color.parseColor("#2563EB")
    val ColorBlueBg = Color.parseColor("#EFF6FF")
    val ColorGreenIncome = Color.parseColor("#16A34A")
    val ColorGreenBg = Color.parseColor("#F0FDF4")
    val ColorRedExpense = Color.parseColor("#DC2626")
    val ColorRedBg = Color.parseColor("#FEF2F2")
    val ColorGrayBorder = Color.parseColor("#E2E8F0")
    val ColorGrayBg = Color.parseColor("#F8FAFC")
    val ColorGrayText = Color.parseColor("#64748B")
    val ColorDarkText = Color.parseColor("#1E293B")

    class Fonts(
        val regular: Typeface,
        val bold: Typeface
    )

    fun loadFonts(context: Context): Fonts {
        val regular = try {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali_regular) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
        val bold = try {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali_semibold) ?: Typeface.DEFAULT_BOLD
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
        return Fonts(regular, bold)
    }

    fun loadLogo(context: Context): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_finora_logo)
        } catch (_: Exception) {
            null
        }
    }

    fun getReportsDir(context: Context): File {
        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun formatDate(timestamp: Long, isEnglish: Boolean = false): String {
        val pattern = if (isEnglish) "dd MMM yyyy, hh:mm a" else "dd MMM yyyy, hh:mm a"
        val sdf = SimpleDateFormat(pattern, if (isEnglish) Locale.ENGLISH else Locale.getDefault())
        val str = sdf.format(Date(timestamp))
        return if (!isEnglish) BengaliFormatter.formatBengaliDate(timestamp) else str
    }

    fun formatMoney(paisa: Long, useBengaliDigits: Boolean = true): String {
        val taka = paisa / 100.0
        return if (useBengaliDigits) {
            BengaliFormatter.formatTaka(taka)
        } else {
            String.format(Locale.US, "৳ %,.2f", taka)
        }
    }

    fun drawWatermark(canvas: Canvas, logoBitmap: Bitmap?) {
        canvas.save()
        val centerX = PAGE_WIDTH / 2f
        val centerY = PAGE_HEIGHT / 2f
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

        val watermarkTextPaint = Paint().apply {
            color = ColorNavyDark
            alpha = 16
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.2f
            isAntiAlias = true
        }
        canvas.drawText("FINORA", centerX, centerY + 65f, watermarkTextPaint)

        val watermarkSubPaint = Paint().apply {
            color = ColorBluePrimary
            alpha = 18
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
            isAntiAlias = true
        }
        canvas.drawText("FINANCIAL STATEMENT", centerX, centerY + 88f, watermarkSubPaint)

        canvas.restore()
    }

    fun drawHeader(
        canvas: Canvas,
        logoBitmap: Bitmap?,
        fonts: Fonts,
        title: String,
        subTitle: String?,
        dateStr: String,
        currentPage: Int,
        totalPages: Int,
        orgInfo: OrganizationInfo? = null
    ): Float {
        var y = MARGIN

        // Top accent line
        val accentBarPaint = Paint().apply {
            color = ColorBluePrimary
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 4f, accentBarPaint)

        y += 18f

        val hasOrg = orgInfo != null && orgInfo.name.isNotBlank()
        val headlineText = if (hasOrg) orgInfo!!.name.trim() else "FINORA"

        // Headline (Organization name if inputted, else FINORA) - without Finora logo
        val brandPaint = Paint().apply {
            color = ColorNavyDark
            textSize = if (hasOrg) 16f else 17f
            typeface = fonts.bold
            isAntiAlias = true
        }
        canvas.drawText(headlineText, MARGIN, y + 14f, brandPaint)

        // Contact / Address line if organization has address or phone
        val contactLine = if (hasOrg) {
            val parts = mutableListOf<String>()
            if (orgInfo!!.address.isNotBlank()) parts.add(orgInfo.address.trim())
            if (orgInfo.phone.isNotBlank()) parts.add("মোবাইল: ${orgInfo.phone.trim()}")
            if (parts.isNotEmpty()) parts.joinToString(" • ") else null
        } else null

        if (contactLine != null) {
            val contactPaint = Paint().apply {
                color = ColorGrayText
                textSize = 8.5f
                typeface = fonts.regular
                isAntiAlias = true
            }
            canvas.drawText(contactLine.take(80), MARGIN, y + 26f, contactPaint)

            val titlePaint = Paint().apply {
                color = ColorBluePrimary
                textSize = 10f
                typeface = fonts.bold
                isAntiAlias = true
            }
            val displayTitle = if (!subTitle.isNullOrBlank()) "$title • $subTitle" else title
            canvas.drawText(displayTitle.take(75), MARGIN, y + 39f, titlePaint)
        } else {
            val titlePaint = Paint().apply {
                color = ColorDarkText
                textSize = 10.5f
                typeface = fonts.bold
                isAntiAlias = true
            }
            canvas.drawText(title, MARGIN, y + 28f, titlePaint)

            if (!subTitle.isNullOrBlank()) {
                val subPaint = Paint().apply {
                    color = ColorGrayText
                    textSize = 8.5f
                    typeface = fonts.regular
                    isAntiAlias = true
                }
                canvas.drawText(subTitle, MARGIN, y + 40f, subPaint)
            }
        }

        // Right side: Date and Page Info
        val datePaint = Paint().apply {
            color = ColorGrayText
            textSize = 9f
            typeface = fonts.regular
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("তারিখ: $dateStr", PAGE_WIDTH - MARGIN, y + 14f, datePaint)
        canvas.drawText("পৃষ্ঠা $currentPage / $totalPages", PAGE_WIDTH - MARGIN, y + 28f, datePaint)

        // Divider Line
        y += 46f
        val linePaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)

        return y + 14f
    }

    fun drawFooter(
        canvas: Canvas,
        fonts: Fonts,
        currentPage: Int,
        totalPages: Int,
        isEnglish: Boolean = false
    ) {
        val y = PAGE_HEIGHT - MARGIN + 10f
        val linePaint = Paint().apply {
            color = ColorGrayBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(MARGIN, y - 12f, PAGE_WIDTH - MARGIN, y - 12f, linePaint)

        val footerTextPaint = Paint().apply {
            color = ColorGrayText
            textSize = 8.5f
            typeface = fonts.regular
            isAntiAlias = true
        }
        val disclaimerText = "উক্ত হিসাব সাথে ফিনোরা কোন ভাবে সংযুক্ত নয়"
        canvas.drawText(disclaimerText, MARGIN, y, footerTextPaint)

        val pagePaint = Paint().apply {
            color = ColorNavyDark
            textSize = 8.5f
            typeface = fonts.bold
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val pageStr = if (isEnglish) {
            "Page $currentPage of $totalPages"
        } else {
            "পৃষ্ঠা $currentPage / $totalPages"
        }
        canvas.drawText(pageStr, PAGE_WIDTH - MARGIN, y, pagePaint)
    }

    // --- Sharing and Printing ---
    fun sharePdf(context: Context, file: File, subject: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                clipData = ClipData.newRawUri("Finora PDF", uri)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "$subject\n\nFinora ফাইন্যান্স অ্যাপ থেকে তৈরিকৃত অফিসিয়াল রিপোর্ট।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "PDF শেয়ার বা ওপেন করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "শেয়ার করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printPdf(context: Context, file: File, jobName: String) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "প্রিন্ট সেবা পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                return
            }
            printManager.print(
                jobName,
                PdfPrintDocumentAdapter(file),
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .build()
            )
        } catch (e: Exception) {
            Toast.makeText(context, "প্রিন্ট শুরু করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyFileToUri(context: Context, sourceFile: File, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    class PdfPrintDocumentAdapter(private val pdfFile: File) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }
            val pdi = PrintDocumentInfo.Builder(pdfFile.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build()
            callback?.onLayoutFinished(pdi, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            try {
                FileInputStream(pdfFile).use { input ->
                    FileOutputStream(destination?.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback?.onWriteFailed(e.message)
            }
        }
    }
}
