// presentation/settings/SettingsScreen.kt
package com.example.focusplus.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.focusplus.presentation.settings.components.CompactSettingsSection
import com.example.focusplus.presentation.settings.components.CompactDurationSlider
import com.example.focusplus.presentation.settings.components.CompactSettingsToggle
import com.example.focusplus.presentation.settings.components.CompactPresetSelector
import com.example.focusplus.presentation.settings.components.DurationSlider
import com.example.focusplus.presentation.settings.components.PresetSelector
import com.example.focusplus.presentation.settings.components.SettingsDivider
import com.example.focusplus.presentation.settings.components.SettingsGroup
import com.example.focusplus.presentation.settings.components.SettingsSection
import com.example.focusplus.presentation.settings.components.SettingsToggle

/**
 * Settings screen with responsive design
 * 
 * Provides comprehensive settings management with validation,
 * presets, and user-friendly controls for all app preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    
    // Show messages in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Dismiss"
            )
            viewModel.onEvent(SettingsUiEvent.ClearError)
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "OK"
            )
            viewModel.onEvent(SettingsUiEvent.ClearSuccessMessage)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Reset button
                    IconButton(
                        onClick = { viewModel.onEvent(SettingsUiEvent.ShowResetDialog) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to Defaults"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.hasChanges) {
                SettingsBottomBar(
                    canSave = uiState.canSave,
                    onSave = { viewModel.onEvent(SettingsUiEvent.SaveSettings) },
                    onDiscard = { viewModel.onEvent(SettingsUiEvent.DiscardChanges) },
                    isCompact = isCompact
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (isCompact) {
            CompactSettingsLayout(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            RegularSettingsLayout(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
    
    // Reset confirmation dialog
    if (uiState.showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = { viewModel.onEvent(SettingsUiEvent.ConfirmReset) },
            onDismiss = { viewModel.onEvent(SettingsUiEvent.DismissResetDialog) }
        )
    }
    
    // Preset selection dialog
    if (uiState.showPresetDialog) {
        PresetSelectionDialog(
            selectedPreset = uiState.currentPreset,
            onPresetSelected = { preset ->
                viewModel.onEvent(SettingsUiEvent.ApplyPreset(preset))
            },
            onDismiss = { viewModel.onEvent(SettingsUiEvent.DismissPresetDialog) },
            isCompact = isCompact
        )
    }
    
    // Validation error dialog
    if (uiState.showValidationDialog) {
        ValidationErrorDialog(
            errors = uiState.validationErrors,
            onDismiss = { viewModel.onEvent(SettingsUiEvent.DismissValidationDialog) }
        )
    }
}

/**
 * Regular layout for larger screens
 */
@Composable
private fun RegularSettingsLayout(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Presets section
        SettingsSection(
            title = "Quick Setup",
            description = "Choose a preset or customize your own settings"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current: ${uiState.currentPreset?.displayName ?: "Custom"}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                FilledTonalButton(
                    onClick = { onEvent(SettingsUiEvent.ShowPresetDialog) }
                ) {
                    Text("Choose Preset")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Total cycle time: ${uiState.formattedTotalCycleTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Duration settings
        SettingsSection(
            title = "Timer Durations",
            description = "Customize the length of your work and break sessions"
        ) {
            DurationSlider(
                label = "Work Duration",
                value = uiState.tempWorkDuration,
                onValueChange = { onEvent(SettingsUiEvent.UpdateWorkDuration(it)) },
                valueRange = 1f..120f,
                steps = 119
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DurationSlider(
                label = "Short Break Duration",
                value = uiState.tempShortBreakDuration,
                onValueChange = { onEvent(SettingsUiEvent.UpdateShortBreakDuration(it)) },
                valueRange = 1f..60f,
                steps = 59
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DurationSlider(
                label = "Long Break Duration",
                value = uiState.tempLongBreakDuration,
                onValueChange = { onEvent(SettingsUiEvent.UpdateLongBreakDuration(it)) },
                valueRange = 1f..120f,
                steps = 119
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DurationSlider(
                label = "Sessions Until Long Break",
                value = uiState.tempSessionsUntilLongBreak,
                onValueChange = { onEvent(SettingsUiEvent.UpdateSessionsUntilLongBreak(it)) },
                valueRange = 1f..10f,
                steps = 9,
                unit = "sessions"
            )
        }
        
        // Notification settings
        SettingsSection(
            title = "Notifications & Alerts",
            description = "Configure how you want to be notified"
        ) {
            SettingsToggle(
                title = "Enable Notifications",
                description = "Show notifications when sessions complete",
                checked = uiState.settings.isNotificationsEnabled,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateNotifications(it)) }
            )
            
            SettingsDivider()
            
            SettingsToggle(
                title = "Sound Alerts",
                description = "Play sound when sessions complete",
                checked = uiState.settings.isSoundEnabled,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateSound(it)) },
                enabled = uiState.settings.isNotificationsEnabled
            )
            
            SettingsDivider()
            
            SettingsToggle(
                title = "Vibration",
                description = "Vibrate device when sessions complete",
                checked = uiState.settings.isVibrationEnabled,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateVibration(it)) },
                enabled = uiState.settings.isNotificationsEnabled
            )
        }
        
        // Automation settings
        SettingsSection(
            title = "Automation",
            description = "Automatically start the next session"
        ) {
            SettingsToggle(
                title = "Auto-start Breaks",
                description = "Automatically start break sessions",
                checked = uiState.settings.autoStartBreaks,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateAutoStartBreaks(it)) }
            )
            
            SettingsDivider()
            
            SettingsToggle(
                title = "Auto-start Work",
                description = "Automatically start work sessions after breaks",
                checked = uiState.settings.autoStartWork,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateAutoStartWork(it)) }
            )
        }
        
        // Theme settings
        SettingsSection(
            title = "Appearance",
            description = "Customize the app's appearance"
        ) {
            SettingsToggle(
                title = "Dark Theme",
                description = "Use dark theme throughout the app",
                checked = uiState.settings.isDarkTheme,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateTheme(it)) }
            )
        }
    }
}

