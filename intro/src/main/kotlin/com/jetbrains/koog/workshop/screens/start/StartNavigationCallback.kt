package com.jetbrains.koog.workshop.screens.start

import com.jetbrains.koog.workshop.NavRoute

interface StartNavigationCallback {
    fun goSettings()
    fun goAgentDemo(agentDemoRoute: NavRoute.AgentDemoRoute)
}
