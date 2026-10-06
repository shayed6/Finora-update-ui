package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.util.BengaliFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PortfolioWidgetProvider : AppWidgetProvider() {

    private val providerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_portfolio)

        // PendingIntent to launch MainActivity with ACTION_OPEN_PORTFOLIO
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_PORTFOLIO
            putExtra(EXTRA_NAVIGATE_TO, DESTINATION_PORTFOLIO)
            putExtra(EXTRA_FORCE_SCRAPE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Immediate initial render so the home screen host has valid views immediately
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Load cached Room data asynchronously
        providerScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val holdings = db.portfolioDao().getAllHoldingsList()
                val livePrices = db.livePriceDao().getAllLivePricesList()

                val priceMap = livePrices.associateBy {
                    "${it.exchange.uppercase().trim()}:${it.symbol.uppercase().trim()}"
                }

                if (holdings.isEmpty()) {
                    views.setTextViewText(R.id.widget_holding_count, "০ টি")
                    views.setTextViewText(R.id.widget_total_value, "৳ ০.০০")
                    views.setTextViewText(R.id.widget_unrealized_gl, "—")
                    views.setTextColor(
                        R.id.widget_unrealized_gl,
                        ContextCompat.getColor(context, R.color.finora_text_secondary)
                    )
                } else {
                    val countStr = "${BengaliFormatter.toBengaliDigits(holdings.size.toString())} টি"
                    var totalInvested = 0.0
                    var currentTotalValue = 0.0

                    for (holding in holdings) {
                        val cost = holding.quantity * holding.averagePrice
                        totalInvested += cost

                        val key = "${holding.exchange.uppercase().trim()}:${holding.stockName.uppercase().trim()}"
                        val live = priceMap[key]
                        val ltp = live?.ltp ?: holding.averagePrice
                        currentTotalValue += (holding.quantity * ltp)
                    }

                    val unrealizedGL = currentTotalValue - totalInvested
                    val glPercent = if (totalInvested > 0.0) (unrealizedGL / totalInvested) * 100.0 else 0.0

                    views.setTextViewText(R.id.widget_holding_count, countStr)
                    views.setTextViewText(
                        R.id.widget_total_value,
                        BengaliFormatter.formatTaka(currentTotalValue, true)
                    )

                    val isGain = unrealizedGL >= 0
                    val sign = if (isGain) "+" else ""
                    val formattedGL = "$sign${BengaliFormatter.formatTaka(unrealizedGL, true)} ($sign${BengaliFormatter.formatPercent(glPercent, true)})"

                    views.setTextViewText(R.id.widget_unrealized_gl, formattedGL)
                    views.setTextColor(
                        R.id.widget_unrealized_gl,
                        ContextCompat.getColor(
                            context,
                            if (isGain) R.color.finora_green else R.color.finora_red
                        )
                    )
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget id $appWidgetId: ${e.message}", e)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    companion object {
        private const val TAG = "PortfolioWidgetProvider"
        const val ACTION_OPEN_PORTFOLIO = "com.example.finora.ACTION_OPEN_PORTFOLIO"
        const val EXTRA_NAVIGATE_TO = "destination"
        const val DESTINATION_PORTFOLIO = "portfolio"
        const val EXTRA_FORCE_SCRAPE = "force_scrape"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, PortfolioWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, PortfolioWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to broadcast widget update: ${e.message}")
            }
        }
    }
}
