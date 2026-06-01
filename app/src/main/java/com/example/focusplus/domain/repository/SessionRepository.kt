// domain/repository/SessionRepository.kt
package com.example.focusplus.domain.repository

import com.example.focusplus.domain.model.SessionRecord
import com.example.focusplus.domain.model.SessionStats
import com.example.focusplus.domain.model.TimerMode
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository interface for session-related operations
 * 
 * This interface defines the contract for session data operations
 * following Clean Architecture principles. It's part of the domain layer
 * and has no dependencies on Android or Room.
 */
interface SessionRepository {
    
    // ==================== BASIC CRUD OPERATIONS ====================
    
    /**
     * Insert a new session record
     * @param session The session to insert
     * @return The ID of the inserted session
     */
    suspend fun insertSession(session: SessionRecord): Long
    
    /**
     * Update an existing session record
     * @param session The session to update
     */
    suspend fun updateSession(session: SessionRecord)
    
    /**
     * Delete a session record
     * @param session The session to delete
     */
    suspend fun deleteSession(session: SessionRecord)
    
    /**
     * Delete session by ID
     * @param sessionId The ID of the session to delete
     */
    suspend fun deleteSessionById(sessionId: Long)
    
    /**
     * Delete all session records
     */
    suspend fun deleteAllSessions()
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Get all session records
     * @return Flow of all sessions ordered by completion date (newest first)
     */
    fun getAllSessions(): Flow<List<SessionRecord>>
    
    /**
     * Get session by ID
     * @param sessionId The session ID
     * @return Flow of the session or null if not found
     */
    fun getSessionById(sessionId: Long): Flow<SessionRecord?>
    
    /**
     * Get sessions by mode
     * @param mode The timer mode to filter by
     * @return Flow of sessions for the specified mode
     */
    fun getSessionsByMode(mode: TimerMode): Flow<List<SessionRecord>>
    
    /**
     * Get completed sessions only
     * @return Flow of successfully completed sessions
     */
    fun getCompletedSessions(): Flow<List<SessionRecord>>
    
    /**
     * Get sessions for a specific date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Flow of sessions within the date range
     */
    fun getSessionsInDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<SessionRecord>>
    
    /**
     * Get recent sessions (last N sessions)
     * @param limit Number of recent sessions to retrieve
     * @return Flow of recent sessions
     */
    fun getRecentSessions(limit: Int = 10): Flow<List<SessionRecord>>
    
    // ==================== STATISTICS OPERATIONS ====================
    
    /**
     * Get comprehensive session statistics
     * @return Flow of aggregated session statistics
     */
    fun getSessionStats(): Flow<SessionStats>
    
    /**
     * Get total number of sessions
     * @return Flow of total session count
     */
    fun getTotalSessionCount(): Flow<Int>
    
    /**
     * Get total number of completed sessions
     * @return Flow of completed session count
     */
    fun getCompletedSessionCount(): Flow<Int>
    
    /**
     * Get sessions completed today
     * @return Flow of today's completed session count
     */
    fun getSessionsCompletedToday(): Flow<Int>
    
    /**
     * Get sessions completed this week
     * @return Flow of this week's completed session count
     */
    fun getSessionsCompletedThisWeek(): Flow<Int>
    
    /**
     * Get total work minutes for completed sessions
     * @return Flow of total work minutes
     */
    fun getTotalWorkMinutes(): Flow<Int>
    
    /**
     * Get work minutes completed today
     * @return Flow of today's work minutes
     */
    fun getWorkMinutesToday(): Flow<Int>
    
    /**
     * Get work minutes completed this week
     * @return Flow of this week's work minutes
     */
    fun getWorkMinutesThisWeek(): Flow<Int>
    
    /**
     * Get average session duration for completed sessions
     * @return Flow of average duration in minutes
     */
    fun getAverageSessionDuration(): Flow<Double>
    
    /**
     * Get completion rate (percentage of completed vs total sessions)
     * @return Flow of completion rate as decimal (0.0 to 1.0)
     */
    fun getCompletionRate(): Flow<Double>
    
    /**
     * Get current streak (consecutive days with completed sessions)
     * @return Flow of current streak count in days
     */
    fun getCurrentStreak(): Flow<Int>
    
    /**
     * Get longest streak achieved
     * @return Flow of longest streak count in days
     */
    fun getLongestStreak(): Flow<Int>
    
    // ==================== ADVANCED ANALYTICS ====================
    
    /**
     * Get daily session counts for the last N days
     * @param days Number of days to look back (default 30)
     * @return Flow of daily session counts
     */
    fun getDailySessionCounts(days: Int = 30): Flow<Map<LocalDateTime, Int>>
    
    /**
     * Get weekly session trends
     * @param weeks Number of weeks to look back (default 12)
     * @return Flow of weekly session statistics
     */
    fun getWeeklyTrends(weeks: Int = 12): Flow<Map<String, Int>>
    
    /**
     * Get productivity pattern by hour of day
     * @return Flow of session counts by hour (0-23)
     */
    fun getHourlyProductivityPattern(): Flow<Map<Int, Int>>
    
    /**
     * Get completion rate by day of week
     * @return Flow of completion rates for each day of the week
     */
    fun getCompletionRateByDayOfWeek(): Flow<Map<String, Double>>
    
    /**
     * Get sessions by cycle number
     * @param cycleNumber The cycle number to filter by
     * @return Flow of sessions for the specified cycle
     */
    fun getSessionsByCycle(cycleNumber: Int): Flow<List<SessionRecord>>
    
    /**
     * Get the highest cycle number achieved
     * @return Flow of the maximum cycle number
     */
    fun getMaxCycleNumber(): Flow<Int>
    
    // ==================== GOAL TRACKING ====================
    
    /**
     * Check if daily goal is met
     * @param dailyGoal Target number of sessions per day
     * @return Flow indicating if today's goal is met
     */
    fun isDailyGoalMet(dailyGoal: Int): Flow<Boolean>
    
    /**
     * Get progress towards daily goal
     * @param dailyGoal Target number of sessions per day
     * @return Flow of goal progress (0.0 to 1.0+)
     */
    fun getDailyGoalProgress(dailyGoal: Int): Flow<Double>
    
    /**
     * Get best performing days (highest session counts)
     * @param limit Number of top days to return
     * @return Flow of best performing dates with session counts
     */
    fun getBestPerformingDays(limit: Int = 10): Flow<Map<LocalDateTime, Int>>
}
