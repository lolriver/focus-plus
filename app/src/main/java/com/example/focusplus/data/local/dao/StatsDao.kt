// data/local/dao/StatsDao.kt
package com.example.focusplus.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Specialized DAO for complex statistics queries
 * Separates advanced analytics from basic CRUD operations
 */
@Dao
interface StatsDao {
    
    // ==================== PRODUCTIVITY ANALYTICS ====================
    
    /**
     * Get daily session counts for the last 30 days
     * @param thirtyDaysAgo Date 30 days ago in ISO format
     * @return Flow of daily session counts with dates
     */
    @Query("""
        SELECT 
            DATE(completed_at) as date,
            COUNT(*) as session_count,
            SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) as completed_count
        FROM session_records 
        WHERE completed_at >= :thirtyDaysAgo
        GROUP BY DATE(completed_at)
        ORDER BY date DESC
    """)
    fun getDailySessionCounts(thirtyDaysAgo: String): Flow<List<DailySessionCount>>
    
    /**
     * Get hourly productivity pattern (which hours are most productive)
     * @return Flow of productivity by hour of day
     */
    @Query("""
        SELECT 
            CAST(strftime('%H', completed_at) AS INTEGER) as hour,
            COUNT(*) as session_count,
            AVG(actual_duration_minutes) as avg_duration,
            SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) as completed_count
        FROM session_records 
        WHERE was_completed = 1
        GROUP BY CAST(strftime('%H', completed_at) AS INTEGER)
        ORDER BY hour
    """)
    fun getHourlyProductivityPattern(): Flow<List<HourlyProductivity>>
    
    /**
     * Get weekly session trends (last 12 weeks)
     * @param twelveWeeksAgo Date 12 weeks ago in ISO format
     * @return Flow of weekly session statistics
     */
    @Query("""
        SELECT 
            strftime('%Y-%W', completed_at) as week,
            COUNT(*) as session_count,
            SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) as completed_count,
            SUM(CASE WHEN was_completed = 1 AND mode = 'WORK' THEN actual_duration_minutes ELSE 0 END) as work_minutes
        FROM session_records 
        WHERE completed_at >= :twelveWeeksAgo
        GROUP BY strftime('%Y-%W', completed_at)
        ORDER BY week DESC
    """)
    fun getWeeklyTrends(twelveWeeksAgo: String): Flow<List<WeeklyStats>>
    
    /**
     * Get session mode distribution (Work vs Break sessions)
     * @return Flow of session counts by mode
     */
    @Query("""
        SELECT 
            mode,
            COUNT(*) as total_sessions,
            SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) as completed_sessions,
            AVG(actual_duration_minutes) as avg_duration,
            SUM(actual_duration_minutes) as total_minutes
        FROM session_records 
        GROUP BY mode
    """)
    fun getSessionModeDistribution(): Flow<List<ModeDistribution>>
    
    // ==================== STREAK CALCULATIONS ====================
    
    /**
     * Get consecutive days with completed sessions for streak calculation
     * @return Flow of dates with at least one completed session
     */
    @Query("""
        SELECT DISTINCT DATE(completed_at) as date
        FROM session_records 
        WHERE was_completed = 1
        ORDER BY date DESC
    """)
    fun getStreakDates(): Flow<List<String>>
    
    /**
     * Get best performing days (highest session counts)
     * @param limit Number of top days to return
     * @return Flow of best performing dates
     */
    @Query("""
        SELECT 
            DATE(completed_at) as date,
            COUNT(*) as session_count,
            SUM(actual_duration_minutes) as total_minutes
        FROM session_records 
        WHERE was_completed = 1
        GROUP BY DATE(completed_at)
        ORDER BY session_count DESC, total_minutes DESC
        LIMIT :limit
    """)
    fun getBestPerformingDays(limit: Int = 10): Flow<List<DailyPerformance>>
    
    // ==================== GOAL TRACKING ====================
    
