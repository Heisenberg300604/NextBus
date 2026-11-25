package com.ncs.nextbus.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.ConnectivityRepository
import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.domain.ObserveReferenceLocationUseCase
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.ReferenceLocation
import com.ncs.nextbus.core.model.Route
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

data class MapContent(
    val routes: List<Route>,
    val visibleRoutes: List<Route>,
    val buses: List<LiveBus>,
    val filterRouteId: String?,
    val selectedBus: LiveBus?,
    val reference: ReferenceLocation,
    val isOnline: Boolean,
)

sealed interface MapUiState {
    data object Loading : MapUiState
    data object Error : MapUiState
    data class Success(val content: MapContent) : MapUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    transitRepository: TransitRepository,
    liveBusRepository: LiveBusRepository,
    observeReferenceLocation: ObserveReferenceLocationUseCase,
    connectivityRepository: ConnectivityRepository,
) : ViewModel() {
    private val filter = savedStateHandle.getStateFlow<String?>(KEY_FILTER, null)
    private val selected = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)
    private val retryTrigger = MutableStateFlow(0)

    private val selection = combine(filter, selected) { f, s -> f to s }

    val uiState: StateFlow<MapUiState> = retryTrigger
        .flatMapLatest {
            combine(
                transitRepository.observeNetwork(),
                liveBusRepository.liveBuses,
                selection,
                observeReferenceLocation(),
                connectivityRepository.isOnline,
            ) { network, buses, (filterId, selectedId), reference, online ->
                val visibleRoutes = network.routes.filter { filterId == null || it.id == filterId }
                val visibleBuses = buses.filter { filterId == null || it.route.id == filterId }
                MapUiState.Success(
                    MapContent(
                        routes = network.routes,
                        visibleRoutes = visibleRoutes,
                        buses = visibleBuses,
                        filterRouteId = filterId,
                        selectedBus = visibleBuses.firstOrNull { it.bus.id == selectedId },
                        reference = reference,
                        isOnline = online,
                    ),
                ) as MapUiState
            }
                .onStart { emit(MapUiState.Loading) }
                .catch { error ->
                    Timber.e(error, "Map failed")
                    emit(MapUiState.Error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState.Loading)

    fun setFilter(routeId: String?) {
        savedStateHandle[KEY_FILTER] = routeId
        savedStateHandle[KEY_SELECTED] = null
    }

    fun selectBus(busId: String?) {
        savedStateHandle[KEY_SELECTED] = busId
    }

    fun retry() = retryTrigger.update { it + 1 }

    companion object {
        const val KEY_FILTER = "route_filter"
        const val KEY_SELECTED = "selected_bus"
    }
}
