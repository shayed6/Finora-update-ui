package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finora_user_preferences")

enum class AppThemeMode(val titleBn: String, val subtitleBn: String) {
    SYSTEM("সিস্টেম ডিফল্ট", "ডিভাইসের সিস্টেম থিম অনুসরণ করবে"),
    LIGHT("লাইট মোড", "উজ্জ্বল ও স্পষ্ট ইন্টারফেস"),
    DARK("ডার্ক মোড", "চোখের জন্য আরামদায়ক ডার্ক থিম")
}

class UserPreferencesRepository private constructor(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val USE_BENGALI_DIGITS_KEY = booleanPreferencesKey("use_bengali_digits")

        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeName = preferences[THEME_MODE_KEY] ?: AppThemeMode.SYSTEM.name
            try {
                AppThemeMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                AppThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun toggleDarkMode(enableDark: Boolean) {
        setThemeMode(if (enableDark) AppThemeMode.DARK else AppThemeMode.LIGHT)
    }

    val useBengaliDigits: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[USE_BENGALI_DIGITS_KEY] ?: true
        }

    suspend fun setUseBengaliDigits(useBengali: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_BENGALI_DIGITS_KEY] = useBengali
        }
    }
}
