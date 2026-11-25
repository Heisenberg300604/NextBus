package com.ncs.nextbus.core.common

import com.ncs.nextbus.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {
    @Test
    fun `one degree of latitude is about 111 km`() {
        val d = GeoMath.distanceMeters(GeoPoint(30.0, 76.0), GeoPoint(31.0, 76.0))
        assertEquals(111_195.0, d, 100.0)
    }

    @Test
    fun `distance to self is zero`() {
        val p = GeoPoint(30.7398, 76.7827)
        assertEquals(0.0, GeoMath.distanceMeters(p, p), 1e-9)
    }

    @Test
    fun `bearing points north east south and west`() {
        val origin = GeoPoint(30.0, 76.0)
        assertEquals(0f, GeoMath.bearingDegrees(origin, GeoPoint(30.1, 76.0)), 0.01f)
        assertEquals(90f, GeoMath.bearingDegrees(origin, GeoPoint(30.0, 76.1)), 0.1f)
        assertEquals(180f, GeoMath.bearingDegrees(GeoPoint(30.1, 76.0), origin), 0.01f)
        assertEquals(270f, GeoMath.bearingDegrees(GeoPoint(30.0, 76.1), origin), 0.1f)
    }

    @Test
    fun `normalizeDegrees wraps into 0 until 360`() {
        assertEquals(350f, GeoMath.normalizeDegrees(-10f), 0f)
        assertEquals(10f, GeoMath.normalizeDegrees(370f), 0f)
    }

    @Test
    fun `polyline measure interpolates along segments`() {
        val a = GeoPoint(30.0, 76.0)
        val b = GeoPoint(30.01, 76.0)
        val c = GeoPoint(30.01, 76.01)
        val measure = PolylineMeasure(listOf(a, b, c))
        val ab = GeoMath.distanceMeters(a, b)

        assertEquals(ab + GeoMath.distanceMeters(b, c), measure.lengthMeters, 1e-6)
        assertEquals(30.005, measure.pointAt(ab / 2).latitude, 1e-6)
        assertEquals(b, measure.pointAt(ab))
        assertEquals(0f, measure.bearingAt(ab / 2), 0.01f)
        assertEquals(90f, measure.bearingAt(ab + 10), 0.1f)
        assertEquals(a, measure.pointAt(-50.0))
        assertEquals(c, measure.pointAt(measure.lengthMeters + 50))
    }
}
