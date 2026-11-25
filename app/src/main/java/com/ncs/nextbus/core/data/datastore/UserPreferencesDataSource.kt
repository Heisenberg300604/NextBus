package com.ncs.nextbus.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ncs.nextbus.core.model.EmergencyContact
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val safeData: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            Timber.w(error, "Reading preferences failed; using defaults")
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    val userPreferences: Flow<UserPreferences> = safeData.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            emergencyContacts = decodeContacts(prefs[Keys.EMERGENCY_CONTACTS]),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setDisplayName(name: String) = dataStore.edit { it[Keys.DISPLAY_NAME] = name }

    suspend fun setOnboardingCompleted(completed: Boolean) =
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }

    suspend fun setEmergencyContacts(contacts: List<EmergencyContact>) =
        dataStore.edit { it[Keys.EMERGENCY_CONTACTS] = json.encodeToString(contacts) }

    suspend fun seedVersion(): Int = safeData.first()[Keys.SEED_VERSION] ?: 0

    suspend fun setSeedVersion(version: Int) = dataStore.edit { it[Keys.SEED_VERSION] = version }

    private fun decodeContacts(raw: String?): List<EmergencyContact> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<EmergencyContact>>(raw) }
            .onFailure { Timber.w(it, "Discarding unreadable emergency contacts") }
            .getOrDefault(emptyList())
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val EMERGENCY_CONTACTS = stringPreferencesKey("emergency_contacts")
        val SEED_VERSION = intPreferencesKey("seed_version")
    }
}
