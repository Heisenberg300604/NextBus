package com.ncs.nextbus.core.model

/** A WGS84 coordinate. Kept free of Maps SDK types so the domain stays plain Kotlin and unit-testable. */
data class GeoPoint(val latitude: Double, val longitude: Double)

enum class FuelType { ELECTRIC, CNG, DIESEL }

/**
 * Travel direction along a route. [OUTBOUND] runs from the first stop to the last stop,
 * [INBOUND] runs back from the last stop to the first.
 */
enum class Direction { OUTBOUND, INBOUND }

data class Stop(
    val id: String,
    val name: String,
    val location: GeoPoint,
)

data class RouteStop(
    val stop: Stop,
    val index: Int,
    val distanceFromStartMeters: Double,
    /** Scheduled outbound arrival, in minutes after the bus leaves the first stop. */
    val minutesFromStart: Double,
)

data class Route(
    val id: String,
    val number: String,
    val name: String,
    val colorArgb: Long,
    val fuelType: FuelType,
    /** A bus leaves each terminus every [headwayMinutes]. */
    val headwayMinutes: Int,
    /** Scheduled wait at each terminus before the return run. */
    val layoverMinutes: Double,
    val path: List<GeoPoint>,
    val stops: List<RouteStop>,
) {
    val origin: Stop get() = stops.first().stop
    val terminus: Stop get() = stops.last().stop
    val lengthMeters: Double get() = stops.last().distanceFromStartMeters
    val runMinutes: Double get() = stops.last().minutesFromStart

    /** The stop a bus travelling in [direction] is heading towards. */
    fun headsign(direction: Direction): Stop =
        if (direction == Direction.OUTBOUND) terminus else origin

    fun indexOf(stopId: String): Int = stops.indexOfFirst { it.stop.id == stopId }
}

data class Bus(
    val id: String,
    val routeId: String,
    val registration: String,
    val capacity: Int,
    val driverName: String,
    /** Only ever surfaced inside the SOS share payload, never on regular screens. */
    val driverPhone: String,
    /** Position of this bus in its route's rotation (0-based). */
    val fleetIndex: Int,
    /** Demo flag: this bus periodically stops reporting, to exercise stale/offline states. */
    val hasUnreliableGps: Boolean,
)

/** The static part of the transit network: stops, routes and the buses assigned to them. */
data class TransitNetwork(
    val stops: List<Stop>,
    val routes: List<Route>,
    val buses: List<Bus>,
) {
    val stopsById: Map<String, Stop> = stops.associateBy { it.id }
    val routesById: Map<String, Route> = routes.associateBy { it.id }
    val busesById: Map<String, Bus> = buses.associateBy { it.id }

    fun routesServing(stopId: String): List<Route> =
        routes.filter { route -> route.stops.any { it.stop.id == stopId } }
}
