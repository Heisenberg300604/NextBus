package com.ncs.nextbus.testing

import com.ncs.nextbus.core.data.repository.ConnectivityRepository
import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.LocationRepository
import com.ncs.nextbus.core.data.repository.ScheduleRepository
import com.ncs.nextbus.core.data.simulator.BusSimulator
import com.ncs.nextbus.core.model.DeviceLocation
import com.ncs.nextbus.core.model.Direction
import com.ncs.nextbus.core.model.LiveBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/** Serves a fixed simulator snapshot; tests can push new snapshots via [buses]. */
class FakeLiveBusRepository(
    initial: List<LiveBus> = BusSimulator(seedNetwork).snapshot(FIXED_NOW),
) : LiveBusRepository, ScheduleRepository {
    val buses = MutableStateFlow(initial)
    override val liveBuses: Flow<List<LiveBus>> = buses

    override suspend fun scheduledArrivals(
        routeId: String,
        stopIndex: Int,
        direction: Direction,
        fromMillis: Long,
        count: Int,
    ): List<Long> = BusSimulator(seedNetwork).scheduledArrivals(routeId, stopIndex, direction, fromMillis, count)
}

class FakeLocationRepository(initial: DeviceLocation = DeviceLocation.NoPermission) : LocationRepository {
    val location = MutableStateFlow(initial)
    override fun hasLocationPermission(): Boolean = location.value != DeviceLocation.NoPermission
    override fun observeLocation(): Flow<DeviceLocation> = location
    override fun onPermissionChanged() = Unit
}

class FakeConnectivityRepository(online: Boolean = true) : ConnectivityRepository {
    override val isOnline: Flow<Boolean> = flowOf(online)
}
