package com.ncs.nextbus.feature.route

import com.ncs.nextbus.core.data.simulator.BusSimulator
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.testing.FIXED_NOW
import com.ncs.nextbus.testing.seedNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDetailContentTest {
    private val route = seedNetwork.routes.first()
    private val buses = BusSimulator(seedNetwork).snapshot(FIXED_NOW)

    @Test
    fun `inbound reverses the stop order and restarts offsets at zero`() {
        val outbound = RouteDetailViewModel.buildContent(route, Direction.OUTBOUND, emptyList(), buses, false)
        val inbound = RouteDetailViewModel.buildContent(route, Direction.INBOUND, emptyList(), buses, false)
        assertEquals(outbound.stops.map { it.stopId }.reversed(), inbound.stops.map { it.stopId })
        assertEquals(0, outbound.stops.first().scheduledOffsetMinutes)
        assertEquals(0, inbound.stops.first().scheduledOffsetMinutes)
        assertEquals(outbound.stops.map { it.scheduledOffsetMinutes }.sorted(), outbound.stops.map { it.scheduledOffsetMinutes })
    }

    @Test
    fun `only this route's buses are listed, selected direction first`() {
        val content = RouteDetailViewModel.buildContent(route, Direction.INBOUND, emptyList(), buses, true)
        assertTrue(content.liveBuses.all { it.route.id == route.id })
        val directions = content.liveBuses.map { it.direction == Direction.INBOUND }
        assertEquals(directions.sortedDescending(), directions)
        assertTrue(content.isFavorite)
        assertTrue(content.minFare <= content.maxFare)
    }

    @Test
    fun `every stop gets the soonest live eta in the selected direction`() {
        val content = RouteDetailViewModel.buildContent(route, Direction.OUTBOUND, emptyList(), buses, false)
        content.stops.forEach { row ->
            val expected = buses.filter { it.route.id == route.id }
                .mapNotNull { it.etaSecondsTo(row.stopId, Direction.OUTBOUND) }.minOrNull()
            assertEquals(expected, row.nextEtaSeconds)
        }
    }
}
