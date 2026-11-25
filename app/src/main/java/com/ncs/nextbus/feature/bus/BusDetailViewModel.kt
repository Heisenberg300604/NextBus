package com.ncs.nextbus.feature.bus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.ConnectivityRepository
import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.domain.StartTripResult
import com.ncs.nextbus.core.domain.TripManager
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class BusDetailContent(
    val liveBus: LiveBus,
    val isFavoriteRoute: Boolean,
    val activeTrip: Trip?,
    val isOnline: Boolean,
) {
    val isOnThisBus: Boolean get() = activeTrip?.busId == liveBus.bus.id
}

sealed interface BusDetailUiState {
    data object Loading : BusDetailUiState
    data object NotFound : BusDetailUiState
    data object Error : BusDetailUiState
    data class Success(val content: BusDetailContent) : BusDetailUiState
}

sealed interface BusDetailEvent {
    data object TripStarted : BusDetailEvent
    data object TripEnded : BusDetailEvent
    data class AlreadyOnTrip(val routeNumber: String) : BusDetailEvent
    data class FavoriteChanged(val isFavorite: Boolean) : BusDetailEvent
    data object ActionFailed : BusDetailEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BusDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    transitRepository: TransitRepository,
    liveBusRepository: LiveBusRepository,
    private val userDataRepository: UserDataRepository,
    connectivityRepository: ConnectivityRepository,
    private val tripManager: TripManager,
) : ViewModel() {
    private val busId: String = savedStateHandle["busId"] ?: ""
    private val retryTrigger = MutableStateFlow(0)
    private val events = Channel<BusDetailEvent>(Channel.BUFFERED)
    val eventFlow: Flow<BusDetailEvent> = events.receiveAsFlow()

    val uiState: StateFlow<BusDetailUiState> = retryTrigger
        .flatMapLatest {
            transitRepository.observeNetwork().flatMapLatest { network ->
                if (busId !in network.busesById) {
                    flowOf(BusDetailUiState.NotFound)
                } else {
                    combine(
                        liveBusRepository.observeBus(busId).filterNotNull(),
                        userDataRepository.favoriteRouteIds,
                        userDataRepository.activeTrip,
                        connectivityRepository.isOnline,
                    ) { bus, favorites, trip, online ->
                        BusDetailUiState.Success(
                            BusDetailContent(bus, bus.route.id in favorites, trip, online),
                        )
                    }
                }
            }
                .onStart { emit(BusDetailUiState.Loading) }
                .catch { error ->
                    Timber.e(error, "Bus detail failed")
                    emit(BusDetailUiState.Error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusDetailUiState.Loading)

    fun startTrip() = withContent { content ->
        when (val result = tripManager.start(content.liveBus)) {
            is StartTripResult.Started -> events.send(BusDetailEvent.TripStarted)
            is StartTripResult.AlreadyActive -> events.send(BusDetailEvent.AlreadyOnTrip(result.trip.routeNumber))
        }
    }

    fun endTrip() = withContent { content ->
        val trip = content.activeTrip ?: return@withContent
        tripManager.end(trip, content.liveBus)
        events.send(BusDetailEvent.TripEnded)
    }

    fun toggleFavoriteRoute() = withContent { content ->
        val favorite = !content.isFavoriteRoute
        userDataRepository.setFavoriteRoute(content.liveBus.route.id, favorite)
        events.send(BusDetailEvent.FavoriteChanged(favorite))
    }

    fun retry() = retryTrigger.update { it + 1 }

    private fun withContent(block: suspend (BusDetailContent) -> Unit) {
        val content = (uiState.value as? BusDetailUiState.Success)?.content ?: return
        viewModelScope.launch {
            runCatching { block(content) }.onFailure {
                Timber.e(it, "Bus detail action failed")
                events.send(BusDetailEvent.ActionFailed)
            }
        }
    }
}
