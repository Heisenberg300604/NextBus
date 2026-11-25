package com.ncs.nextbus.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StopEntity::class,
        RouteEntity::class,
        RouteStopEntity::class,
        BusEntity::class,
        FavoriteRouteEntity::class,
        SavedJourneyEntity::class,
        RecentSearchEntity::class,
        TripEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NextBusDatabase : RoomDatabase() {
    abstract fun transitDao(): TransitDao
    abstract fun userDataDao(): UserDataDao
}
