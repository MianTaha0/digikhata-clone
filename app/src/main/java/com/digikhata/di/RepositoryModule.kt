package com.digikhata.di

import com.digikhata.data.repository.DigiRepositoryImpl
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
}
