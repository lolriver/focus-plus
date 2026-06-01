// data/local/entity/SettingsEntity.kt
package com.example.focusplus.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.focusplus.domain.model.Settings
import java.time.LocalDateTime

/**
 * Room entity for storing user settings in the local database
 * Uses a single row approach with a fixed ID for settings storage
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // Always use ID = 1 for single settings row
    
    @ColumnInfo(name = "work_duration_minutes")
    val workDurationMinutes: Int = 25,
    
    @ColumnInfo(name = "short_break_duration_minutes")
    val shortBreakDurationMinutes: Int = 5,
    
    @ColumnInfo(name = "long_break_duration_minutes")
    val longBreakDurationMinutes: Int = 15,
    
    @ColumnInfo(name = "sessions_until_long_break")
    val sessionsUntilLongBreak: Int = 4,
    
    @ColumnInfo(name = "is_dark_theme")
    val isDarkTheme: Boolean = false,
    
    @ColumnInfo(name = "is_notifications_enabled")
    val isNotificationsEnabled: Boolean = true,
    
    @ColumnInfo(name = "is_sound_enabled")
    val isSoundEnabled: Boolean = true,
    
    @ColumnInfo(name = "is_vibration_enabled")
    val isVibrationEnabled: Boolean = true,
    
    @ColumnInfo(name = "auto_start_breaks")
    val autoStartBreaks: Boolean = false,
    
    @ColumnInfo(name = "auto_start_work")
    val autoStartWork: Boolean = false,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = LocalDateTime.now().toString() // Audit field
) {
    /**
     * Convert Room entity to domain model
     */
    fun toDomainModel(): Settings {
        return Settings(
            workDurationMinutes = workDurationMinutes,
            shortBreakDurationMinutes = shortBreakDurationMinutes,
            longBreakDurationMinutes = longBreakDurationMinutes,
            sessionsUntilLongBreak = sessionsUntilLongBreak,
            isDarkTheme = isDarkTheme,
            isNotificationsEnabled = isNotificationsEnabled,
            isSoundEnabled = isSoundEnabled,
            isVibrationEnabled = isVibrationEnabled,
            autoStartBreaks = autoStartBreaks,
            autoStartWork = autoStartWork
        )
    }
    
    companion object {
        /**
         * Convert domain model to Room entity
         */
        fun fromDomainModel(settings: Settings): SettingsEntity {
            return SettingsEntity(
                id = 1, // Always use fixed ID for settings
                workDurationMinutes = settings.workDurationMinutes,
                shortBreakDurationMinutes = settings.shortBreakDurationMinutes,
                longBreakDurationMinutes = settings.longBreakDurationMinutes,
                sessionsUntilLongBreak = settings.sessionsUntilLongBreak,
                isDarkTheme = settings.isDarkTheme,
                isNotificationsEnabled = settings.isNotificationsEnabled,
                isSoundEnabled = settings.isSoundEnabled,
                isVibrationEnabled = settings.isVibrationEnabled,
                autoStartBreaks = settings.autoStartBreaks,
                autoStartWork = settings.autoStartWork,
                updatedAt = LocalDateTime.now().toString()
            )
        }
        
        /**
         * Default settings entity
         */
        fun default(): SettingsEntity {
            return fromDomainModel(Settings.DEFAULT)
        }
    }
}
