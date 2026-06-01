// data/local/dao/SettingsDao.kt
package com.example.focusplus.data.local.dao

import androidx.room.*
import com.example.focusplus.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Settings operations
 * Uses a single-row approach where settings always have ID = 1
 */
@Dao
interface SettingsDao {
    
    // ==================== BASIC OPERATIONS ====================
    
    /**
     * Insert or update settings (upsert operation)
     * Since we use a fixed ID (1), this will always update existing settings
     * @param settings The settings to save
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SettingsEntity)
    
    /**
     * Update existing settings
     * @param settings The settings to update
     */
    @Update
    suspend fun updateSettings(settings: SettingsEntity)
    
    /**
     * Delete all settings (rarely used, mainly for testing)
     */
    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings()
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Get current settings
     * @return Flow of current settings (reactive updates)
     */
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>
    
    /**
     * Get settings synchronously (for initialization)
     * @return Current settings or null if not found
     */
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsSync(): SettingsEntity?
    
    /**
     * Check if settings exist
     * @return Flow indicating whether settings are configured
     */
    @Query("SELECT COUNT(*) > 0 FROM settings WHERE id = 1")
    fun hasSettings(): Flow<Boolean>
    
    /**
     * Check if settings exist synchronously
     * @return Boolean indicating whether settings are configured
     */
    @Query("SELECT COUNT(*) > 0 FROM settings WHERE id = 1")
    suspend fun hasSettingsSync(): Boolean
    
    // ==================== SPECIFIC SETTING UPDATES ====================
    
    /**
     * Update work duration only
     * @param workDurationMinutes New work duration in minutes
     */
    @Query("UPDATE settings SET work_duration_minutes = :workDurationMinutes, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateWorkDuration(workDurationMinutes: Int)
    
    /**
     * Update short break duration only
     * @param shortBreakDurationMinutes New short break duration in minutes
     */
    @Query("UPDATE settings SET short_break_duration_minutes = :shortBreakDurationMinutes, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateShortBreakDuration(shortBreakDurationMinutes: Int)
    
    /**
     * Update long break duration only
     * @param longBreakDurationMinutes New long break duration in minutes
     */
    @Query("UPDATE settings SET long_break_duration_minutes = :longBreakDurationMinutes, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateLongBreakDuration(longBreakDurationMinutes: Int)
    
    /**
     * Update sessions until long break only
     * @param sessionsUntilLongBreak New number of sessions before long break
     */
    @Query("UPDATE settings SET sessions_until_long_break = :sessionsUntilLongBreak, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateSessionsUntilLongBreak(sessionsUntilLongBreak: Int)
    
    /**
     * Update theme preference only
     * @param isDarkTheme Whether dark theme is enabled
     */
    @Query("UPDATE settings SET is_dark_theme = :isDarkTheme, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateTheme(isDarkTheme: Boolean)
    
    /**
     * Update notification preference only
     * @param isNotificationsEnabled Whether notifications are enabled
     */
    @Query("UPDATE settings SET is_notifications_enabled = :isNotificationsEnabled, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateNotifications(isNotificationsEnabled: Boolean)
    
    /**
     * Update sound preference only
     * @param isSoundEnabled Whether sound alerts are enabled
     */
    @Query("UPDATE settings SET is_sound_enabled = :isSoundEnabled, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateSound(isSoundEnabled: Boolean)
    
    /**
     * Update vibration preference only
     * @param isVibrationEnabled Whether vibration alerts are enabled
     */
    @Query("UPDATE settings SET is_vibration_enabled = :isVibrationEnabled, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateVibration(isVibrationEnabled: Boolean)
    
    /**
     * Update auto-start breaks preference only
     * @param autoStartBreaks Whether breaks should start automatically
     */
    @Query("UPDATE settings SET auto_start_breaks = :autoStartBreaks, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateAutoStartBreaks(autoStartBreaks: Boolean)
    
    /**
     * Update auto-start work preference only
     * @param autoStartWork Whether work sessions should start automatically
     */
    @Query("UPDATE settings SET auto_start_work = :autoStartWork, updated_at = datetime('now') WHERE id = 1")
    suspend fun updateAutoStartWork(autoStartWork: Boolean)
    
    // ==================== BULK UPDATES ====================
    
    /**
     * Update all duration settings at once
     * @param workDuration Work session duration in minutes
     * @param shortBreakDuration Short break duration in minutes
     * @param longBreakDuration Long break duration in minutes
     * @param sessionsUntilLongBreak Number of sessions before long break
     */
    @Query("""
        UPDATE settings SET 
            work_duration_minutes = :workDuration,
            short_break_duration_minutes = :shortBreakDuration,
            long_break_duration_minutes = :longBreakDuration,
            sessions_until_long_break = :sessionsUntilLongBreak,
            updated_at = datetime('now')
        WHERE id = 1
    """)
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
    @Query("""
        UPDATE settings SET 
            is_notifications_enabled = :isNotificationsEnabled,
            is_sound_enabled = :isSoundEnabled,
            is_vibration_enabled = :isVibrationEnabled,
            updated_at = datetime('now')
        WHERE id = 1
    """)
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
    @Query("""
        UPDATE settings SET 
            auto_start_breaks = :autoStartBreaks,
            auto_start_work = :autoStartWork,
            updated_at = datetime('now')
        WHERE id = 1
    """)
    suspend fun updateAutoStartSettings(
        autoStartBreaks: Boolean,
        autoStartWork: Boolean
    )
    
    // ==================== GETTER QUERIES FOR SPECIFIC VALUES ====================
    
    /**
     * Get work duration only
     * @return Flow of work duration in minutes
     */
    @Query("SELECT work_duration_minutes FROM settings WHERE id = 1")
    fun getWorkDuration(): Flow<Int?>
    
    /**
     * Get short break duration only
     * @return Flow of short break duration in minutes
     */
    @Query("SELECT short_break_duration_minutes FROM settings WHERE id = 1")
    fun getShortBreakDuration(): Flow<Int?>
    
    /**
     * Get long break duration only
     * @return Flow of long break duration in minutes
     */
    @Query("SELECT long_break_duration_minutes FROM settings WHERE id = 1")
    fun getLongBreakDuration(): Flow<Int?>
    
    /**
     * Get theme preference only
     * @return Flow of theme preference (true for dark theme)
     */
    @Query("SELECT is_dark_theme FROM settings WHERE id = 1")
    fun getThemePreference(): Flow<Boolean?>
    
    /**
     * Get notification preference only
     * @return Flow of notification preference
     */
    @Query("SELECT is_notifications_enabled FROM settings WHERE id = 1")
    fun getNotificationPreference(): Flow<Boolean?>
    
    /**
     * Reset settings to default values
     */
    @Query("""
        UPDATE settings SET 
            work_duration_minutes = 25,
            short_break_duration_minutes = 5,
            long_break_duration_minutes = 15,
            sessions_until_long_break = 4,
            is_dark_theme = 0,
            is_notifications_enabled = 1,
            is_sound_enabled = 1,
            is_vibration_enabled = 1,
            auto_start_breaks = 0,
            auto_start_work = 0,
            updated_at = datetime('now')
        WHERE id = 1
    """)
    suspend fun resetToDefaults()
}
