// domain/model/TimerMode.kt
package com.example.focusplus.domain.model

/**
 * Represents the different modes of the Pomodoro timer
 */
enum class TimerMode(
    val displayName: String,
    val defaultDurationMinutes: Int
) {
    /**
     * Work session - focused productivity time
     */
    WORK("Work", 25),
    
    /**
     * Short break - brief rest between work sessions
     */
    SHORT_BREAK("Short Break", 5),
    
    /**
     * Long break - extended rest after completing a cycle of work sessions
     */
    LONG_BREAK("Long Break", 15);
    
    /**
     * Check if this mode is a break mode
     */
    val isBreak: Boolean
        get() = this == SHORT_BREAK || this == LONG_BREAK
    
    /**
     * Check if this mode is a work mode
     */
    val isWork: Boolean
        get() = this == WORK
}
