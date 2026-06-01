// data/local/TimerStateManager.kt
package com.example.focusplus.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.domain.model.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for persisting timer state across app restarts
 * 
 * Uses DataStore to save and restore timer state, ensuring
 * users don't lose their progress when the app is killed.
 */
@Singleton
class TimerStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_state")
    
    // DataStore keys
    private object Keys {
        val TIMER_STATE = stringPreferencesKey("timer_state")
        val CURRENT_MODE = stringPreferencesKey("current_mode")
        val TIME_REMAINING_SECONDS = longPreferencesKey("time_remaining_seconds")
        val TOTAL_TIME_SECONDS = longPreferencesKey("total_time_seconds")
        val CURRENT_CYCLE = intPreferencesKey("current_cycle")
        val SESSIONS_COMPLETED_IN_CYCLE = intPreferencesKey("sessions_completed_in_cycle")
        val SESSION_START_TIME = longPreferencesKey("session_start_time")
        val SESSION_PAUSE_TIME = longPreferencesKey("session_pause_time")
        val TOTAL_PAUSED_DURATION = longPreferencesKey("total_paused_duration")
        val SESSION_NOTES = stringPreferencesKey("session_notes")
        val IS_SERVICE_RUNNING = booleanPreferencesKey("is_service_running")
    }
    
    /**
     * Save timer state to persistent storage
     */
    suspend fun saveTimerState(state: PersistedTimerState) {
        context.timerDataStore.edit { preferences ->
            preferences[Keys.TIMER_STATE] = state.timerState.name
            preferences[Keys.CURRENT_MODE] = state.currentMode.name
            preferences[Keys.TIME_REMAINING_SECONDS] = state.timeRemainingSeconds
            preferences[Keys.TOTAL_TIME_SECONDS] = state.totalTimeSeconds
            preferences[Keys.CURRENT_CYCLE] = state.currentCycle
            preferences[Keys.SESSIONS_COMPLETED_IN_CYCLE] = state.sessionsCompletedInCycle
            
            state.sessionStartTime?.let { preferences[Keys.SESSION_START_TIME] = it }
            state.sessionPauseTime?.let { preferences[Keys.SESSION_PAUSE_TIME] = it }
            preferences[Keys.TOTAL_PAUSED_DURATION] = state.totalPausedDuration
            state.sessionNotes?.let { preferences[Keys.SESSION_NOTES] = it }
            preferences[Keys.IS_SERVICE_RUNNING] = state.isServiceRunning
        }
    }
    
    /**
     * Load timer state from persistent storage
     */
    fun getTimerState(): Flow<PersistedTimerState?> {
        return context.timerDataStore.data.map { preferences ->
            val timerStateString = preferences[Keys.TIMER_STATE]
            if (timerStateString == null) {
                null
            } else {
                try {
                    PersistedTimerState(
                        timerState = TimerState.valueOf(timerStateString),
                        currentMode = TimerMode.valueOf(preferences[Keys.CURRENT_MODE] ?: TimerMode.WORK.name),
                        timeRemainingSeconds = preferences[Keys.TIME_REMAINING_SECONDS] ?: 0L,
                        totalTimeSeconds = preferences[Keys.TOTAL_TIME_SECONDS] ?: 0L,
                        currentCycle = preferences[Keys.CURRENT_CYCLE] ?: 1,
                        sessionsCompletedInCycle = preferences[Keys.SESSIONS_COMPLETED_IN_CYCLE] ?: 0,
                        sessionStartTime = preferences[Keys.SESSION_START_TIME],
                        sessionPauseTime = preferences[Keys.SESSION_PAUSE_TIME],
                        totalPausedDuration = preferences[Keys.TOTAL_PAUSED_DURATION] ?: 0L,
                        sessionNotes = preferences[Keys.SESSION_NOTES],
                        isServiceRunning = preferences[Keys.IS_SERVICE_RUNNING] ?: false
                    )
                } catch (e: Exception) {
                    // If there's an error parsing the state, return null
                    null
                }
            }
        }
    }
    
    /**
     * Clear saved timer state
     */
    suspend fun clearTimerState() {
        context.timerDataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Update only the timer state (for quick updates)
     */
    suspend fun updateTimerState(timerState: TimerState) {
        context.timerDataStore.edit { preferences ->
            preferences[Keys.TIMER_STATE] = timerState.name
        }
    }
    
    /**
     * Update only the time remaining (for countdown updates)
     */
    suspend fun updateTimeRemaining(timeRemainingSeconds: Long) {
        context.timerDataStore.edit { preferences ->
            preferences[Keys.TIME_REMAINING_SECONDS] = timeRemainingSeconds
        }
    }
    
    /**
     * Update service running status
     */
    suspend fun updateServiceRunning(isRunning: Boolean) {
        context.timerDataStore.edit { preferences ->
            preferences[Keys.IS_SERVICE_RUNNING] = isRunning
        }
    }
    
    /**
     * Update pause time
     */
    suspend fun updatePauseTime(pauseTime: Long?) {
        context.timerDataStore.edit { preferences ->
            if (pauseTime != null) {
                preferences[Keys.SESSION_PAUSE_TIME] = pauseTime
            } else {
                preferences.remove(Keys.SESSION_PAUSE_TIME)
            }
        }
    }
    
    /**
     * Update total paused duration
     */
    suspend fun updateTotalPausedDuration(duration: Long) {
        context.timerDataStore.edit { preferences ->
            preferences[Keys.TOTAL_PAUSED_DURATION] = duration
        }
    }
    
    /**
     * Check if there's a saved timer state
     */
    fun hasSavedState(): Flow<Boolean> {
        return context.timerDataStore.data.map { preferences ->
            preferences[Keys.TIMER_STATE] != null
        }
    }
    
    /**
     * Get only the timer state without other details
     */
    fun getTimerStateOnly(): Flow<TimerState?> {
        return context.timerDataStore.data.map { preferences ->
            preferences[Keys.TIMER_STATE]?.let { stateString ->
                try {
                    TimerState.valueOf(stateString)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    /**
     * Check if service should be running based on saved state
     */
    fun shouldRestoreService(): Flow<Boolean> {
        return context.timerDataStore.data.map { preferences ->
            val isServiceRunning = preferences[Keys.IS_SERVICE_RUNNING] ?: false
            val timerState = preferences[Keys.TIMER_STATE]?.let { 
                try { TimerState.valueOf(it) } catch (e: Exception) { null }
            }
            
            isServiceRunning && (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED)
        }
    }
}

/**
 * Data class representing persisted timer state
 */
data class PersistedTimerState(
    val timerState: TimerState,
    val currentMode: TimerMode,
    val timeRemainingSeconds: Long,
    val totalTimeSeconds: Long,
    val currentCycle: Int,
    val sessionsCompletedInCycle: Int,
    val sessionStartTime: Long? = null,
    val sessionPauseTime: Long? = null,
    val totalPausedDuration: Long = 0L,
    val sessionNotes: String? = null,
    val isServiceRunning: Boolean = false
) {
    /**
     * Check if this state represents an active session
     */
    val isActiveSession: Boolean
        get() = timerState == TimerState.RUNNING || timerState == TimerState.PAUSED
    
    /**
     * Get progress percentage (0.0 to 1.0)
     */
    val progress: Float
        get() = if (totalTimeSeconds > 0) {
            1f - (timeRemainingSeconds.toFloat() / totalTimeSeconds.toFloat())
        } else 0f
    
    /**
     * Calculate actual elapsed time in seconds
     */
    val elapsedTimeSeconds: Long
        get() = totalTimeSeconds - timeRemainingSeconds
    
    /**
     * Check if session should be restored (not too old)
     */
    fun shouldRestore(maxAgeMillis: Long = 24 * 60 * 60 * 1000L): Boolean { // 24 hours default
        val currentTime = System.currentTimeMillis()
        val sessionTime = sessionStartTime ?: return false
        
        return (currentTime - sessionTime) <= maxAgeMillis && isActiveSession
    }
    
    /**
     * Calculate how much time has passed since pause (if paused)
     */
    fun getTimeSincePause(): Long? {
        return if (timerState == TimerState.PAUSED && sessionPauseTime != null) {
            System.currentTimeMillis() - sessionPauseTime
        } else null
    }
    
    /**
     * Get adjusted time remaining accounting for time passed while app was closed
     */
    fun getAdjustedTimeRemaining(): Long {
        return when (timerState) {
            TimerState.RUNNING -> {
                // If timer was running, subtract time that passed while app was closed
                sessionStartTime?.let { startTime ->
                    val currentTime = System.currentTimeMillis()
                    val totalElapsed = (currentTime - startTime) / 1000L // Convert to seconds
                    val adjustedElapsed = totalElapsed - totalPausedDuration
                    val adjustedRemaining = totalTimeSeconds - adjustedElapsed
                    adjustedRemaining.coerceAtLeast(0L)
                } ?: timeRemainingSeconds
            }
            TimerState.PAUSED -> {
                // If timer was paused, time remaining should be the same
                timeRemainingSeconds
            }
            else -> timeRemainingSeconds
        }
    }
}
