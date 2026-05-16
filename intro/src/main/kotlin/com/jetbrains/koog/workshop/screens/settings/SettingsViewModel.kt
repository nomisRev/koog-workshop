package com.jetbrains.koog.workshop.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.koog.workshop.settings.AppSettings
import com.jetbrains.koog.workshop.settings.AppSettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val navigationCallback: SettingsNavigationCallback,
    private val appSettings: AppSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun onEvent(event: SettingsUiEvents) {
        viewModelScope.launch {
            when (event) {
                is SettingsUiEvents.UpdateAppearance -> {
                    _uiState.value = _uiState.value.copy(appearanceMode = event.appearanceMode)
                    appSettings.updateAppearanceMode(event.appearanceMode)
                }
                SettingsUiEvents.NavigateBack -> navigationCallback.goBack()
                SettingsUiEvents.SaveSettings -> {
                    saveSettings()
                    navigationCallback.goBack()
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = appSettings.getCurrentSettings()
            _uiState.value = SettingsUiState(
                appearanceMode = settings.appearanceMode,
                isLoading = false,
            )
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            appSettings.setCurrentSettings(
                AppSettingsData(appearanceMode = _uiState.value.appearanceMode)
            )
        }
    }
}
