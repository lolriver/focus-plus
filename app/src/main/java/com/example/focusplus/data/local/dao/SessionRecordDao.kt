// data/local/dao/SessionRecordDao.kt
package com.example.focusplus.data.local.dao

import androidx.room.*
import com.example.focusplus.data.local.entity.SessionRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SessionRecord operations
 * Provides reactive queries using Flow for real-time UI updates
 */
@Dao
interface SessionRecordDao {
    
    // ==================== BASIC CRUD OPERATIONS ====================
    
    /**
     * Insert a new session record
     * @param session The session to insert
     * @return The ID of the inserted session
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionRecordEntity): Long
    
    /**
     * Insert multiple session records
     * @param sessions List of sessions to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionRecordEntity>)
    
    /**
     * Update an existing session record
     * @param session The session to update
     */
    @Update
    suspend fun updateSession(session: SessionRecordEntity)
    
    /**
     * Delete a session record
     * @param session The session to delete
     */
    @Delete
    suspend fun deleteSession(session: SessionRecordEntity)
    
    /**
     * Delete session by ID
     * @param sessionId The ID of the session to delete
     */
    @Query("DELETE FROM session_records WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
    
    /**
     * Delete all session records
     */
    @Query("DELETE FROM session_records")
    suspend fun deleteAllSessions()
    
    // ==================== QUERY OPERATIONS ====================
    
    /**
     * Get all session records ordered by completion date (newest first)
     * @return Flow of all sessions for reactive UI updates
     */
    @Query("SELECT * FROM session_records ORDER BY completed_at DESC")
    fun getAllSessions(): Flow<List<SessionRecordEntity>>
    
    /**
     * Get session by ID
     * @param sessionId The session ID
     * @return Flow of the session or null if not found
     */
    @Query("SELECT * FROM session_records WHERE id = :sessionId")
    fun getSessionById(sessionId: Long): Flow<SessionRecordEntity?>
    
    /**
     * Get sessions by mode (Work, Short Break, Long Break)
     * @param mode The timer mode
     * @return Flow of sessions for the specified mode
     */
    @Query("SELECT * FROM session_records WHERE mode = :mode ORDER BY completed_at DESC")
    fun getSessionsByMode(mode: String): Flow<List<SessionRecordEntity>>
    
    /**
     * Get completed sessions only
     * @return Flow of successfully completed sessions
     */
    @Query("SELECT * FROM session_records WHERE was_completed = 1 ORDER BY completed_at DESC")
    fun getCompletedSessions(): Flow<List<SessionRecordEntity>>
    
    /**
     * Get sessions for a specific date range
     * @param startDate Start date in ISO string format
     * @param endDate End date in ISO string format
     * @return Flow of sessions within the date range
     */
    @Query("""
        SELECT * FROM session_records 
        WHERE completed_at >= :startDate AND completed_at <= :endDate 
        ORDER BY completed_at DESC
    """)
    fun getSessionsInDateRange(startDate: String, endDate: String): Flow<List<SessionRecordEntity>>
    
    // ==================== STATISTICS QUERIES ====================
    
    /**
     * Get total number of sessions
     * @return Flow of total session count
     */
    @Query("SELECT COUNT(*) FROM session_records")
    fun getTotalSessionCount(): Flow<Int>
    
    /**
     * Get total number of completed sessions
     * @return Flow of completed session count
     */
    @Query("SELECT COUNT(*) FROM session_records WHERE was_completed = 1")
    fun getCompletedSessionCount(): Flow<Int>
    
    /**
     * Get sessions completed today
     * @param todayStart Start of today in ISO string format
     * @param todayEnd End of today in ISO string format
     * @return Flow of today's session count
     */
    @Query("""
        SELECT COUNT(*) FROM session_records 
        WHERE was_completed = 1 
        AND completed_at >= :todayStart 
        AND completed_at <= :todayEnd
    """)
    fun getSessionsCompletedToday(todayStart: String, todayEnd: String): Flow<Int>
    
    /**
     * Get sessions completed this week
     * @param weekStart Start of week in ISO string format
     * @param weekEnd End of week in ISO string format
     * @return Flow of this week's session count
     */
    @Query("""
        SELECT COUNT(*) FROM session_records 
        WHERE was_completed = 1 
        AND completed_at >= :weekStart 
        AND completed_at <= :weekEnd
    """)
    fun getSessionsCompletedThisWeek(weekStart: String, weekEnd: String): Flow<Int>
    
    /**
     * Get total work minutes for completed sessions
     * @return Flow of total work minutes
     */
    @Query("""
        SELECT COALESCE(SUM(actual_duration_minutes), 0) 
        FROM session_records 
        WHERE was_completed = 1 AND mode = 'WORK'
    """)
    fun getTotalWorkMinutes(): Flow<Int>
    
    /**
     * Get work minutes completed today
     * @param todayStart Start of today in ISO string format
     * @param todayEnd End of today in ISO string format
     * @return Flow of today's work minutes
     */
    @Query("""
        SELECT COALESCE(SUM(actual_duration_minutes), 0) 
        FROM session_records 
        WHERE was_completed = 1 
        AND mode = 'WORK'
        AND completed_at >= :todayStart 
        AND completed_at <= :todayEnd
    """)
    fun getWorkMinutesToday(todayStart: String, todayEnd: String): Flow<Int>
    
    /**
     * Get work minutes completed this week
     * @param weekStart Start of week in ISO string format
     * @param weekEnd End of week in ISO string format
     * @return Flow of this week's work minutes
     */
    @Query("""
        SELECT COALESCE(SUM(actual_duration_minutes), 0) 
        FROM session_records 
        WHERE was_completed = 1 
        AND mode = 'WORK'
        AND completed_at >= :weekStart 
        AND completed_at <= :weekEnd
    """)
    fun getWorkMinutesThisWeek(weekStart: String, weekEnd: String): Flow<Int>
    
    /**
     * Get average session duration for completed sessions
     * @return Flow of average duration in minutes
     */
    @Query("""
        SELECT COALESCE(AVG(actual_duration_minutes), 0.0) 
        FROM session_records 
        WHERE was_completed = 1
    """)
    fun getAverageSessionDuration(): Flow<Double>
    
    /**
     * Get completion rate (percentage of completed vs total sessions)
     * @return Flow of completion rate as decimal (0.0 to 1.0)
     */
    @Query("""
        SELECT CASE 
            WHEN COUNT(*) = 0 THEN 0.0 
            ELSE CAST(SUM(CASE WHEN was_completed = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) 
        END 
        FROM session_records
    """)
    fun getCompletionRate(): Flow<Double>
    
    /**
     * Get sessions grouped by date for streak calculation
     * @return Flow of distinct dates with completed sessions
     */
    @Query("""
        SELECT DISTINCT DATE(completed_at) as date 
        FROM session_records 
        WHERE was_completed = 1 
        ORDER BY date DESC
    """)
    fun getCompletedSessionDates(): Flow<List<String>>
    
    /**
     * Get recent sessions for dashboard (last 10)
     * @return Flow of recent sessions
     */
    @Query("""
        SELECT * FROM session_records 
        ORDER BY completed_at DESC 
        LIMIT 10
    """)
    fun getRecentSessions(): Flow<List<SessionRecordEntity>>
    
    /**
     * Get sessions by cycle number
     * @param cycleNumber The cycle number to filter by
     * @return Flow of sessions for the specified cycle
     */
    @Query("SELECT * FROM session_records WHERE cycle_number = :cycleNumber ORDER BY completed_at DESC")
    fun getSessionsByCycle(cycleNumber: Int): Flow<List<SessionRecordEntity>>
    
    /**
     * Get the highest cycle number achieved
     * @return Flow of the maximum cycle number
     */
    @Query("SELECT COALESCE(MAX(cycle_number), 0) FROM session_records")
    fun getMaxCycleNumber(): Flow<Int>
}
