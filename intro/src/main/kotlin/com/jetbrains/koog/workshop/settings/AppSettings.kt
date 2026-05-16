package com.jetbrains.koog.workshop.settings

import kotlinx.coroutines.flow.StateFlow

interface AppSettings {
    val appearanceModeFlow: StateFlow<AppearanceMode>
    suspend fun getCurrentSettings(): AppSettingsData
    suspend fun setCurrentSettings(settings: AppSettingsData)
    suspend fun updateAppearanceMode(mode: AppearanceMode)
}

data class AppSettingsData(
    val appearanceMode: AppearanceMode = AppearanceMode.Auto,
)

enum class AppearanceMode(val label: String) {
    Auto("Auto"),
    Light("Light"),
    Dark("Dark"),
}
