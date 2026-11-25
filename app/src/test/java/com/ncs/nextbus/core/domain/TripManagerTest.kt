package com.ncs.nextbus.core.domain

import com.ncs.nextbus.core.data.simulator.BusSimulator
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.testing.FIXED_NOW
import com.ncs.nextbus.testing.FakeTransitRepository
import com.ncs.nextbus.testing.FakeUserDataRepository
import com.ncs.nextbus.testing.seedNetwork
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TripManagerTest {
    private val simulator = BusSimulator(seedNetwork)
    private val userData = FakeUserDataRepository()
    private val manager = TripManager(userData, FakeTransitRepository())

    @Test
    fun `starting a trip boards at the bus's next stop`() = runTest {
        val bus = simulator.snapshot(FIXED_NOW).first()
        val result = manager.start(bus)
        assertTrue(result is StartTripResult.Started)
        val trip = userData.currentActiveTrip()!!
        assertEquals(bus.nextStop.stop.id, trip.boardStopId)
        assertEquals(bus.route.number, trip.routeNumber)
    }

    @Test
    fun `only one trip can be active`() = runTest {
        val buses = simulator.snapshot(FIXED_NOW)
        manager.start(buses[0])
        val second = manager.start(buses[1])
        assertTrue(second is StartTripResult.AlreadyActive)
        assertEquals(1, userData.tripsState.value.size)
    }

    @Test
    fun `ending a trip records alight stop and distance along the route`() = runTest {
        val start = simulator.snapshot(FIXED_NOW).first { it.direction == Direction.OUTBOUND && it.nextStopIndex <= 2 }
        manager.start(start)
        val trip = userData.currentActiveTrip()!!
        val later = simulator.snapshot(start.bus.id, FIXED_NOW + 8 * 60_000)!!

        manager.end(trip, later)

        val ended = userData.tripsState.value.single()
        assertFalse(ended.isActive)
        assertEquals(later.nextStop.stop.id, ended.alightStopId)
        val expected = abs(later.nextStop.distanceFromStartMeters - start.nextStop.distanceFromStartMeters)
        assertEquals(expected, ended.distanceMeters!!, 1e-6)
    }

    @Test
    fun `ending without a live position closes the trip at the boarding stop`() = runTest {
        manager.start(simulator.snapshot(FIXED_NOW).first())
        val trip = userData.currentActiveTrip()!!
        manager.end(trip, liveBus = null)
        val ended = userData.tripsState.value.single()
        assertEquals(trip.boardStopId, ended.alightStopId)
        assertEquals(0.0, ended.distanceMeters!!, 0.0)
    }
}
