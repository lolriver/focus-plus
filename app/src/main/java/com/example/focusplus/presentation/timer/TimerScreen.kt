// presentation/timer/TimerScreen.kt
package com.example.focusplus.presentation.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.focusplus.presentation.timer.components.CircularTimerProgress
import com.example.focusplus.presentation.timer.components.CompactModeSelector
import com.example.focusplus.presentation.timer.components.CompactSessionInfoCard
import com.example.focusplus.presentation.timer.components.CompactTimerControls
import com.example.focusplus.presentation.timer.components.ModeSelector
import com.example.focusplus.presentation.timer.components.SessionInfoCard
import com.example.focusplus.presentation.timer.components.TimerControls

/**
 * Main Timer screen with responsive design
 * 
 * Adapts layout based on screen size and provides complete timer functionality
 * including progress visualization, controls, and session information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    
    // Show error messages in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Dismiss"
            )
            viewModel.onEvent(TimerUiEvent.ClearError)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FocusPulse",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (isCompact) {
            CompactTimerLayout(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            RegularTimerLayout(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
    
    // Completion Dialog
    if (uiState.showCompletionDialog) {
        SessionCompletionDialog(
            mode = uiState.currentMode,
            nextMode = uiState.nextMode,
            shouldAutoStart = uiState.shouldAutoStartNext,
            onStartNext = { viewModel.onEvent(TimerUiEvent.StartNextSession) },
            onDismiss = { viewModel.onEvent(TimerUiEvent.DismissCompletionDialog) }
        )
    }
    
    // Skip Confirmation Dialog
    if (uiState.showSkipDialog) {
        SkipConfirmationDialog(
            onConfirm = { viewModel.onEvent(TimerUiEvent.ConfirmSkip) },
            onDismiss = { viewModel.onEvent(TimerUiEvent.DismissSkipDialog) }
        )
    }
}

/**
 * Regular layout for larger screens
 */
@Composable
private fun RegularTimerLayout(
    uiState: TimerUiState,
    onEvent: (TimerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Mode selector
        ModeSelector(
            currentMode = uiState.currentMode,
            isTimerActive = uiState.timerState.isActive,
            onEvent = onEvent
        )
        
        // Main timer display
        CircularTimerProgress(
            progress = uiState.progress,
            timeRemaining = uiState.formattedTimeRemaining,
            totalTime = uiState.formattedTotalTime,
            timerMode = uiState.currentMode,
            timerState = uiState.timerState,
            size = 320.dp
        )
        
        // Timer controls
        TimerControls(
            timerState = uiState.timerState,
            canStart = uiState.canStart,
            canPause = uiState.canPause,
            canReset = uiState.canReset,
            canSkip = uiState.canSkip,
            onEvent = onEvent
        )
        
        // Session information
        SessionInfoCard(
            currentCycle = uiState.currentCycle,
            sessionsCompletedInCycle = uiState.sessionsCompletedInCycle,
            sessionsUntilLongBreak = uiState.sessionsUntilLongBreak,
            todaySessionCount = uiState.todaySessionCount,
            todayWorkMinutes = uiState.todayWorkMinutes,
            currentStreak = uiState.currentStreak,
            nextMode = uiState.nextModeDisplayText,
            motivationalMessage = uiState.motivationalMessage
        )
    }
}

/**
 * Compact layout for smaller screens
 */
@Composable
private fun CompactTimerLayout(
    uiState: TimerUiState,
    onEvent: (TimerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact mode selector
        CompactModeSelector(
            currentMode = uiState.currentMode,
            isTimerActive = uiState.timerState.isActive,
            onEvent = onEvent
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Main timer display (smaller)
        CircularTimerProgress(
            progress = uiState.progress,
            timeRemaining = uiState.formattedTimeRemaining,
            totalTime = uiState.formattedTotalTime,
            timerMode = uiState.currentMode,
            timerState = uiState.timerState,
            size = 240.dp,
            strokeWidth = 10.dp
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Motivational message
        if (uiState.motivationalMessage.isNotEmpty()) {
            Text(
                text = uiState.motivationalMessage,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Compact timer controls
        CompactTimerControls(
            timerState = uiState.timerState,
            canStart = uiState.canStart,
            canPause = uiState.canPause,
            canReset = uiState.canReset,
            canSkip = uiState.canSkip,
            onEvent = onEvent
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Compact session info
        CompactSessionInfoCard(
            currentCycle = uiState.currentCycle,
            todaySessionCount = uiState.todaySessionCount,
            currentStreak = uiState.currentStreak,
            nextMode = uiState.nextModeDisplayText
        )
    }
}

/**
 * Session completion dialog
 */
@Composable
private fun SessionCompletionDialog(
    mode: com.example.focusplus.domain.model.TimerMode,
    nextMode: com.example.focusplus.domain.model.TimerMode,
    shouldAutoStart: Boolean,
    onStartNext: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${mode.displayName} Complete! 🎉",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Great job completing your ${mode.displayName.lowercase()} session!",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ready for your ${nextMode.displayName.lowercase()}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onStartNext) {
                Text("Start ${nextMode.displayName}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

/**
 * Skip confirmation dialog
 */
@Composable
private fun SkipConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Skip Session?",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text = "Are you sure you want to skip this session? This will mark it as incomplete.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Skip")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
