package com.ncs.nextbus.core.model

/** One direct-bus way of getting between two stops. */
data class JourneyOption(
    val route: Route,
    val direction: Direction,
    val boardStop: RouteStop,
    val alightStop: RouteStop,
    val rideMinutes: Int,
    val distanceMeters: Double,
    val fareRupees: Int,
    /** Live buses that will reach [boardStop] in [direction], soonest first. */
    val upcoming: List<UpcomingBus>,
) {
    val nextEtaSeconds: Int? get() = upcoming.firstOrNull()?.etaSeconds
}

data class UpcomingBus(val liveBus: LiveBus, val etaSeconds: Int)

/** A bus expected at a given stop. */
data class StopArrival(
    val liveBus: LiveBus,
    val direction: Direction,
    val etaSeconds: Int,
)

data class NearbyBus(val liveBus: LiveBus, val distanceMeters: Double)

data class StopWithDistance(val stop: Stop, val distanceMeters: Double)

/** Where "nearby" is measured from. Falls back to the city centre when the device location can't be used. */
data class ReferenceLocation(val point: GeoPoint, val source: LocationSource) {
    val isDevice: Boolean get() = source == LocationSource.DEVICE
}

enum class LocationSource {
    DEVICE,
    LOCATING,
    NO_PERMISSION,
    OUT_OF_SERVICE_AREA,
    UNAVAILABLE,
}

/** What the device location provider currently reports. */
sealed interface DeviceLocation {
    data object NoPermission : DeviceLocation
    data object Unavailable : DeviceLocation
    data class Located(val point: GeoPoint) : DeviceLocation
}
