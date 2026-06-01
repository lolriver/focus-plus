// data/repository/RepositoryTestUtils.kt
package com.example.focusplus.data.repository

import com.example.focusplus.domain.model.SessionRecord
import com.example.focusplus.domain.model.Settings
import com.example.focusplus.domain.model.TimerMode
import java.time.LocalDateTime

/**
 * Utility class for creating test data for repositories
 * 
 * This class provides factory methods for creating test instances
 * of domain models, useful for unit testing and development.
 */
object RepositoryTestUtils {
    
    // ==================== SESSION RECORD FACTORIES ====================
    
    /**
     * Create a sample work session
     */
    fun createSampleWorkSession(
        id: Long = 0,
        plannedDuration: Int = 25,
        actualDuration: Int = 25,
        completedAt: LocalDateTime = LocalDateTime.now(),
        wasCompleted: Boolean = true,
        cycleNumber: Int = 1,
        notes: String? = null
    ): SessionRecord {
        return SessionRecord(
            id = id,
            mode = TimerMode.WORK,
            plannedDurationMinutes = plannedDuration,
            actualDurationMinutes = actualDuration,
            completedAt = completedAt,
            wasCompleted = wasCompleted,
            cycleNumber = cycleNumber,
            notes = notes
        )
    }
    
    /**
     * Create a sample short break session
     */
    fun createSampleShortBreakSession(
        id: Long = 0,
        plannedDuration: Int = 5,
        actualDuration: Int = 5,
        completedAt: LocalDateTime = LocalDateTime.now(),
        wasCompleted: Boolean = true,
        cycleNumber: Int = 1
    ): SessionRecord {
        return SessionRecord(
            id = id,
            mode = TimerMode.SHORT_BREAK,
            plannedDurationMinutes = plannedDuration,
            actualDurationMinutes = actualDuration,
            completedAt = completedAt,
            wasCompleted = wasCompleted,
            cycleNumber = cycleNumber,
            notes = null
        )
    }
    
    /**
     * Create a sample long break session
     */
    fun createSampleLongBreakSession(
        id: Long = 0,
        plannedDuration: Int = 15,
        actualDuration: Int = 15,
        completedAt: LocalDateTime = LocalDateTime.now(),
        wasCompleted: Boolean = true,
        cycleNumber: Int = 1
    ): SessionRecord {
        return SessionRecord(
            id = id,
            mode = TimerMode.LONG_BREAK,
            plannedDurationMinutes = plannedDuration,
            actualDurationMinutes = actualDuration,
            completedAt = completedAt,
            wasCompleted = wasCompleted,
            cycleNumber = cycleNumber,
            notes = null
        )
    }
    
    /**
     * Create a list of sample sessions for testing
     */
    fun createSampleSessionList(count: Int = 10): List<SessionRecord> {
        val sessions = mutableListOf<SessionRecord>()
        val now = LocalDateTime.now()
        
        repeat(count) { index ->
            val sessionTime = now.minusHours(index.toLong())
            val mode = when (index % 3) {
                0 -> TimerMode.WORK
                1 -> TimerMode.SHORT_BREAK
                else -> TimerMode.LONG_BREAK
            }
            
            sessions.add(
                SessionRecord(
                    id = index.toLong() + 1,
                    mode = mode,
                    plannedDurationMinutes = mode.defaultDurationMinutes,
                    actualDurationMinutes = mode.defaultDurationMinutes,
                    completedAt = sessionTime,
                    wasCompleted = index % 4 != 0, // 75% completion rate
                    cycleNumber = (index / 4) + 1,
                    notes = if (index % 5 == 0) "Test session $index" else null
                )
            )
        }
        
        return sessions
    }
    
    /**
     * Create sessions for streak testing
     */
    fun createStreakTestSessions(streakDays: Int): List<SessionRecord> {
        val sessions = mutableListOf<SessionRecord>()
        val today = LocalDateTime.now()
        
        repeat(streakDays) { dayIndex ->
            val sessionDate = today.minusDays(dayIndex.toLong())
            
            // Add 2-3 sessions per day
            repeat((2..3).random()) { sessionIndex ->
                sessions.add(
                    createSampleWorkSession(
                        id = (dayIndex * 10 + sessionIndex).toLong(),
                        completedAt = sessionDate.plusHours(sessionIndex.toLong() * 2),
                        wasCompleted = true,
                        cycleNumber = sessionIndex + 1
                    )
                )
            }
        }
        
        return sessions
    }
    
    // ==================== SETTINGS FACTORIES ====================
    
    /**
     * Create sample settings with default values
     */
    fun createSampleSettings(): Settings {
        return Settings.DEFAULT
    }
    
