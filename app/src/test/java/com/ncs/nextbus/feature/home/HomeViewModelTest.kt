package com.ncs.nextbus.feature.home

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ncs.nextbus.core.domain.ObserveNearbyBusesUseCase
import com.ncs.nextbus.core.domain.ObserveReferenceLocationUseCase
import com.ncs.nextbus.core.model.DeviceLocation
import com.ncs.nextbus.core.model.GeoPoint
import com.ncs.nextbus.core.model.LocationSource
import com.ncs.nextbus.navigation.StopField
import com.ncs.nextbus.testing.FakeLiveBusRepository
import com.ncs.nextbus.testing.FakeLocationRepository
import com.ncs.nextbus.testing.FakePreferencesRepository
import com.ncs.nextbus.testing.FakeTransitRepository
import com.ncs.nextbus.testing.FakeUserDataRepository
import com.ncs.nextbus.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val location = FakeLocationRepository()
    private val viewModel by lazy {
        HomeViewModel(
            SavedStateHandle(),
            FakeTransitRepository(),
            ObserveNearbyBusesUseCase(FakeLiveBusRepository(), ObserveReferenceLocationUseCase(location)),
            FakeUserDataRepository(),
            FakePreferencesRepository(),
        )
    }

    private suspend fun ReceiveTurbine<HomeUiState>.awaitContent(predicate: (HomeContent) -> Boolean = { true }): HomeContent {
        while (true) {
            val state = awaitItem()
            if (state is HomeUiState.Success && predicate(state.content)) return state.content
        }
    }

    @Test
    fun `without permission content is measured from the city centre`() = runTest {
        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals(LocationSource.NO_PERMISSION, content.reference.source)
            assertEquals("plaza", content.nearestStop?.stop?.id)
            assertTrue(content.nearbyBuses.size <= HomeViewModel.NEARBY_LIMIT)
            assertNull(content.activeTrip)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `device location near a stop makes it the nearest stop`() = runTest {
        location.location.value = DeviceLocation.Located(GeoPoint(30.7586, 76.7681))
        viewModel.uiState.test {
            val content = awaitContent { it.reference.isDevice }
            assertEquals("pu", content.nearestStop?.stop?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `picking and swapping stops drives the planner`() = runTest {
        viewModel.uiState.test {
            awaitContent()
            viewModel.onStopPicked(StopField.FROM, "plaza")
            viewModel.onStopPicked(StopField.TO, "tribune")
            var content = awaitContent { it.to != null }
            assertEquals("plaza", content.from?.id)
            assertTrue(content.canSearch)

            viewModel.swapStops()
            content = awaitContent { it.from?.id == "tribune" }
            assertEquals("plaza", content.to?.id)

            viewModel.onStopPicked(StopField.TO, "tribune")
            content = awaitContent { it.isSameStop }
            assertFalse(content.canSearch)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
