package com.ncs.nextbus.core.domain

import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.StopArrival
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Buses due at a stop, soonest first. Excludes buses that terminate at the stop (nobody can board them). */
class ObserveStopArrivalsUseCase @Inject constructor(
    private val liveBusRepository: LiveBusRepository,
) {
    operator fun invoke(stopId: String, horizonSeconds: Int = DEFAULT_HORIZON_SECONDS): Flow<List<StopArrival>> =
        liveBusRepository.liveBuses.map { arrivalsAt(stopId, it, horizonSeconds) }

    companion object {
        const val DEFAULT_HORIZON_SECONDS = 60 * 60

        fun arrivalsAt(stopId: String, buses: List<LiveBus>, horizonSeconds: Int): List<StopArrival> =
            buses.flatMap { bus ->
                bus.predictions
                    .filter { it.stopId == stopId && it.etaSeconds <= horizonSeconds }
                    .filterNot { prediction ->
                        val terminates = when (prediction.direction) {
                            Direction.OUTBOUND -> prediction.stopIndex == bus.route.stops.lastIndex
                            Direction.INBOUND -> prediction.stopIndex == 0
                        }
                        terminates
                    }
                    .map { StopArrival(bus, it.direction, it.etaSeconds) }
            }.sortedBy { it.etaSeconds }
    }
}
