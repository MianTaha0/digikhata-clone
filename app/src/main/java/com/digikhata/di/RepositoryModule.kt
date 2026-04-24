package com.digikhata.di

import com.digikhata.data.repository.DigiRepositoryImpl
import com.digikhata.data.sync.CloudSyncRepository
import com.digikhata.data.sync.CloudSyncRepositoryImpl
import com.digikhata.data.sync.PushTrigger
import com.digikhata.data.sync.SyncScheduler
import com.digikhata.domain.repository.DigiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDigiRepository(impl: DigiRepositoryImpl): DigiRepository

    @Binds
    @Singleton
    abstract fun bindCloudSyncRepository(impl: CloudSyncRepositoryImpl): CloudSyncRepository

    @Binds
    @Singleton
    abstract fun bindPushTrigger(impl: SyncScheduler): PushTrigger
}
