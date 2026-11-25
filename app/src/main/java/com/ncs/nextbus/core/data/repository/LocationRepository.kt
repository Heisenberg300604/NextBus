package com.ncs.nextbus.core.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.ncs.nextbus.core.model.DeviceLocation
import com.ncs.nextbus.core.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    fun hasLocationPermission(): Boolean

    /** Device location updates; re-evaluates permission whenever [onPermissionChanged] is called. */
    fun observeLocation(): Flow<DeviceLocation>

    fun onPermissionChanged()
}

@Singleton
class FusedLocationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationRepository {
    private val permissionVersion = MutableStateFlow(0)

    override fun hasLocationPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onPermissionChanged() = permissionVersion.update { it + 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLocation(): Flow<DeviceLocation> = permissionVersion
        .flatMapLatest {
            if (hasLocationPermission()) locationUpdates() else flowOf(DeviceLocation.NoPermission)
        }
        .distinctUntilChanged()

    @SuppressLint("MissingPermission") // Guarded by hasLocationPermission() in observeLocation().
    private fun locationUpdates(): Flow<DeviceLocation> = callbackFlow {
        var hasFix = false
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    hasFix = true
                    trySend(DeviceLocation.Located(GeoPoint(it.latitude, it.longitude)))
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable && !hasFix) trySend(DeviceLocation.Unavailable)
            }
        }
        try {
            client.lastLocation.await()?.let {
                hasFix = true
                trySend(DeviceLocation.Located(GeoPoint(it.latitude, it.longitude)))
            }
            val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL_MILLIS)
                .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS)
                .build()
            client.requestLocationUpdates(request, callback, Looper.getMainLooper()).await()
        } catch (e: SecurityException) {
            trySend(DeviceLocation.NoPermission)
        } catch (e: Exception) {
            Timber.w(e, "Location updates unavailable")
            if (!hasFix) trySend(DeviceLocation.Unavailable)
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    companion object {
        const val UPDATE_INTERVAL_MILLIS = 15_000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 5_000L
    }
}
