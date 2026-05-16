package com.jetbrains.koog.workshop.screens.settings

import com.jetbrains.koog.workshop.settings.AppearanceMode

data class SettingsUiState(
    val appearanceMode: AppearanceMode = AppearanceMode.Auto,
    val isLoading: Boolean = true,
)
