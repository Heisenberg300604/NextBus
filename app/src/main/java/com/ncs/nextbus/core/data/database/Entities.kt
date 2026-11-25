package com.ncs.nextbus.core.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Transit network (seeded, replaced wholesale when the seed version changes) ──

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val number: String,
    val name: String,
    val colorArgb: Long,
    val fuelType: String,
    val headwayMinutes: Int,
    val layoverMinutes: Double,
    /** Route geometry as `lat,lng;lat,lng;...`. */
    val path: String,
)

@Entity(
    tableName = "route_stops",
    primaryKeys = ["routeId", "sequence"],
    indices = [Index("stopId")],
)
data class RouteStopEntity(
    val routeId: String,
    val sequence: Int,
    val stopId: String,
    val distanceFromStartMeters: Double,
    val minutesFromStart: Double,
)

@Entity(tableName = "buses", indices = [Index("routeId")])
data class BusEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val registration: String,
    val capacity: Int,
    val driverName: String,
    val driverPhone: String,
    val fleetIndex: Int,
    val hasUnreliableGps: Boolean,
)

// ── User data (never touched by re-seeding) ──

@Entity(tableName = "favorite_routes")
data class FavoriteRouteEntity(
    @PrimaryKey val routeId: String,
    val addedAtMillis: Long,
)

@Entity(tableName = "saved_journeys", primaryKeys = ["fromStopId", "toStopId"])
data class SavedJourneyEntity(
    val fromStopId: String,
    val toStopId: String,
    val savedAtMillis: Long,
)

@Entity(tableName = "recent_searches", primaryKeys = ["fromStopId", "toStopId"])
data class RecentSearchEntity(
    val fromStopId: String,
    val toStopId: String,
    val searchedAtMillis: Long,
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val busId: String,
    val busRegistration: String,
    val routeId: String,
    val routeNumber: String,
    val routeName: String,
    val direction: String,
    val boardStopId: String,
    val boardStopName: String,
    val alightStopId: String?,
    val alightStopName: String?,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val distanceMeters: Double?,
)
