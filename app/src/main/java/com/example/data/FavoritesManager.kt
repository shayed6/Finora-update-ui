package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesManager {
    private const val PREFS_NAME = "finora_favorites_prefs"
    private const val KEY_FAVORITES = "favorite_calculator_ids"
    private const val KEY_MAX_SLOTS = "favorite_max_slots"

    const val DEFAULT_MAX_SLOTS = 4
    const val SLOTS_PER_REWARD = 3

    private var sharedPreferences: SharedPreferences? = null
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _maxSlots = MutableStateFlow<Int>(DEFAULT_MAX_SLOTS)
    val maxSlots: StateFlow<Int> = _maxSlots.asStateFlow()

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = prefs
            val savedSet = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
            _favorites.value = savedSet
            _maxSlots.value = prefs.getInt(KEY_MAX_SLOTS, DEFAULT_MAX_SLOTS)
        }
    }

    fun isFavorite(calculatorId: String): Boolean {
        return _favorites.value.contains(calculatorId)
    }

    fun canAddMoreFavorites(): Boolean {
        return _favorites.value.size < _maxSlots.value
    }

    /**
     * Toggles favorite status.
     * Returns true if operation succeeded (removed or added within limit).
     * Returns false if limit was reached and item could not be added.
     */
    fun toggleFavorite(calculatorId: String): Boolean {
        val current = _favorites.value.toMutableSet()
        if (current.contains(calculatorId)) {
            current.remove(calculatorId)
            val updated = current.toSet()
            _favorites.value = updated
            sharedPreferences?.edit()?.putStringSet(KEY_FAVORITES, updated)?.apply()
            return true
        } else {
            if (current.size < _maxSlots.value) {
                current.add(calculatorId)
                val updated = current.toSet()
                _favorites.value = updated
                sharedPreferences?.edit()?.putStringSet(KEY_FAVORITES, updated)?.apply()
                return true
            }
            return false // Slots full
        }
    }

    fun setFavorite(calculatorId: String, isFavorite: Boolean): Boolean {
        val current = _favorites.value.toMutableSet()
        if (isFavorite) {
            if (current.size < _maxSlots.value || current.contains(calculatorId)) {
                current.add(calculatorId)
                val updated = current.toSet()
                _favorites.value = updated
                sharedPreferences?.edit()?.putStringSet(KEY_FAVORITES, updated)?.apply()
                return true
            }
            return false
        } else {
            current.remove(calculatorId)
            val updated = current.toSet()
            _favorites.value = updated
            sharedPreferences?.edit()?.putStringSet(KEY_FAVORITES, updated)?.apply()
            return true
        }
    }

    /**
     * Unlocks extra favorite slots through rewarded ads.
     */
    fun unlockExtraSlots(additionalSlots: Int = SLOTS_PER_REWARD) {
        val newLimit = _maxSlots.value + additionalSlots
        _maxSlots.value = newLimit
        sharedPreferences?.edit()?.putInt(KEY_MAX_SLOTS, newLimit)?.apply()
    }
}
