package com.ncs.nextbus.core.model

import kotlinx.serialization.Serializable

data class SavedJourney(val from: Stop, val to: Stop, val savedAtMillis: Long)

data class RecentSearch(val from: Stop, val to: Stop, val searchedAtMillis: Long)

/** A ride the user tracked. Names are stored with the trip so history survives data changes. */
data class Trip(
    val id: Long,
    val busId: String,
    val busRegistration: String,
    val routeId: String,
    val routeNumber: String,
    val routeName: String,
    val direction: Direction,
    val boardStopId: String,
    val boardStopName: String,
    val alightStopId: String?,
    val alightStopName: String?,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val distanceMeters: Double?,
) {
    val isActive: Boolean get() = endedAtMillis == null
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class EmergencyContact(val name: String, val phone: String)

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val displayName: String = "",
    val onboardingCompleted: Boolean = false,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
)
