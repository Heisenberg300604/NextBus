package com.ncs.nextbus.core.data.seed

import com.ncs.nextbus.core.common.GeoMath
import com.ncs.nextbus.core.model.Bus
import com.ncs.nextbus.core.model.FuelType
import com.ncs.nextbus.core.model.GeoPoint
import com.ncs.nextbus.core.model.Route
import com.ncs.nextbus.core.model.RouteStop
import com.ncs.nextbus.core.model.Stop
import com.ncs.nextbus.core.model.TransitNetwork
import kotlin.math.ceil
import kotlin.math.min

/**
 * Expands [SeedNetwork] into a full [TransitNetwork]: stop distances, a scheduled timetable and
 * a fleet sized so that the advertised headway is exactly achievable.
 */
internal object SeedBuilder {
    /** Dwell at each intermediate stop. */
    const val DWELL_MINUTES = 0.5

    /** Minimum turnaround time at each terminus. */
    const val MIN_LAYOVER_MINUTES = 3.0

    fun build(): TransitNetwork {
        val stops = SeedNetwork.stops.map { Stop(it.id, it.name, GeoPoint(it.latitude, it.longitude)) }
        val stopsById = stops.associateBy { it.id }
        val routes = SeedNetwork.routes.map { buildRoute(it, stopsById) }
        val buses = buildFleet(routes)
        return TransitNetwork(stops = stops, routes = routes, buses = buses)
    }

    /** Typical urban bus speed for a segment: short hops between close stops are slower. */
    fun segmentSpeedKmh(segmentMeters: Double): Double = 16.0 + min(14.0, segmentMeters / 1000.0 * 4.0)

    private fun buildRoute(seed: SeedNetwork.SeedRoute, stopsById: Map<String, Stop>): Route {
        val routeStops = seed.stopIds.map { id -> requireNotNull(stopsById[id]) { "Unknown stop $id in ${seed.id}" } }
        val path = routeStops.map { it.location }

        var distance = 0.0
        var arrival = 0.0
        val built = routeStops.mapIndexed { index, stop ->
            if (index > 0) {
                val segment = GeoMath.distanceMeters(path[index - 1], path[index])
                val departurePrevious = arrival + if (index - 1 > 0) DWELL_MINUTES else 0.0
                distance += segment
                arrival = departurePrevious + segment / (segmentSpeedKmh(segment) * 1000.0 / 60.0)
            }
            RouteStop(stop = stop, index = index, distanceFromStartMeters = distance, minutesFromStart = arrival)
        }

        val runMinutes = built.last().minutesFromStart
        val busCount = busesNeeded(runMinutes, seed.headwayMinutes)
        // One full cycle (out, layover, back, layover) must equal busCount * headway exactly.
        val layover = seed.headwayMinutes * busCount / 2.0 - runMinutes

        return Route(
            id = seed.id,
            number = seed.number,
            name = seed.name,
            colorArgb = seed.colorArgb,
            fuelType = seed.fuelType,
            headwayMinutes = seed.headwayMinutes,
            layoverMinutes = layover,
            path = path,
            stops = built,
        )
    }

    fun busesNeeded(runMinutes: Double, headwayMinutes: Int): Int =
        ceil(2 * (runMinutes + MIN_LAYOVER_MINUTES) / headwayMinutes).toInt()

    private fun buildFleet(routes: List<Route>): List<Bus> {
        var counter = 0
        return routes.flatMap { route ->
            val seed = SeedNetwork.routes.first { it.id == route.id }
            val cycleMinutes = 2 * (route.runMinutes + route.layoverMinutes)
            val fleetSize = Math.round(cycleMinutes / route.headwayMinutes).toInt()
            (0 until fleetSize).map { fleetIndex ->
                val n = counter++
                Bus(
                    id = "${route.id}-b${fleetIndex + 1}",
                    routeId = route.id,
                    registration = "CH01 G${seed.registrationSeries} ${2101 + n}",
                    capacity = when (route.fuelType) {
                        FuelType.ELECTRIC -> 40
                        FuelType.CNG -> 52
                        FuelType.DIESEL -> 56
                    },
                    driverName = SeedNetwork.driverNames[n % SeedNetwork.driverNames.size],
                    // Deliberately non-dialable demo numbers (000 exchange).
                    driverPhone = "+91 172 000 %04d".format(1100 + n),
                    fleetIndex = fleetIndex,
                    hasUnreliableGps = route.id == SeedNetwork.UNRELIABLE_GPS_ROUTE_ID &&
                        fleetIndex == SeedNetwork.UNRELIABLE_GPS_FLEET_INDEX,
                )
            }
        }
    }
}
