package com.ncs.nextbus.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.model.RecentSearch
import com.ncs.nextbus.core.model.Route
import com.ncs.nextbus.core.model.SavedJourney
import com.ncs.nextbus.core.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SavedContent(
    val favoriteRoutes: List<Route>,
    val savedJourneys: List<SavedJourney>,
    val recentSearches: List<RecentSearch>,
    val activeTrip: Trip?,
    val pastTrips: List<Trip>,
    val routeColors: Map<String, Long>,
) {
    fun colorOf(routeId: String): Long = routeColors[routeId] ?: DEFAULT_ROUTE_COLOR

    companion object {
        const val DEFAULT_ROUTE_COLOR = 0xFF1F5BD3
    }
}

sealed interface SavedUiState {
    data object Loading : SavedUiState
    data object Error : SavedUiState
    data class Success(val content: SavedContent) : SavedUiState
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    transitRepository: TransitRepository,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    val uiState: StateFlow<SavedUiState> = combine(
        transitRepository.observeNetwork(),
        userDataRepository.favoriteRouteIds,
        userDataRepository.savedJourneys,
        userDataRepository.recentSearches,
        userDataRepository.trips,
    ) { network, favorites, journeys, recents, trips ->
        SavedUiState.Success(
            SavedContent(
                favoriteRoutes = favorites.mapNotNull(network.routesById::get),
                savedJourneys = journeys.mapNotNull { ref ->
                    val from = network.stopsById[ref.fromStopId]
                    val to = network.stopsById[ref.toStopId]
                    if (from != null && to != null) SavedJourney(from, to, ref.timestampMillis) else null
                },
                recentSearches = recents.mapNotNull { ref ->
                    val from = network.stopsById[ref.fromStopId]
                    val to = network.stopsById[ref.toStopId]
                    if (from != null && to != null) RecentSearch(from, to, ref.timestampMillis) else null
                },
                activeTrip = trips.firstOrNull { it.isActive },
                pastTrips = trips.filterNot { it.isActive },
                routeColors = network.routes.associate { it.id to it.colorArgb },
            ),
        ) as SavedUiState
    }
        .catch { error ->
            Timber.e(error, "Saved failed")
            emit(SavedUiState.Error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState.Loading)

    fun removeFavoriteRoute(routeId: String) = launch { userDataRepository.setFavoriteRoute(routeId, false) }

    fun removeSavedJourney(journey: SavedJourney) =
        launch { userDataRepository.setSavedJourney(journey.from.id, journey.to.id, false) }

    fun clearRecentSearches() = launch { userDataRepository.clearRecentSearches() }

    fun deleteTrip(tripId: Long) = launch { userDataRepository.deleteTrip(tripId) }

    fun clearTripHistory() = launch { userDataRepository.clearTripHistory() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() }.onFailure { Timber.e(it, "Saved action failed") } }
    }
}
