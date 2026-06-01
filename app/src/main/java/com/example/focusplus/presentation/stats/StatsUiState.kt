// presentation/stats/StatsUiState.kt
package com.example.focusplus.presentation.stats

import com.example.focusplus.domain.model.SessionRecord
import com.example.focusplus.domain.model.SessionStats
import com.example.focusplus.domain.model.TimerMode
import java.time.LocalDateTime

/**
 * UI state for the Statistics screen
 * 
 * This data class represents the complete state of the statistics UI,
 * including session data, analytics, and display preferences.
 */
data class StatsUiState(
    // Core statistics
    val sessionStats: SessionStats = SessionStats(),
    
    // Recent sessions
    val recentSessions: List<SessionRecord> = emptyList(),
    
    // Analytics data
    val dailySessionCounts: Map<LocalDateTime, Int> = emptyMap(),
    val weeklyTrends: Map<String, Int> = emptyMap(),
    val hourlyProductivity: Map<Int, Int> = emptyMap(),
    val completionRateByDay: Map<String, Double> = emptyMap(),
    val bestPerformingDays: Map<LocalDateTime, Int> = emptyMap(),
    
    // UI state
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    
    // Display preferences
    val selectedTimeRange: TimeRange = TimeRange.LAST_30_DAYS,
    val selectedChartType: ChartType = ChartType.DAILY_SESSIONS,
    val showOnlyCompletedSessions: Boolean = false,
    val selectedMode: TimerMode? = null, // null means all modes
    
    // Goal tracking
    val dailyGoal: Int = 8, // Default daily goal
    val weeklyGoal: Int = 40, // Default weekly goal
    val monthlyGoal: Int = 160 // Default monthly goal
) {
    /**
     * Get today's progress towards daily goal
     */
    val dailyGoalProgress: Float
        get() = if (dailyGoal > 0) {
            (sessionStats.sessionsToday.toFloat() / dailyGoal.toFloat()).coerceAtMost(1f)
        } else 0f
    
    /**
     * Get this week's progress towards weekly goal
     */
    val weeklyGoalProgress: Float
        get() = if (weeklyGoal > 0) {
            (sessionStats.sessionsThisWeek.toFloat() / weeklyGoal.toFloat()).coerceAtMost(1f)
        } else 0f
    
    /**
     * Check if daily goal is met
     */
    val isDailyGoalMet: Boolean
        get() = sessionStats.sessionsToday >= dailyGoal
    
    /**
     * Check if weekly goal is met
     */
    val isWeeklyGoalMet: Boolean
        get() = sessionStats.sessionsThisWeek >= weeklyGoal
    
    /**
     * Get formatted daily goal progress
     */
    val formattedDailyGoalProgress: String
        get() = "${sessionStats.sessionsToday}/${dailyGoal}"
    
    /**
     * Get formatted weekly goal progress
     */
    val formattedWeeklyGoalProgress: String
        get() = "${sessionStats.sessionsThisWeek}/${weeklyGoal}"
    
    /**
     * Get most productive hour of the day
     */
    val mostProductiveHour: Int?
        get() = hourlyProductivity.maxByOrNull { it.value }?.key
    
    /**
     * Get formatted most productive hour
     */
    val formattedMostProductiveHour: String
        get() = mostProductiveHour?.let { hour ->
            when {
                hour == 0 -> "12:00 AM"
                hour < 12 -> "${hour}:00 AM"
                hour == 12 -> "12:00 PM"
                else -> "${hour - 12}:00 PM"
            }
        } ?: "N/A"
    
    /**
     * Get best day of the week for completion rate
     */
    val bestDayOfWeek: String?
        get() = completionRateByDay.maxByOrNull { it.value }?.key
    
    /**
     * Get average session length in minutes
     */
    val averageSessionLengthMinutes: Int
        get() = sessionStats.averageSessionLength.toInt()
    
    /**
     * Get total focus time today in formatted string
     */
    val formattedFocusTimeToday: String
        get() = formatMinutes(sessionStats.workMinutesToday)
    
    /**
     * Get total focus time this week in formatted string
     */
    val formattedFocusTimeThisWeek: String
        get() = formatMinutes(sessionStats.workMinutesThisWeek)
    
    /**
     * Get total focus time overall in formatted string
     */
    val formattedTotalFocusTime: String
        get() = formatMinutes(sessionStats.totalWorkMinutes)
    
    /**
     * Get current streak status message
     */
    val streakStatusMessage: String
        get() = when {
            sessionStats.currentStreak == 0 -> "Start your streak today!"
            sessionStats.currentStreak == 1 -> "Great start! Keep it going."
            sessionStats.currentStreak < 7 -> "You're building momentum!"
            sessionStats.currentStreak < 30 -> "Excellent consistency!"
            else -> "You're a focus master!"
        }
    
    /**
     * Get productivity level description
     */
    val productivityDescription: String
        get() = sessionStats.productivityLevel.description
    
    /**
     * Check if there's enough data to show analytics
     */
    val hasEnoughDataForAnalytics: Boolean
        get() = sessionStats.totalSessions >= 5
    
    /**
     * Get filtered recent sessions based on current filters
     */
    val filteredRecentSessions: List<SessionRecord>
        get() = recentSessions.filter { session ->
            val modeMatch = selectedMode?.let { it == session.mode } ?: true
            val completionMatch = if (showOnlyCompletedSessions) session.wasCompleted else true
            modeMatch && completionMatch
        }
    
    /**
     * Get chart data based on selected chart type
     */
    val chartData: List<ChartDataPoint>
        get() = when (selectedChartType) {
            ChartType.DAILY_SESSIONS -> dailySessionCounts.map { (date, count) ->
                ChartDataPoint(
                    label = formatDateLabel(date),
                    value = count.toFloat(),
                    date = date
                )
            }.sortedBy { it.date }
            
            ChartType.HOURLY_PRODUCTIVITY -> hourlyProductivity.map { (hour, count) ->
                ChartDataPoint(
                    label = formatHourLabel(hour),
                    value = count.toFloat(),
                    hour = hour
                )
            }.sortedBy { it.hour }
            
            ChartType.COMPLETION_RATE -> completionRateByDay.map { (day, rate) ->
                ChartDataPoint(
                    label = day,
                    value = (rate * 100).toFloat(), // Convert to percentage
                    dayOfWeek = day
                )
            }
        }
    
    /**
     * Format minutes into human-readable string
     */
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
    
    /**
     * Format date for chart labels
     */
    private fun formatDateLabel(date: LocalDateTime): String {
        return "${date.monthValue}/${date.dayOfMonth}"
    }
    
    /**
     * Format hour for chart labels
     */
    private fun formatHourLabel(hour: Int): String {
        return when {
            hour == 0 -> "12AM"
            hour < 12 -> "${hour}AM"
            hour == 12 -> "12PM"
            else -> "${hour - 12}PM"
        }
    }
}

