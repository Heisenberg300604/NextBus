package com.ncs.nextbus.core.domain

/** Distance-slab fare table (demo values, in rupees). */
object FareCalculator {
    private val slabs = listOf(3_000.0 to 10, 6_000.0 to 15, 10_000.0 to 20)
    private const val MAX_FARE = 25

    fun fareFor(distanceMeters: Double): Int =
        slabs.firstOrNull { distanceMeters <= it.first }?.second ?: MAX_FARE
}
