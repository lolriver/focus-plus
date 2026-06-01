// presentation/timer/components/TimerControls.kt
package com.example.focusplus.presentation.timer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.focusplus.domain.model.TimerState
import com.example.focusplus.presentation.timer.TimerUiEvent

/**
 * Timer control buttons with haptic feedback
 * 
 * Provides play/pause, reset, and skip functionality with
 * appropriate visual states and accessibility support.
 */
@Composable
fun TimerControls(
    timerState: TimerState,
    canStart: Boolean,
    canPause: Boolean,
    canReset: Boolean,
    canSkip: Boolean,
    onEvent: (TimerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primary action button (Play/Pause)
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                when (timerState) {
                    TimerState.IDLE, TimerState.PAUSED -> onEvent(TimerUiEvent.StartTimer)
                    TimerState.RUNNING -> onEvent(TimerUiEvent.PauseTimer)
                    else -> { /* No action for completed state */ }
                }
            },
            enabled = canStart || canPause,
            modifier = Modifier.size(width = 160.dp, height = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (timerState) {
                    TimerState.RUNNING -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Icon(
                imageVector = when (timerState) {
                    TimerState.RUNNING -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = when (timerState) {
                    TimerState.RUNNING -> "Pause Timer"
                    else -> "Start Timer"
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (timerState) {
                    TimerState.RUNNING -> "Pause"
                    TimerState.PAUSED -> "Resume"
                    else -> "Start"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Secondary action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset button
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(TimerUiEvent.ResetTimer)
                },
                enabled = canReset,
                modifier = Modifier.size(width = 100.dp, height = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Timer",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            // Skip button
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(TimerUiEvent.SkipTimer)
                },
                enabled = canSkip,
                modifier = Modifier.size(width = 100.dp, height = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip Timer",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Compact timer controls for smaller screens
 */
@Composable
fun CompactTimerControls(
    timerState: TimerState,
    canStart: Boolean,
    canPause: Boolean,
    canReset: Boolean,
    canSkip: Boolean,
    onEvent: (TimerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        OutlinedButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEvent(TimerUiEvent.ResetTimer)
            },
            enabled = canReset,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Timer",
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Primary play/pause button
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                when (timerState) {
                    TimerState.IDLE, TimerState.PAUSED -> onEvent(TimerUiEvent.StartTimer)
                    TimerState.RUNNING -> onEvent(TimerUiEvent.PauseTimer)
                    else -> { /* No action */ }
                }
            },
            enabled = canStart || canPause,
            modifier = Modifier.size(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (timerState) {
                    TimerState.RUNNING -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Icon(
                imageVector = when (timerState) {
                    TimerState.RUNNING -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = when (timerState) {
                    TimerState.RUNNING -> "Pause Timer"
                    else -> "Start Timer"
                },
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Skip button
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEvent(TimerUiEvent.SkipTimer)
            },
            enabled = canSkip,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip Timer",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
