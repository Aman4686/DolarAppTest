package com.example.dolarapptest.data.di

import com.example.dolarapptest.data.repository.TickersRepositoryImpl
import com.example.dolarapptest.domain.repository.TickersRepository
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
    abstract fun bindTickersRepository(impl: TickersRepositoryImpl): TickersRepository
}
