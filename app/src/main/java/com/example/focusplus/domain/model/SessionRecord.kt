// domain/model/SessionRecord.kt
package com.example.focusplus.domain.model

import java.time.LocalDateTime

/**
 * Domain model representing a completed Pomodoro session
 * This is the clean domain model without any Android/Room dependencies
 */
data class SessionRecord(
    val id: Long = 0,
    val mode: TimerMode,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val completedAt: LocalDateTime,
    val wasCompleted: Boolean,
    val cycleNumber: Int = 1,
    val notes: String? = null
) {
    /**
     * Check if the session was completed successfully
     */
    val isSuccessful: Boolean
        get() = wasCompleted && actualDurationMinutes >= (plannedDurationMinutes * 0.8) // 80% completion threshold
    
    /**
     * Get completion percentage
     */
    val completionPercentage: Float
        get() = if (plannedDurationMinutes > 0) {
            (actualDurationMinutes.toFloat() / plannedDurationMinutes.toFloat() * 100f).coerceAtMost(100f)
        } else 0f
    
    /**
     * Get formatted duration string
     */
    val formattedDuration: String
        get() = "${actualDurationMinutes}m"
    
    /**
     * Check if this session was today
     */
    fun isToday(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return completedAt.toLocalDate() == now.toLocalDate()
    }
    
    /**
     * Check if this session was this week
     */
    fun isThisWeek(now: LocalDateTime = LocalDateTime.now()): Boolean {
        // Simple week calculation - can be enhanced with proper week-of-year logic
        val daysDifference = java.time.temporal.ChronoUnit.DAYS.between(completedAt.toLocalDate(), now.toLocalDate())
        return daysDifference in 0..6
    }
}
