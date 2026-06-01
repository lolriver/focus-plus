// presentation/timer/TimerViewModel.kt
package com.example.focusplus.presentation.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusplus.data.local.PersistedTimerState
import com.example.focusplus.data.local.TimerStateManager
import com.example.focusplus.domain.model.SessionRecord
import com.example.focusplus.domain.model.Settings
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.domain.model.TimerState
import com.example.focusplus.domain.repository.SessionRepository
import com.example.focusplus.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for the Timer screen
 * 
 * Manages timer state, session logic, and integration with repositories.
 * Handles timer countdown, session completion, and state persistence.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val timerStateManager: TimerStateManager
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()
    
    // Timer job for countdown
    private var timerJob: Job? = null
    
    // Session tracking
    private var sessionStartTime: LocalDateTime? = null
    private var sessionPauseTime: LocalDateTime? = null
    private var totalPausedDuration: Long = 0L // in seconds
    private var sessionNotes: String? = null
    
    init {
        initializeTimer()
        restoreTimerState()
    }
    
    // ==================== INITIALIZATION ====================
    
    /**
     * Initialize timer with settings and statistics
     */
    private fun initializeTimer() {
        viewModelScope.launch {
            try {
                // Combine settings and statistics
                combine(
                    settingsRepository.getSettings(),
                    sessionRepository.getSessionsCompletedToday(),
                    sessionRepository.getWorkMinutesToday(),
                    sessionRepository.getCurrentStreak()
                ) { settings, todaySessions, todayMinutes, streak ->
                    _uiState.value.copy(
                        settings = settings,
                        todaySessionCount = todaySessions,
                        todayWorkMinutes = todayMinutes,
                        currentStreak = streak,
                        totalTimeSeconds = (settings.getDurationForMode(_uiState.value.currentMode) * 60).toLong(),
                        timeRemainingSeconds = (settings.getDurationForMode(_uiState.value.currentMode) * 60).toLong(),
                        sessionsUntilLongBreak = settings.sessionsUntilLongBreak,
                        isLoading = false
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load timer settings: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    // ==================== TIMER CONTROL ====================
    
    /**
     * Handle UI events from the timer screen
     */
    fun onEvent(event: TimerUiEvent) {
        when (event) {
            is TimerUiEvent.StartTimer -> startTimer()
            is TimerUiEvent.PauseTimer -> pauseTimer()
            is TimerUiEvent.ResetTimer -> resetTimer()
            is TimerUiEvent.SkipTimer -> showSkipDialog()
            is TimerUiEvent.CompleteSession -> completeSession()
            is TimerUiEvent.DismissCompletionDialog -> dismissCompletionDialog()
            is TimerUiEvent.DismissSkipDialog -> dismissSkipDialog()
            is TimerUiEvent.ConfirmSkip -> confirmSkip()
            is TimerUiEvent.StartNextSession -> startNextSession()
            is TimerUiEvent.SwitchMode -> switchMode(event.mode)
            is TimerUiEvent.UpdateSessionNotes -> updateSessionNotes(event.notes)
            is TimerUiEvent.ClearError -> clearError()
        }
    }
    
    /**
     * Start the timer
     */
    private fun startTimer() {
        if (_uiState.value.timerState != TimerState.IDLE && _uiState.value.timerState != TimerState.PAUSED) {
            return
        }
        
        val currentState = _uiState.value
        
        // Record session start time
        if (currentState.timerState == TimerState.IDLE) {
            sessionStartTime = LocalDateTime.now()
            totalPausedDuration = 0L
        } else if (currentState.timerState == TimerState.PAUSED) {
            // Calculate paused duration
            sessionPauseTime?.let { pauseTime ->
                totalPausedDuration += java.time.Duration.between(pauseTime, LocalDateTime.now()).seconds
            }
        }
        
        _uiState.value = currentState.copy(
            timerState = TimerState.RUNNING,
            error = null
        )
        
        saveTimerState()
        startCountdown()
    }
    
    /**
     * Pause the timer
     */
    private fun pauseTimer() {
        if (_uiState.value.timerState != TimerState.RUNNING) return
        
        sessionPauseTime = LocalDateTime.now()
        timerJob?.cancel()
        
        _uiState.value = _uiState.value.copy(
            timerState = TimerState.PAUSED
        )
        
        saveTimerState()
    }
    
    /**
     * Reset the timer
     */
    private fun resetTimer() {
        timerJob?.cancel()
        sessionStartTime = null
        sessionPauseTime = null
        totalPausedDuration = 0L
        sessionNotes = null
        
        val currentState = _uiState.value
        val durationMinutes = currentState.settings.getDurationForMode(currentState.currentMode)
        val durationSeconds = (durationMinutes * 60).toLong()
        
        _uiState.value = currentState.copy(
            timerState = TimerState.IDLE,
            timeRemainingSeconds = durationSeconds,
            totalTimeSeconds = durationSeconds,
            progress = 0f,
            error = null
        )
        
        clearPersistedState()
    }
    
    /**
     * Show skip confirmation dialog
     */
    private fun showSkipDialog() {
        if (!_uiState.value.canSkip) return
        
        _uiState.value = _uiState.value.copy(
            showSkipDialog = true
        )
    }
    
    /**
     * Confirm skip and complete session as skipped
     */
    private fun confirmSkip() {
        timerJob?.cancel()
        
        val currentState = _uiState.value
        val actualDurationMinutes = calculateActualDuration()
        
        // Save skipped session
        saveSession(
            wasCompleted = false,
            wasSkipped = true,
            actualDurationMinutes = actualDurationMinutes
        )
        
        _uiState.value = currentState.copy(
            timerState = TimerState.COMPLETED,
            showSkipDialog = false,
            showCompletionDialog = true
        )
    }
    
    /**
     * Complete the current session
     */
    private fun completeSession() {
        timerJob?.cancel()
        
        val currentState = _uiState.value
        val actualDurationMinutes = calculateActualDuration()
        
        // Save completed session
        saveSession(
            wasCompleted = true,
            wasSkipped = false,
            actualDurationMinutes = actualDurationMinutes
        )
        
        _uiState.value = currentState.copy(
            timerState = TimerState.COMPLETED,
            timeRemainingSeconds = 0L,
            progress = 1f,
            showCompletionDialog = true
        )
    }
    
    // ==================== TIMER COUNTDOWN ====================
    
    /**
     * Start the countdown timer
     */
    private fun startCountdown() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0 && _uiState.value.timerState == TimerState.RUNNING) {
                delay(1000L) // 1 second intervals
                
                val currentState = _uiState.value
                val newTimeRemaining = currentState.timeRemainingSeconds - 1
                val newProgress = if (currentState.totalTimeSeconds > 0) {
                    1f - (newTimeRemaining.toFloat() / currentState.totalTimeSeconds.toFloat())
                } else 0f
                
                _uiState.value = currentState.copy(
                    timeRemainingSeconds = newTimeRemaining,
                    progress = newProgress
                )
                
                // Check if timer completed
                if (newTimeRemaining <= 0) {
                    completeSession()
                    break
                }
            }
        }
    }
    
    // ==================== SESSION MANAGEMENT ====================
    
    /**
     * Switch to a different timer mode
     */
    private fun switchMode(mode: TimerMode) {
        if (_uiState.value.timerState.isActive) {
            // Can't switch mode while timer is active
            return
        }
        
        val currentState = _uiState.value
        val durationMinutes = currentState.settings.getDurationForMode(mode)
        val durationSeconds = (durationMinutes * 60).toLong()
        
        _uiState.value = currentState.copy(
            currentMode = mode,
            timeRemainingSeconds = durationSeconds,
            totalTimeSeconds = durationSeconds,
            progress = 0f,
            timerState = TimerState.IDLE
        )
        
        // Reset session tracking
        resetSessionTracking()
    }
    
    /**
     * Start the next session in the cycle
     */
    private fun startNextSession() {
        val currentState = _uiState.value
        val nextMode = currentState.nextMode
        
        // Update cycle information
        val newCycle = if (nextMode == TimerMode.WORK && currentState.currentMode == TimerMode.LONG_BREAK) {
            currentState.currentCycle + 1
        } else {
            currentState.currentCycle
        }
        
        val newSessionsCompleted = if (currentState.currentMode == TimerMode.WORK) {
            (currentState.sessionsCompletedInCycle + 1) % currentState.sessionsUntilLongBreak
        } else {
            currentState.sessionsCompletedInCycle
        }
        
        // Switch to next mode
        val durationMinutes = currentState.settings.getDurationForMode(nextMode)
        val durationSeconds = (durationMinutes * 60).toLong()
        
        _uiState.value = currentState.copy(
            currentMode = nextMode,
            timeRemainingSeconds = durationSeconds,
            totalTimeSeconds = durationSeconds,
            progress = 0f,
            timerState = TimerState.IDLE,
            currentCycle = newCycle,
            sessionsCompletedInCycle = newSessionsCompleted,
            showCompletionDialog = false
        )
        
        // Reset session tracking
        resetSessionTracking()
        
        // Auto-start if enabled
        if (currentState.shouldAutoStartNext) {
            startTimer()
        }
    }
    
    /**
     * Save the completed session to repository
     */
    private fun saveSession(wasCompleted: Boolean, wasSkipped: Boolean, actualDurationMinutes: Int) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val session = SessionRecord(
                    mode = currentState.currentMode,
                    plannedDurationMinutes = currentState.currentSessionDurationMinutes,
                    actualDurationMinutes = actualDurationMinutes,
                    completedAt = LocalDateTime.now(),
                    wasCompleted = wasCompleted,
                    cycleNumber = currentState.currentCycle,
                    notes = sessionNotes
                )
                
                sessionRepository.insertSession(session)
                
                // Update statistics in UI state
                updateStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save session: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Calculate actual session duration in minutes
     */
    private fun calculateActualDuration(): Int {
        val currentState = _uiState.value
        val totalPlannedSeconds = currentState.totalTimeSeconds
        val remainingSeconds = currentState.timeRemainingSeconds
        val elapsedSeconds = totalPlannedSeconds - remainingSeconds
        
        // Subtract paused duration
        val actualElapsedSeconds = elapsedSeconds - totalPausedDuration
        
        return (actualElapsedSeconds / 60).toInt().coerceAtLeast(0)
    }
    
    /**
     * Update session notes
     */
    private fun updateSessionNotes(notes: String) {
        sessionNotes = notes.takeIf { it.isNotBlank() }
    }
    
    /**
     * Reset session tracking variables
     */
    private fun resetSessionTracking() {
        sessionStartTime = null
        sessionPauseTime = null
        totalPausedDuration = 0L
        sessionNotes = null
    }
    
    // ==================== UI STATE MANAGEMENT ====================
    
    /**
     * Dismiss completion dialog
     */
    private fun dismissCompletionDialog() {
        _uiState.value = _uiState.value.copy(
            showCompletionDialog = false
        )
    }
    
    /**
     * Dismiss skip dialog
     */
    private fun dismissSkipDialog() {
        _uiState.value = _uiState.value.copy(
            showSkipDialog = false
        )
    }
    
    /**
     * Clear error message
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null
        )
    }
    
    /**
     * Update statistics in UI state
     */
    private fun updateStatistics() {
        viewModelScope.launch {
            try {
                combine(
                    sessionRepository.getSessionsCompletedToday(),
                    sessionRepository.getWorkMinutesToday(),
                    sessionRepository.getCurrentStreak()
                ) { todaySessions, todayMinutes, streak ->
                    _uiState.value.copy(
                        todaySessionCount = todaySessions,
                        todayWorkMinutes = todayMinutes,
                        currentStreak = streak
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                // Handle error silently for statistics update
            }
        }
    }
    
    // ==================== LIFECYCLE ====================
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
    
    // ==================== PUBLIC UTILITY METHODS ====================
    
    /**
     * Get current session result for external use
     */
    fun getCurrentSessionResult(): SessionResult? {
        val currentState = _uiState.value
        if (currentState.timerState != TimerState.COMPLETED) return null
        
        return SessionResult(
            mode = currentState.currentMode,
            plannedDurationMinutes = currentState.currentSessionDurationMinutes,
            actualDurationMinutes = calculateActualDuration(),
            wasCompleted = currentState.timerState == TimerState.COMPLETED,
            wasSkipped = false, // This would be tracked separately
            cycleNumber = currentState.currentCycle,
            notes = sessionNotes
        )
    }
    
    /**
     * Force refresh settings and statistics
     */
    fun refreshData() {
        initializeTimer()
    }
    
    // ==================== STATE PERSISTENCE ====================
    
    /**
     * Restore timer state from persistent storage
     */
    private fun restoreTimerState() {
        viewModelScope.launch {
            try {
                timerStateManager.getTimerState().collect { persistedState ->
                    persistedState?.let { state ->
                        if (state.shouldRestore()) {
                            restoreFromPersistedState(state)
                        } else {
                            // Clear old state if it's too old
                            timerStateManager.clearTimerState()
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently, continue with default state
            }
        }
    }
    
    /**
     * Restore UI state from persisted state
     */
    private fun restoreFromPersistedState(persistedState: PersistedTimerState) {
        val currentState = _uiState.value
        val adjustedTimeRemaining = persistedState.getAdjustedTimeRemaining()
        
        _uiState.value = currentState.copy(
            timerState = if (adjustedTimeRemaining <= 0) TimerState.COMPLETED else persistedState.timerState,
            currentMode = persistedState.currentMode,
            timeRemainingSeconds = adjustedTimeRemaining,
            totalTimeSeconds = persistedState.totalTimeSeconds,
            currentCycle = persistedState.currentCycle,
            sessionsCompletedInCycle = persistedState.sessionsCompletedInCycle,
            progress = persistedState.progress
        )
        
        // Restore session tracking variables
        sessionStartTime = persistedState.sessionStartTime?.let { 
            LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC) 
        }
        sessionPauseTime = persistedState.sessionPauseTime?.let { 
            LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC) 
        }
        totalPausedDuration = persistedState.totalPausedDuration
        sessionNotes = persistedState.sessionNotes
        
        // Resume timer if it was running
        if (persistedState.timerState == TimerState.RUNNING && adjustedTimeRemaining > 0) {
            startCountdown()
        }
    }
    
    /**
     * Save current timer state to persistent storage
     */
    private fun saveTimerState() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val persistedState = PersistedTimerState(
                    timerState = currentState.timerState,
                    currentMode = currentState.currentMode,
                    timeRemainingSeconds = currentState.timeRemainingSeconds,
                    totalTimeSeconds = currentState.totalTimeSeconds,
                    currentCycle = currentState.currentCycle,
                    sessionsCompletedInCycle = currentState.sessionsCompletedInCycle,
                    sessionStartTime = sessionStartTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
                    sessionPauseTime = sessionPauseTime?.atZone(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
                    totalPausedDuration = totalPausedDuration,
                    sessionNotes = sessionNotes,
                    isServiceRunning = currentState.timerState.isActive
                )
                
                timerStateManager.saveTimerState(persistedState)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    /**
     * Clear persisted timer state
     */
    private fun clearPersistedState() {
        viewModelScope.launch {
            try {
                timerStateManager.clearTimerState()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}
