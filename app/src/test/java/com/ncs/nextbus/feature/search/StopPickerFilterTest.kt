package com.ncs.nextbus.feature.search

import com.ncs.nextbus.core.data.seed.SeedNetwork
import com.ncs.nextbus.core.domain.ObserveNearestStopsUseCase
import com.ncs.nextbus.core.model.LocationSource
import com.ncs.nextbus.core.model.ReferenceLocation
import com.ncs.nextbus.testing.seedNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopPickerFilterTest {
    private val options = ObserveNearestStopsUseCase
        .sortByDistance(ReferenceLocation(SeedNetwork.CITY_CENTER, LocationSource.DEVICE), seedNetwork.stops)
        .map { StopOption(it.stop, it.distanceMeters, seedNetwork.routesServing(it.stop.id)) }

    private fun search(query: String) = StopPickerViewModel.filter(query, options, seedNetwork).map { it.stop.id }

    @Test
    fun `blank query returns every stop, nearest first`() {
        assertEquals(seedNetwork.stops.size, search("  ").size)
        assertEquals("plaza", search("").first())
    }

    @Test
    fun `matches words case-insensitively and ignores punctuation`() {
        assertEquals(listOf("pu"), search("panjab UNIVERSITY"))
        assertEquals(listOf("gc11"), search("govt college"))
        assertTrue(search("sector").containsAll(listOf("sec22", "sec35", "plaza")))
    }

    @Test
    fun `name prefix matches rank before other matches`() {
        val results = search("isbt")
        assertEquals(setOf("isbt17", "isbt43"), results.toSet())
        assertTrue(search("sector").first().let { it.startsWith("sec") || it == "plaza" })
    }

    @Test
    fun `route number finds every stop on that route`() {
        val route = seedNetwork.routes.first { it.number == "1A" }
        assertEquals(route.stops.map { it.stop.id }.toSet(), search("1a").toSet())
    }

    @Test
    fun `no match gives an empty list`() {
        assertTrue(search("airport").isEmpty())
    }
}
