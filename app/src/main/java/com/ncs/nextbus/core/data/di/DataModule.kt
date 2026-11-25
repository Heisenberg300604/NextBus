package com.ncs.nextbus.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ncs.nextbus.core.common.DefaultDispatcher
import com.ncs.nextbus.core.common.IoDispatcher
import com.ncs.nextbus.core.common.SystemTimeSource
import com.ncs.nextbus.core.common.TimeSource
import com.ncs.nextbus.core.data.database.NextBusDatabase
import com.ncs.nextbus.core.data.database.UserDataDao
import com.ncs.nextbus.core.data.repository.AndroidConnectivityRepository
import com.ncs.nextbus.core.data.repository.ConnectivityRepository
import com.ncs.nextbus.core.data.repository.DataStorePreferencesRepository
import com.ncs.nextbus.core.data.repository.FusedLocationRepository
import com.ncs.nextbus.core.data.repository.LiveBusRepository
import com.ncs.nextbus.core.data.repository.LocationRepository
import com.ncs.nextbus.core.data.repository.OfflineTransitRepository
import com.ncs.nextbus.core.data.repository.PreferencesRepository
import com.ncs.nextbus.core.data.repository.RoomUserDataRepository
import com.ncs.nextbus.core.data.repository.ScheduleRepository
import com.ncs.nextbus.core.data.repository.SimulatedLiveBusRepository
import com.ncs.nextbus.core.data.repository.TransitRepository
import com.ncs.nextbus.core.data.repository.UserDataRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NextBusDatabase =
        Room.databaseBuilder(context, NextBusDatabase::class.java, "nextbus.db").build()

    @Provides
    fun provideUserDataDao(database: NextBusDatabase): UserDataDao = database.userDataDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("user_preferences") })

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource

    @Binds
    abstract fun bindTransitRepository(impl: OfflineTransitRepository): TransitRepository

    @Binds
    abstract fun bindLiveBusRepository(impl: SimulatedLiveBusRepository): LiveBusRepository

    @Binds
    abstract fun bindScheduleRepository(impl: SimulatedLiveBusRepository): ScheduleRepository

    @Binds
    abstract fun bindUserDataRepository(impl: RoomUserDataRepository): UserDataRepository

    @Binds
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds
    abstract fun bindLocationRepository(impl: FusedLocationRepository): LocationRepository

    @Binds
    abstract fun bindConnectivityRepository(impl: AndroidConnectivityRepository): ConnectivityRepository
}
