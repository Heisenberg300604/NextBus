package com.ncs.nextbus.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.domain.SearchJourneysUseCase
import com.ncs.nextbus.core.model.JourneyOption
import com.ncs.nextbus.core.model.Stop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface SearchResultsUiState {
    data object Loading : SearchResultsUiState
    data object Error : SearchResultsUiState

    /** One of the stops no longer exists (e.g. an old recent search after a data update). */
    data object UnknownStops : SearchResultsUiState
    data class Success(
        val from: Stop,
        val to: Stop,
        val options: List<JourneyOption>,
        val isSaved: Boolean,
    ) : SearchResultsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transitRepository: TransitRepository,
    private val searchJourneys: SearchJourneysUseCase,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {
    private val fromStopId: String = savedStateHandle["fromStopId"] ?: ""
    private val toStopId: String = savedStateHandle["toStopId"] ?: ""
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<SearchResultsUiState> = retryTrigger
        .flatMapLatest {
            transitRepository.observeNetwork().flatMapLatest { network ->
                val from = network.stopsById[fromStopId]
                val to = network.stopsById[toStopId]
                if (from == null || to == null) {
                    flowOf(SearchResultsUiState.UnknownStops)
                } else {
                    combine(searchJourneys(fromStopId, toStopId), userDataRepository.savedJourneys) { options, saved ->
                        SearchResultsUiState.Success(
                            from = from,
                            to = to,
                            options = options,
                            isSaved = saved.any { it.fromStopId == fromStopId && it.toStopId == toStopId },
                        )
                    }
                }
            }
                .onStart { emit(SearchResultsUiState.Loading) }
                .catch { error ->
                    Timber.e(error, "Journey search failed")
                    emit(SearchResultsUiState.Error)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResultsUiState.Loading)

    init {
        viewModelScope.launch {
            runCatching { userDataRepository.recordSearch(fromStopId, toStopId) }
                .onFailure { Timber.w(it, "Could not record recent search") }
        }
    }

    fun toggleSaved() {
        val current = uiState.value as? SearchResultsUiState.Success ?: return
        viewModelScope.launch { userDataRepository.setSavedJourney(fromStopId, toStopId, !current.isSaved) }
    }

    fun retry() = retryTrigger.update { it + 1 }
}
