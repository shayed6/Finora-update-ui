package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WatchlistStockKey(
    val exchange: String,
    val symbol: String
) {
    val key: String get() = "${exchange.trim().uppercase()}:${symbol.trim().uppercase()}"

    companion object {
        fun fromKey(key: String): WatchlistStockKey? {
            val parts = key.split(":")
            if (parts.size != 2) return null
            return WatchlistStockKey(parts[0].trim().uppercase(), parts[1].trim().uppercase())
        }
    }
}

/**
 * Local Watchlist and Sparkline history manager.
 * Persists watchlisted symbols and up to 30 historical price points locally using SharedPreferences.
 * Zero external dependencies, ultra-lightweight, and survives app restarts.
 */
object WatchlistManager {
    private const val PREFS_NAME = "finora_watchlist_prefs"
    private const val KEY_SYMBOLS = "watchlist_symbols"
    private const val PREFIX_SPARKLINE = "sparkline_"
    private const val MAX_SPARKLINE_POINTS = 30

    private var prefs: SharedPreferences? = null

    private val _watchlist = MutableStateFlow<Set<String>>(emptySet())
    val watchlist: StateFlow<Set<String>> = _watchlist.asStateFlow()

    private val _sparklineCache = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val sparklineCache: StateFlow<Map<String, List<Double>>> = _sparklineCache.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sp
            val savedSymbols = sp.getStringSet(KEY_SYMBOLS, emptySet()) ?: emptySet()
            _watchlist.value = savedSymbols

            // Load sparklines for all saved symbols
            val loadedSparklines = mutableMapOf<String, List<Double>>()
            for (key in savedSymbols) {
                val csv = sp.getString(PREFIX_SPARKLINE + key, null)
                if (!csv.isNullOrBlank()) {
                    val points = csv.split(",").mapNotNull { it.toDoubleOrNull() }
                    if (points.isNotEmpty()) {
                        loadedSparklines[key] = points
                    }
                }
            }
            _sparklineCache.value = loadedSparklines
        }
    }

    fun isWatchlisted(exchange: String, symbol: String): Boolean {
        val key = WatchlistStockKey(exchange, symbol).key
        return _watchlist.value.contains(key)
    }

    fun addToWatchlist(exchange: String, symbol: String): Boolean {
        val stockKey = WatchlistStockKey(exchange, symbol).key
        val current = _watchlist.value.toMutableSet()
        if (current.add(stockKey)) {
            val updated = current.toSet()
            _watchlist.value = updated
            prefs?.edit()?.putStringSet(KEY_SYMBOLS, updated)?.apply()
            return true
        }
        return false
    }

    fun removeFromWatchlist(exchange: String, symbol: String): Boolean {
        val stockKey = WatchlistStockKey(exchange, symbol).key
        val current = _watchlist.value.toMutableSet()
        if (current.remove(stockKey)) {
            val updated = current.toSet()
            _watchlist.value = updated
            prefs?.edit()
                ?.putStringSet(KEY_SYMBOLS, updated)
                ?.remove(PREFIX_SPARKLINE + stockKey)
                ?.apply()

            val updatedSparklines = _sparklineCache.value.toMutableMap()
            updatedSparklines.remove(stockKey)
            _sparklineCache.value = updatedSparklines
            return true
        }
        return false
    }

    /**
     * Records a new price observation into the local sparkline cache (max 30 points).
     */
    fun recordPrice(exchange: String, symbol: String, price: Double) {
        if (price <= 0.0) return
        val key = WatchlistStockKey(exchange, symbol).key
        val currentList = _sparklineCache.value[key].orEmpty().toMutableList()

        // Avoid appending duplicate identical consecutive points
        if (currentList.isNotEmpty() && currentList.last() == price) return

        currentList.add(price)
        while (currentList.size > MAX_SPARKLINE_POINTS) {
            currentList.removeAt(0)
        }

        val updatedMap = _sparklineCache.value.toMutableMap()
        updatedMap[key] = currentList
        _sparklineCache.value = updatedMap

        val csv = currentList.joinToString(",")
        prefs?.edit()?.putString(PREFIX_SPARKLINE + key, csv)?.apply()
    }

    fun getSparkline(exchange: String, symbol: String): List<Double> {
        val key = WatchlistStockKey(exchange, symbol).key
        return _sparklineCache.value[key].orEmpty()
    }
}
