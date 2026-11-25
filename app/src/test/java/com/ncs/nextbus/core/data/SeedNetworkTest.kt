package com.ncs.nextbus.core.data

import com.ncs.nextbus.core.common.GeoMath
import com.ncs.nextbus.core.data.database.toEntities
import com.ncs.nextbus.core.data.seed.SeedBuilder
import com.ncs.nextbus.core.data.seed.SeedNetwork
import com.ncs.nextbus.testing.seedNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedNetworkTest {
    private val network = seedNetwork

    @Test
    fun `stop route and bus ids are unique`() {
        assertEquals(network.stops.size, network.stops.map { it.id }.toSet().size)
        assertEquals(network.routes.size, network.routes.map { it.id }.toSet().size)
        assertEquals(network.buses.size, network.buses.map { it.id }.toSet().size)
    }

    @Test
    fun `every stop is served by at least one route`() {
        network.stops.forEach { stop ->
            assertTrue("${stop.id} is unserved", network.routesServing(stop.id).isNotEmpty())
        }
    }

    @Test
    fun `route timetables increase monotonically`() {
        network.routes.forEach { route ->
            route.stops.zipWithNext().forEach { (a, b) ->
                assertTrue("${route.id} distance", b.distanceFromStartMeters > a.distanceFromStartMeters)
                assertTrue("${route.id} minutes", b.minutesFromStart > a.minutesFromStart)
            }
        }
    }

    @Test
    fun `fleet size makes the advertised headway exact`() {
        network.routes.forEach { route ->
            val fleet = network.buses.filter { it.routeId == route.id }
            val cycle = 2 * (route.runMinutes + route.layoverMinutes)
            assertEquals("${route.id} cycle", fleet.size * route.headwayMinutes.toDouble(), cycle, 1e-6)
            assertTrue("${route.id} layover too short", route.layoverMinutes >= SeedBuilder.MIN_LAYOVER_MINUTES)
            assertEquals((0 until fleet.size).toList(), fleet.map { it.fleetIndex }.sorted())
        }
    }

    @Test
    fun `run times and speeds are realistic for city buses`() {
        network.routes.forEach { route ->
            val kmh = route.lengthMeters / 1000 / (route.runMinutes / 60)
            assertTrue("${route.id} average $kmh km/h", kmh in 12.0..30.0)
            assertTrue("${route.id} run ${route.runMinutes} min", route.runMinutes in 10.0..45.0)
        }
    }

    @Test
    fun `exactly one bus simulates an unreliable gps`() {
        assertEquals(1, network.buses.count { it.hasUnreliableGps })
    }

    @Test
    fun `stops are within the service area`() {
        network.stops.forEach { stop ->
            val d = GeoMath.distanceMeters(stop.location, SeedNetwork.CITY_CENTER)
            assertTrue("${stop.id} is ${d}m from centre", d < 10_000)
        }
    }

    @Test
    fun `network survives a round trip through the database entities`() {
        val restored = network.toEntities().toNetwork()
        assertEquals(network.routes, restored.routes)
        assertEquals(network.buses, restored.buses)
        assertEquals(network.stops.toSet(), restored.stops.toSet())
    }
}
