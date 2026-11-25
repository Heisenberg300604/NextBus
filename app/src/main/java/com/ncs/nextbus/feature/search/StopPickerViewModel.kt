package com.ncs.nextbus.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.domain.ObserveNearestStopsUseCase
import com.ncs.nextbus.core.domain.ObserveReferenceLocationUseCase
import com.ncs.nextbus.core.model.Route
import com.ncs.nextbus.core.model.Stop
import com.ncs.nextbus.core.model.TransitNetwork
import com.ncs.nextbus.navigation.StopField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

data class StopOption(val stop: Stop, val distanceMeters: Double, val routes: List<Route>)

sealed interface StopPickerUiState {
    data object Loading : StopPickerUiState
    data object Error : StopPickerUiState
    data class Success(
        val query: String,
        /** Shown above the results when the query is empty and the device location is known. */
        val nearest: StopOption?,
        val results: List<StopOption>,
    ) : StopPickerUiState
}

@HiltViewModel
class StopPickerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    transitRepository: TransitRepository,
    observeReferenceLocation: ObserveReferenceLocationUseCase,
) : ViewModel() {
    val field: StopField = savedStateHandle.get<String>("field")
        ?.let { runCatching { StopField.valueOf(it) }.getOrNull() }
        ?: StopField.FROM

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")

    val uiState: StateFlow<StopPickerUiState> = combine(
        transitRepository.observeNetwork(),
        observeReferenceLocation(),
        query,
    ) { network, reference, q ->
        val options = ObserveNearestStopsUseCase.sortByDistance(reference, network.stops).map {
            StopOption(it.stop, it.distanceMeters, network.routesServing(it.stop.id))
        }
        StopPickerUiState.Success(
            query = q,
            nearest = if (q.isBlank() && reference.isDevice) options.firstOrNull() else null,
            results = filter(q, options, network),
        ) as StopPickerUiState
    }
        .catch { emit(StopPickerUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StopPickerUiState.Loading)

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value.take(MAX_QUERY_LENGTH)
    }

    companion object {
        const val KEY_QUERY = "query"
        const val MAX_QUERY_LENGTH = 60

        private fun normalize(text: String) = text.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), " ")

        /**
         * Matches stop names containing every word of the query, or stops served by a route whose number
         * equals the query (e.g. "1a"). Name-prefix matches rank first, then nearer stops.
         */
        fun filter(query: String, options: List<StopOption>, network: TransitNetwork): List<StopOption> {
            val q = normalize(query).trim()
            if (q.isEmpty()) return options
            val tokens = q.split(Regex("\\s+"))
            val routeMatch = network.routes.filter { normalize(it.number).trim() == q }.map { it.id }.toSet()
            return options
                .filter { option ->
                    val name = normalize(option.stop.name)
                    tokens.all { name.contains(it) } || option.routes.any { it.id in routeMatch }
                }
                .sortedWith(compareBy({ !normalize(it.stop.name).trimStart().startsWith(q) }, { it.distanceMeters }))
        }
    }
}
