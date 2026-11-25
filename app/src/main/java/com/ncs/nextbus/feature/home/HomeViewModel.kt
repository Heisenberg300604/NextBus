package com.ncs.nextbus.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.JourneyRef
import com.ncs.nextbus.core.data.repository.PreferencesRepository
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.domain.NearbyBuses
import com.ncs.nextbus.core.domain.ObserveNearbyBusesUseCase
import com.ncs.nextbus.core.domain.ObserveNearestStopsUseCase
import com.ncs.nextbus.core.domain.ObserveStopArrivalsUseCase
import com.ncs.nextbus.core.model.Freshness
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.NearbyBus
import com.ncs.nextbus.core.model.RecentSearch
import com.ncs.nextbus.core.model.ReferenceLocation
import com.ncs.nextbus.core.model.Route
import com.ncs.nextbus.core.model.Stop
import com.ncs.nextbus.core.model.StopArrival
import com.ncs.nextbus.core.model.StopWithDistance
import com.ncs.nextbus.core.model.TransitNetwork
import com.ncs.nextbus.core.model.Trip
import com.ncs.nextbus.navigation.StopField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

data class HomeContent(
    val displayName: String,
    val from: Stop?,
    val to: Stop?,
    val reference: ReferenceLocation,
    val nearestStop: StopWithDistance?,
    val nearestStopArrivals: List<StopArrival>,
    val nearbyBuses: List<NearbyBus>,
    val favoriteRoutes: List<Route>,
    val recentSearches: List<RecentSearch>,
    val activeTrip: Trip?,
    val activeTripBus: LiveBus?,
) {
    val isSameStop: Boolean get() = from != null && from.id == to?.id
    val canSearch: Boolean get() = from != null && to != null && !isSameStop
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Error : HomeUiState
    data class Success(val content: HomeContent) : HomeUiState
}

private data class JourneySelection(val fromStopId: String?, val toStopId: String?)

private data class UserContent(
    val favoriteRouteIds: List<String>,
    val recentSearches: List<JourneyRef>,
    val activeTrip: Trip?,
    val displayName: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val transitRepository: TransitRepository,
    private val observeNearbyBuses: ObserveNearbyBusesUseCase,
    userDataRepository: UserDataRepository,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    /** From/to as one value so a swap is a single, atomic update (no transient "same stop" state). */
    private val journey = MutableStateFlow(
        JourneySelection(savedStateHandle.get<String>(KEY_FROM), savedStateHandle.get<String>(KEY_TO)),
    )
    private val retryTrigger = MutableStateFlow(0)

    private val userContent = combine(
        userDataRepository.favoriteRouteIds,
        userDataRepository.recentSearches,
        userDataRepository.activeTrip,
        preferencesRepository.userPreferences,
    ) { favorites, recents, trip, preferences ->
        UserContent(favorites, recents, trip, preferences.displayName)
    }

    val uiState: StateFlow<HomeUiState> = retryTrigger
        .flatMapLatest {
            combine(
                transitRepository.observeNetwork(),
                observeNearbyBuses(),
                userContent,
                journey,
            ) { network, nearby, user, selection ->
                HomeUiState.Success(buildContent(network, nearby, user, selection.fromStopId, selection.toStopId)) as HomeUiState
            }
                .onStart { emit(HomeUiState.Loading) }
                .catch { error ->
                    Timber.e(error, "Home failed to load")
                    emit(HomeUiState.Error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun onStopPicked(field: StopField, stopId: String) {
        val current = journey.value
        updateJourney(if (field == StopField.FROM) current.copy(fromStopId = stopId) else current.copy(toStopId = stopId))
    }

    fun swapStops() {
        val current = journey.value
        updateJourney(JourneySelection(fromStopId = current.toStopId, toStopId = current.fromStopId))
    }

    private fun updateJourney(selection: JourneySelection) {
        journey.value = selection
        savedStateHandle[KEY_FROM] = selection.fromStopId
        savedStateHandle[KEY_TO] = selection.toStopId
    }

    fun retry() = retryTrigger.update { it + 1 }

    companion object {
        const val KEY_FROM = "from_stop_id"
        const val KEY_TO = "to_stop_id"
        const val NEARBY_LIMIT = 5
        const val ARRIVALS_LIMIT = 3
        const val RECENTS_LIMIT = 3

        private fun buildContent(
            network: TransitNetwork,
            nearby: NearbyBuses,
            user: UserContent,
            fromId: String?,
            toId: String?,
        ): HomeContent {
            val liveBuses = nearby.buses.map { it.liveBus }
            val nearestStop = ObserveNearestStopsUseCase.sortByDistance(nearby.reference, network.stops).firstOrNull()
            val arrivals = nearestStop?.let {
                ObserveStopArrivalsUseCase.arrivalsAt(it.stop.id, liveBuses, ObserveStopArrivalsUseCase.DEFAULT_HORIZON_SECONDS)
                    .take(ARRIVALS_LIMIT)
            }.orEmpty()
            val now = liveBuses.maxOfOrNull { it.lastUpdatedMillis } ?: 0L
            return HomeContent(
                displayName = user.displayName,
                from = fromId?.let(network.stopsById::get),
                to = toId?.let(network.stopsById::get),
                reference = nearby.reference,
                nearestStop = nearestStop,
                nearestStopArrivals = arrivals,
                nearbyBuses = nearby.buses
                    .filter { Freshness.of(it.liveBus.lastUpdatedMillis, now) != Freshness.OFFLINE }
                    .take(NEARBY_LIMIT),
                favoriteRoutes = user.favoriteRouteIds.mapNotNull(network.routesById::get),
                recentSearches = user.recentSearches.mapNotNull { ref ->
                    val from = network.stopsById[ref.fromStopId]
                    val to = network.stopsById[ref.toStopId]
                    if (from != null && to != null) RecentSearch(from, to, ref.timestampMillis) else null
                }.take(RECENTS_LIMIT),
                activeTrip = user.activeTrip,
                activeTripBus = user.activeTrip?.let { trip -> liveBuses.firstOrNull { it.bus.id == trip.busId } },
            )
        }
    }
}
