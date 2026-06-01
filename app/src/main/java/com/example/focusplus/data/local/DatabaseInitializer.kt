// data/local/DatabaseInitializer.kt
package com.example.focusplus.data.local

import com.example.focusplus.data.local.dao.SettingsDao
import com.example.focusplus.data.local.entity.SettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database initializer for ensuring proper app setup
 * 
 * This class handles:
 * - First-time database setup
 * - Default settings initialization
 * - Data migration from previous versions
 * - Database health checks
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val settingsDao: SettingsDao
) {
    
    /**
     * Initialize the database for first-time use
     * This should be called when the app starts
     */
    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        try {
            // Check if settings exist, if not create default settings
            if (!settingsDao.hasSettingsSync()) {
                createDefaultSettings()
            }
            
            // Perform any necessary data migrations
            performDataMigrations()
            
            // Validate database integrity
            validateDatabaseIntegrity()
            
        } catch (e: Exception) {
            // Log error and handle gracefully
            handleInitializationError(e)
        }
    }
    
    /**
     * Create default settings if none exist
     */
    private suspend fun createDefaultSettings() {
        val defaultSettings = SettingsEntity.default()
        settingsDao.upsertSettings(defaultSettings)
    }
    
    /**
     * Perform any necessary data migrations
     * This is separate from schema migrations and handles data transformations
     */
    private suspend fun performDataMigrations() {
        // Example: Migrate old preference format to new format
        // This would be implemented based on actual migration needs
        
        // Check current settings version and apply necessary transformations
        val currentSettings = settingsDao.getSettingsSync()
        currentSettings?.let { settings ->
            // Example migration logic:
            // if (needsMigration(settings)) {
            //     val migratedSettings = migrateSettings(settings)
            //     settingsDao.updateSettings(migratedSettings)
            // }
        }
    }
    
    /**
     * Validate database integrity
     */
    private suspend fun validateDatabaseIntegrity() {
        // Ensure settings exist and are valid
        val settings = settingsDao.getSettingsSync()
        if (settings == null) {
            throw DatabaseIntegrityException("Settings not found after initialization")
        }
        
        // Validate settings values
        val domainSettings = settings.toDomainModel()
        if (!domainSettings.isValid()) {
            // Fix invalid settings
            val validSettings = SettingsEntity.default()
            settingsDao.updateSettings(validSettings)
        }
    }
    
    /**
     * Handle initialization errors gracefully
     */
    private suspend fun handleInitializationError(error: Exception) {
        when (error) {
            is DatabaseIntegrityException -> {
                // Try to recover by resetting to defaults
                try {
                    settingsDao.deleteAllSettings()
                    createDefaultSettings()
                } catch (recoveryError: Exception) {
                    // If recovery fails, this is a critical error
                    throw DatabaseInitializationException("Failed to initialize database", recoveryError)
                }
            }
            else -> {
                throw DatabaseInitializationException("Database initialization failed", error)
            }
        }
    }
    
    /**
     * Reset database to default state
     * Useful for testing or user-requested reset
     */
    suspend fun resetToDefaults() = withContext(Dispatchers.IO) {
        try {
            // Reset settings to defaults
            settingsDao.resetToDefaults()
            
            // Optionally clear all session data (with user confirmation)
            // sessionRecordDao.deleteAllSessions()
            
        } catch (e: Exception) {
            throw DatabaseResetException("Failed to reset database", e)
        }
    }
    
    /**
     * Check if database needs initialization
     */
    suspend fun needsInitialization(): Boolean = withContext(Dispatchers.IO) {
        try {
            !settingsDao.hasSettingsSync()
        } catch (e: Exception) {
            // If we can't check, assume initialization is needed
            true
        }
    }
    
    /**
     * Get database status for debugging
     */
    suspend fun getDatabaseStatus(): DatabaseStatus = withContext(Dispatchers.IO) {
        try {
            val hasSettings = settingsDao.hasSettingsSync()
            val settings = if (hasSettings) settingsDao.getSettingsSync() else null
            val isSettingsValid = settings?.toDomainModel()?.isValid() ?: false
            
            DatabaseStatus(
                isInitialized = hasSettings,
                hasValidSettings = isSettingsValid,
                settingsVersion = settings?.updatedAt ?: "Unknown"
            )
        } catch (e: Exception) {
            DatabaseStatus(
                isInitialized = false,
                hasValidSettings = false,
                settingsVersion = "Error: ${e.message}",
                error = e
            )
        }
    }
}

/**
 * Database status information for debugging and monitoring
 */
data class DatabaseStatus(
    val isInitialized: Boolean,
    val hasValidSettings: Boolean,
    val settingsVersion: String,
    val error: Exception? = null
) {
    val isHealthy: Boolean
        get() = isInitialized && hasValidSettings && error == null
}

/**
 * Custom exceptions for database initialization
 */
class DatabaseInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class DatabaseIntegrityException(message: String, cause: Throwable? = null) : Exception(message, cause)
class DatabaseResetException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Database health monitor for ongoing monitoring
 */
@Singleton
class DatabaseHealthMonitor @Inject constructor(
    private val databaseInitializer: DatabaseInitializer
) {
    
    /**
     * Perform a health check on the database
     */
    suspend fun performHealthCheck(): DatabaseHealthResult {
        return try {
            val status = databaseInitializer.getDatabaseStatus()
            
            when {
                status.isHealthy -> DatabaseHealthResult.Healthy
                !status.isInitialized -> DatabaseHealthResult.NeedsInitialization
                !status.hasValidSettings -> DatabaseHealthResult.InvalidSettings
                status.error != null -> DatabaseHealthResult.Error(status.error)
                else -> DatabaseHealthResult.Unknown
            }
        } catch (e: Exception) {
            DatabaseHealthResult.Error(e)
        }
    }
}

/**
 * Database health check results
 */
sealed class DatabaseHealthResult {
    object Healthy : DatabaseHealthResult()
    object NeedsInitialization : DatabaseHealthResult()
    object InvalidSettings : DatabaseHealthResult()
    object Unknown : DatabaseHealthResult()
    data class Error(val exception: Exception) : DatabaseHealthResult()
    
    val isHealthy: Boolean
        get() = this is Healthy
}
