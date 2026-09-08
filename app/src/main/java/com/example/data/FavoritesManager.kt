package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesManager {
    private const val PREFS_NAME = "finora_favorites_prefs"
    private const val KEY_FAVORITES = "favorite_calculator_ids"

    private var sharedPreferences: SharedPreferences? = null
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun init(context: Context) {
        if (sharedPreferences == null) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = prefs
            val savedSet = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
            _favorites.value = savedSet
        }
    }

    fun isFavorite(calculatorId: String): Boolean {
        return _favorites.value.contains(calculatorId)
    }

    fun toggleFavorite(calculatorId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(calculatorId)) {
            current.remove(calculatorId)
        } else {
            current.add(calculatorId)
        }
        val updated = current.toSet()
        _favorites.value = updated

        sharedPreferences?.edit()?.putStringSet(KEY_FAVORITES, updated)?.apply()
    }

    fun setFavorite(calculatorId: String, isFavorite: Boolean) {
        val current = _favorites.value.toMutableSet()
        if (isFavorite) {
            current.add(calculatorId)
        } else {
            current.remove(calculatorId)
        }
        val updated = current.toSet()
        _favorites.value = updated

        sharedPreferences?.edit()?.putStringSet(KEY_FAVORITES, updated)?.apply()
    }
}
