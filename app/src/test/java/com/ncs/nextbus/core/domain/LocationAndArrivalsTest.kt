package com.ncs.nextbus.core.domain

import com.ncs.nextbus.core.data.seed.SeedNetwork
import com.ncs.nextbus.core.data.simulator.BusSimulator
import com.ncs.nextbus.core.model.DeviceLocation
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.GeoPoint
import com.ncs.nextbus.core.model.LocationSource
import com.ncs.nextbus.core.model.ReferenceLocation
import com.ncs.nextbus.testing.FIXED_NOW
import com.ncs.nextbus.testing.seedNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAndArrivalsTest {
    private val buses = BusSimulator(seedNetwork).snapshot(FIXED_NOW)

    @Test
    fun `device location inside the service area is used`() {
        val pu = GeoPoint(30.7585, 76.7680)
        assertEquals(
            ReferenceLocation(pu, LocationSource.DEVICE),
            ObserveReferenceLocationUseCase.resolve(DeviceLocation.Located(pu)),
        )
    }

    @Test
    fun `falls back to the city centre with a reason`() {
        val mountainView = GeoPoint(37.422, -122.084)
        assertEquals(
            LocationSource.OUT_OF_SERVICE_AREA,
            ObserveReferenceLocationUseCase.resolve(DeviceLocation.Located(mountainView)).source,
        )
        assertEquals(LocationSource.NO_PERMISSION, ObserveReferenceLocationUseCase.resolve(DeviceLocation.NoPermission).source)
        val unavailable = ObserveReferenceLocationUseCase.resolve(DeviceLocation.Unavailable)
        assertEquals(LocationSource.UNAVAILABLE, unavailable.source)
        assertEquals(SeedNetwork.CITY_CENTER, unavailable.point)
    }

    @Test
    fun `nearby buses and stops are sorted by distance`() {
        val reference = ReferenceLocation(SeedNetwork.CITY_CENTER, LocationSource.DEVICE)
        val nearbyBuses = ObserveNearbyBusesUseCase.sortByDistance(reference, buses)
        assertEquals(buses.size, nearbyBuses.size)
        assertEquals(nearbyBuses.sortedBy { it.distanceMeters }, nearbyBuses)

        val stops = ObserveNearestStopsUseCase.sortByDistance(reference, seedNetwork.stops)
        assertEquals("plaza", stops.first().stop.id)
        assertEquals(0.0, stops.first().distanceMeters, 1.0)
    }

    @Test
    fun `stop arrivals are sorted, within horizon, and exclude terminating buses`() {
        val arrivals = ObserveStopArrivalsUseCase.arrivalsAt("pec", buses, horizonSeconds = 60 * 60)
        assertTrue(arrivals.isNotEmpty())
        assertEquals(arrivals.sortedBy { it.etaSeconds }, arrivals)
        assertTrue(arrivals.all { it.etaSeconds <= 3600 })
        // PEC is the last stop of route 1A, so only buses heading back (inbound) can be boarded there.
        assertTrue(arrivals.all { it.direction == Direction.INBOUND })
    }
}
