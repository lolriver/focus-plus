// data/repository/SessionRepositoryImpl.kt
package com.example.focusplus.data.repository

import com.example.focusplus.data.local.dao.SessionRecordDao
import com.example.focusplus.data.local.dao.StatsDao
import com.example.focusplus.data.local.entity.SessionRecordEntity
import com.example.focusplus.domain.model.SessionRecord
import com.example.focusplus.domain.model.SessionStats
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SessionRepository interface
 * 
 * This class bridges the domain layer with the data layer,
 * converting between domain models and data entities.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionRecordDao,
    private val statsDao: StatsDao
) : SessionRepository {
    
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    // ==================== BASIC CRUD OPERATIONS ====================
    
    override suspend fun insertSession(session: SessionRecord): Long {
        val entity = SessionRecordEntity.fromDomainModel(session)
        return sessionDao.insertSession(entity)
    }
    
    override suspend fun updateSession(session: SessionRecord) {
        val entity = SessionRecordEntity.fromDomainModel(session)
        sessionDao.updateSession(entity)
    }
    
    override suspend fun deleteSession(session: SessionRecord) {
        val entity = SessionRecordEntity.fromDomainModel(session)
        sessionDao.deleteSession(entity)
    }
    
    override suspend fun deleteSessionById(sessionId: Long) {
        sessionDao.deleteSessionById(sessionId)
    }
    
    override suspend fun deleteAllSessions() {
        sessionDao.deleteAllSessions()
    }
    
    // ==================== QUERY OPERATIONS ====================
    
    override fun getAllSessions(): Flow<List<SessionRecord>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getSessionById(sessionId: Long): Flow<SessionRecord?> {
        return sessionDao.getSessionById(sessionId).map { entity ->
            entity?.toDomainModel()
        }
    }
    
    override fun getSessionsByMode(mode: TimerMode): Flow<List<SessionRecord>> {
        return sessionDao.getSessionsByMode(mode.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getCompletedSessions(): Flow<List<SessionRecord>> {
        return sessionDao.getCompletedSessions().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getSessionsInDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<SessionRecord>> {
        val startDateString = startDate.format(dateTimeFormatter)
        val endDateString = endDate.format(dateTimeFormatter)
        
        return sessionDao.getSessionsInDateRange(startDateString, endDateString).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getRecentSessions(limit: Int): Flow<List<SessionRecord>> {
        return sessionDao.getRecentSessions().map { entities ->
            entities.take(limit).map { it.toDomainModel() }
        }
    }
    
    // ==================== STATISTICS OPERATIONS ====================
    
    override fun getSessionStats(): Flow<SessionStats> {
        return combine(
            getSessionsCompletedToday(),
            getSessionsCompletedThisWeek(),
            getTotalSessionCount(),
            getWorkMinutesToday(),
            getWorkMinutesThisWeek(),
            getTotalWorkMinutes(),
            getCurrentStreak(),
            getLongestStreak(),
            getCompletionRate(),
            getAverageSessionDuration()
        ) { sessionsToday, sessionsThisWeek, totalSessions, workToday, workThisWeek, 
            totalWork, currentStreak, longestStreak, completionRate, avgDuration ->
            
            SessionStats(
                sessionsToday = sessionsToday,
                sessionsThisWeek = sessionsThisWeek,
                totalSessions = totalSessions,
                workMinutesToday = workToday,
                workMinutesThisWeek = workThisWeek,
                totalWorkMinutes = totalWork,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                completionRate = completionRate.toFloat(),
                averageSessionLength = avgDuration.toFloat()
            )
        }
    }
    
    override fun getTotalSessionCount(): Flow<Int> {
        return sessionDao.getTotalSessionCount()
    }
    
    override fun getCompletedSessionCount(): Flow<Int> {
        return sessionDao.getCompletedSessionCount()
    }
    
    override fun getSessionsCompletedToday(): Flow<Int> {
        val today = LocalDateTime.now()
        val todayStart = today.toLocalDate().atStartOfDay().format(dateTimeFormatter)
        val todayEnd = today.toLocalDate().atTime(23, 59, 59).format(dateTimeFormatter)
        
        return sessionDao.getSessionsCompletedToday(todayStart, todayEnd)
    }
    
    override fun getSessionsCompletedThisWeek(): Flow<Int> {
        val today = LocalDateTime.now()
        val weekStart = today.minusDays(today.dayOfWeek.value - 1L).toLocalDate().atStartOfDay()
        val weekEnd = weekStart.plusDays(6).toLocalDate().atTime(23, 59, 59)
        
        return sessionDao.getSessionsCompletedThisWeek(
            weekStart.format(dateTimeFormatter),
            weekEnd.format(dateTimeFormatter)
        )
    }
    
    override fun getTotalWorkMinutes(): Flow<Int> {
        return sessionDao.getTotalWorkMinutes()
    }
    
    override fun getWorkMinutesToday(): Flow<Int> {
        val today = LocalDateTime.now()
        val todayStart = today.toLocalDate().atStartOfDay().format(dateTimeFormatter)
        val todayEnd = today.toLocalDate().atTime(23, 59, 59).format(dateTimeFormatter)
        
        return sessionDao.getWorkMinutesToday(todayStart, todayEnd)
    }
    
    override fun getWorkMinutesThisWeek(): Flow<Int> {
        val today = LocalDateTime.now()
        val weekStart = today.minusDays(today.dayOfWeek.value - 1L).toLocalDate().atStartOfDay()
        val weekEnd = weekStart.plusDays(6).toLocalDate().atTime(23, 59, 59)
        
        return sessionDao.getWorkMinutesThisWeek(
            weekStart.format(dateTimeFormatter),
            weekEnd.format(dateTimeFormatter)
        )
    }
    
    override fun getAverageSessionDuration(): Flow<Double> {
        return sessionDao.getAverageSessionDuration()
    }
    
    override fun getCompletionRate(): Flow<Double> {
        return sessionDao.getCompletionRate()
    }
    
    override fun getCurrentStreak(): Flow<Int> {
        return sessionDao.getCompletedSessionDates().map { dates ->
            calculateCurrentStreak(dates)
        }
    }
    
    override fun getLongestStreak(): Flow<Int> {
        return sessionDao.getCompletedSessionDates().map { dates ->
            calculateLongestStreak(dates)
        }
    }
    
    // ==================== ADVANCED ANALYTICS ====================
    
    override fun getDailySessionCounts(days: Int): Flow<Map<LocalDateTime, Int>> {
        val startDate = LocalDateTime.now().minusDays(days.toLong()).format(dateTimeFormatter)
        
        return statsDao.getDailySessionCounts(startDate).map { dailyCounts ->
            dailyCounts.associate { 
                LocalDateTime.parse(it.date + "T00:00:00") to it.sessionCount 
            }
        }
    }
    
    override fun getWeeklyTrends(weeks: Int): Flow<Map<String, Int>> {
        val startDate = LocalDateTime.now().minusWeeks(weeks.toLong()).format(dateTimeFormatter)
        
        return statsDao.getWeeklyTrends(startDate).map { weeklyStats ->
            weeklyStats.associate { it.week to it.sessionCount }
        }
    }
    
    override fun getHourlyProductivityPattern(): Flow<Map<Int, Int>> {
        return statsDao.getHourlyProductivityPattern().map { hourlyStats ->
            hourlyStats.associate { it.hour to it.sessionCount }
        }
    }
    
    override fun getCompletionRateByDayOfWeek(): Flow<Map<String, Double>> {
        return statsDao.getCompletionRateByDayOfWeek().map { dayStats ->
            dayStats.associate { it.dayOfWeek to it.completionRate }
        }
    }
    
    override fun getSessionsByCycle(cycleNumber: Int): Flow<List<SessionRecord>> {
        return sessionDao.getSessionsByCycle(cycleNumber).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getMaxCycleNumber(): Flow<Int> {
        return sessionDao.getMaxCycleNumber()
    }
    
    // ==================== GOAL TRACKING ====================
    
    override fun isDailyGoalMet(dailyGoal: Int): Flow<Boolean> {
        return getSessionsCompletedToday().map { sessionsToday ->
            sessionsToday >= dailyGoal
        }
    }
    
    override fun getDailyGoalProgress(dailyGoal: Int): Flow<Double> {
        return getSessionsCompletedToday().map { sessionsToday ->
            if (dailyGoal > 0) sessionsToday.toDouble() / dailyGoal.toDouble() else 0.0
        }
    }
    
    override fun getBestPerformingDays(limit: Int): Flow<Map<LocalDateTime, Int>> {
        return statsDao.getBestPerformingDays(limit).map { bestDays ->
            bestDays.associate { 
                LocalDateTime.parse(it.date + "T00:00:00") to it.sessionCount 
            }
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Calculate current streak from list of dates
     */
    private fun calculateCurrentStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        
        val sortedDates = dates.map { LocalDateTime.parse("${it}T00:00:00").toLocalDate() }
            .sorted()
            .reversed() // Most recent first
        
        var streak = 0
        var currentDate = LocalDateTime.now().toLocalDate()
        
        for (date in sortedDates) {
            val daysDiff = ChronoUnit.DAYS.between(date, currentDate)
            
            when {
                daysDiff == 0L || daysDiff == 1L -> {
                    streak++
                    currentDate = date
                }
                daysDiff > 1L -> break // Gap in streak
            }
        }
        
        return streak
    }
    
    /**
     * Calculate longest streak from list of dates
     */
    private fun calculateLongestStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        
        val sortedDates = dates.map { LocalDateTime.parse("${it}T00:00:00").toLocalDate() }
            .sorted()
        
        var longestStreak = 1
        var currentStreak = 1
        
        for (i in 1 until sortedDates.size) {
            val daysDiff = ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i])
            
            if (daysDiff == 1L) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return longestStreak
    }
}
