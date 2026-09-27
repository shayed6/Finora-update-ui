package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.HoldingEntity
import com.example.ui.screens.portfolio.HoldingWithDividends
import com.example.ui.screens.portfolio.PortfolioSummary
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting current stock portfolio status and unrealized gain/loss data
 * to a local CSV file for user records, Excel, and spreadsheet analysis.
 */
object CsvReportGenerator {

    /**
     * Generates a CSV file containing holding details, current market prices,
     * unrealized profit/loss, dividends, and total portfolio summary.
     */
    fun exportPortfolioCsv(
        context: Context,
        holdings: List<HoldingEntity>,
        summary: PortfolioSummary,
        useBengaliDigits: Boolean = false,
        holdingsWithDividends: List<HoldingWithDividends> = emptyList()
    ): File? {
        if (holdings.isEmpty() && holdingsWithDividends.isEmpty()) {
            Toast.makeText(context, "কোন পোর্টফোলিও হোল্ডিং পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val displayDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val fileName = "Finora_Portfolio_$timestamp.csv"

            // Save to app's files directory (which is exposed via FileProvider)
            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val csvFile = File(exportDir, fileName)

            // UTF-8 with BOM (\uFEFF) ensures Excel correctly recognizes UTF-8 text
            FileOutputStream(csvFile).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Write BOM
                    writer.write("\uFEFF")

                    // Section 1: Metadata header
                    writer.write("Finora Portfolio Status & Unrealized Gain/Loss Report\n")
                    writer.write("Generated Date,\"$displayDate\"\n")
                    writer.write("App Version,\"Finora 1.0 (100% On-Device Secure)\"\n\n")

                    // Section 2: Table Columns
                    val headers = listOf(
                        "Exchange",
                        "Stock Symbol",
                        "Quantity (Shares)",
                        "Avg Buying Price (BDT)",
                        "Total Investment (BDT)",
                        "Current Market Price (BDT)",
                        "Current Market Value (BDT)",
                        "Unrealized Gain/Loss (BDT)",
                        "Unrealized Gain/Loss (%)",
                        "Dividends Received (BDT)",
                        "Dividend Count",
                        "Net Return with Dividends (BDT)",
                        "Price Status"
                    )
                    writer.write(headers.joinToString(separator = ",", postfix = "\n") { escapeCsv(it) })

                    // Section 3: Data Rows
                    val items = if (holdingsWithDividends.isNotEmpty()) {
                        holdingsWithDividends
                    } else {
                        holdings.map { h ->
                            HoldingWithDividends(
                                holding = h,
                                totalDividend = 0.0,
                                dividendCount = 0,
                                currentPrice = null,
                                unrealizedGainLoss = null,
                                unrealizedGainLossPercent = null,
                                isPriceOutdated = false
                            )
                        }
                    }

                    for (item in items) {
                        val h = item.holding
                        val totalCost = h.quantity * h.averagePrice
                        val currentPrice = item.currentPrice
                        val currentVal = if (currentPrice != null && currentPrice > 0.0) h.quantity * currentPrice else null
                        val ugl = item.unrealizedGainLoss
                        val uglPct = item.unrealizedGainLossPercent
                        val netReturn = if (currentVal != null) (currentVal - totalCost + item.totalDividend) else null

                        val status = when {
                            currentPrice == null || currentPrice <= 0.0 -> "Price Unavailable"
                            item.isPriceOutdated -> "Outdated (>5m)"
                            else -> "Live Scraped"
                        }

                        val row = listOf(
                            h.exchange.uppercase(),
                            h.stockName,
                            h.quantity.toString(),
                            String.format(Locale.US, "%.2f", h.averagePrice),
                            String.format(Locale.US, "%.2f", totalCost),
                            if (currentPrice != null && currentPrice > 0.0) String.format(Locale.US, "%.2f", currentPrice) else "—",
                            if (currentVal != null) String.format(Locale.US, "%.2f", currentVal) else "—",
                            if (ugl != null) String.format(Locale.US, "%+.2f", ugl) else "—",
                            if (uglPct != null) String.format(Locale.US, "%+.2f%%", uglPct) else "—",
                            String.format(Locale.US, "%.2f", item.totalDividend),
                            item.dividendCount.toString(),
                            if (netReturn != null) String.format(Locale.US, "%+.2f", netReturn) else "—",
                            status
                        )
                        writer.write(row.joinToString(separator = ",", postfix = "\n") { escapeCsv(it) })
                    }

                    // Section 4: Summary Totals
                    writer.write("\n")
                    writer.write("PORTFOLIO SUMMARY METRICS,VALUE\n")
                    writer.write("Total Invested Capital (BDT),\"${String.format(Locale.US, "%.2f", summary.totalInvested)}\"\n")
                    val curValStr = if (summary.hasAnyLivePrice) String.format(Locale.US, "%.2f", summary.currentTotalValue) else "—"
                    writer.write("Total Current Market Value (BDT),\"$curValStr\"\n")

                    val totUglStr = if (summary.hasAnyLivePrice) String.format(Locale.US, "%+.2f", summary.totalUnrealizedGainLoss) else "—"
                    writer.write("Total Unrealized Gain/Loss (BDT),\"$totUglStr\"\n")

                    val totUglPctStr = if (summary.hasAnyLivePrice && summary.totalInvested > 0.0) {
                        String.format(Locale.US, "%+.2f%%", (summary.totalUnrealizedGainLoss / summary.totalInvested) * 100.0)
                    } else "—"
                    writer.write("Total Unrealized Gain/Loss (%),\"$totUglPctStr\"\n")

                    writer.write("Total Dividend Income Received (BDT),\"${String.format(Locale.US, "%.2f", summary.totalDividend)}\"\n")

                    val netRetStr = if (summary.hasAnyLivePrice) {
                        String.format(Locale.US, "%+.2f", summary.totalUnrealizedGainLoss + summary.totalDividend)
                    } else {
                        String.format(Locale.US, "%.2f", summary.totalDividend)
                    }
                    writer.write("Net Return with Dividends (BDT),\"$netRetStr\"\n")

                    writer.flush()
                }
            }

            // Share / Export via FileProvider so user can save or open in Sheets/Excel/Drive/Telegram
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                clipData = ClipData.newRawUri("Finora CSV", fileUri)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Finora শেয়ার পোর্টফোলিও CSV রেকর্ড ($timestamp)")
                putExtra(Intent.EXTRA_TEXT, "Finora অ্যাপ থেকে এক্সপোর্ট করা পোর্টফোলিও স্ট্যাটাস ও অবাস্তবায়িত লাভ/ক্ষতির CSV ফাইল।")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "পোর্টফোলিও CSV সংরক্ষণ বা ওপেন করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "পোর্টফোলিও CSV সফলভাবে তৈরি হয়েছে ($fileName)", Toast.LENGTH_SHORT).show()

            return csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "CSV তৈরিতে ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
