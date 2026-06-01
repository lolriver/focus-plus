// domain/model/SessionStats.kt
package com.example.focusplus.domain.model

/**
 * Domain model representing aggregated session statistics
 */
data class SessionStats(
    val sessionsToday: Int = 0,
    val sessionsThisWeek: Int = 0,
    val totalSessions: Int = 0,
    val workMinutesToday: Int = 0,
    val workMinutesThisWeek: Int = 0,
    val totalWorkMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val completionRate: Float = 0f, // Percentage of completed vs started sessions
    val averageSessionLength: Float = 0f // Average minutes per session
) {
    /**
     * Get formatted work time for today
     */
    val formattedWorkTimeToday: String
        get() = formatMinutes(workMinutesToday)
    
    /**
     * Get formatted work time for this week
     */
    val formattedWorkTimeThisWeek: String
        get() = formatMinutes(workMinutesThisWeek)
    
    /**
     * Get formatted total work time
     */
    val formattedTotalWorkTime: String
        get() = formatMinutes(totalWorkMinutes)
    
    /**
     * Get completion rate as percentage string
     */
    val formattedCompletionRate: String
        get() = "${(completionRate * 100).toInt()}%"
    
    /**
     * Check if user has any activity today
     */
    val hasActivityToday: Boolean
        get() = sessionsToday > 0
    
    /**
     * Check if user is on a streak
     */
    val isOnStreak: Boolean
        get() = currentStreak > 0
    
    /**
     * Get productivity level based on sessions today
     */
    val productivityLevel: ProductivityLevel
        get() = when {
            sessionsToday == 0 -> ProductivityLevel.NONE
            sessionsToday < 2 -> ProductivityLevel.LOW
            sessionsToday < 4 -> ProductivityLevel.MEDIUM
            sessionsToday < 8 -> ProductivityLevel.HIGH
            else -> ProductivityLevel.EXCELLENT
        }
    
    private fun formatMinutes(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m"
            minutes < 1440 -> { // Less than 24 hours
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                if (remainingMinutes == 0) "${hours}h" else "${hours}h ${remainingMinutes}m"
            }
            else -> { // 24 hours or more
                val days = minutes / 1440
                val remainingHours = (minutes % 1440) / 60
                if (remainingHours == 0) "${days}d" else "${days}d ${remainingHours}h"
            }
        }
    }
}

/**
 * Represents different levels of productivity
 */
enum class ProductivityLevel(val displayName: String, val description: String) {
    NONE("No Activity", "Start your first session today!"),
    LOW("Getting Started", "Good start! Keep building momentum."),
    MEDIUM("Productive", "You're in a good rhythm!"),
    HIGH("Highly Productive", "Excellent focus today!"),
    EXCELLENT("Outstanding", "Amazing productivity! You're on fire!")
}
