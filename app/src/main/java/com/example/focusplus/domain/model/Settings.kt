// domain/model/Settings.kt
package com.example.focusplus.domain.model

/**
 * Domain model representing user settings and preferences
 * This is the clean domain model without any Android/Room dependencies
 */
data class Settings(
    val workDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val sessionsUntilLongBreak: Int = 4,
    val isDarkTheme: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val autoStartBreaks: Boolean = false,
    val autoStartWork: Boolean = false
) {
    /**
     * Get duration for specific timer mode
     */
    fun getDurationForMode(mode: TimerMode): Int {
        return when (mode) {
            TimerMode.WORK -> workDurationMinutes
            TimerMode.SHORT_BREAK -> shortBreakDurationMinutes
            TimerMode.LONG_BREAK -> longBreakDurationMinutes
        }
    }
    
    /**
     * Validate settings values
     */
    fun isValid(): Boolean {
        return workDurationMinutes > 0 &&
                shortBreakDurationMinutes > 0 &&
                longBreakDurationMinutes > 0 &&
                sessionsUntilLongBreak > 0 &&
                workDurationMinutes <= 120 && // Max 2 hours
                shortBreakDurationMinutes <= 60 && // Max 1 hour
                longBreakDurationMinutes <= 120 && // Max 2 hours
                sessionsUntilLongBreak <= 10 // Max 10 sessions
    }
    
    /**
     * Get validation errors
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (workDurationMinutes <= 0) errors.add("Work duration must be greater than 0")
        if (workDurationMinutes > 120) errors.add("Work duration cannot exceed 120 minutes")
        
        if (shortBreakDurationMinutes <= 0) errors.add("Short break duration must be greater than 0")
        if (shortBreakDurationMinutes > 60) errors.add("Short break duration cannot exceed 60 minutes")
        
        if (longBreakDurationMinutes <= 0) errors.add("Long break duration must be greater than 0")
        if (longBreakDurationMinutes > 120) errors.add("Long break duration cannot exceed 120 minutes")
        
        if (sessionsUntilLongBreak <= 0) errors.add("Sessions until long break must be greater than 0")
        if (sessionsUntilLongBreak > 10) errors.add("Sessions until long break cannot exceed 10")
        
        return errors
    }
    
    companion object {
        /**
         * Default settings instance
         */
        val DEFAULT = Settings()
        
        /**
         * Pomodoro technique standard settings
         */
        val POMODORO_STANDARD = Settings(
            workDurationMinutes = 25,
            shortBreakDurationMinutes = 5,
            longBreakDurationMinutes = 15,
            sessionsUntilLongBreak = 4
        )
        
        /**
         * Extended work sessions for deep focus
         */
        val DEEP_FOCUS = Settings(
            workDurationMinutes = 50,
            shortBreakDurationMinutes = 10,
            longBreakDurationMinutes = 30,
            sessionsUntilLongBreak = 3
        )
        
        /**
         * Short bursts for tasks requiring frequent breaks
         */
        val SHORT_BURSTS = Settings(
            workDurationMinutes = 15,
            shortBreakDurationMinutes = 3,
            longBreakDurationMinutes = 10,
            sessionsUntilLongBreak = 6
        )
    }
}
