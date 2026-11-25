package com.ncs.nextbus.core.data

import com.ncs.nextbus.core.data.simulator.BusSimulator
import com.ncs.nextbus.core.model.BusActivity
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.Freshness
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.testing.FIXED_NOW
import com.ncs.nextbus.testing.seedNetwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BusSimulatorTest {
    private val simulator = BusSimulator(seedNetwork)
    private val minute = 60_000L

    /** Samples a full day at one-minute steps. */
    private fun day(): Sequence<Pair<Long, List<LiveBus>>> =
        (0 until 24 * 60).asSequence().map { FIXED_NOW + it * minute }.map { it to simulator.snapshot(it) }

    @Test
    fun `snapshot is deterministic`() {
        assertEquals(simulator.snapshot(FIXED_NOW), BusSimulator(seedNetwork).snapshot(FIXED_NOW))
    }

    @Test
    fun `every bus is reported with a full set of predictions`() {
        val snapshot = simulator.snapshot(FIXED_NOW)
        assertEquals(seedNetwork.buses.size, snapshot.size)
        snapshot.forEach { bus ->
            assertEquals(bus.route.stops.size * 2, bus.predictions.size)
            assertTrue(bus.predictions.all { it.etaSeconds >= 0 })
        }
    }

    @Test
    fun `buses stay on their route and within bounds all day`() {
        day().forEach { (_, buses) ->
            buses.forEach { bus ->
                assertTrue(bus.distanceAlongRouteMeters in -0.001..bus.route.lengthMeters + 0.001)
                assertTrue(bus.nextStopIndex in bus.route.stops.indices)
                assertTrue("speed ${bus.speedKmh}", bus.speedKmh in 0.0..40.0)
                assertTrue(bus.delayMinutes in 0..3)
                assertTrue(bus.bearingDegrees in 0f..360f)
                if (bus.activity != BusActivity.MOVING) assertEquals(0.0, bus.speedKmh, 0.0)
            }
        }
    }

    @Test
    fun `moving buses advance in their direction of travel`() {
        var checked = 0
        day().forEach { (time, buses) ->
            buses.filter { !it.bus.hasUnreliableGps && it.activity == BusActivity.MOVING }.forEach { before ->
                val after = simulator.snapshot(before.bus.id, time + 5_000) ?: return@forEach
                if (after.activity != BusActivity.MOVING || after.direction != before.direction) return@forEach
                val delta = after.distanceAlongRouteMeters - before.distanceAlongRouteMeters
                if (before.direction == Direction.OUTBOUND) assertTrue(delta > 0) else assertTrue(delta < 0)
                checked++
            }
        }
        assertTrue("expected many moving samples, got $checked", checked > 1000)
    }

    @Test
    fun `eta counts down roughly in real time`() {
        val bus = simulator.snapshot(FIXED_NOW).first { !it.bus.hasUnreliableGps }
        val target = bus.predictions.first { it.etaSeconds in 300..1200 }
        val later = simulator.snapshot(bus.bus.id, FIXED_NOW + minute)!!
        val laterEta = later.etaSecondsTo(target.stopId, target.direction)!!
        // Delay drift changes at most ~0.28 min per minute, so the countdown stays within ~20 s of 60 s.
        assertTrue("eta went ${target.etaSeconds} -> $laterEta", abs(target.etaSeconds - laterEta - 60) <= 20)
    }

    @Test
    fun `upcoming stops follow travel order`() {
        simulator.snapshot(FIXED_NOW).forEach { bus ->
            val indices = bus.upcomingStops().map { it.stopIndex }
            assertTrue(indices.isNotEmpty())
            assertEquals(bus.nextStopIndex, indices.first())
            if (bus.direction == Direction.OUTBOUND) {
                assertEquals(indices.sorted(), indices)
            } else {
                assertEquals(indices.sortedDescending(), indices)
            }
        }
    }

    @Test
    fun `scheduled departures from the first stop are one headway apart`() {
        seedNetwork.routes.forEach { route ->
            val times = simulator.scheduledArrivals(route.id, 0, Direction.OUTBOUND, FIXED_NOW, 4)
            assertEquals(4, times.size)
            assertTrue(times.first() >= FIXED_NOW)
            assertTrue(times.first() - FIXED_NOW <= route.headwayMinutes * minute)
            times.zipWithNext().forEach { (a, b) ->
                assertTrue(abs(b - a - route.headwayMinutes * minute) <= 1)
            }
        }
    }

    @Test
    fun `every route has buses in layover at some point`() {
        val seen = mutableSetOf<String>()
        day().forEach { (_, buses) -> buses.filter { it.activity == BusActivity.LAYOVER }.forEach { seen += it.route.id } }
        assertEquals(seedNetwork.routes.map { it.id }.toSet(), seen)
    }

    @Test
    fun `unreliable gps bus goes stale then offline while reliable buses stay live`() {
        val flaky = seedNetwork.buses.single { it.hasUnreliableGps }.id
        val freshness = mutableSetOf<Freshness>()
        day().forEach { (time, buses) ->
            buses.forEach { bus ->
                val f = Freshness.of(bus.lastUpdatedMillis, time)
                if (bus.bus.id == flaky) freshness += f else assertEquals(Freshness.LIVE, f)
            }
        }
        assertEquals(Freshness.entries.toSet(), freshness)
    }

    @Test
    fun `a dwelling bus is predicted at its current stop now`() {
        val dwelling = day().flatMap { it.second.asSequence() }
            .firstOrNull { it.activity == BusActivity.AT_STOP && !it.bus.hasUnreliableGps }
        assertNotNull(dwelling)
        assertEquals(0, dwelling!!.etaSecondsTo(dwelling.nextStop.stop.id, dwelling.direction))
    }
}
