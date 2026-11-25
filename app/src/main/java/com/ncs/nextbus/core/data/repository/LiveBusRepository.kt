package com.ncs.nextbus.core.data.repository

import com.ncs.nextbus.core.common.DefaultDispatcher
import com.ncs.nextbus.core.common.TimeSource
import com.ncs.nextbus.core.data.simulator.BusSimulator
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.LiveBus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time vehicle positions and stop predictions, shaped like a GTFS-realtime feed
 * (vehicle positions + trip updates). Swap the implementation to plug in a real backend.
 */
interface LiveBusRepository {
    /** Every bus in the fleet, refreshed continuously while collected. */
    val liveBuses: Flow<List<LiveBus>>

    fun observeBus(busId: String): Flow<LiveBus?> =
        liveBuses.map { buses -> buses.firstOrNull { it.bus.id == busId } }.distinctUntilChanged()
}

/** Published timetable. */
interface ScheduleRepository {
    suspend fun scheduledArrivals(
        routeId: String,
        stopIndex: Int,
        direction: Direction,
        fromMillis: Long,
        count: Int,
    ): List<Long>
}

@Singleton
class SimulatedLiveBusRepository @Inject constructor(
    private val transitRepository: TransitRepository,
    private val timeSource: TimeSource,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : LiveBusRepository, ScheduleRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val liveBuses: Flow<List<LiveBus>> = transitRepository.observeNetwork()
        .map { BusSimulator(it) }
        .flatMapLatest { simulator ->
            flow {
                while (true) {
                    emit(simulator.snapshot(timeSource.nowMillis()))
                    delay(TICK_MILLIS)
                }
            }
        }
        .flowOn(defaultDispatcher)

    override suspend fun scheduledArrivals(
        routeId: String,
        stopIndex: Int,
        direction: Direction,
        fromMillis: Long,
        count: Int,
    ): List<Long> = withContext(defaultDispatcher) {
        BusSimulator(transitRepository.getNetwork())
            .scheduledArrivals(routeId, stopIndex, direction, fromMillis, count)
    }

    companion object {
        const val TICK_MILLIS = 1_000L
    }
}
