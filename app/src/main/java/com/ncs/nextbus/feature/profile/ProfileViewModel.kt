package com.ncs.nextbus.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.DataStorePreferencesRepository
import com.ncs.nextbus.core.data.repository.PreferencesRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.domain.PhoneNumbers
import com.ncs.nextbus.core.model.EmergencyContact
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TravelStats(val completedTrips: Int, val totalDistanceMeters: Double, val savedItems: Int)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(val preferences: UserPreferences, val stats: TravelStats) : ProfileUiState
    data object Error : ProfileUiState
}

enum class ContactError { NAME_REQUIRED, INVALID_PHONE, DUPLICATE, LIMIT_REACHED }

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    userDataRepository: UserDataRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        preferencesRepository.userPreferences,
        userDataRepository.trips,
        userDataRepository.favoriteRouteIds,
        userDataRepository.savedJourneys,
    ) { preferences, trips, favorites, journeys ->
        val completed = trips.filterNot { it.isActive }
        ProfileUiState.Ready(
            preferences = preferences,
            stats = TravelStats(
                completedTrips = completed.size,
                totalDistanceMeters = completed.sumOf { it.distanceMeters ?: 0.0 },
                savedItems = favorites.size + journeys.size,
            ),
        ) as ProfileUiState
    }
        .catch { emit(ProfileUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    fun setThemeMode(mode: ThemeMode) = launch { preferencesRepository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) = launch { preferencesRepository.setDynamicColor(enabled) }

    /** Returns false (and saves nothing) when the name is too long. */
    fun setDisplayName(name: String): Boolean {
        if (!isValidName(name)) return false
        launch { preferencesRepository.setDisplayName(name) }
        return true
    }

    /** Validates and saves a contact; returns the problem, or null on success. */
    fun addEmergencyContact(name: String, phone: String): ContactError? {
        val current = (uiState.value as? ProfileUiState.Ready)?.preferences?.emergencyContacts.orEmpty()
        val error = validateContact(name, phone, current)
        if (error == null) {
            launch { preferencesRepository.addEmergencyContact(EmergencyContact(name.trim(), PhoneNumbers.normalize(phone)!!)) }
        }
        return error
    }

    fun removeEmergencyContact(contact: EmergencyContact) = launch { preferencesRepository.removeEmergencyContact(contact) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        const val MAX_NAME_LENGTH = DataStorePreferencesRepository.MAX_NAME_LENGTH
        const val MAX_CONTACTS = DataStorePreferencesRepository.MAX_CONTACTS

        fun isValidName(name: String): Boolean = name.trim().length <= MAX_NAME_LENGTH

        fun validateContact(name: String, phone: String, existing: List<EmergencyContact>): ContactError? {
            val normalized = PhoneNumbers.normalize(phone)
            return when {
                name.isBlank() -> ContactError.NAME_REQUIRED
                normalized == null -> ContactError.INVALID_PHONE
                existing.any { it.phone == normalized } -> ContactError.DUPLICATE
                existing.size >= MAX_CONTACTS -> ContactError.LIMIT_REACHED
                else -> null
            }
        }
    }
}
