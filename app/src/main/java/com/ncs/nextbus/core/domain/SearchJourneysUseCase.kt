package com.ncs.nextbus.core.domain

import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.JourneyOption
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.TransitNetwork
import com.ncs.nextbus.core.model.UpcomingBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

/** Finds every direct route between two stops, with the next live buses for each. */
class SearchJourneysUseCase @Inject constructor(
    private val transitRepository: TransitRepository,
    private val liveBusRepository: LiveBusRepository,
) {
    operator fun invoke(fromStopId: String, toStopId: String): Flow<List<JourneyOption>> =
        combine(transitRepository.observeNetwork(), liveBusRepository.liveBuses) { network, buses ->
            findJourneys(network, buses, fromStopId, toStopId)
        }

    companion object {
        const val MAX_UPCOMING = 3

        fun findJourneys(
            network: TransitNetwork,
            liveBuses: List<LiveBus>,
            fromStopId: String,
            toStopId: String,
        ): List<JourneyOption> {
            if (fromStopId == toStopId) return emptyList()
            val busesByRoute = liveBuses.groupBy { it.route.id }
            return network.routes.mapNotNull { route ->
                val from = route.indexOf(fromStopId)
                val to = route.indexOf(toStopId)
                if (from < 0 || to < 0) return@mapNotNull null

                val direction = if (from < to) Direction.OUTBOUND else Direction.INBOUND
                val board = route.stops[from]
                val alight = route.stops[to]
                val distance = abs(alight.distanceFromStartMeters - board.distanceFromStartMeters)
                val upcoming = busesByRoute[route.id].orEmpty()
                    .mapNotNull { bus -> bus.etaSecondsTo(fromStopId, direction)?.let { UpcomingBus(bus, it) } }
                    .sortedBy { it.etaSeconds }
                    .take(MAX_UPCOMING)

                JourneyOption(
                    route = route,
                    direction = direction,
                    boardStop = board,
                    alightStop = alight,
                    rideMinutes = abs(alight.minutesFromStart - board.minutesFromStart).roundToInt().coerceAtLeast(1),
                    distanceMeters = distance,
                    fareRupees = FareCalculator.fareFor(distance),
                    upcoming = upcoming,
                )
            }.sortedBy { option ->
                // Soonest arrival at the destination first.
                (option.nextEtaSeconds ?: Int.MAX_VALUE / 2) + option.rideMinutes * 60
            }
        }
    }
}
