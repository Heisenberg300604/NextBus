package com.ncs.nextbus.navigation

import kotlinx.serialization.Serializable

/** Which journey field a stop picker fills in. */
enum class StopField { FROM, TO }

/** [field] is a [StopField] name (kept as a String so it round-trips through navigation arguments). */
@Serializable data class StopPickerRoute(val field: String)
@Serializable data class SearchResultsRoute(val fromStopId: String, val toStopId: String)
@Serializable data class BusDetailRoute(val busId: String)
@Serializable data class RouteDetailRoute(val routeId: String)
@Serializable data class StopDetailRoute(val stopId: String)
@Serializable data object SosRoute

/** Keys used to hand a picked stop back to the screen that opened the picker. */
object NavResults {
    const val PICKED_FROM_STOP = "picked_from_stop"
    const val PICKED_TO_STOP = "picked_to_stop"

    fun keyFor(field: StopField) = if (field == StopField.FROM) PICKED_FROM_STOP else PICKED_TO_STOP
}
