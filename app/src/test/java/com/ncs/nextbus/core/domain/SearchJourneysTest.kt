package com.ncs.nextbus.core.domain

import com.ncs.nextbus.core.data.simulator.BusSimulator
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.testing.FIXED_NOW
import com.ncs.nextbus.testing.seedNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchJourneysTest {
    private val buses = BusSimulator(seedNetwork).snapshot(FIXED_NOW)

    private fun search(from: String, to: String) =
        SearchJourneysUseCase.findJourneys(seedNetwork, buses, from, to)

    @Test
    fun `finds outbound and inbound directions`() {
        val out = search("isbt43", "pec")
        assertEquals(listOf("r1"), out.map { it.route.id })
        assertEquals(Direction.OUTBOUND, out.single().direction)

        val back = search("pec", "isbt43")
        assertEquals(Direction.INBOUND, back.single().direction)
        assertEquals(out.single().distanceMeters, back.single().distanceMeters, 1e-6)
    }

    @Test
    fun `returns every route serving both stops`() {
        val options = search("plaza", "tribune")
        assertEquals(setOf("r3", "r4"), options.map { it.route.id }.toSet())
    }

    @Test
    fun `upcoming buses are on the route, in the right direction, soonest first`() {
        search("sec35", "tribune").forEach { option ->
            assertTrue(option.upcoming.size in 1..SearchJourneysUseCase.MAX_UPCOMING)
            assertEquals(option.upcoming.sortedBy { it.etaSeconds }, option.upcoming)
            option.upcoming.forEach { assertEquals(option.route.id, it.liveBus.route.id) }
        }
    }

    @Test
    fun `results are ordered by earliest arrival`() {
        val options = search("plaza", "tribune")
        val arrivals = options.map { (it.nextEtaSeconds ?: 0) + it.rideMinutes * 60 }
        assertEquals(arrivals.sorted(), arrivals)
    }

    @Test
    fun `no direct route or same stop gives no results`() {
        assertTrue(search("pec", "elante").isEmpty())
        assertTrue(search("plaza", "plaza").isEmpty())
        assertTrue(search("plaza", "nowhere").isEmpty())
    }

    @Test
    fun `fare follows the distance slabs`() {
        assertEquals(10, FareCalculator.fareFor(0.0))
        assertEquals(10, FareCalculator.fareFor(3_000.0))
        assertEquals(15, FareCalculator.fareFor(3_001.0))
        assertEquals(20, FareCalculator.fareFor(9_999.0))
        assertEquals(25, FareCalculator.fareFor(25_000.0))
        val option = search("isbt43", "pec").single()
        assertEquals(FareCalculator.fareFor(option.distanceMeters), option.fareRupees)
    }
}
