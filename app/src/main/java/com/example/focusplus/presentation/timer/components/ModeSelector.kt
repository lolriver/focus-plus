// presentation/timer/components/ModeSelector.kt
package com.example.focusplus.presentation.timer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.presentation.timer.TimerUiEvent

/**
 * Mode selector chips for switching between Work, Short Break, and Long Break
 * 
 * Allows users to manually switch timer modes when timer is not active.
 * Provides visual feedback for the currently selected mode.
 */
@Composable
fun ModeSelector(
    currentMode: TimerMode,
    isTimerActive: Boolean,
    onEvent: (TimerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimerMode.values().forEach { mode ->
            FilterChip(
                selected = currentMode == mode,
                onClick = {
                    if (!isTimerActive && currentMode != mode) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEvent(TimerUiEvent.SwitchMode(mode))
                    }
                },
                label = {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (currentMode == mode) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                },
                enabled = !isTimerActive,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (mode) {
                        TimerMode.WORK -> MaterialTheme.colorScheme.primaryContainer
                        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
                        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                    selectedLabelColor = when (mode) {
                        TimerMode.WORK -> MaterialTheme.colorScheme.onPrimaryContainer
                        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.onSecondaryContainer
                        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Compact mode selector for smaller screens
 */
@Composable
fun CompactModeSelector(
    currentMode: TimerMode,
    isTimerActive: Boolean,
    onEvent: (TimerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimerMode.values().forEach { mode ->
            FilterChip(
                selected = currentMode == mode,
                onClick = {
                    if (!isTimerActive && currentMode != mode) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEvent(TimerUiEvent.SwitchMode(mode))
                    }
                },
                label = {
                    Text(
                        text = when (mode) {
                            TimerMode.WORK -> "Work"
                            TimerMode.SHORT_BREAK -> "Break"
                            TimerMode.LONG_BREAK -> "Long"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (currentMode == mode) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                },
                enabled = !isTimerActive,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (mode) {
                        TimerMode.WORK -> MaterialTheme.colorScheme.primaryContainer
                        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
                        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                    selectedLabelColor = when (mode) {
                        TimerMode.WORK -> MaterialTheme.colorScheme.onPrimaryContainer
                        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.onSecondaryContainer
                        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
