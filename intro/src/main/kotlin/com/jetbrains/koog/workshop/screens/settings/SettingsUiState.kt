package com.jetbrains.koog.workshop.screens.settings

import com.jetbrains.koog.workshop.settings.AppearanceMode
import com.jetbrains.koog.workshop.settings.SelectedOption

data class SettingsUiState(
    val openAiToken: String = "",
    val anthropicToken: String = "",
    val geminiToken: String = "",
    val selectedOption: SelectedOption = SelectedOption.Anthropic,
    val appearanceMode: AppearanceMode = AppearanceMode.Auto,
    val isLoading: Boolean = true,
)