/**
 * UI events that can be triggered from the Stats screen
 */
sealed class StatsUiEvent {
    object RefreshStats : StatsUiEvent()
    data class UpdateTimeRange(val timeRange: TimeRange) : StatsUiEvent()
    data class UpdateChartType(val chartType: ChartType) : StatsUiEvent()
    data class UpdateModeFilter(val mode: TimerMode?) : StatsUiEvent()
    data class UpdateShowOnlyCompleted(val showOnly: Boolean) : StatsUiEvent()
    data class UpdateDailyGoal(val goal: Int) : StatsUiEvent()
    data class UpdateWeeklyGoal(val goal: Int) : StatsUiEvent()
    data class UpdateMonthlyGoal(val goal: Int) : StatsUiEvent()
    object ClearError : StatsUiEvent()
    data class ViewSessionDetails(val session: SessionRecord) : StatsUiEvent()
    object ExportData : StatsUiEvent()
    object ResetStats : StatsUiEvent()
}

/**
 * Time range options for statistics
 */
enum class TimeRange(val displayName: String, val days: Int) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_90_DAYS("Last 90 Days", 90),
    ALL_TIME("All Time", Int.MAX_VALUE)
}

/**
 * Chart type options
 */
enum class ChartType(val displayName: String) {
    DAILY_SESSIONS("Daily Sessions"),
    HOURLY_PRODUCTIVITY("Hourly Productivity"),
    COMPLETION_RATE("Completion Rate by Day")
}

/**
 * Chart data point for visualization
 */
data class ChartDataPoint(
    val label: String,
    val value: Float,
    val date: LocalDateTime? = null,
    val hour: Int? = null,
    val dayOfWeek: String? = null
)

/**
 * Statistics summary for quick overview
 */
data class StatsSummary(
    val totalSessions: Int,
    val totalFocusTime: String,
    val currentStreak: Int,
    val completionRate: String,
    val averageSessionLength: String,
    val mostProductiveHour: String,
    val bestDayOfWeek: String?
)
