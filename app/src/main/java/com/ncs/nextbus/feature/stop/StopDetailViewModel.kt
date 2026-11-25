package com.ncs.nextbus.feature.stop

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.domain.ObserveStopArrivalsUseCase
import com.ncs.nextbus.core.model.Route
import com.ncs.nextbus.core.model.Stop
import com.ncs.nextbus.core.model.StopArrival
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

sealed interface StopDetailUiState {
    data object Loading : StopDetailUiState
    data object NotFound : StopDetailUiState
    data object Error : StopDetailUiState
    data class Success(val stop: Stop, val routes: List<Route>, val arrivals: List<StopArrival>) : StopDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StopDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    transitRepository: TransitRepository,
    liveBusRepository: LiveBusRepository,
) : ViewModel() {
    private val stopId: String = savedStateHandle["stopId"] ?: ""
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<StopDetailUiState> = retryTrigger
        .flatMapLatest {
            transitRepository.observeNetwork().flatMapLatest { network ->
                val stop = network.stopsById[stopId]
                if (stop == null) {
                    flowOf(StopDetailUiState.NotFound)
                } else {
                    val routes = network.routesServing(stopId)
                    liveBusRepository.liveBuses.map { buses ->
                        StopDetailUiState.Success(
                            stop = stop,
                            routes = routes,
                            arrivals = ObserveStopArrivalsUseCase.arrivalsAt(stopId, buses, ObserveStopArrivalsUseCase.DEFAULT_HORIZON_SECONDS),
                        )
                    }
                }
            }
                .onStart { emit(StopDetailUiState.Loading) }
                .catch { error ->
                    Timber.e(error, "Stop detail failed")
                    emit(StopDetailUiState.Error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StopDetailUiState.Loading)

    fun retry() = retryTrigger.update { it + 1 }
}
