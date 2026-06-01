// data/repository/SettingsRepositoryImpl.kt
package com.example.focusplus.data.repository

import com.example.focusplus.data.local.dao.SettingsDao
import com.example.focusplus.data.local.entity.SettingsEntity
import com.example.focusplus.domain.model.Settings
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository interface
 * 
 * This class bridges the domain layer with the data layer,
 * converting between domain models and data entities.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {
    
    // ==================== BASIC OPERATIONS ====================
    
    override fun getSettings(): Flow<Settings> {
        return settingsDao.getSettings().map { entity ->
            entity?.toDomainModel() ?: Settings.DEFAULT
        }
    }
    
    override suspend fun getSettingsSync(): Settings {
        val entity = settingsDao.getSettingsSync()
        return entity?.toDomainModel() ?: Settings.DEFAULT
    }
    
    override suspend fun saveSettings(settings: Settings) {
        val entity = SettingsEntity.fromDomainModel(settings)
        settingsDao.upsertSettings(entity)
    }
    
    override suspend fun updateSettings(settings: Settings) {
        val entity = SettingsEntity.fromDomainModel(settings)
        settingsDao.updateSettings(entity)
    }
    
    override fun hasSettings(): Flow<Boolean> {
        return settingsDao.hasSettings()
    }
    
    override suspend fun resetToDefaults() {
        settingsDao.resetToDefaults()
    }
    
    // ==================== SPECIFIC SETTING UPDATES ====================
    
    override suspend fun updateWorkDuration(durationMinutes: Int) {
        if (isValidDuration(durationMinutes, 1, 120)) {
            settingsDao.updateWorkDuration(durationMinutes)
        } else {
            throw IllegalArgumentException("Work duration must be between 1 and 120 minutes")
        }
    }
    
    override suspend fun updateShortBreakDuration(durationMinutes: Int) {
        if (isValidDuration(durationMinutes, 1, 60)) {
            settingsDao.updateShortBreakDuration(durationMinutes)
        } else {
            throw IllegalArgumentException("Short break duration must be between 1 and 60 minutes")
        }
    }
    
    override suspend fun updateLongBreakDuration(durationMinutes: Int) {
        if (isValidDuration(durationMinutes, 1, 120)) {
            settingsDao.updateLongBreakDuration(durationMinutes)
        } else {
            throw IllegalArgumentException("Long break duration must be between 1 and 120 minutes")
        }
    }
    
    override suspend fun updateSessionsUntilLongBreak(sessions: Int) {
        if (sessions in 1..10) {
            settingsDao.updateSessionsUntilLongBreak(sessions)
        } else {
            throw IllegalArgumentException("Sessions until long break must be between 1 and 10")
        }
    }
    
    override suspend fun updateTheme(isDarkTheme: Boolean) {
        settingsDao.updateTheme(isDarkTheme)
    }
    
    override suspend fun updateNotifications(isEnabled: Boolean) {
        settingsDao.updateNotifications(isEnabled)
    }
    
    override suspend fun updateSound(isEnabled: Boolean) {
        settingsDao.updateSound(isEnabled)
    }
    
    override suspend fun updateVibration(isEnabled: Boolean) {
        settingsDao.updateVibration(isEnabled)
    }
    
    override suspend fun updateAutoStartBreaks(isEnabled: Boolean) {
        settingsDao.updateAutoStartBreaks(isEnabled)
    }
    
    override suspend fun updateAutoStartWork(isEnabled: Boolean) {
        settingsDao.updateAutoStartWork(isEnabled)
    }
    
    // ==================== BULK UPDATES ====================
    
    override suspend fun updateAllDurations(
        workDuration: Int,
        shortBreakDuration: Int,
        longBreakDuration: Int,
        sessionsUntilLongBreak: Int
    ) {
        // Validate all durations before updating
        validateDurations(workDuration, shortBreakDuration, longBreakDuration, sessionsUntilLongBreak)
        
        settingsDao.updateAllDurations(
            workDuration,
            shortBreakDuration,
            longBreakDuration,
            sessionsUntilLongBreak
        )
    }
    
    override suspend fun updateNotificationSettings(
        isNotificationsEnabled: Boolean,
        isSoundEnabled: Boolean,
        isVibrationEnabled: Boolean
    ) {
        settingsDao.updateNotificationSettings(
            isNotificationsEnabled,
            isSoundEnabled,
            isVibrationEnabled
        )
    }
    
    override suspend fun updateAutoStartSettings(
        autoStartBreaks: Boolean,
        autoStartWork: Boolean
    ) {
        settingsDao.updateAutoStartSettings(autoStartBreaks, autoStartWork)
    }
    
    // ==================== GETTER OPERATIONS ====================
    
    override fun getDurationForMode(mode: TimerMode): Flow<Int> {
        return getSettings().map { settings ->
            settings.getDurationForMode(mode)
        }
    }
    
    override fun getWorkDuration(): Flow<Int> {
        return settingsDao.getWorkDuration().map { duration ->
            duration ?: Settings.DEFAULT.workDurationMinutes
        }
    }
    
    override fun getShortBreakDuration(): Flow<Int> {
        return settingsDao.getShortBreakDuration().map { duration ->
            duration ?: Settings.DEFAULT.shortBreakDurationMinutes
        }
    }
    
    override fun getLongBreakDuration(): Flow<Int> {
        return settingsDao.getLongBreakDuration().map { duration ->
            duration ?: Settings.DEFAULT.longBreakDurationMinutes
        }
    }
    
    override fun getSessionsUntilLongBreak(): Flow<Int> {
        return getSettings().map { settings ->
            settings.sessionsUntilLongBreak
        }
    }
    
    override fun getThemePreference(): Flow<Boolean> {
        return settingsDao.getThemePreference().map { theme ->
            theme ?: Settings.DEFAULT.isDarkTheme
        }
    }
    
    override fun getNotificationPreference(): Flow<Boolean> {
        return settingsDao.getNotificationPreference().map { notifications ->
            notifications ?: Settings.DEFAULT.isNotificationsEnabled
        }
    }
    
    override fun getSoundPreference(): Flow<Boolean> {
        return getSettings().map { settings ->
            settings.isSoundEnabled
        }
    }
    
    override fun getVibrationPreference(): Flow<Boolean> {
        return getSettings().map { settings ->
            settings.isVibrationEnabled
        }
    }
    
    override fun getAutoStartBreaksPreference(): Flow<Boolean> {
        return getSettings().map { settings ->
            settings.autoStartBreaks
        }
    }
    
    override fun getAutoStartWorkPreference(): Flow<Boolean> {
        return getSettings().map { settings ->
            settings.autoStartWork
        }
    }
    
    // ==================== VALIDATION ====================
    
    override fun validateSettings(settings: Settings): Boolean {
        return settings.isValid()
    }
    
    override fun getValidationErrors(settings: Settings): List<String> {
        return settings.getValidationErrors()
    }
    
    // ==================== PRESETS ====================
    
    override suspend fun applyPomodoroStandard() {
        saveSettings(Settings.POMODORO_STANDARD)
    }
    
    override suspend fun applyDeepFocus() {
        saveSettings(Settings.DEEP_FOCUS)
    }
    
    override suspend fun applyShortBursts() {
        saveSettings(Settings.SHORT_BURSTS)
    }
    
    override suspend fun applyCustomPreset(preset: Settings) {
        if (validateSettings(preset)) {
            saveSettings(preset)
        } else {
            val errors = getValidationErrors(preset)
            throw IllegalArgumentException("Invalid preset settings: ${errors.joinToString(", ")}")
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Validate duration value within range
     */
    private fun isValidDuration(duration: Int, min: Int, max: Int): Boolean {
        return duration in min..max
    }
    
    /**
     * Validate all duration settings
     */
    private fun validateDurations(
        workDuration: Int,
        shortBreakDuration: Int,
        longBreakDuration: Int,
        sessionsUntilLongBreak: Int
    ) {
        val errors = mutableListOf<String>()
        
        if (!isValidDuration(workDuration, 1, 120)) {
            errors.add("Work duration must be between 1 and 120 minutes")
        }
        
        if (!isValidDuration(shortBreakDuration, 1, 60)) {
            errors.add("Short break duration must be between 1 and 60 minutes")
        }
        
        if (!isValidDuration(longBreakDuration, 1, 120)) {
            errors.add("Long break duration must be between 1 and 120 minutes")
        }
        
        if (sessionsUntilLongBreak !in 1..10) {
            errors.add("Sessions until long break must be between 1 and 10")
        }
        
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Invalid duration settings: ${errors.joinToString(", ")}")
        }
    }
    
    /**
     * Ensure settings exist, create defaults if not
     */
    private suspend fun ensureSettingsExist(): Settings {
        return if (settingsDao.hasSettingsSync()) {
            getSettingsSync()
        } else {
            val defaultSettings = Settings.DEFAULT
            saveSettings(defaultSettings)
            defaultSettings
        }
    }
    
    /**
     * Get settings with fallback to defaults
     */
    private suspend fun getSettingsWithFallback(): Settings {
        return try {
            getSettingsSync()
        } catch (e: Exception) {
            // If there's an error getting settings, return defaults
            Settings.DEFAULT
        }
    }
    
    /**
     * Safely update settings with validation
     */
    private suspend fun safeUpdateSettings(updateAction: suspend () -> Unit) {
        try {
            updateAction()
        } catch (e: Exception) {
            // Log error and potentially recover
            throw SettingsUpdateException("Failed to update settings", e)
        }
    }
}

/**
 * Custom exception for settings update failures
 */
class SettingsUpdateException(message: String, cause: Throwable? = null) : Exception(message, cause)
