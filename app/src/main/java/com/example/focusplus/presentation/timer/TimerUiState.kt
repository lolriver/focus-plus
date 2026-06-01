// presentation/timer/TimerUiState.kt
package com.example.focusplus.presentation.timer

import com.example.focusplus.domain.model.Settings
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.domain.model.TimerState

/**
 * UI state for the Timer screen
 * 
 * This data class represents the complete state of the timer UI,
 * including current session, timer state, and user preferences.
 */
data class TimerUiState(
    // Timer State
    val timerState: TimerState = TimerState.IDLE,
    val currentMode: TimerMode = TimerMode.WORK,
    val timeRemainingSeconds: Long = 0L,
    val totalTimeSeconds: Long = 0L,
    val progress: Float = 0f,
    
    // Session Information
    val currentCycle: Int = 1,
    val sessionsUntilLongBreak: Int = 4,
    val sessionsCompletedInCycle: Int = 0,
    
    // Settings
    val settings: Settings = Settings.DEFAULT,
    
    // UI State
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCompletionDialog: Boolean = false,
    val showSkipDialog: Boolean = false,
    
    // Statistics (for quick display)
    val todaySessionCount: Int = 0,
    val todayWorkMinutes: Int = 0,
    val currentStreak: Int = 0
) {
    /**
     * Get formatted time remaining as MM:SS
     */
    val formattedTimeRemaining: String
        get() {
            val minutes = timeRemainingSeconds / 60
            val seconds = timeRemainingSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    
    /**
     * Get formatted total time as MM:SS
     */
    val formattedTotalTime: String
        get() {
            val minutes = totalTimeSeconds / 60
            val seconds = totalTimeSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    
    /**
     * Check if timer can be started
     */
    val canStart: Boolean
        get() = timerState.canStart
    
    /**
     * Check if timer can be paused
     */
    val canPause: Boolean
        get() = timerState.canPause
    
    /**
     * Check if timer can be reset
     */
    val canReset: Boolean
        get() = timerState.canReset
    
    /**
     * Check if timer can be skipped
     */
    val canSkip: Boolean
        get() = timerState.isActive
    
    /**
     * Get progress percentage (0.0 to 1.0)
     */
    val progressPercentage: Float
        get() = if (totalTimeSeconds > 0) {
            1f - (timeRemainingSeconds.toFloat() / totalTimeSeconds.toFloat())
        } else 0f
    
    /**
     * Get next timer mode after current session
     */
    val nextMode: TimerMode
        get() = when {
            currentMode == TimerMode.WORK && sessionsCompletedInCycle >= sessionsUntilLongBreak - 1 -> {
                TimerMode.LONG_BREAK
            }
            currentMode == TimerMode.WORK -> TimerMode.SHORT_BREAK
            else -> TimerMode.WORK
        }
    
    /**
     * Check if current session is a break
     */
    val isBreakSession: Boolean
        get() = currentMode.isBreak
    
    /**
     * Check if current session is work
     */
    val isWorkSession: Boolean
        get() = currentMode.isWork
    
    /**
     * Get current session duration in minutes
     */
    val currentSessionDurationMinutes: Int
        get() = settings.getDurationForMode(currentMode)
    
    /**
     * Check if auto-start is enabled for next session
     */
    val shouldAutoStartNext: Boolean
        get() = when (nextMode) {
            TimerMode.WORK -> settings.autoStartWork
            TimerMode.SHORT_BREAK, TimerMode.LONG_BREAK -> settings.autoStartBreaks
        }
    
    /**
     * Get display text for current mode
     */
    val currentModeDisplayText: String
        get() = currentMode.displayName
    
    /**
     * Get display text for next mode
     */
    val nextModeDisplayText: String
        get() = nextMode.displayName
    
    /**
     * Check if session is nearly complete (last 10% of time)
     */
    val isNearlyComplete: Boolean
        get() = progress >= 0.9f
    
    /**
     * Get motivational message based on current state
     */
    val motivationalMessage: String
        get() = when {
            timerState == TimerState.IDLE && isWorkSession -> "Ready to focus?"
            timerState == TimerState.IDLE && isBreakSession -> "Time for a break!"
            timerState == TimerState.RUNNING && isWorkSession && progress < 0.5f -> "Stay focused!"
            timerState == TimerState.RUNNING && isWorkSession && progress >= 0.5f -> "You're doing great!"
            timerState == TimerState.RUNNING && isBreakSession -> "Enjoy your break"
            timerState == TimerState.PAUSED -> "Take your time"
            timerState == TimerState.COMPLETED && isWorkSession -> "Great work!"
            timerState == TimerState.COMPLETED && isBreakSession -> "Break complete!"
            else -> ""
        }
}

/**
 * UI events that can be triggered from the Timer screen
 */
sealed class TimerUiEvent {
    object StartTimer : TimerUiEvent()
    object PauseTimer : TimerUiEvent()
    object ResetTimer : TimerUiEvent()
    object SkipTimer : TimerUiEvent()
    object CompleteSession : TimerUiEvent()
    object DismissCompletionDialog : TimerUiEvent()
    object DismissSkipDialog : TimerUiEvent()
    object ConfirmSkip : TimerUiEvent()
    object StartNextSession : TimerUiEvent()
    data class SwitchMode(val mode: TimerMode) : TimerUiEvent()
    data class UpdateSessionNotes(val notes: String) : TimerUiEvent()
    object ClearError : TimerUiEvent()
}

/**
 * Timer actions that can be performed
 */
sealed class TimerAction {
    object Start : TimerAction()
    object Pause : TimerAction()
    object Resume : TimerAction()
    object Reset : TimerAction()
    object Skip : TimerAction()
    object Complete : TimerAction()
    data class SwitchMode(val mode: TimerMode) : TimerAction()
}

/**
 * Timer session result after completion
 */
data class SessionResult(
    val mode: TimerMode,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val wasCompleted: Boolean,
    val wasSkipped: Boolean,
    val cycleNumber: Int,
    val notes: String? = null
) {
    /**
     * Check if session was successful (completed or nearly completed)
     */
    val isSuccessful: Boolean
        get() = wasCompleted || (actualDurationMinutes >= plannedDurationMinutes * 0.8)
    
    /**
     * Get completion percentage
     */
    val completionPercentage: Float
        get() = if (plannedDurationMinutes > 0) {
            (actualDurationMinutes.toFloat() / plannedDurationMinutes.toFloat()).coerceAtMost(1f)
        } else 0f
}
