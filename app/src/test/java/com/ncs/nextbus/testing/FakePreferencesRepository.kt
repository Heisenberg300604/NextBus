package com.ncs.nextbus.testing

import com.ncs.nextbus.core.data.repository.PreferencesRepository
import com.ncs.nextbus.core.model.EmergencyContact
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.core.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakePreferencesRepository(initial: UserPreferences = UserPreferences()) : PreferencesRepository {
    val state = MutableStateFlow(initial)
    override val userPreferences = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(useDynamicColor = enabled) }
    override suspend fun setDisplayName(name: String) = state.update { it.copy(displayName = name.trim()) }
    override suspend fun setOnboardingCompleted(completed: Boolean) = state.update { it.copy(onboardingCompleted = completed) }
    override suspend fun addEmergencyContact(contact: EmergencyContact) =
        state.update { if (it.emergencyContacts.any { c -> c.phone == contact.phone }) it else it.copy(emergencyContacts = it.emergencyContacts + contact) }
    override suspend fun removeEmergencyContact(contact: EmergencyContact) =
        state.update { it.copy(emergencyContacts = it.emergencyContacts - contact) }
}
