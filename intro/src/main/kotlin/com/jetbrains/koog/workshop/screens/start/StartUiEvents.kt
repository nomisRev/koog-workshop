package com.jetbrains.koog.workshop.screens.start

import com.jetbrains.koog.workshop.NavRoute

sealed interface StartUiEvents {
    data object Settings : StartUiEvents
    data class AgentDemo(val agentDemoRoute: NavRoute.AgentDemoRoute) : StartUiEvents
}