/**
 * Compact layout for smaller screens
 */
@Composable
private fun CompactSettingsLayout(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick preset selection
        CompactSettingsSection(title = "Quick Setup") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.currentPreset?.displayName ?: "Custom",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cycle: ${uiState.formattedTotalCycleTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                FilledTonalButton(
                    onClick = { onEvent(SettingsUiEvent.ShowPresetDialog) }
                ) {
                    Text("Presets")
                }
            }
        }
        
        // Duration settings
        CompactSettingsSection(title = "Durations") {
            CompactDurationSlider(
                label = "Work",
                value = uiState.tempWorkDuration,
                onValueChange = { onEvent(SettingsUiEvent.UpdateWorkDuration(it)) },
                valueRange = 1f..120f
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CompactDurationSlider(
                label = "Short Break",
                value = uiState.tempShortBreakDuration,
                onValueChange = { onEvent(SettingsUiEvent.UpdateShortBreakDuration(it)) },
                valueRange = 1f..60f
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CompactDurationSlider(
                label = "Long Break",
                value = uiState.tempLongBreakDuration,
                onValueChange = { onEvent(SettingsUiEvent.UpdateLongBreakDuration(it)) },
                valueRange = 1f..120f
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CompactDurationSlider(
                label = "Sessions to Long Break",
                value = uiState.tempSessionsUntilLongBreak,
                onValueChange = { onEvent(SettingsUiEvent.UpdateSessionsUntilLongBreak(it)) },
                valueRange = 1f..10f,
                unit = "sessions"
            )
        }
        
        // Notifications
        CompactSettingsSection(title = "Notifications") {
            CompactSettingsToggle(
                title = "Enable Notifications",
                checked = uiState.settings.isNotificationsEnabled,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateNotifications(it)) }
            )
            
            CompactSettingsToggle(
                title = "Sound",
                checked = uiState.settings.isSoundEnabled,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateSound(it)) },
                enabled = uiState.settings.isNotificationsEnabled
            )
            
            CompactSettingsToggle(
                title = "Vibration",
                checked = uiState.settings.isVibrationEnabled,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateVibration(it)) },
                enabled = uiState.settings.isNotificationsEnabled
            )
        }
        
        // Automation & Theme
        CompactSettingsSection(title = "Other") {
            CompactSettingsToggle(
                title = "Auto-start Breaks",
                checked = uiState.settings.autoStartBreaks,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateAutoStartBreaks(it)) }
            )
            
            CompactSettingsToggle(
                title = "Auto-start Work",
                checked = uiState.settings.autoStartWork,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateAutoStartWork(it)) }
            )
            
            CompactSettingsToggle(
                title = "Dark Theme",
                checked = uiState.settings.isDarkTheme,
                onCheckedChange = { onEvent(SettingsUiEvent.UpdateTheme(it)) }
            )
        }
    }
}

/**
 * Bottom bar for save/discard actions
 */
@Composable
private fun SettingsBottomBar(
    canSave: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDiscard,
            modifier = Modifier.weight(1f)
        ) {
            Text("Discard")
        }
        
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null
            )
            if (!isCompact) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes")
            }
        }
    }
}

/**
 * Reset confirmation dialog
 */
@Composable
private fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reset Settings?",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text = "This will reset all settings to their default values. This action cannot be undone.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Preset selection dialog
 */
@Composable
private fun PresetSelectionDialog(
    selectedPreset: SettingsPreset?,
    onPresetSelected: (SettingsPreset) -> Unit,
    onDismiss: () -> Unit,
    isCompact: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Preset",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            if (isCompact) {
                CompactPresetSelector(
                    selectedPreset = selectedPreset,
                    onPresetSelected = onPresetSelected
                )
            } else {
                PresetSelector(
                    selectedPreset = selectedPreset,
                    onPresetSelected = onPresetSelected
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Validation error dialog
 */
@Composable
private fun ValidationErrorDialog(
    errors: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Invalid Settings",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Please fix the following issues:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                errors.forEach { error ->
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
