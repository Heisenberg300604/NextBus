package com.ncs.nextbus.core.data.seed

import com.ncs.nextbus.core.model.FuelType
import com.ncs.nextbus.core.model.GeoPoint

/**
 * Demo network for Chandigarh. Stops sit at real landmarks (coordinates are approximate);
 * routes, timetables, fleet and driver details are fictional sample data.
 *
 * Bump [VERSION] whenever this data changes so existing installs re-seed on next launch.
 */
internal object SeedNetwork {
    const val VERSION = 1

    /** Sector 17 Plaza: the fallback reference point when the device location can't be used. */
    val CITY_CENTER = GeoPoint(30.7398, 76.7827)

    /** Device locations farther than this from [CITY_CENTER] are treated as outside the service area. */
    const val SERVICE_RADIUS_METERS = 40_000.0

    data class SeedStop(val id: String, val name: String, val latitude: Double, val longitude: Double)

    data class SeedRoute(
        val id: String,
        val number: String,
        val name: String,
        val colorArgb: Long,
        val fuelType: FuelType,
        val headwayMinutes: Int,
        val stopIds: List<String>,
        val registrationSeries: Char,
    )

    val stops = listOf(
        SeedStop("isbt43", "ISBT Sector 43", 30.7224, 76.7507),
        SeedStop("sec35", "Sector 35 Market", 30.7253, 76.7608),
        SeedStop("sec22", "Sector 22 Market", 30.7335, 76.7745),
        SeedStop("isbt17", "ISBT Sector 17", 30.7426, 76.7797),
        SeedStop("sec15", "Sector 15 Market", 30.7505, 76.7720),
        SeedStop("pu", "Panjab University", 30.7585, 76.7680),
        SeedStop("pgi", "PGIMER", 30.7640, 76.7755),
        SeedStop("pec", "PEC Campus", 30.7663, 76.7860),
        SeedStop("rly", "Railway Station", 30.7008, 76.8212),
        SeedStop("sec26", "Sector 26 Grain Market", 30.7290, 76.8080),
        SeedStop("sukhna", "Sukhna Lake", 30.7423, 76.8180),
        SeedStop("rock", "Rock Garden", 30.7524, 76.8072),
        SeedStop("capitol", "Capitol Complex", 30.7588, 76.8040),
        SeedStop("gc11", "Govt. College, Sector 11", 30.7570, 76.7795),
        SeedStop("museum", "Museum, Sector 10", 30.7498, 76.7882),
        SeedStop("rose", "Rose Garden, Sector 16", 30.7463, 76.7823),
        SeedStop("plaza", "Sector 17 Plaza", 30.7398, 76.7827),
        SeedStop("tribune", "Tribune Chowk", 30.7225, 76.7930),
        SeedStop("ind", "Industrial Area Phase 1", 30.7105, 76.8060),
        SeedStop("gmch", "GMCH, Sector 32", 30.7155, 76.7765),
        SeedStop("elante", "Elante Mall", 30.7056, 76.8013),
    )

    val routes = listOf(
        SeedRoute(
            id = "r1", number = "1A", name = "Campus Connect", colorArgb = 0xFF2E7D32,
            fuelType = FuelType.ELECTRIC, headwayMinutes = 12, registrationSeries = 'A',
            stopIds = listOf("isbt43", "sec35", "sec22", "isbt17", "sec15", "pu", "pgi", "pec"),
        ),
        SeedRoute(
            id = "r2", number = "2B", name = "Lake Shuttle", colorArgb = 0xFF00838F,
            fuelType = FuelType.CNG, headwayMinutes = 15, registrationSeries = 'B',
            stopIds = listOf("rly", "sec26", "sukhna", "rock", "capitol"),
        ),
        SeedRoute(
            id = "r3", number = "5", name = "Crosstown Express", colorArgb = 0xFFD84315,
            fuelType = FuelType.DIESEL, headwayMinutes = 20, registrationSeries = 'C',
            stopIds = listOf("pgi", "gc11", "museum", "rose", "plaza", "tribune", "ind", "rly"),
        ),
        SeedRoute(
            id = "r4", number = "7C", name = "Mall & Medical Link", colorArgb = 0xFF6A1B9A,
            fuelType = FuelType.ELECTRIC, headwayMinutes = 10, registrationSeries = 'D',
            stopIds = listOf("plaza", "sec22", "gmch", "tribune", "elante"),
        ),
        SeedRoute(
            id = "r5", number = "12", name = "Sector Link", colorArgb = 0xFF283593,
            fuelType = FuelType.CNG, headwayMinutes = 15, registrationSeries = 'E',
            stopIds = listOf("isbt43", "sec35", "gmch", "tribune", "sec26", "sukhna"),
        ),
    )

    /** The bus that periodically drops off the feed, to demo stale and offline states. */
    const val UNRELIABLE_GPS_ROUTE_ID = "r3"
    const val UNRELIABLE_GPS_FLEET_INDEX = 1

    val driverNames = listOf(
        "Harpreet Singh", "Anil Kumar", "Sunita Devi", "Rajesh Sharma", "Gurpreet Kaur", "Manoj Verma",
        "Kuldeep Singh", "Pooja Rani", "Vikram Thakur", "Sandeep Kumar", "Neha Bhardwaj", "Amarjit Singh",
    )
}
