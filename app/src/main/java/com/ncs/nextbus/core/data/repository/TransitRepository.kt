package com.ncs.nextbus.core.data.repository

import androidx.room.withTransaction
import com.ncs.nextbus.core.common.IoDispatcher
import com.ncs.nextbus.core.data.database.NextBusDatabase
import com.ncs.nextbus.core.data.database.TransitEntities
import com.ncs.nextbus.core.data.database.toEntities
import com.ncs.nextbus.core.data.datastore.UserPreferencesDataSource
import com.ncs.nextbus.core.data.seed.SeedBuilder
import com.ncs.nextbus.core.data.seed.SeedNetwork
import com.ncs.nextbus.core.model.TransitNetwork
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Static network data: stops, routes, timetables and fleet. */
interface TransitRepository {
    suspend fun getNetwork(): TransitNetwork

    fun observeNetwork(): Flow<TransitNetwork> = flow { emit(getNetwork()) }
}

/** Serves the network from Room, seeding it from [SeedNetwork] on first use or when the seed version changes. */
@Singleton
class OfflineTransitRepository @Inject constructor(
    private val seeder: TransitSeeder,
    private val database: NextBusDatabase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransitRepository {
    private val mutex = Mutex()

    @Volatile
    private var cached: TransitNetwork? = null

    override suspend fun getNetwork(): TransitNetwork = cached ?: mutex.withLock {
        cached ?: withContext(ioDispatcher) {
            seeder.ensureSeeded()
            val dao = database.transitDao()
            TransitEntities(dao.stops(), dao.routes(), dao.routeStops(), dao.buses()).toNetwork()
        }.also { cached = it }
    }
}

@Singleton
class TransitSeeder @Inject constructor(
    private val database: NextBusDatabase,
    private val preferences: UserPreferencesDataSource,
) {
    suspend fun ensureSeeded() {
        val dao = database.transitDao()
        if (preferences.seedVersion() == SeedNetwork.VERSION && dao.stopCount() > 0) return

        Timber.i("Seeding transit network v%d", SeedNetwork.VERSION)
        val entities = SeedBuilder.build().toEntities()
        database.withTransaction {
            dao.clearBuses()
            dao.clearRouteStops()
            dao.clearRoutes()
            dao.clearStops()
            dao.insertStops(entities.stops)
            dao.insertRoutes(entities.routes)
            dao.insertRouteStops(entities.routeStops)
            dao.insertBuses(entities.buses)
        }
        preferences.setSeedVersion(SeedNetwork.VERSION)
    }
}
