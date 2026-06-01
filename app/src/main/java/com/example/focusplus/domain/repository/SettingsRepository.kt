// domain/repository/SettingsRepository.kt
package com.example.focusplus.domain.repository

import com.example.focusplus.domain.model.Settings
import com.example.focusplus.domain.model.TimerMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for settings-related operations
 * 
 * This interface defines the contract for settings data operations
 * following Clean Architecture principles. It's part of the domain layer
 * and has no dependencies on Android or Room.
 */
interface SettingsRepository {
    
    // ==================== BASIC OPERATIONS ====================
    
    /**
     * Get current settings
     * @return Flow of current settings (reactive updates)
     */
    fun getSettings(): Flow<Settings>
    
    /**
     * Get settings synchronously (for initialization)
     * @return Current settings or default if not found
     */
    suspend fun getSettingsSync(): Settings
    
    /**
     * Save settings (insert or update)
     * @param settings The settings to save
     */
    suspend fun saveSettings(settings: Settings)
    
    /**
     * Update settings
     * @param settings The settings to update
     */
    suspend fun updateSettings(settings: Settings)
    
    /**
     * Check if settings exist
     * @return Flow indicating whether settings are configured
     */
    fun hasSettings(): Flow<Boolean>
    
    /**
     * Reset settings to default values
     */
    suspend fun resetToDefaults()
    
    // ==================== SPECIFIC SETTING UPDATES ====================
    
    /**
     * Update work duration only
     * @param durationMinutes New work duration in minutes
     */
    suspend fun updateWorkDuration(durationMinutes: Int)
    
    /**
     * Update short break duration only
     * @param durationMinutes New short break duration in minutes
     */
    suspend fun updateShortBreakDuration(durationMinutes: Int)
    
    /**
     * Update long break duration only
     * @param durationMinutes New long break duration in minutes
     */
    suspend fun updateLongBreakDuration(durationMinutes: Int)
    
    /**
     * Update sessions until long break
     * @param sessions New number of sessions before long break
     */
    suspend fun updateSessionsUntilLongBreak(sessions: Int)
    
    /**
     * Update theme preference
     * @param isDarkTheme Whether dark theme is enabled
     */
    suspend fun updateTheme(isDarkTheme: Boolean)
    
    /**
     * Update notification preference
     * @param isEnabled Whether notifications are enabled
     */
    suspend fun updateNotifications(isEnabled: Boolean)
    
    /**
     * Update sound preference
     * @param isEnabled Whether sound alerts are enabled
     */
    suspend fun updateSound(isEnabled: Boolean)
    
    /**
     * Update vibration preference
     * @param isEnabled Whether vibration alerts are enabled
     */
    suspend fun updateVibration(isEnabled: Boolean)
    
    /**
     * Update auto-start breaks preference
     * @param isEnabled Whether breaks should start automatically
     */
    suspend fun updateAutoStartBreaks(isEnabled: Boolean)
    
    /**
     * Update auto-start work preference
     * @param isEnabled Whether work sessions should start automatically
     */
    suspend fun updateAutoStartWork(isEnabled: Boolean)
    
    // ==================== BULK UPDATES ====================
    
    /**
     * Update all duration settings at once
     * @param workDuration Work session duration in minutes
     * @param shortBreakDuration Short break duration in minutes
     * @param longBreakDuration Long break duration in minutes
     * @param sessionsUntilLongBreak Number of sessions before long break
     */
    suspend fun updateAllDurations(
        workDuration: Int,
        shortBreakDuration: Int,
        longBreakDuration: Int,
        sessionsUntilLongBreak: Int
    )
    
    /**
     * Update all notification settings at once
     * @param isNotificationsEnabled Whether notifications are enabled
     * @param isSoundEnabled Whether sound alerts are enabled
     * @param isVibrationEnabled Whether vibration alerts are enabled
     */
    suspend fun updateNotificationSettings(
        isNotificationsEnabled: Boolean,
        isSoundEnabled: Boolean,
        isVibrationEnabled: Boolean
    )
    
    /**
     * Update all auto-start settings at once
     * @param autoStartBreaks Whether breaks should start automatically
     * @param autoStartWork Whether work sessions should start automatically
     */
    suspend fun updateAutoStartSettings(
        autoStartBreaks: Boolean,
        autoStartWork: Boolean
    )
    
    // ==================== GETTER OPERATIONS ====================
    
    /**
     * Get duration for specific timer mode
     * @param mode The timer mode
     * @return Flow of duration in minutes for the specified mode
     */
    fun getDurationForMode(mode: TimerMode): Flow<Int>
    
    /**
     * Get work duration only
     * @return Flow of work duration in minutes
     */
    fun getWorkDuration(): Flow<Int>
    
    /**
     * Get short break duration only
     * @return Flow of short break duration in minutes
     */
    fun getShortBreakDuration(): Flow<Int>
    
    /**
     * Get long break duration only
     * @return Flow of long break duration in minutes
     */
    fun getLongBreakDuration(): Flow<Int>
    
    /**
     * Get sessions until long break
     * @return Flow of number of sessions before long break
     */
    fun getSessionsUntilLongBreak(): Flow<Int>
    
    /**
     * Get theme preference
     * @return Flow of theme preference (true for dark theme)
     */
    fun getThemePreference(): Flow<Boolean>
    
    /**
     * Get notification preference
     * @return Flow of notification preference
     */
    fun getNotificationPreference(): Flow<Boolean>
    
    /**
     * Get sound preference
     * @return Flow of sound preference
     */
    fun getSoundPreference(): Flow<Boolean>
    
    /**
     * Get vibration preference
     * @return Flow of vibration preference
     */
    fun getVibrationPreference(): Flow<Boolean>
    
    /**
     * Get auto-start breaks preference
     * @return Flow of auto-start breaks preference
     */
    fun getAutoStartBreaksPreference(): Flow<Boolean>
    
    /**
     * Get auto-start work preference
     * @return Flow of auto-start work preference
     */
    fun getAutoStartWorkPreference(): Flow<Boolean>
    
    // ==================== VALIDATION ====================
    
    /**
     * Validate settings
     * @param settings The settings to validate
     * @return True if settings are valid, false otherwise
     */
    fun validateSettings(settings: Settings): Boolean
    
    /**
     * Get validation errors for settings
     * @param settings The settings to validate
     * @return List of validation error messages
     */
    fun getValidationErrors(settings: Settings): List<String>
    
    // ==================== PRESETS ====================
    
    /**
     * Apply Pomodoro standard settings (25/5/15/4)
     */
    suspend fun applyPomodoroStandard()
    
    /**
     * Apply deep focus settings (50/10/30/3)
     */
    suspend fun applyDeepFocus()
    
    /**
     * Apply short bursts settings (15/3/10/6)
     */
    suspend fun applyShortBursts()
    
    /**
     * Apply custom preset
     * @param preset The preset settings to apply
     */
    suspend fun applyCustomPreset(preset: Settings)
}
