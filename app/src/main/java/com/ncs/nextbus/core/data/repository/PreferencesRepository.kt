package com.ncs.nextbus.core.data.repository

import com.ncs.nextbus.core.data.datastore.UserPreferencesDataSource
import com.ncs.nextbus.core.model.EmergencyContact
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface PreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setDisplayName(name: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun addEmergencyContact(contact: EmergencyContact)
    suspend fun removeEmergencyContact(contact: EmergencyContact)
}

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : PreferencesRepository {
    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataSource.setThemeMode(mode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataSource.setDynamicColor(enabled)
    }

    override suspend fun setDisplayName(name: String) {
        dataSource.setDisplayName(name.trim().take(MAX_NAME_LENGTH))
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataSource.setOnboardingCompleted(completed)
    }

    override suspend fun addEmergencyContact(contact: EmergencyContact) {
        val current = dataSource.userPreferences.first().emergencyContacts
        if (current.any { it.phone == contact.phone }) return
        dataSource.setEmergencyContacts((current + contact).take(MAX_CONTACTS))
    }

    override suspend fun removeEmergencyContact(contact: EmergencyContact) {
        val current = dataSource.userPreferences.first().emergencyContacts
        dataSource.setEmergencyContacts(current - contact)
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
        const val MAX_CONTACTS = 5
    }
}
