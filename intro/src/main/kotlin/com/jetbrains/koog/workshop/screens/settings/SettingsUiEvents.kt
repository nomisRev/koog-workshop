package com.jetbrains.koog.workshop.screens.settings

import com.jetbrains.koog.workshop.settings.AppearanceMode

sealed interface SettingsUiEvents {
    data object NavigateBack : SettingsUiEvents
    data object SaveSettings : SettingsUiEvents
    data class UpdateAppearance(val appearanceMode: AppearanceMode) : SettingsUiEvents
}
