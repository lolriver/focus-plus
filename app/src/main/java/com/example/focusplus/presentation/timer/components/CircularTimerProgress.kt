// presentation/timer/components/CircularTimerProgress.kt
package com.example.focusplus.presentation.timer.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusplus.domain.model.TimerMode
import com.example.focusplus.domain.model.TimerState

/**
 * Circular progress indicator for the timer
 * 
 * Shows progress with smooth animations and displays time remaining
 * in the center with mode-specific colors.
 */
@Composable
fun CircularTimerProgress(
    progress: Float,
    timeRemaining: String,
    totalTime: String,
    timerMode: TimerMode,
    timerState: TimerState,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 12.dp
) {
    // Animate progress changes
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress_animation"
    )
    
    // Get colors based on timer mode and state
    val progressColor = getProgressColor(timerMode, timerState)
    val backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(-90f) // Start from top
        ) {
            // Background arc
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = 0f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
        
        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer mode
            Text(
                text = timerMode.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            // Time remaining
            Text(
                text = timeRemaining,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 48.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // Total time (smaller text)
            Text(
                text = "of $totalTime",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            // Timer state indicator
            if (timerState == TimerState.PAUSED) {
                Text(
                    text = "PAUSED",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Get progress color based on timer mode and state
 */
@Composable
private fun getProgressColor(timerMode: TimerMode, timerState: TimerState): Color {
    return when {
        timerState == TimerState.PAUSED -> MaterialTheme.colorScheme.error
        timerMode == TimerMode.WORK -> MaterialTheme.colorScheme.primary
        timerMode == TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        timerMode == TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}
