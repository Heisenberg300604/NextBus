package com.ncs.nextbus.feature.route

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.common.TimeSource
import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.ScheduleRepository
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.domain.FareCalculator
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RouteStopRow(
    val stopId: String,
    val name: String,
    /** Scheduled minutes from the first stop of the selected direction. */
    val scheduledOffsetMinutes: Int,
    /** Soonest live arrival in the selected direction, if any bus is due. */
    val nextEtaSeconds: Int?,
)

data class RouteDetailContent(
    val route: Route,
    val direction: Direction,
    val stops: List<RouteStopRow>,
    /** Buses heading in the selected direction first, then the rest. */
    val liveBuses: List<LiveBus>,
    val departures: List<Long>,
    val isFavorite: Boolean,
    val minFare: Int,
    val maxFare: Int,
)

sealed interface RouteDetailUiState {
    data object Loading : RouteDetailUiState
    data object NotFound : RouteDetailUiState
    data object Error : RouteDetailUiState
    data class Success(val content: RouteDetailContent) : RouteDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    transitRepository: TransitRepository,
    liveBusRepository: LiveBusRepository,
    private val scheduleRepository: ScheduleRepository,
    private val userDataRepository: UserDataRepository,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val routeId: String = savedStateHandle["routeId"] ?: ""
    private val direction = savedStateHandle.getStateFlow(KEY_DIRECTION, Direction.OUTBOUND.name)
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<RouteDetailUiState> = retryTrigger
        .flatMapLatest {
            transitRepository.observeNetwork().flatMapLatest { network ->
                val route = network.routesById[routeId]
                if (route == null) {
                    flowOf(RouteDetailUiState.NotFound)
                } else {
                    val selected = direction.flatMapLatest { name ->
                        val dir = Direction.valueOf(name)
                        departuresFlow(route, dir).let { departures -> combine(flowOf(dir), departures) { d, times -> d to times } }
                    }
                    combine(selected, liveBusRepository.liveBuses, userDataRepository.favoriteRouteIds) { (dir, departures), buses, favorites ->
                        RouteDetailUiState.Success(buildContent(route, dir, departures, buses, routeId in favorites))
                    }
                }
            }
                .onStart { emit(RouteDetailUiState.Loading) }
                .catch { error ->
                    Timber.e(error, "Route detail failed")
                    emit(RouteDetailUiState.Error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RouteDetailUiState.Loading)

    fun selectDirection(direction: Direction) {
        savedStateHandle[KEY_DIRECTION] = direction.name
    }

    fun toggleFavorite() {
        val content = (uiState.value as? RouteDetailUiState.Success)?.content ?: return
        viewModelScope.launch { userDataRepository.setFavoriteRoute(routeId, !content.isFavorite) }
    }

    fun retry() = retryTrigger.update { it + 1 }

    /** Next scheduled departures from the first stop of [direction], refreshed every 30 s. */
    private fun departuresFlow(route: Route, direction: Direction): Flow<List<Long>> = flow {
        val originIndex = if (direction == Direction.OUTBOUND) 0 else route.stops.lastIndex
        while (true) {
            emit(scheduleRepository.scheduledArrivals(route.id, originIndex, direction, timeSource.nowMillis(), DEPARTURES_SHOWN))
            delay(DEPARTURES_REFRESH_MILLIS)
        }
    }

    companion object {
        const val KEY_DIRECTION = "direction"
        const val DEPARTURES_SHOWN = 5
        const val DEPARTURES_REFRESH_MILLIS = 30_000L

        fun buildContent(
            route: Route,
            direction: Direction,
            departures: List<Long>,
            buses: List<LiveBus>,
            isFavorite: Boolean,
        ): RouteDetailContent {
            val onRoute = buses.filter { it.route.id == route.id }
            val ordered = if (direction == Direction.OUTBOUND) route.stops else route.stops.reversed()
            val origin = ordered.first().minutesFromStart
            val rows = ordered.map { routeStop ->
                RouteStopRow(
                    stopId = routeStop.stop.id,
                    name = routeStop.stop.name,
                    scheduledOffsetMinutes = kotlin.math.abs(routeStop.minutesFromStart - origin).toInt(),
                    nextEtaSeconds = onRoute.mapNotNull { it.etaSecondsTo(routeStop.stop.id, direction) }.minOrNull(),
                )
            }
            val shortestHop = route.stops.zipWithNext { a, b -> b.distanceFromStartMeters - a.distanceFromStartMeters }.minOrNull() ?: 0.0
            return RouteDetailContent(
                route = route,
                direction = direction,
                stops = rows,
                liveBuses = onRoute.sortedWith(compareBy({ it.direction != direction }, { it.bus.fleetIndex })),
                departures = departures,
                isFavorite = isFavorite,
                minFare = FareCalculator.fareFor(shortestHop),
                maxFare = FareCalculator.fareFor(route.lengthMeters),
            )
        }
    }
}
