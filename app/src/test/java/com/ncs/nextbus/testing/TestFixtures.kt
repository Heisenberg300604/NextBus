package com.ncs.nextbus.testing

import com.ncs.nextbus.core.data.repository.JourneyRef
import com.ncs.nextbus.core.data.repository.NewTrip
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import com.ncs.nextbus.core.data.seed.SeedBuilder
import com.ncs.nextbus.core.model.TransitNetwork
import com.ncs.nextbus.core.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** The real seed network: tests exercise the same data the app ships with. */
val seedNetwork: TransitNetwork by lazy { SeedBuilder.build() }

/** A fixed, arbitrary instant (2026-01-15 09:00 UTC) so tests are reproducible. */
const val FIXED_NOW = 1_768_467_600_000L

class FakeTransitRepository(private val network: TransitNetwork = seedNetwork) : TransitRepository {
    override suspend fun getNetwork(): TransitNetwork = network
}

class FakeUserDataRepository(private val now: () -> Long = { FIXED_NOW }) : UserDataRepository {
    private val favorites = MutableStateFlow<List<String>>(emptyList())
    private val saved = MutableStateFlow<List<JourneyRef>>(emptyList())
    private val recents = MutableStateFlow<List<JourneyRef>>(emptyList())
    val tripsState = MutableStateFlow<List<Trip>>(emptyList())
    private var nextTripId = 1L

    override val favoriteRouteIds: Flow<List<String>> = favorites
    override suspend fun setFavoriteRoute(routeId: String, favorite: Boolean) {
        favorites.value = if (favorite) listOf(routeId) + (favorites.value - routeId) else favorites.value - routeId
    }

    override val savedJourneys: Flow<List<JourneyRef>> = saved
    override suspend fun setSavedJourney(fromStopId: String, toStopId: String, saved: Boolean) {
        val rest = this.saved.value.filterNot { it.fromStopId == fromStopId && it.toStopId == toStopId }
        this.saved.value = if (saved) listOf(JourneyRef(fromStopId, toStopId, now())) + rest else rest
    }

    override val recentSearches: Flow<List<JourneyRef>> = recents
    override suspend fun recordSearch(fromStopId: String, toStopId: String) {
        val rest = recents.value.filterNot { it.fromStopId == fromStopId && it.toStopId == toStopId }
        recents.value = (listOf(JourneyRef(fromStopId, toStopId, now())) + rest).take(10)
    }

    override suspend fun clearRecentSearches() {
        recents.value = emptyList()
    }

    override val trips: Flow<List<Trip>> = tripsState
    override val activeTrip: Flow<Trip?> = tripsState.map { list -> list.firstOrNull { it.isActive } }
    override suspend fun currentActiveTrip(): Trip? = tripsState.value.firstOrNull { it.isActive }

    override suspend fun startTrip(trip: NewTrip): Long {
        val id = nextTripId++
        tripsState.value = listOf(
            Trip(
                id = id, busId = trip.busId, busRegistration = trip.busRegistration, routeId = trip.routeId,
                routeNumber = trip.routeNumber, routeName = trip.routeName, direction = trip.direction,
                boardStopId = trip.boardStopId, boardStopName = trip.boardStopName, alightStopId = null,
                alightStopName = null, startedAtMillis = now(), endedAtMillis = null, distanceMeters = null,
            ),
        ) + tripsState.value
        return id
    }

    override suspend fun endTrip(tripId: Long, alightStopId: String, alightStopName: String, distanceMeters: Double) {
        tripsState.value = tripsState.value.map {
            if (it.id == tripId) {
                it.copy(
                    alightStopId = alightStopId, alightStopName = alightStopName,
                    endedAtMillis = now(), distanceMeters = distanceMeters,
                )
            } else {
                it
            }
        }
    }

    override suspend fun deleteTrip(tripId: Long) {
        tripsState.value = tripsState.value.filterNot { it.id == tripId }
    }

    override suspend fun clearTripHistory() {
        tripsState.value = tripsState.value.filter { it.isActive }
    }
}
