package com.example.data.scraper

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase

/**
 * WorkManager worker for performing asynchronous background scrape of DSE/CSE market quotes
 * and updating the local Room database.
 */
class LivePriceScraperWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val scraper = MarketPriceScraper()
            val force = inputData.getBoolean(KEY_FORCE_REFRESH, false)
            val prices = scraper.fetchAllPrices(forceRefresh = force)

            if (prices.isNotEmpty()) {
                val db = AppDatabase.getDatabase(applicationContext)
                db.livePriceDao().insertOrUpdatePrices(prices)
                Log.d(TAG, "ScraperWorker saved ${prices.size} live prices to database")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ScraperWorker encountered error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "LivePriceScraperWorker"
        const val WORK_NAME = "finora_live_price_scraper_work"
        const val KEY_FORCE_REFRESH = "key_force_refresh"
    }
}
