package com.ncs.nextbus.core.data.simulator

import com.ncs.nextbus.core.common.PolylineMeasure
import com.ncs.nextbus.core.data.seed.SeedBuilder
import com.ncs.nextbus.core.model.BusActivity
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.Route

/**
 * The repeating schedule of one route, expressed as a cycle phase in minutes:
 *
 * ```
 * [0, L)            layover at the first stop, about to run OUTBOUND
 * [L, L+T)          outbound run
 * [L+T, 2L+T)       layover at the last stop, about to run INBOUND
 * [2L+T, 2L+2T)     inbound run
 * ```
 * where L is the layover and T the scheduled run time. Bus `k` of the fleet is `k * headway` minutes
 * ahead in the cycle, so a bus leaves each terminus exactly every headway.
 */
internal class RouteTiming(val route: Route) {
    private val stopCount = route.stops.size
    private val lastIndex = stopCount - 1
    private val measure = PolylineMeasure(route.path)

    /** Outbound scheduled arrival at each stop, minutes after leaving stop 0. */
    private val arrivals = DoubleArray(stopCount) { route.stops[it].minutesFromStart }

    /** Outbound scheduled departure from each stop (arrival + dwell for intermediate stops). */
    private val departures = DoubleArray(stopCount) { i ->
        when (i) {
            0 -> 0.0
            lastIndex -> arrivals[i]
            else -> arrivals[i] + SeedBuilder.DWELL_MINUTES
        }
    }

    val run: Double = arrivals[lastIndex]
    val layover: Double = route.layoverMinutes
    val cycle: Double = 2 * (run + layover)

    /** Cycle phase at which a bus arrives at (or, for a run's first stop, departs from) [stopIndex]. */
    fun arrivalPhase(stopIndex: Int, direction: Direction): Double = when (direction) {
        Direction.OUTBOUND -> if (stopIndex == 0) layover else layover + arrivals[stopIndex]
        Direction.INBOUND -> {
            val inboundStart = 2 * layover + run
            if (stopIndex == lastIndex) inboundStart else inboundStart + (run - departures[stopIndex])
        }
    }

    /** Cycle phase at which a bus leaves [stopIndex]. Equal to the arrival phase at a run's last stop. */
    fun departurePhase(stopIndex: Int, direction: Direction): Double = when (direction) {
        Direction.OUTBOUND -> layover + departures[stopIndex]
        Direction.INBOUND -> {
            val inboundStart = 2 * layover + run
            if (stopIndex == lastIndex) inboundStart else inboundStart + (run - arrivals[stopIndex])
        }
    }

    data class Position(
        val direction: Direction,
        val activity: BusActivity,
        val distanceMeters: Double,
        val nextStopIndex: Int,
        val speedKmh: Double,
        val bearingDegrees: Float,
    )

    fun positionAt(phase: Double): Position {
        val p = floorMod(phase, cycle)
        return when {
            p < layover -> stationary(Direction.OUTBOUND, BusActivity.LAYOVER, 0)
            p < layover + run -> moving(Direction.OUTBOUND, outboundTime = p - layover)
            p < 2 * layover + run -> stationary(Direction.INBOUND, BusActivity.LAYOVER, lastIndex)
            else -> moving(Direction.INBOUND, outboundTime = run - (p - (2 * layover + run)))
        }
    }

    private fun stationary(direction: Direction, activity: BusActivity, stopIndex: Int): Position {
        val distance = route.stops[stopIndex].distanceFromStartMeters
        return Position(direction, activity, distance, stopIndex, 0.0, bearing(distance, direction))
    }

    /**
     * Positions are computed on the outbound timeline. An inbound bus at inbound time `e` is where an
     * outbound bus would be at `T - e`, so dwell windows mirror correctly.
     */
    private fun moving(direction: Direction, outboundTime: Double): Position {
        val t = outboundTime.coerceIn(0.0, run)
        var i = 0
        while (i < lastIndex - 1 && t >= arrivals[i + 1]) i++

        if (t <= departures[i] || t >= run) {
            val stopIndex = if (t >= run) lastIndex else i
            val distance = route.stops[stopIndex].distanceFromStartMeters
            return Position(direction, BusActivity.AT_STOP, distance, stopIndex, 0.0, bearing(distance, direction))
        }

        val from = route.stops[i].distanceFromStartMeters
        val to = route.stops[i + 1].distanceFromStartMeters
        val travelMinutes = arrivals[i + 1] - departures[i]
        val fraction = (t - departures[i]) / travelMinutes
        val distance = from + (to - from) * fraction
        val speedKmh = (to - from) / travelMinutes * 60.0 / 1000.0
        val next = if (direction == Direction.OUTBOUND) i + 1 else i
        return Position(direction, BusActivity.MOVING, distance, next, speedKmh, bearing(distance, direction))
    }

    fun pointAt(distanceMeters: Double) = measure.pointAt(distanceMeters)

    private fun bearing(distance: Double, direction: Direction): Float {
        // Look slightly ahead/behind so a bus sitting exactly on a vertex faces its next segment.
        val probe = if (direction == Direction.OUTBOUND) distance + 1.0 else distance - 1.0
        val forward = measure.bearingAt(probe.coerceIn(0.0, measure.lengthMeters))
        return if (direction == Direction.OUTBOUND) forward else (forward + 180f) % 360f
    }

    companion object {
        fun floorMod(value: Double, modulus: Double): Double = ((value % modulus) + modulus) % modulus
    }
}
