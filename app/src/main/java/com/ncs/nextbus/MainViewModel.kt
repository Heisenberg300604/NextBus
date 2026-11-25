package com.ncs.nextbus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.LocationRepository
import com.ncs.nextbus.core.data.repository.PreferencesRepository
import com.ncs.nextbus.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val preferences: UserPreferences) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = preferencesRepository.userPreferences
        .map<UserPreferences, MainUiState> { MainUiState.Ready(it) }
        .catch { emit(MainUiState.Ready(UserPreferences())) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    fun completeOnboarding() {
        viewModelScope.launch { preferencesRepository.setOnboardingCompleted(true) }
    }

    /** Re-checks location permission (after a permission dialog, or returning from system settings). */
    fun refreshLocationPermission() = locationRepository.onPermissionChanged()
}