    /**
     * Get progress towards daily goal (if user sets a daily session target)
     * @param todayStart Start of today in ISO format
     * @param todayEnd End of today in ISO format
     * @param dailyGoal Target number of sessions per day
     * @return Flow of goal progress
     */
    @Query("""
        SELECT 
            :dailyGoal as goal,
            COUNT(*) as current_sessions,
            SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) as completed_sessions,
            CAST(SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) AS FLOAT) / :dailyGoal as progress_percentage
        FROM session_records 
        WHERE completed_at >= :todayStart AND completed_at <= :todayEnd
    """)
    fun getDailyGoalProgress(todayStart: String, todayEnd: String, dailyGoal: Int): Flow<GoalProgress?>
    
    /**
     * Get monthly summary statistics
     * @param monthStart Start of month in ISO format
     * @param monthEnd End of month in ISO format
     * @return Flow of monthly statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as total_sessions,
            SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) as completed_sessions,
            SUM(CASE WHEN was_completed = 1 AND mode = 'WORK' THEN actual_duration_minutes ELSE 0 END) as work_minutes,
            AVG(CASE WHEN was_completed = 1 THEN actual_duration_minutes END) as avg_session_duration,
            COUNT(DISTINCT DATE(completed_at)) as active_days
        FROM session_records 
        WHERE completed_at >= :monthStart AND completed_at <= :monthEnd
    """)
    fun getMonthlySummary(monthStart: String, monthEnd: String): Flow<MonthlySummary?>
    
    // ==================== PERFORMANCE INSIGHTS ====================
    
    /**
     * Get completion rate by day of week
     * @return Flow of completion rates for each day of the week
     */
    @Query("""
        SELECT 
            CASE CAST(strftime('%w', completed_at) AS INTEGER)
                WHEN 0 THEN 'Sunday'
                WHEN 1 THEN 'Monday'
                WHEN 2 THEN 'Tuesday'
                WHEN 3 THEN 'Wednesday'
                WHEN 4 THEN 'Thursday'
                WHEN 5 THEN 'Friday'
                WHEN 6 THEN 'Saturday'
            END as day_of_week,
            COUNT(*) as total_sessions,
            SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) as completed_sessions,
            CAST(SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) as completion_rate
        FROM session_records 
        GROUP BY CAST(strftime('%w', completed_at) AS INTEGER)
        ORDER BY CAST(strftime('%w', completed_at) AS INTEGER)
    """)
    fun getCompletionRateByDayOfWeek(): Flow<List<DayOfWeekStats>>
}

// ==================== DATA CLASSES FOR QUERY RESULTS ====================

/**
 * Result class for daily session counts
 */
data class DailySessionCount(
    val date: String,
    val sessionCount: Int,
    val completedCount: Int
)

/**
 * Result class for hourly productivity patterns
 */
data class HourlyProductivity(
    val hour: Int,
    val sessionCount: Int,
    val avgDuration: Double,
    val completedCount: Int
)

/**
 * Result class for weekly statistics
 */
data class WeeklyStats(
    val week: String,
    val sessionCount: Int,
    val completedCount: Int,
    val workMinutes: Int
)

/**
 * Result class for session mode distribution
 */
data class ModeDistribution(
    val mode: String,
    val totalSessions: Int,
    val completedSessions: Int,
    val avgDuration: Double,
    val totalMinutes: Int
)

/**
 * Result class for daily performance
 */
data class DailyPerformance(
    val date: String,
    val sessionCount: Int,
    val totalMinutes: Int
)

/**
 * Result class for goal progress tracking
 */
data class GoalProgress(
    val goal: Int,
    val currentSessions: Int,
    val completedSessions: Int,
    val progressPercentage: Double
)

/**
 * Result class for monthly summary
 */
data class MonthlySummary(
    val totalSessions: Int,
    val completedSessions: Int,
    val workMinutes: Int,
    val avgSessionDuration: Double,
    val activeDays: Int
)

/**
 * Result class for day of week statistics
 */
data class DayOfWeekStats(
    val dayOfWeek: String,
    val totalSessions: Int,
    val completedSessions: Int,
    val completionRate: Double
)
