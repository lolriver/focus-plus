// data/repository/RepositoryModule.kt
package com.example.focusplus.data.repository

import com.example.focusplus.domain.repository.SessionRepository
import com.example.focusplus.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations
 * 
 * This module provides the dependency injection bindings for repositories,
 * allowing the domain layer to depend on interfaces while the data layer
 * provides the concrete implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Bind SessionRepository interface to SessionRepositoryImpl
     * 
     * @param sessionRepositoryImpl The concrete implementation
     * @return SessionRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        sessionRepositoryImpl: SessionRepositoryImpl
    ): SessionRepository
    
    /**
     * Bind SettingsRepository interface to SettingsRepositoryImpl
     * 
     * @param settingsRepositoryImpl The concrete implementation
     * @return SettingsRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
