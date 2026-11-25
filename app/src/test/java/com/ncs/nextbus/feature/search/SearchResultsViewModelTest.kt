package com.ncs.nextbus.feature.search

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ncs.nextbus.core.domain.SearchJourneysUseCase
import com.ncs.nextbus.testing.FakeLiveBusRepository
import com.ncs.nextbus.testing.FakeTransitRepository
import com.ncs.nextbus.testing.FakeUserDataRepository
import com.ncs.nextbus.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchResultsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userData = FakeUserDataRepository()

    private fun viewModel(from: String, to: String): SearchResultsViewModel {
        val transit = FakeTransitRepository()
        return SearchResultsViewModel(
            SavedStateHandle(mapOf("fromStopId" to from, "toStopId" to to)),
            transit,
            SearchJourneysUseCase(transit, FakeLiveBusRepository()),
            userData,
        )
    }

    @Test
    fun `shows direct options and records the search`() = runTest {
        val vm = viewModel("isbt43", "pec")
        vm.uiState.test {
            var state = awaitItem()
            while (state !is SearchResultsUiState.Success) state = awaitItem()
            assertEquals("r1", state.options.single().route.id)
            assertFalse(state.isSaved)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("isbt43", userData.recentSearches.first().single().fromStopId)
    }

    @Test
    fun `saving toggles the saved flag`() = runTest {
        val vm = viewModel("plaza", "tribune")
        vm.uiState.test {
            while (awaitItem() !is SearchResultsUiState.Success) Unit
            vm.toggleSaved()
            var state = awaitItem()
            while (state !is SearchResultsUiState.Success || !state.isSaved) state = awaitItem()
            assertTrue(state.isSaved)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unknown stop ids give a dedicated state`() = runTest {
        viewModel("nowhere", "pec").uiState.test {
            var state = awaitItem()
            while (state == SearchResultsUiState.Loading) state = awaitItem()
            assertEquals(SearchResultsUiState.UnknownStops, state)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
