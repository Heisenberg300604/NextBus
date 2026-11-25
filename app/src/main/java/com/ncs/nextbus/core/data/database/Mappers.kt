package com.ncs.nextbus.core.data.database

import com.ncs.nextbus.core.model.Bus
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.FuelType
import com.ncs.nextbus.core.model.GeoPoint
import com.ncs.nextbus.core.model.Route
import com.ncs.nextbus.core.model.RouteStop
import com.ncs.nextbus.core.model.Stop
import com.ncs.nextbus.core.model.TransitNetwork
import com.ncs.nextbus.core.model.Trip

internal object PathCodec {
    fun encode(points: List<GeoPoint>): String = points.joinToString(";") { "${it.latitude},${it.longitude}" }

    fun decode(encoded: String): List<GeoPoint> = encoded.split(';').mapNotNull { pair ->
        val parts = pair.split(',')
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lng = parts.getOrNull(1)?.toDoubleOrNull()
        if (lat != null && lng != null) GeoPoint(lat, lng) else null
    }
}

internal fun TransitNetwork.toEntities(): TransitEntities = TransitEntities(
    stops = stops.map { StopEntity(it.id, it.name, it.location.latitude, it.location.longitude) },
    routes = routes.map {
        RouteEntity(
            id = it.id,
            number = it.number,
            name = it.name,
            colorArgb = it.colorArgb,
            fuelType = it.fuelType.name,
            headwayMinutes = it.headwayMinutes,
            layoverMinutes = it.layoverMinutes,
            path = PathCodec.encode(it.path),
        )
    },
    routeStops = routes.flatMap { route ->
        route.stops.map {
            RouteStopEntity(route.id, it.index, it.stop.id, it.distanceFromStartMeters, it.minutesFromStart)
        }
    },
    buses = buses.map {
        BusEntity(
            id = it.id,
            routeId = it.routeId,
            registration = it.registration,
            capacity = it.capacity,
            driverName = it.driverName,
            driverPhone = it.driverPhone,
            fleetIndex = it.fleetIndex,
            hasUnreliableGps = it.hasUnreliableGps,
        )
    },
)

internal data class TransitEntities(
    val stops: List<StopEntity>,
    val routes: List<RouteEntity>,
    val routeStops: List<RouteStopEntity>,
    val buses: List<BusEntity>,
) {
    /** Rebuilds the domain network, skipping any row that references missing data. */
    fun toNetwork(): TransitNetwork {
        val stopsById = stops.associate {
            it.id to Stop(it.id, it.name, GeoPoint(it.latitude, it.longitude))
        }
        val stopsByRoute = routeStops.groupBy { it.routeId }
        val domainRoutes = routes.mapNotNull { entity ->
            val routeStops = stopsByRoute[entity.id].orEmpty().sortedBy { it.sequence }.mapIndexedNotNull { index, rs ->
                stopsById[rs.stopId]?.let { RouteStop(it, index, rs.distanceFromStartMeters, rs.minutesFromStart) }
            }
            val path = PathCodec.decode(entity.path)
            val fuel = runCatching { FuelType.valueOf(entity.fuelType) }.getOrNull()
            if (routeStops.size < 2 || path.size < 2 || fuel == null) return@mapNotNull null
            Route(
                id = entity.id,
                number = entity.number,
                name = entity.name,
                colorArgb = entity.colorArgb,
                fuelType = fuel,
                headwayMinutes = entity.headwayMinutes,
                layoverMinutes = entity.layoverMinutes,
                path = path,
                stops = routeStops,
            )
        }
        val routeIds = domainRoutes.map { it.id }.toSet()
        val domainBuses = buses.filter { it.routeId in routeIds }.map {
            Bus(
                id = it.id,
                routeId = it.routeId,
                registration = it.registration,
                capacity = it.capacity,
                driverName = it.driverName,
                driverPhone = it.driverPhone,
                fleetIndex = it.fleetIndex,
                hasUnreliableGps = it.hasUnreliableGps,
            )
        }
        return TransitNetwork(stops = stopsById.values.sortedBy { it.name }, routes = domainRoutes, buses = domainBuses)
    }
}

internal fun TripEntity.toDomain(): Trip = Trip(
    id = id,
    busId = busId,
    busRegistration = busRegistration,
    routeId = routeId,
    routeNumber = routeNumber,
    routeName = routeName,
    direction = runCatching { Direction.valueOf(direction) }.getOrDefault(Direction.OUTBOUND),
    boardStopId = boardStopId,
    boardStopName = boardStopName,
    alightStopId = alightStopId,
    alightStopName = alightStopName,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    distanceMeters = distanceMeters,
)
