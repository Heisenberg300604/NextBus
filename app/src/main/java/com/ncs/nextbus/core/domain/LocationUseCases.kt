package com.ncs.nextbus.core.domain

import com.ncs.nextbus.core.common.GeoMath
import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.LocationRepository
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.seed.SeedNetwork
import com.ncs.nextbus.core.model.DeviceLocation
import com.ncs.nextbus.core.model.LiveBus
import com.ncs.nextbus.core.model.LocationSource
import com.ncs.nextbus.core.model.NearbyBus
import com.ncs.nextbus.core.model.ReferenceLocation
import com.ncs.nextbus.core.model.Stop
import com.ncs.nextbus.core.model.StopWithDistance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * The point "nearby" is measured from: the device when it is usable and inside the service area,
 * otherwise the city centre (with the reason, so the UI can explain it).
 */
class ObserveReferenceLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
) {
    operator fun invoke(): Flow<ReferenceLocation> = locationRepository.observeLocation()
        .map(::resolve)
        .onStart {
            val source = if (locationRepository.hasLocationPermission()) {
                LocationSource.LOCATING
            } else {
                LocationSource.NO_PERMISSION
            }
            emit(ReferenceLocation(SeedNetwork.CITY_CENTER, source))
        }
        .distinctUntilChanged()

    companion object {
        fun resolve(location: DeviceLocation): ReferenceLocation = when (location) {
            DeviceLocation.NoPermission -> ReferenceLocation(SeedNetwork.CITY_CENTER, LocationSource.NO_PERMISSION)
            DeviceLocation.Unavailable -> ReferenceLocation(SeedNetwork.CITY_CENTER, LocationSource.UNAVAILABLE)
            is DeviceLocation.Located ->
                if (GeoMath.distanceMeters(location.point, SeedNetwork.CITY_CENTER) <= SeedNetwork.SERVICE_RADIUS_METERS) {
                    ReferenceLocation(location.point, LocationSource.DEVICE)
                } else {
                    ReferenceLocation(SeedNetwork.CITY_CENTER, LocationSource.OUT_OF_SERVICE_AREA)
                }
        }
    }
}

data class NearbyBuses(val reference: ReferenceLocation, val buses: List<NearbyBus>)

class ObserveNearbyBusesUseCase @Inject constructor(
    private val liveBusRepository: LiveBusRepository,
    private val observeReferenceLocation: ObserveReferenceLocationUseCase,
) {
    operator fun invoke(): Flow<NearbyBuses> =
        combine(observeReferenceLocation(), liveBusRepository.liveBuses) { reference, buses ->
            NearbyBuses(reference, sortByDistance(reference, buses))
        }

    companion object {
        fun sortByDistance(reference: ReferenceLocation, buses: List<LiveBus>): List<NearbyBus> = buses
            .map { NearbyBus(it, GeoMath.distanceMeters(reference.point, it.position)) }
            .sortedBy { it.distanceMeters }
    }
}

data class NearestStops(val reference: ReferenceLocation, val stops: List<StopWithDistance>)

class ObserveNearestStopsUseCase @Inject constructor(
    private val transitRepository: TransitRepository,
    private val observeReferenceLocation: ObserveReferenceLocationUseCase,
) {
    operator fun invoke(): Flow<NearestStops> =
        combine(observeReferenceLocation(), transitRepository.observeNetwork()) { reference, network ->
            NearestStops(reference, sortByDistance(reference, network.stops))
        }

    companion object {
        fun sortByDistance(reference: ReferenceLocation, stops: List<Stop>): List<StopWithDistance> = stops
            .map { StopWithDistance(it, GeoMath.distanceMeters(reference.point, it.location)) }
            .sortedBy { it.distanceMeters }
    }
}
