// presentation/stats/StatsViewModel.kt
package com.example.focusplus.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusplus.domain.model.SessionRecord
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the Statistics screen
 * 
 * Manages statistics data, analytics calculations, and display preferences.
 * Provides comprehensive session analytics and goal tracking.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()
    
    init {
        loadStatistics()
    }
    
    // ==================== INITIALIZATION ====================
    
    /**
     * Load all statistics data
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Combine all statistics flows
                combine(
                    sessionRepository.getSessionStats(),
                    sessionRepository.getRecentSessions(20), // Get last 20 sessions
                    sessionRepository.getDailySessionCounts(_uiState.value.selectedTimeRange.days),
                    sessionRepository.getWeeklyTrends(12), // Last 12 weeks
                    sessionRepository.getHourlyProductivityPattern(),
                    sessionRepository.getCompletionRateByDayOfWeek(),
                    sessionRepository.getBestPerformingDays(10)
                ) { sessionStats, recentSessions, dailyCounts, weeklyTrends, 
                    hourlyProductivity, completionByDay, bestDays ->
                    
                    _uiState.value.copy(
                        sessionStats = sessionStats,
                        recentSessions = recentSessions,
                        dailySessionCounts = dailyCounts,
                        weeklyTrends = weeklyTrends,
                        hourlyProductivity = hourlyProductivity,
                        completionRateByDay = completionByDay,
                        bestPerformingDays = bestDays,
                        isLoading = false,
                        isRefreshing = false
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load statistics: ${e.message}",
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }
    
    // ==================== EVENT HANDLING ====================
    
    /**
     * Handle UI events from the stats screen
     */
    fun onEvent(event: StatsUiEvent) {
        when (event) {
            is StatsUiEvent.RefreshStats -> refreshStats()
            is StatsUiEvent.UpdateTimeRange -> updateTimeRange(event.timeRange)
            is StatsUiEvent.UpdateChartType -> updateChartType(event.chartType)
            is StatsUiEvent.UpdateModeFilter -> updateModeFilter(event.mode)
            is StatsUiEvent.UpdateShowOnlyCompleted -> updateShowOnlyCompleted(event.showOnly)
            is StatsUiEvent.UpdateDailyGoal -> updateDailyGoal(event.goal)
            is StatsUiEvent.UpdateWeeklyGoal -> updateWeeklyGoal(event.goal)
            is StatsUiEvent.UpdateMonthlyGoal -> updateMonthlyGoal(event.goal)
            is StatsUiEvent.ClearError -> clearError()
            is StatsUiEvent.ViewSessionDetails -> viewSessionDetails(event.session)
            is StatsUiEvent.ExportData -> exportData()
            is StatsUiEvent.ResetStats -> resetStats()
        }
    }
    
    // ==================== DISPLAY PREFERENCES ====================
    
    /**
     * Update time range for statistics
     */
    private fun updateTimeRange(timeRange: TimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = timeRange)
        
        // Reload daily session counts for new time range
        viewModelScope.launch {
            try {
                sessionRepository.getDailySessionCounts(timeRange.days).collect { dailyCounts ->
                    _uiState.value = _uiState.value.copy(dailySessionCounts = dailyCounts)
                }
            } catch (e: Exception) {
                showError("Failed to update time range: ${e.message}")
            }
        }
    }
    
    /**
     * Update chart type
     */
    private fun updateChartType(chartType: ChartType) {
        _uiState.value = _uiState.value.copy(selectedChartType = chartType)
    }
    
    /**
     * Update mode filter
     */
    private fun updateModeFilter(mode: TimerMode?) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }
    
    /**
     * Update show only completed sessions filter
     */
    private fun updateShowOnlyCompleted(showOnly: Boolean) {
        _uiState.value = _uiState.value.copy(showOnlyCompletedSessions = showOnly)
    }
    
    // ==================== GOAL MANAGEMENT ====================
    
    /**
     * Update daily goal
     */
    private fun updateDailyGoal(goal: Int) {
        if (goal in 1..50) { // Reasonable range
            _uiState.value = _uiState.value.copy(dailyGoal = goal)
            // TODO: Persist goal to preferences
        }
    }
    
    /**
     * Update weekly goal
     */
    private fun updateWeeklyGoal(goal: Int) {
        if (goal in 1..200) { // Reasonable range
            _uiState.value = _uiState.value.copy(weeklyGoal = goal)
            // TODO: Persist goal to preferences
        }
    }
    
    /**
     * Update monthly goal
     */
    private fun updateMonthlyGoal(goal: Int) {
        if (goal in 1..1000) { // Reasonable range
            _uiState.value = _uiState.value.copy(monthlyGoal = goal)
            // TODO: Persist goal to preferences
        }
    }
    
    // ==================== DATA OPERATIONS ====================
    
    /**
     * Refresh all statistics
     */
    private fun refreshStats() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadStatistics()
    }
    
    /**
     * View session details
     */
    private fun viewSessionDetails(session: SessionRecord) {
        // TODO: Navigate to session details screen or show dialog
        // This would typically trigger navigation or show a detailed view
    }
    
    /**
     * Export statistics data
     */
    private fun exportData() {
        viewModelScope.launch {
            try {
                // TODO: Implement data export functionality
                // This could export to CSV, JSON, or other formats
                val exportData = generateExportData()
                // Handle export (save to file, share, etc.)
                
            } catch (e: Exception) {
                showError("Failed to export data: ${e.message}")
            }
        }
    }
    
    /**
     * Reset all statistics (with confirmation)
     */
    private fun resetStats() {
        viewModelScope.launch {
            try {
                // TODO: Implement with confirmation dialog
                // sessionRepository.deleteAllSessions()
                // loadStatistics()
                
            } catch (e: Exception) {
                showError("Failed to reset statistics: ${e.message}")
            }
        }
    }
    
    // ==================== ANALYTICS CALCULATIONS ====================
    
    /**
     * Calculate productivity insights
     */
    fun getProductivityInsights(): List<ProductivityInsight> {
        val currentState = _uiState.value
        val insights = mutableListOf<ProductivityInsight>()
        
        // Streak insights
        when {
            currentState.sessionStats.currentStreak == 0 -> {
                insights.add(
                    ProductivityInsight(
                        title = "Start Your Journey",
                        description = "Complete your first session today to begin building a streak!",
                        type = InsightType.MOTIVATION
                    )
                )
            }
            currentState.sessionStats.currentStreak < currentState.sessionStats.longestStreak -> {
                insights.add(
                    ProductivityInsight(
                        title = "Beat Your Record",
                        description = "You're ${currentState.sessionStats.longestStreak - currentState.sessionStats.currentStreak} days away from your longest streak!",
                        type = InsightType.GOAL
                    )
                )
            }
            currentState.sessionStats.currentStreak == currentState.sessionStats.longestStreak && currentState.sessionStats.currentStreak > 0 -> {
                insights.add(
                    ProductivityInsight(
                        title = "New Record!",
                        description = "You've reached your longest streak of ${currentState.sessionStats.currentStreak} days!",
                        type = InsightType.ACHIEVEMENT
                    )
                )
            }
        }
        
        // Productivity time insights
        currentState.mostProductiveHour?.let { hour ->
            insights.add(
                ProductivityInsight(
                    title = "Peak Performance",
                    description = "You're most productive at ${currentState.formattedMostProductiveHour}. Schedule important work during this time!",
                    type = InsightType.TIP
                )
            )
        }
        
        // Completion rate insights
        val completionRate = currentState.sessionStats.completionRate
        when {
            completionRate < 0.5f -> {
                insights.add(
                    ProductivityInsight(
                        title = "Focus on Completion",
                        description = "Try shorter sessions to improve your ${(completionRate * 100).toInt()}% completion rate.",
                        type = InsightType.TIP
                    )
                )
            }
            completionRate > 0.9f -> {
                insights.add(
                    ProductivityInsight(
                        title = "Excellent Focus",
                        description = "Your ${(completionRate * 100).toInt()}% completion rate shows great discipline!",
                        type = InsightType.ACHIEVEMENT
                    )
                )
            }
        }
        
        // Goal progress insights
        if (currentState.isDailyGoalMet) {
            insights.add(
                ProductivityInsight(
                    title = "Daily Goal Achieved",
                    description = "Great job completing ${currentState.sessionStats.sessionsToday} sessions today!",
                    type = InsightType.ACHIEVEMENT
                )
            )
        } else if (currentState.dailyGoalProgress > 0.5f) {
            val remaining = currentState.dailyGoal - currentState.sessionStats.sessionsToday
            insights.add(
                ProductivityInsight(
                    title = "Almost There",
                    description = "Just $remaining more sessions to reach your daily goal!",
                    type = InsightType.MOTIVATION
                )
            )
        }
        
        return insights
    }
    
    /**
     * Generate export data
     */
    private fun generateExportData(): StatsExportData {
        val currentState = _uiState.value
        
        return StatsExportData(
            exportDate = LocalDateTime.now(),
            sessionStats = currentState.sessionStats,
            recentSessions = currentState.recentSessions,
            dailySessionCounts = currentState.dailySessionCounts,
            weeklyTrends = currentState.weeklyTrends,
            hourlyProductivity = currentState.hourlyProductivity,
            completionRateByDay = currentState.completionRateByDay,
            goals = GoalData(
                dailyGoal = currentState.dailyGoal,
                weeklyGoal = currentState.weeklyGoal,
                monthlyGoal = currentState.monthlyGoal
            )
        )
    }
    
    // ==================== UI STATE MANAGEMENT ====================
    
    /**
     * Clear error message
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Show error message
     */
    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }
    
    // ==================== PUBLIC UTILITY METHODS ====================
    
    /**
     * Get statistics summary for quick overview
     */
    fun getStatsSummary(): StatsSummary {
        val currentState = _uiState.value
        
        return StatsSummary(
            totalSessions = currentState.sessionStats.totalSessions,
            totalFocusTime = currentState.formattedTotalFocusTime,
            currentStreak = currentState.sessionStats.currentStreak,
            completionRate = currentState.sessionStats.formattedCompletionRate,
            averageSessionLength = "${currentState.averageSessionLengthMinutes}m",
            mostProductiveHour = currentState.formattedMostProductiveHour,
            bestDayOfWeek = currentState.bestDayOfWeek
        )
    }
    
    /**
     * Check if user has achieved any milestones
     */
    fun checkMilestones(): List<Milestone> {
        val currentState = _uiState.value
        val milestones = mutableListOf<Milestone>()
        
        // Session count milestones
        val totalSessions = currentState.sessionStats.totalSessions
        val sessionMilestones = listOf(1, 5, 10, 25, 50, 100, 250, 500, 1000)
        sessionMilestones.forEach { milestone ->
            if (totalSessions >= milestone) {
                milestones.add(
                    Milestone(
                        title = "$milestone Sessions",
                        description = "Completed $milestone focus sessions",
                        isAchieved = true,
                        category = MilestoneCategory.SESSIONS
                    )
                )
            }
        }
        
        // Streak milestones
        val currentStreak = currentState.sessionStats.currentStreak
        val streakMilestones = listOf(3, 7, 14, 30, 60, 100)
        streakMilestones.forEach { milestone ->
            if (currentStreak >= milestone) {
                milestones.add(
                    Milestone(
                        title = "$milestone Day Streak",
                        description = "Maintained focus for $milestone consecutive days",
                        isAchieved = true,
                        category = MilestoneCategory.STREAK
                    )
                )
            }
        }
        
        // Focus time milestones (in hours)
        val totalHours = currentState.sessionStats.totalWorkMinutes / 60
        val timeMilestones = listOf(1, 5, 10, 25, 50, 100, 250, 500)
        timeMilestones.forEach { milestone ->
            if (totalHours >= milestone) {
                milestones.add(
                    Milestone(
                        title = "$milestone Hours Focused",
                        description = "Accumulated $milestone hours of focused work",
                        isAchieved = true,
                        category = MilestoneCategory.TIME
                    )
                )
            }
        }
        
        return milestones.sortedByDescending { it.isAchieved }
    }
    
    /**
     * Force refresh all data
     */
    fun forceRefresh() {
        loadStatistics()
    }
}

// ==================== DATA CLASSES ====================

/**
 * Productivity insight for user guidance
 */
data class ProductivityInsight(
    val title: String,
    val description: String,
    val type: InsightType
)

/**
 * Types of productivity insights
 */
enum class InsightType {
    MOTIVATION,
    TIP,
    ACHIEVEMENT,
    GOAL
}

/**
 * Milestone achievement
 */
data class Milestone(
    val title: String,
    val description: String,
    val isAchieved: Boolean,
    val category: MilestoneCategory
)

/**
 * Milestone categories
 */
enum class MilestoneCategory {
    SESSIONS,
    STREAK,
    TIME
}

/**
 * Export data structure
 */
data class StatsExportData(
    val exportDate: LocalDateTime,
    val sessionStats: com.example.focusplus.domain.model.SessionStats,
    val recentSessions: List<SessionRecord>,
    val dailySessionCounts: Map<LocalDateTime, Int>,
    val weeklyTrends: Map<String, Int>,
    val hourlyProductivity: Map<Int, Int>,
    val completionRateByDay: Map<String, Double>,
    val goals: GoalData
)

/**
 * Goal data for export
 */
data class GoalData(
    val dailyGoal: Int,
    val weeklyGoal: Int,
    val monthlyGoal: Int
)
