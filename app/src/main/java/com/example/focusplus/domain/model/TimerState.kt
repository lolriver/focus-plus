// domain/model/TimerState.kt
package com.example.focusplus.domain.model

/**
 * Represents the current state of the timer
 */
enum class TimerState {
    /**
     * Timer is idle/stopped
     */
    IDLE,
    
    /**
     * Timer is actively running
     */
    RUNNING,
    
    /**
     * Timer is paused
     */
    PAUSED,
    
    /**
     * Timer has completed
     */
    COMPLETED;
    
    /**
     * Check if timer is active (running or paused)
     */
    val isActive: Boolean
        get() = this == RUNNING || this == PAUSED
    
    /**
     * Check if timer can be started
     */
    val canStart: Boolean
        get() = this == IDLE || this == PAUSED
    
    /**
     * Check if timer can be paused
     */
    val canPause: Boolean
        get() = this == RUNNING
    
    /**
     * Check if timer can be reset
     */
    val canReset: Boolean
        get() = this != IDLE
}
