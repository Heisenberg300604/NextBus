package com.ncs.nextbus.core.common

import com.ncs.nextbus.core.model.GeoPoint
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_008.8

    /** Great-circle distance (haversine). */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /** Initial compass bearing from [from] to [to], in degrees within [0, 360). */
    fun bearingDegrees(from: GeoPoint, to: GeoPoint): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return normalizeDegrees(Math.toDegrees(atan2(y, x)).toFloat())
    }

    /** Linear interpolation; accurate enough for the sub-kilometre segments between stops. */
    fun interpolate(a: GeoPoint, b: GeoPoint, fraction: Double): GeoPoint {
        val f = fraction.coerceIn(0.0, 1.0)
        return GeoPoint(
            latitude = a.latitude + (b.latitude - a.latitude) * f,
            longitude = a.longitude + (b.longitude - a.longitude) * f,
        )
    }

    fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f
}

/** Measures positions along a polyline by distance from its start. */
class PolylineMeasure(private val points: List<GeoPoint>) {
    private val cumulative: DoubleArray

    init {
        require(points.size >= 2) { "A polyline needs at least two points" }
        cumulative = DoubleArray(points.size)
        for (i in 1 until points.size) {
            cumulative[i] = cumulative[i - 1] + GeoMath.distanceMeters(points[i - 1], points[i])
        }
    }

    val lengthMeters: Double get() = cumulative.last()

    /** Distance from the start to vertex [index]. */
    fun distanceAtVertex(index: Int): Double = cumulative[index]

    fun pointAt(distanceMeters: Double): GeoPoint {
        val i = segmentIndex(distanceMeters)
        val segmentLength = cumulative[i + 1] - cumulative[i]
        if (segmentLength <= 0.0) return points[i]
        val fraction = (distanceMeters.coerceIn(0.0, lengthMeters) - cumulative[i]) / segmentLength
        return GeoMath.interpolate(points[i], points[i + 1], fraction)
    }

    /** Bearing of the segment containing [distanceMeters], in the direction of increasing distance. */
    fun bearingAt(distanceMeters: Double): Float {
        val i = segmentIndex(distanceMeters)
        return GeoMath.bearingDegrees(points[i], points[i + 1])
    }

    private fun segmentIndex(distanceMeters: Double): Int {
        val d = distanceMeters.coerceIn(0.0, lengthMeters)
        // Last segment whose start is at or before d.
        var index = 0
        for (i in 0 until points.size - 1) {
            if (cumulative[i] <= d) index = i else break
        }
        return index
    }
}
