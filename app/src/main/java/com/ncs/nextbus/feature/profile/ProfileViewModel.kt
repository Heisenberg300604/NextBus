package com.ncs.nextbus.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.DataStorePreferencesRepository
import com.ncs.nextbus.core.data.repository.PreferencesRepository
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(val preferences: UserPreferences) : ProfileUiState
    data object Error : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = preferencesRepository.userPreferences
        .map<UserPreferences, ProfileUiState> { ProfileUiState.Ready(it) }
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

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        const val MAX_NAME_LENGTH = DataStorePreferencesRepository.MAX_NAME_LENGTH

        fun isValidName(name: String): Boolean = name.trim().length <= MAX_NAME_LENGTH
    }
}