    /**
     * Create custom settings for testing
     */
    fun createCustomSettings(
        workDuration: Int = 30,
        shortBreakDuration: Int = 10,
        longBreakDuration: Int = 20,
        sessionsUntilLongBreak: Int = 3,
        isDarkTheme: Boolean = true,
        isNotificationsEnabled: Boolean = true,
        isSoundEnabled: Boolean = false,
        isVibrationEnabled: Boolean = true,
        autoStartBreaks: Boolean = true,
        autoStartWork: Boolean = false
    ): Settings {
        return Settings(
            workDurationMinutes = workDuration,
            shortBreakDurationMinutes = shortBreakDuration,
            longBreakDurationMinutes = longBreakDuration,
            sessionsUntilLongBreak = sessionsUntilLongBreak,
            isDarkTheme = isDarkTheme,
            isNotificationsEnabled = isNotificationsEnabled,
            isSoundEnabled = isSoundEnabled,
            isVibrationEnabled = isVibrationEnabled,
            autoStartBreaks = autoStartBreaks,
            autoStartWork = autoStartWork
        )
    }
    
    /**
     * Create invalid settings for validation testing
     */
    fun createInvalidSettings(): Settings {
        return Settings(
            workDurationMinutes = 0, // Invalid: too low
            shortBreakDurationMinutes = 100, // Invalid: too high
            longBreakDurationMinutes = -5, // Invalid: negative
            sessionsUntilLongBreak = 15, // Invalid: too high
            isDarkTheme = false,
            isNotificationsEnabled = true,
            isSoundEnabled = true,
            isVibrationEnabled = true,
            autoStartBreaks = false,
            autoStartWork = false
        )
    }
    
    // ==================== DATE UTILITIES ====================
    
    /**
     * Get start of today
     */
    fun getStartOfToday(): LocalDateTime {
        return LocalDateTime.now().toLocalDate().atStartOfDay()
    }
    
    /**
     * Get end of today
     */
    fun getEndOfToday(): LocalDateTime {
        return LocalDateTime.now().toLocalDate().atTime(23, 59, 59)
    }
    
    /**
     * Get start of this week
     */
    fun getStartOfThisWeek(): LocalDateTime {
        val today = LocalDateTime.now()
        return today.minusDays(today.dayOfWeek.value - 1L).toLocalDate().atStartOfDay()
    }
    
    /**
     * Get end of this week
     */
    fun getEndOfThisWeek(): LocalDateTime {
        val startOfWeek = getStartOfThisWeek()
        return startOfWeek.plusDays(6).toLocalDate().atTime(23, 59, 59)
    }
    
    /**
     * Create date range for testing
     */
    fun createDateRange(daysBack: Int): Pair<LocalDateTime, LocalDateTime> {
        val end = LocalDateTime.now()
        val start = end.minusDays(daysBack.toLong())
        return start to end
    }
    
    // ==================== VALIDATION UTILITIES ====================
    
    /**
     * Validate session record for testing
     */
    fun isValidSessionRecord(session: SessionRecord): Boolean {
        return session.plannedDurationMinutes > 0 &&
                session.actualDurationMinutes >= 0 &&
                session.cycleNumber > 0
    }
    
    /**
     * Validate settings for testing
     */
    fun isValidSettings(settings: Settings): Boolean {
        return settings.isValid()
    }
    
    // ==================== MOCK DATA GENERATORS ====================
    
    /**
     * Generate realistic session data for UI testing
     */
    fun generateRealisticSessionData(days: Int = 30): List<SessionRecord> {
        val sessions = mutableListOf<SessionRecord>()
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        
        for (day in 0 until days) {
            val currentDate = startDate.plusDays(day.toLong())
            
            // Skip some days randomly to simulate real usage
            if ((0..100).random() < 20) continue // 20% chance to skip a day
            
            // Generate 1-8 sessions per active day
            val sessionsPerDay = (1..8).random()
            
            repeat(sessionsPerDay) { sessionIndex ->
                val sessionTime = currentDate.plusHours((8..20).random().toLong())
                    .plusMinutes((0..59).random().toLong())
                
                val mode = when {
                    sessionIndex % 4 == 3 -> TimerMode.LONG_BREAK
                    sessionIndex % 2 == 1 -> TimerMode.SHORT_BREAK
                    else -> TimerMode.WORK
                }
                
                val plannedDuration = mode.defaultDurationMinutes
                val actualDuration = if ((0..100).random() < 85) { // 85% completion rate
                    plannedDuration + (-2..2).random() // Small variation
                } else {
                    (plannedDuration * 0.3).toInt() // Interrupted session
                }
                
                sessions.add(
                    SessionRecord(
                        id = sessions.size.toLong() + 1,
                        mode = mode,
                        plannedDurationMinutes = plannedDuration,
                        actualDurationMinutes = actualDuration,
                        completedAt = sessionTime,
                        wasCompleted = actualDuration >= (plannedDuration * 0.8),
                        cycleNumber = (sessionIndex / 2) + 1,
                        notes = if ((0..100).random() < 10) "Session note" else null
                    )
                )
            }
        }
        
        return sessions.sortedBy { it.completedAt }
    }
}
