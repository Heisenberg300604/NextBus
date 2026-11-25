package com.ncs.nextbus.feature.bus

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ncs.nextbus.core.domain.TripManager
import com.ncs.nextbus.testing.FakeConnectivityRepository
import com.ncs.nextbus.testing.FakeLiveBusRepository
import com.ncs.nextbus.testing.FakeTransitRepository
import com.ncs.nextbus.testing.FakeUserDataRepository
import com.ncs.nextbus.testing.MainDispatcherRule
import com.ncs.nextbus.testing.seedNetwork
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BusDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userData = FakeUserDataRepository()

    private fun viewModel(busId: String): BusDetailViewModel {
        val transit = FakeTransitRepository()
        return BusDetailViewModel(
            SavedStateHandle(mapOf("busId" to busId)),
            transit,
            FakeLiveBusRepository(),
            userData,
            FakeConnectivityRepository(),
            TripManager(userData, transit),
        )
    }

    @Test
    fun `unknown bus id shows not found`() = runTest {
        viewModel("missing").uiState.test {
            var state = awaitItem()
            while (state == BusDetailUiState.Loading) state = awaitItem()
            assertEquals(BusDetailUiState.NotFound, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `start then end a trip, and toggle favourite`() = runTest {
        val busId = seedNetwork.buses.first().id
        val vm = viewModel(busId)
        vm.uiState.test {
            var state = awaitItem()
            while (state !is BusDetailUiState.Success) state = awaitItem()
            assertFalse(state.content.isOnThisBus)

            vm.startTrip()
            while (!state.let { it is BusDetailUiState.Success && it.content.isOnThisBus }) state = awaitItem()

            vm.toggleFavoriteRoute()
            while (!state.let { it is BusDetailUiState.Success && it.content.isFavoriteRoute }) state = awaitItem()

            vm.endTrip()
            while (!state.let { it is BusDetailUiState.Success && it.content.activeTrip == null }) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        val trip = userData.tripsState.value.single()
        assertFalse(trip.isActive)
        assertTrue(trip.alightStopId != null)
        vm.eventFlow.test {
            assertEquals(BusDetailEvent.TripStarted, awaitItem())
            assertEquals(BusDetailEvent.FavoriteChanged(true), awaitItem())
            assertEquals(BusDetailEvent.TripEnded, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
