package com.example.ads

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local in-memory logger for AdMob events (Impressions, Clicks, Loads, Failures, Consent).
 * Stores recent events without any PII for developer debugging and verification.
 */
object AdLog {
    private const val TAG = "FinoraAdLog"
    private const val MAX_LOG_ENTRIES = 50

    enum class AdFormat {
        BANNER,
        INTERSTITIAL,
        REWARDED,
        NATIVE,
        CONSENT
    }

    enum class EventType {
        IMPRESSION,
        CLICK,
        LOADED,
        FAILED_TO_LOAD,
        OPENED,
        CLOSED,
        REWARD_EARNED,
        CONSENT_UPDATED
    }

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val format: AdFormat,
        val eventType: EventType,
        val message: String
    ) {
        fun formattedTime(): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(format: AdFormat, eventType: EventType, message: String) {
        val entry = LogEntry(
            format = format,
            eventType = eventType,
            message = message
        )
        Log.d(TAG, "[${format.name}] ${eventType.name}: $message")

        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > MAX_LOG_ENTRIES) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logs.value = currentList
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
