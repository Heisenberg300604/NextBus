package com.ncs.nextbus.core.data.simulator

import com.ncs.nextbus.core.model.Bus
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.StopPrediction
import com.ncs.nextbus.core.model.TransitNetwork
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Deterministic, clock-driven bus simulator. Given the same network and time it always produces the
 * same snapshot, so it needs no stored state and is fully unit-testable.
 *
 * Realism knobs: each bus drifts between 0 and ~3 minutes late on a slow sine wave, and buses flagged
 * [Bus.hasUnreliableGps] stop reporting for a few minutes every [SIGNAL_LOSS_PERIOD_MINUTES].
 */
class BusSimulator(private val network: TransitNetwork) {
    private val timings: Map<String, RouteTiming> = network.routes.associate { it.id to RouteTiming(it) }

    fun snapshot(nowMillis: Long): List<LiveBus> =
        network.buses.mapNotNull { bus -> simulate(bus, nowMillis) }

    fun snapshot(busId: String, nowMillis: Long): LiveBus? =
        network.busesById[busId]?.let { simulate(it, nowMillis) }

    /**
     * Scheduled (delay-free) times at which buses on [routeId] reach [stopIndex] in [direction],
     * starting at or after [fromMillis].
     */
    fun scheduledArrivals(
        routeId: String,
        stopIndex: Int,
        direction: Direction,
        fromMillis: Long,
        count: Int,
    ): List<Long> {
        val timing = timings[routeId] ?: return emptyList()
        val headway = timing.route.headwayMinutes.toDouble()
        val target = timing.arrivalPhase(stopIndex, direction)
        // Bus k reaches the stop when (t + k*headway) mod cycle == target, i.e. t ≡ target (mod headway).
        val fromMinutes = fromMillis / MILLIS_PER_MINUTE
        val first = floor((fromMinutes - target) / headway) * headway + target
        return generateSequence(first) { it + headway }
            .map { (it * MILLIS_PER_MINUTE).toLong() }
            .filter { it >= fromMillis }
            .take(count)
            .toList()
    }

    private fun simulate(bus: Bus, nowMillis: Long): LiveBus? {
        val timing = timings[bus.routeId] ?: return null
        val route = timing.route

        val reportMillis = lastReportMillis(bus, nowMillis)
        val reportMinutes = reportMillis / MILLIS_PER_MINUTE
        val delay = delayMinutes(bus, reportMinutes)
        val phase = RouteTiming.floorMod(
            reportMinutes - delay + bus.fleetIndex * route.headwayMinutes,
            timing.cycle,
        )

        val position = timing.positionAt(phase)
        val staleSeconds = ((nowMillis - reportMillis) / 1000).toInt()

        val predictions = buildList {
            for (direction in Direction.entries) {
                for (stop in route.stops) {
                    val eta = etaMinutes(timing, stop.index, direction, phase)
                    add(
                        StopPrediction(
                            stopId = stop.stop.id,
                            stopIndex = stop.index,
                            direction = direction,
                            etaSeconds = max(0, (eta * 60).roundToInt() - staleSeconds),
                        ),
                    )
                }
            }
        }

        return LiveBus(
            bus = bus,
            route = route,
            position = timing.pointAt(position.distanceMeters),
            bearingDegrees = position.bearingDegrees,
            speedKmh = position.speedKmh,
            direction = position.direction,
            activity = position.activity,
            distanceAlongRouteMeters = position.distanceMeters,
            nextStopIndex = position.nextStopIndex,
            delayMinutes = delay.roundToInt(),
            lastUpdatedMillis = reportMillis,
            predictions = predictions,
        )
    }

    /** Minutes until the bus at [phase] reaches the stop; zero while it is dwelling there. */
    private fun etaMinutes(timing: RouteTiming, stopIndex: Int, direction: Direction, phase: Double): Double {
        val arrival = timing.arrivalPhase(stopIndex, direction)
        val departure = timing.departurePhase(stopIndex, direction)
        if (departure > arrival && phase >= arrival && phase <= departure) return 0.0
        return RouteTiming.floorMod(arrival - phase, timing.cycle)
    }

    private fun lastReportMillis(bus: Bus, nowMillis: Long): Long {
        if (!bus.hasUnreliableGps) return nowMillis
        val period = SIGNAL_LOSS_PERIOD_MINUTES * MILLIS_PER_MINUTE
        val intoPeriod = nowMillis % period.toLong()
        return if (intoPeriod < SIGNAL_LOSS_DURATION_MINUTES * MILLIS_PER_MINUTE) nowMillis - intoPeriod else nowMillis
    }

    /** Slowly varying lateness in [0, ~2.8] minutes. Its rate of change stays well below 1, so buses never reverse. */
    internal fun delayMinutes(bus: Bus, nowMinutes: Double): Double {
        val offset = bus.fleetIndex * 1.7 + bus.routeId.hashCode() % 7
        return max(0.0, 1.0 + 1.8 * sin(2 * PI * nowMinutes / DELAY_PERIOD_MINUTES + offset))
    }

    companion object {
        const val MILLIS_PER_MINUTE = 60_000.0
        const val DELAY_PERIOD_MINUTES = 41.0
        const val SIGNAL_LOSS_PERIOD_MINUTES = 20.0
        const val SIGNAL_LOSS_DURATION_MINUTES = 7.0
    }
}
