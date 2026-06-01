// presentation/settings/SettingsViewModel.kt
package com.example.focusplus.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusplus.domain.model.Settings
import com.example.focusplus.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen
 * 
 * Manages settings state, validation, and persistence.
 * Handles user preferences and preset configurations.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    // ==================== INITIALIZATION ====================
    
    /**
     * Load settings from repository
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                settingsRepository.getSettings().collect { settings ->
                    _uiState.value = _uiState.value.copy(
                        settings = settings,
                        tempWorkDuration = settings.workDurationMinutes,
                        tempShortBreakDuration = settings.shortBreakDurationMinutes,
                        tempLongBreakDuration = settings.longBreakDurationMinutes,
                        tempSessionsUntilLongBreak = settings.sessionsUntilLongBreak,
                        isLoading = false,
                        hasUnsavedChanges = false,
                        validationErrors = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load settings: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    // ==================== EVENT HANDLING ====================
    
    /**
     * Handle UI events from the settings screen
     */
    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            // Duration updates
            is SettingsUiEvent.UpdateWorkDuration -> updateWorkDuration(event.minutes)
            is SettingsUiEvent.UpdateShortBreakDuration -> updateShortBreakDuration(event.minutes)
            is SettingsUiEvent.UpdateLongBreakDuration -> updateLongBreakDuration(event.minutes)
            is SettingsUiEvent.UpdateSessionsUntilLongBreak -> updateSessionsUntilLongBreak(event.sessions)
            
            // Toggle updates
            is SettingsUiEvent.UpdateTheme -> updateTheme(event.isDarkTheme)
            is SettingsUiEvent.UpdateNotifications -> updateNotifications(event.isEnabled)
            is SettingsUiEvent.UpdateSound -> updateSound(event.isEnabled)
            is SettingsUiEvent.UpdateVibration -> updateVibration(event.isEnabled)
            is SettingsUiEvent.UpdateAutoStartBreaks -> updateAutoStartBreaks(event.isEnabled)
            is SettingsUiEvent.UpdateAutoStartWork -> updateAutoStartWork(event.isEnabled)
            
            // Actions
            is SettingsUiEvent.SaveSettings -> saveSettings()
            is SettingsUiEvent.ResetToDefaults -> showResetDialog()
            is SettingsUiEvent.ShowResetDialog -> showResetDialog()
            is SettingsUiEvent.DismissResetDialog -> dismissResetDialog()
            is SettingsUiEvent.ConfirmReset -> confirmReset()
            
            // Presets
            is SettingsUiEvent.ShowPresetDialog -> showPresetDialog()
            is SettingsUiEvent.DismissPresetDialog -> dismissPresetDialog()
            is SettingsUiEvent.ApplyPreset -> applyPreset(event.preset)
            
            // Validation
            is SettingsUiEvent.ShowValidationDialog -> showValidationDialog()
            is SettingsUiEvent.DismissValidationDialog -> dismissValidationDialog()
            
            // UI
            is SettingsUiEvent.ClearError -> clearError()
            is SettingsUiEvent.ClearSuccessMessage -> clearSuccessMessage()
            is SettingsUiEvent.DiscardChanges -> discardChanges()
        }
    }
    
    // ==================== DURATION UPDATES ====================
    
    /**
     * Update work duration
     */
    private fun updateWorkDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(
            tempWorkDuration = minutes,
            hasUnsavedChanges = true
        )
        validateSettings()
    }
    
    /**
     * Update short break duration
     */
    private fun updateShortBreakDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(
            tempShortBreakDuration = minutes,
            hasUnsavedChanges = true
        )
        validateSettings()
    }
    
    /**
     * Update long break duration
     */
    private fun updateLongBreakDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(
            tempLongBreakDuration = minutes,
            hasUnsavedChanges = true
        )
        validateSettings()
    }
    
    /**
     * Update sessions until long break
     */
    private fun updateSessionsUntilLongBreak(sessions: Int) {
        _uiState.value = _uiState.value.copy(
            tempSessionsUntilLongBreak = sessions,
            hasUnsavedChanges = true
        )
        validateSettings()
    }
    
    // ==================== TOGGLE UPDATES ====================
    
    /**
     * Update theme preference
     */
    private fun updateTheme(isDarkTheme: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateTheme(isDarkTheme)
                showSuccessMessage("Theme updated")
            } catch (e: Exception) {
                showError("Failed to update theme: ${e.message}")
            }
        }
    }
    
    /**
     * Update notification preference
     */
    private fun updateNotifications(isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateNotifications(isEnabled)
                showSuccessMessage("Notifications ${if (isEnabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                showError("Failed to update notifications: ${e.message}")
            }
        }
    }
    
    /**
     * Update sound preference
     */
    private fun updateSound(isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateSound(isEnabled)
                showSuccessMessage("Sound ${if (isEnabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                showError("Failed to update sound: ${e.message}")
            }
        }
    }
    
    /**
     * Update vibration preference
     */
    private fun updateVibration(isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateVibration(isEnabled)
                showSuccessMessage("Vibration ${if (isEnabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                showError("Failed to update vibration: ${e.message}")
            }
        }
    }
    
    /**
     * Update auto-start breaks preference
     */
    private fun updateAutoStartBreaks(isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateAutoStartBreaks(isEnabled)
                showSuccessMessage("Auto-start breaks ${if (isEnabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                showError("Failed to update auto-start breaks: ${e.message}")
            }
        }
    }
    
    /**
     * Update auto-start work preference
     */
    private fun updateAutoStartWork(isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateAutoStartWork(isEnabled)
                showSuccessMessage("Auto-start work ${if (isEnabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                showError("Failed to update auto-start work: ${e.message}")
            }
        }
    }
    
    // ==================== SETTINGS ACTIONS ====================
    
    /**
     * Save current settings
     */
    private fun saveSettings() {
        val currentState = _uiState.value
        
        if (!currentState.isValid) {
            showValidationDialog()
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(isSaving = true, error = null)
                
                settingsRepository.updateAllDurations(
                    workDuration = currentState.tempWorkDuration,
                    shortBreakDuration = currentState.tempShortBreakDuration,
                    longBreakDuration = currentState.tempLongBreakDuration,
                    sessionsUntilLongBreak = currentState.tempSessionsUntilLongBreak
                )
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hasUnsavedChanges = false,
                    successMessage = "Settings saved successfully"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save settings: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Show reset confirmation dialog
     */
    private fun showResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = true)
    }
    
    /**
     * Dismiss reset dialog
     */
    private fun dismissResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = false)
    }
    
    /**
     * Confirm reset to defaults
     */
    private fun confirmReset() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isSaving = true,
                    showResetDialog = false,
                    error = null
                )
                
                settingsRepository.resetToDefaults()
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hasUnsavedChanges = false,
                    successMessage = "Settings reset to defaults"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to reset settings: ${e.message}"
                )
            }
        }
    }
    
    // ==================== PRESET MANAGEMENT ====================
    
    /**
     * Show preset selection dialog
     */
    private fun showPresetDialog() {
        _uiState.value = _uiState.value.copy(showPresetDialog = true)
    }
    
    /**
     * Dismiss preset dialog
     */
    private fun dismissPresetDialog() {
        _uiState.value = _uiState.value.copy(showPresetDialog = false)
    }
    
    /**
     * Apply a settings preset
     */
    private fun applyPreset(preset: SettingsPreset) {
        if (preset == SettingsPreset.CUSTOM) {
            dismissPresetDialog()
            return
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isSaving = true,
                    showPresetDialog = false,
                    error = null
                )
                
                settingsRepository.applyCustomPreset(preset.settings)
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hasUnsavedChanges = false,
                    successMessage = "${preset.displayName} preset applied"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to apply preset: ${e.message}"
                )
            }
        }
    }
    
    // ==================== VALIDATION ====================
    
    /**
     * Validate current settings
     */
    private fun validateSettings() {
        val currentState = _uiState.value
        val tempSettings = currentState.settingsWithTempValues
        
        val errors = settingsRepository.getValidationErrors(tempSettings)
        
        _uiState.value = currentState.copy(
            validationErrors = errors
        )
    }
    
    /**
     * Show validation dialog
     */
    private fun showValidationDialog() {
        _uiState.value = _uiState.value.copy(showValidationDialog = true)
    }
    
    /**
     * Dismiss validation dialog
     */
    private fun dismissValidationDialog() {
        _uiState.value = _uiState.value.copy(showValidationDialog = false)
    }
    
    // ==================== UI STATE MANAGEMENT ====================
    
    /**
     * Clear error message
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Clear success message
     */
    private fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    /**
     * Show error message
     */
    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }
    
    /**
     * Show success message
     */
    private fun showSuccessMessage(message: String) {
        _uiState.value = _uiState.value.copy(successMessage = message)
    }
    
    /**
     * Discard unsaved changes
     */
    private fun discardChanges() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            tempWorkDuration = currentState.settings.workDurationMinutes,
            tempShortBreakDuration = currentState.settings.shortBreakDurationMinutes,
            tempLongBreakDuration = currentState.settings.longBreakDurationMinutes,
            tempSessionsUntilLongBreak = currentState.settings.sessionsUntilLongBreak,
            hasUnsavedChanges = false,
            validationErrors = emptyList()
        )
    }
    
    // ==================== PUBLIC UTILITY METHODS ====================
    
    /**
     * Check if settings have unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        return _uiState.value.hasUnsavedChanges
    }
    
    /**
     * Get current validation errors
     */
    fun getValidationErrors(): List<String> {
        return _uiState.value.validationErrors
    }
    
    /**
     * Force refresh settings
     */
    fun refreshSettings() {
        loadSettings()
    }
}
