package com.ncs.nextbus.core.data.repository

import com.ncs.nextbus.core.common.TimeSource
import com.ncs.nextbus.core.data.database.FavoriteRouteEntity
import com.ncs.nextbus.core.data.database.RecentSearchEntity
import com.ncs.nextbus.core.data.database.SavedJourneyEntity
import com.ncs.nextbus.core.data.database.TripEntity
import com.ncs.nextbus.core.data.database.UserDataDao
import com.ncs.nextbus.core.data.database.toDomain
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** A from/to stop pair, referenced by id (resolved against the network by callers). */
data class JourneyRef(val fromStopId: String, val toStopId: String, val timestampMillis: Long)

data class NewTrip(
    val busId: String,
    val busRegistration: String,
    val routeId: String,
    val routeNumber: String,
    val routeName: String,
    val direction: Direction,
    val boardStopId: String,
    val boardStopName: String,
)

/** Everything the user creates: favourites, saved journeys, recent searches and trips. Local-only. */
interface UserDataRepository {
    val favoriteRouteIds: Flow<List<String>>
    suspend fun setFavoriteRoute(routeId: String, favorite: Boolean)

    val savedJourneys: Flow<List<JourneyRef>>
    suspend fun setSavedJourney(fromStopId: String, toStopId: String, saved: Boolean)

    val recentSearches: Flow<List<JourneyRef>>
    suspend fun recordSearch(fromStopId: String, toStopId: String)
    suspend fun clearRecentSearches()

    val trips: Flow<List<Trip>>
    val activeTrip: Flow<Trip?>
    suspend fun currentActiveTrip(): Trip?
    suspend fun startTrip(trip: NewTrip): Long
    suspend fun endTrip(tripId: Long, alightStopId: String, alightStopName: String, distanceMeters: Double)
    suspend fun deleteTrip(tripId: Long)
    suspend fun clearTripHistory()
}

@Singleton
class RoomUserDataRepository @Inject constructor(
    private val dao: UserDataDao,
    private val timeSource: TimeSource,
) : UserDataRepository {

    override val favoriteRouteIds: Flow<List<String>> =
        dao.favoriteRoutes().map { list -> list.map { it.routeId } }

    override suspend fun setFavoriteRoute(routeId: String, favorite: Boolean) {
        if (favorite) {
            dao.upsertFavoriteRoute(FavoriteRouteEntity(routeId, timeSource.nowMillis()))
        } else {
            dao.deleteFavoriteRoute(routeId)
        }
    }

    override val savedJourneys: Flow<List<JourneyRef>> = dao.savedJourneys().map { list ->
        list.map { JourneyRef(it.fromStopId, it.toStopId, it.savedAtMillis) }
    }

    override suspend fun setSavedJourney(fromStopId: String, toStopId: String, saved: Boolean) {
        if (saved) {
            dao.upsertSavedJourney(SavedJourneyEntity(fromStopId, toStopId, timeSource.nowMillis()))
        } else {
            dao.deleteSavedJourney(fromStopId, toStopId)
        }
    }

    override val recentSearches: Flow<List<JourneyRef>> = dao.recentSearches(MAX_RECENT_SEARCHES).map { list ->
        list.map { JourneyRef(it.fromStopId, it.toStopId, it.searchedAtMillis) }
    }

    override suspend fun recordSearch(fromStopId: String, toStopId: String) {
        dao.upsertRecentSearch(RecentSearchEntity(fromStopId, toStopId, timeSource.nowMillis()))
        dao.trimRecentSearches(MAX_RECENT_SEARCHES)
    }

    override suspend fun clearRecentSearches() = dao.clearRecentSearches()

    override val trips: Flow<List<Trip>> = dao.trips().map { list -> list.map { it.toDomain() } }

    override val activeTrip: Flow<Trip?> = dao.activeTrip().map { it?.toDomain() }

    override suspend fun currentActiveTrip(): Trip? = dao.currentActiveTrip()?.toDomain()

    override suspend fun startTrip(trip: NewTrip): Long = dao.insertTrip(
        TripEntity(
            busId = trip.busId,
            busRegistration = trip.busRegistration,
            routeId = trip.routeId,
            routeNumber = trip.routeNumber,
            routeName = trip.routeName,
            direction = trip.direction.name,
            boardStopId = trip.boardStopId,
            boardStopName = trip.boardStopName,
            alightStopId = null,
            alightStopName = null,
            startedAtMillis = timeSource.nowMillis(),
            endedAtMillis = null,
            distanceMeters = null,
        ),
    )

    override suspend fun endTrip(tripId: Long, alightStopId: String, alightStopName: String, distanceMeters: Double) =
        dao.endTrip(tripId, timeSource.nowMillis(), alightStopId, alightStopName, distanceMeters)

    override suspend fun deleteTrip(tripId: Long) = dao.deleteTrip(tripId)

    override suspend fun clearTripHistory() = dao.clearCompletedTrips()

    companion object {
        const val MAX_RECENT_SEARCHES = 10
    }
}
