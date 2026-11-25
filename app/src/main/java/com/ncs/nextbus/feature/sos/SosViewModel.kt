package com.ncs.nextbus.feature.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.PreferencesRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.domain.ObserveReferenceLocationUseCase
import com.ncs.nextbus.core.model.EmergencyContact
import com.ncs.nextbus.core.model.GeoPoint
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

data class SosContent(
    val displayName: String,
    val contacts: List<EmergencyContact>,
    val trip: Trip?,
    val tripBus: LiveBus?,
    /** Best known position: the bus during a trip, otherwise the device (never the city-centre fallback). */
    val location: GeoPoint?,
    val locationTimeMillis: Long?,
)

sealed interface SosUiState {
    data object Loading : SosUiState
    data class Ready(val content: SosContent) : SosUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SosViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    userDataRepository: UserDataRepository,
    liveBusRepository: LiveBusRepository,
    observeReferenceLocation: ObserveReferenceLocationUseCase,
) : ViewModel() {

    private val tripWithBus = userDataRepository.activeTrip.flatMapLatest { trip ->
        if (trip == null) flowOf(null to null) else liveBusRepository.observeBus(trip.busId).map { trip to it }
    }

    val uiState: StateFlow<SosUiState> = combine(
        preferencesRepository.userPreferences,
        tripWithBus,
        observeReferenceLocation(),
    ) { preferences, (trip, bus), reference ->
        val deviceLocation = reference.point.takeIf { reference.isDevice }
        SosUiState.Ready(
            SosContent(
                displayName = preferences.displayName,
                contacts = preferences.emergencyContacts,
                trip = trip,
                tripBus = bus,
                location = bus?.position ?: deviceLocation,
                locationTimeMillis = bus?.lastUpdatedMillis ?: deviceLocation?.let { System.currentTimeMillis() },
            ),
        ) as SosUiState
    }
        .catch { error ->
            // SOS must always work: fall back to a minimal message rather than an error screen.
            Timber.e(error, "SOS state failed")
            emit(SosUiState.Ready(SosContent("", emptyList(), null, null, null, null)))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SosUiState.Loading)
}
