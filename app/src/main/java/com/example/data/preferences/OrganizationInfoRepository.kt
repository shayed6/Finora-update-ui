package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class OrganizationInfo(
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val invoicePrefix: String = "INV-"
)

object WorkProfileKey {
    const val BUSINESS = "business"
    const val JOB = "job"
    const val FREELANCE = "freelance"

    val ALL = setOf(BUSINESS, JOB, FREELANCE)
}

class OrganizationInfoRepository private constructor(private val context: Context) {

    companion object {
        private val ORG_NAME_KEY = stringPreferencesKey("org_name")
        private val ORG_ADDRESS_KEY = stringPreferencesKey("org_address")
        private val ORG_PHONE_KEY = stringPreferencesKey("org_phone")
        private val INVOICE_PREFIX_KEY = stringPreferencesKey("invoice_prefix")

        private val WORK_PROFILES_KEY = stringSetPreferencesKey("selected_work_profiles")
        private val WORK_TYPE_PROMPT_SHOWN_KEY = booleanPreferencesKey("work_type_prompt_shown")

        @Volatile
        private var INSTANCE: OrganizationInfoRepository? = null

        fun getInstance(context: Context): OrganizationInfoRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OrganizationInfoRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    val organizationInfo: Flow<OrganizationInfo> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            OrganizationInfo(
                name = prefs[ORG_NAME_KEY] ?: "",
                address = prefs[ORG_ADDRESS_KEY] ?: "",
                phone = prefs[ORG_PHONE_KEY] ?: "",
                invoicePrefix = prefs[INVOICE_PREFIX_KEY] ?: "INV-"
            )
        }

    suspend fun saveOrganizationInfo(
        name: String,
        address: String,
        phone: String,
        invoicePrefix: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[ORG_NAME_KEY] = name.take(60)
            prefs[ORG_ADDRESS_KEY] = address.take(120)
            prefs[ORG_PHONE_KEY] = phone.take(20)
            prefs[INVOICE_PREFIX_KEY] = invoicePrefix.ifBlank { "INV-" }.take(8)
        }
    }

    val selectedWorkProfiles: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[WORK_PROFILES_KEY] ?: emptySet()
        }

    suspend fun saveWorkProfiles(profiles: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[WORK_PROFILES_KEY] = profiles
        }
    }

    val workTypePromptShown: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[WORK_TYPE_PROMPT_SHOWN_KEY] ?: false
        }

    suspend fun setWorkTypePromptShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[WORK_TYPE_PROMPT_SHOWN_KEY] = shown
        }
    }
}
