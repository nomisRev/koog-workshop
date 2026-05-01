package com.jetbrains.koog.workshop.screens.settings

import com.jetbrains.koog.workshop.settings.SelectedOption

// State for the UI
data class SettingsUiState(
    val openAiToken: String = "",
    val anthropicToken: String = "",
    val geminiToken: String = "",
    val selectedOption: SelectedOption = SelectedOption.OpenAI,
    val isLoading: Boolean = true
)
