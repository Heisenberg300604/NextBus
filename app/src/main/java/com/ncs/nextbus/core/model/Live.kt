package com.ncs.nextbus.core.model

enum class BusActivity {
    /** Waiting at a terminus before the next run. */
    LAYOVER,

    /** Stopped at an intermediate stop to board passengers. */
    AT_STOP,
    MOVING,
}

/** Predicted arrival of one bus at one stop, in one direction. */
data class StopPrediction(
    val stopId: String,
    val stopIndex: Int,
    val direction: Direction,
    val etaSeconds: Int,
)

/** A real-time snapshot of a bus: where it is now and when it will reach each stop. */
data class LiveBus(
    val bus: Bus,
    val route: Route,
    val position: GeoPoint,
    val bearingDegrees: Float,
    val speedKmh: Double,
    val direction: Direction,
    val activity: BusActivity,
    /** Distance from the route's first stop, measured along the route path. */
    val distanceAlongRouteMeters: Double,
    /** The stop the bus is at or heading to next. */
    val nextStopIndex: Int,
    val delayMinutes: Int,
    val lastUpdatedMillis: Long,
    val predictions: List<StopPrediction>,
) {
    val nextStop: RouteStop get() = route.stops[nextStopIndex]
    val headsign: Stop get() = route.headsign(direction)
    val isDelayed: Boolean get() = delayMinutes >= DELAY_THRESHOLD_MINUTES

    fun etaSecondsTo(stopId: String, direction: Direction): Int? =
        predictions.firstOrNull { it.stopId == stopId && it.direction == direction }?.etaSeconds

    /** Predictions for the stops still ahead on the current run, in travel order. */
    fun upcomingStops(): List<StopPrediction> {
        val ahead = if (direction == Direction.OUTBOUND) {
            nextStopIndex..route.stops.lastIndex
        } else {
            nextStopIndex downTo 0
        }
        val byIndex = predictions.filter { it.direction == direction }.associateBy { it.stopIndex }
        return ahead.mapNotNull { byIndex[it] }
    }

    companion object {
        const val DELAY_THRESHOLD_MINUTES = 2
    }
}

/** How recent a bus's last position report is. */
enum class Freshness {
    LIVE,
    STALE,
    OFFLINE;

    companion object {
        const val LIVE_WINDOW_MILLIS = 45_000L
        const val OFFLINE_AFTER_MILLIS = 5 * 60_000L

        fun of(lastUpdatedMillis: Long, nowMillis: Long): Freshness {
            val age = nowMillis - lastUpdatedMillis
            return when {
                age < LIVE_WINDOW_MILLIS -> LIVE
                age < OFFLINE_AFTER_MILLIS -> STALE
                else -> OFFLINE
            }
        }
    }
}
