package com.ncs.nextbus.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitDao {
    @Query("SELECT COUNT(*) FROM stops")
    suspend fun stopCount(): Int

    @Query("SELECT * FROM stops")
    suspend fun stops(): List<StopEntity>

    @Query("SELECT * FROM routes ORDER BY id")
    suspend fun routes(): List<RouteEntity>

    @Query("SELECT * FROM route_stops ORDER BY routeId, sequence")
    suspend fun routeStops(): List<RouteStopEntity>

    @Query("SELECT * FROM buses ORDER BY routeId, fleetIndex")
    suspend fun buses(): List<BusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteStops(routeStops: List<RouteStopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuses(buses: List<BusEntity>)

    @Query("DELETE FROM stops")
    suspend fun clearStops()

    @Query("DELETE FROM routes")
    suspend fun clearRoutes()

    @Query("DELETE FROM route_stops")
    suspend fun clearRouteStops()

    @Query("DELETE FROM buses")
    suspend fun clearBuses()
}

@Dao
interface UserDataDao {
    @Query("SELECT * FROM favorite_routes ORDER BY addedAtMillis DESC")
    fun favoriteRoutes(): Flow<List<FavoriteRouteEntity>>

    @Upsert
    suspend fun upsertFavoriteRoute(entity: FavoriteRouteEntity)

    @Query("DELETE FROM favorite_routes WHERE routeId = :routeId")
    suspend fun deleteFavoriteRoute(routeId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_routes WHERE routeId = :routeId)")
    suspend fun isFavoriteRoute(routeId: String): Boolean

    @Query("SELECT * FROM saved_journeys ORDER BY savedAtMillis DESC")
    fun savedJourneys(): Flow<List<SavedJourneyEntity>>

    @Upsert
    suspend fun upsertSavedJourney(entity: SavedJourneyEntity)

    @Query("DELETE FROM saved_journeys WHERE fromStopId = :fromStopId AND toStopId = :toStopId")
    suspend fun deleteSavedJourney(fromStopId: String, toStopId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_journeys WHERE fromStopId = :fromStopId AND toStopId = :toStopId)")
    suspend fun isSavedJourney(fromStopId: String, toStopId: String): Boolean

    @Query("SELECT * FROM recent_searches ORDER BY searchedAtMillis DESC LIMIT :limit")
    fun recentSearches(limit: Int): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun upsertRecentSearch(entity: RecentSearchEntity)

    @Query(
        "DELETE FROM recent_searches WHERE rowid NOT IN " +
            "(SELECT rowid FROM recent_searches ORDER BY searchedAtMillis DESC LIMIT :keep)",
    )
    suspend fun trimRecentSearches(keep: Int)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()

    @Query("SELECT * FROM trips ORDER BY startedAtMillis DESC")
    fun trips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE endedAtMillis IS NULL ORDER BY startedAtMillis DESC LIMIT 1")
    fun activeTrip(): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE endedAtMillis IS NULL ORDER BY startedAtMillis DESC LIMIT 1")
    suspend fun currentActiveTrip(): TripEntity?

    @Insert
    suspend fun insertTrip(trip: TripEntity): Long

    @Query(
        "UPDATE trips SET endedAtMillis = :endedAtMillis, alightStopId = :alightStopId, " +
            "alightStopName = :alightStopName, distanceMeters = :distanceMeters WHERE id = :tripId",
    )
    suspend fun endTrip(
        tripId: Long,
        endedAtMillis: Long,
        alightStopId: String,
        alightStopName: String,
        distanceMeters: Double,
    )

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    @Query("DELETE FROM trips WHERE endedAtMillis IS NOT NULL")
    suspend fun clearCompletedTrips()
}
