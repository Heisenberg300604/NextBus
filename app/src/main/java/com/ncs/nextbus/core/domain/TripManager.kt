package com.ncs.nextbus.core.domain

import com.ncs.nextbus.core.data.repository.NewTrip
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.Trip
import javax.inject.Inject
import kotlin.math.abs

sealed interface StartTripResult {
    data class Started(val tripId: Long) : StartTripResult

    /** Only one trip can be active at a time. */
    data class AlreadyActive(val trip: Trip) : StartTripResult
}

/** Records rides: boarding at the bus's current/next stop, alighting wherever it is when the trip ends. */
class TripManager @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val transitRepository: TransitRepository,
) {
    suspend fun start(liveBus: LiveBus): StartTripResult {
        userDataRepository.currentActiveTrip()?.let { return StartTripResult.AlreadyActive(it) }
        val board = liveBus.nextStop.stop
        val id = userDataRepository.startTrip(
            NewTrip(
                busId = liveBus.bus.id,
                busRegistration = liveBus.bus.registration,
                routeId = liveBus.route.id,
                routeNumber = liveBus.route.number,
                routeName = liveBus.route.name,
                direction = liveBus.direction,
                boardStopId = board.id,
                boardStopName = board.name,
            ),
        )
        return StartTripResult.Started(id)
    }

    /**
     * Ends [trip]. With a live position the user alights at the bus's current/next stop; without one
     * (bus offline) the trip is closed at the boarding stop with zero distance.
     */
    suspend fun end(trip: Trip, liveBus: LiveBus?) {
        val route = transitRepository.getNetwork().routesById[trip.routeId]
        val boardIndex = route?.indexOf(trip.boardStopId) ?: -1
        val alight = liveBus?.takeIf { it.bus.id == trip.busId }?.nextStop
        if (route == null || boardIndex < 0 || alight == null) {
            userDataRepository.endTrip(trip.id, trip.boardStopId, trip.boardStopName, 0.0)
            return
        }
        val distance = abs(alight.distanceFromStartMeters - route.stops[boardIndex].distanceFromStartMeters)
        userDataRepository.endTrip(trip.id, alight.stop.id, alight.stop.name, distance)
    }
}
