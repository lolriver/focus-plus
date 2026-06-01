// presentation/settings/SettingsUiState.kt
package com.example.focusplus.presentation.settings

import com.example.focusplus.domain.model.Settings
import com.example.focusplus.domain.model.TimerMode

/**
 * UI state for the Settings screen
 * 
 * This data class represents the complete state of the settings UI,
 * including current settings, validation state, and UI interactions.
 */
data class SettingsUiState(
    // Current settings
    val settings: Settings = Settings.DEFAULT,
    
    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    
    // Validation state
    val validationErrors: List<String> = emptyList(),
    val hasUnsavedChanges: Boolean = false,
    
    // Dialog states
    val showResetDialog: Boolean = false,
    val showPresetDialog: Boolean = false,
    val showValidationDialog: Boolean = false,
    
    // Temporary editing values (for sliders/inputs)
    val tempWorkDuration: Int = settings.workDurationMinutes,
    val tempShortBreakDuration: Int = settings.shortBreakDurationMinutes,
    val tempLongBreakDuration: Int = settings.longBreakDurationMinutes,
    val tempSessionsUntilLongBreak: Int = settings.sessionsUntilLongBreak
) {
    /**
     * Check if current settings are valid
     */
    val isValid: Boolean
        get() = validationErrors.isEmpty()
    
    /**
     * Check if any changes have been made
     */
    val hasChanges: Boolean
        get() = tempWorkDuration != settings.workDurationMinutes ||
                tempShortBreakDuration != settings.shortBreakDurationMinutes ||
                tempLongBreakDuration != settings.longBreakDurationMinutes ||
                tempSessionsUntilLongBreak != settings.sessionsUntilLongBreak
    
    /**
     * Get current preset name if settings match a preset
     */
    val currentPreset: SettingsPreset?
        get() = when {
            settings.matchesPreset(Settings.POMODORO_STANDARD) -> SettingsPreset.STANDARD
            settings.matchesPreset(Settings.DEEP_FOCUS) -> SettingsPreset.DEEP_FOCUS
            settings.matchesPreset(Settings.SHORT_BURSTS) -> SettingsPreset.SHORT_BURSTS
            else -> SettingsPreset.CUSTOM
        }
    
    /**
     * Get formatted work duration
     */
    val formattedWorkDuration: String
        get() = "${tempWorkDuration} min"
    
    /**
     * Get formatted short break duration
     */
    val formattedShortBreakDuration: String
        get() = "${tempShortBreakDuration} min"
    
    /**
     * Get formatted long break duration
     */
    val formattedLongBreakDuration: String
        get() = "${tempLongBreakDuration} min"
    
    /**
     * Get formatted sessions until long break
     */
    val formattedSessionsUntilLongBreak: String
        get() = "$tempSessionsUntilLongBreak sessions"
    
    /**
     * Get total cycle time in minutes
     */
    val totalCycleTimeMinutes: Int
        get() {
            val workTime = tempWorkDuration * tempSessionsUntilLongBreak
            val shortBreaks = (tempSessionsUntilLongBreak - 1) * tempShortBreakDuration
            val longBreak = tempLongBreakDuration
            return workTime + shortBreaks + longBreak
        }
    
    /**
     * Get formatted total cycle time
     */
    val formattedTotalCycleTime: String
        get() {
            val totalMinutes = totalCycleTimeMinutes
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
        }
    
    /**
     * Check if can save settings
     */
    val canSave: Boolean
        get() = isValid && hasChanges && !isSaving
    
    /**
     * Get settings with temporary values applied
     */
    val settingsWithTempValues: Settings
        get() = settings.copy(
            workDurationMinutes = tempWorkDuration,
            shortBreakDurationMinutes = tempShortBreakDuration,
            longBreakDurationMinutes = tempLongBreakDuration,
            sessionsUntilLongBreak = tempSessionsUntilLongBreak
        )
}

/**
 * UI events that can be triggered from the Settings screen
 */
sealed class SettingsUiEvent {
    // Duration changes
    data class UpdateWorkDuration(val minutes: Int) : SettingsUiEvent()
    data class UpdateShortBreakDuration(val minutes: Int) : SettingsUiEvent()
    data class UpdateLongBreakDuration(val minutes: Int) : SettingsUiEvent()
    data class UpdateSessionsUntilLongBreak(val sessions: Int) : SettingsUiEvent()
    
    // Toggle settings
    data class UpdateTheme(val isDarkTheme: Boolean) : SettingsUiEvent()
    data class UpdateNotifications(val isEnabled: Boolean) : SettingsUiEvent()
    data class UpdateSound(val isEnabled: Boolean) : SettingsUiEvent()
    data class UpdateVibration(val isEnabled: Boolean) : SettingsUiEvent()
    data class UpdateAutoStartBreaks(val isEnabled: Boolean) : SettingsUiEvent()
    data class UpdateAutoStartWork(val isEnabled: Boolean) : SettingsUiEvent()
    
    // Actions
    object SaveSettings : SettingsUiEvent()
    object ResetToDefaults : SettingsUiEvent()
    object ShowResetDialog : SettingsUiEvent()
    object DismissResetDialog : SettingsUiEvent()
    object ConfirmReset : SettingsUiEvent()
    
    // Presets
    object ShowPresetDialog : SettingsUiEvent()
    object DismissPresetDialog : SettingsUiEvent()
    data class ApplyPreset(val preset: SettingsPreset) : SettingsUiEvent()
    
    // Validation
    object ShowValidationDialog : SettingsUiEvent()
    object DismissValidationDialog : SettingsUiEvent()
    
    // UI
    object ClearError : SettingsUiEvent()
    object ClearSuccessMessage : SettingsUiEvent()
    object DiscardChanges : SettingsUiEvent()
}

/**
 * Available settings presets
 */
enum class SettingsPreset(
    val displayName: String,
    val description: String,
    val settings: Settings
) {
    STANDARD(
        displayName = "Pomodoro Standard",
        description = "Classic 25/5/15 minute intervals",
        settings = Settings.POMODORO_STANDARD
    ),
    DEEP_FOCUS(
        displayName = "Deep Focus",
        description = "Longer 50/10/30 minute sessions",
        settings = Settings.DEEP_FOCUS
    ),
    SHORT_BURSTS(
        displayName = "Short Bursts",
        description = "Quick 15/3/10 minute intervals",
        settings = Settings.SHORT_BURSTS
    ),
    CUSTOM(
        displayName = "Custom",
        description = "Your personalized settings",
        settings = Settings.DEFAULT
    )
}

/**
 * Settings validation result
 */
data class SettingsValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    companion object {
        fun valid() = SettingsValidationResult(true, emptyList())
        fun invalid(errors: List<String>) = SettingsValidationResult(false, errors)
    }
}

/**
 * Extension function to check if settings match a preset
 */
private fun Settings.matchesPreset(preset: Settings): Boolean {
    return workDurationMinutes == preset.workDurationMinutes &&
            shortBreakDurationMinutes == preset.shortBreakDurationMinutes &&
            longBreakDurationMinutes == preset.longBreakDurationMinutes &&
            sessionsUntilLongBreak == preset.sessionsUntilLongBreak
}
