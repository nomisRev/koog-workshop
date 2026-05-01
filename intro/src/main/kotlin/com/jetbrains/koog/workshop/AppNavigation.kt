package com.jetbrains.koog.workshop

import androidx.navigation3.runtime.NavKey
import com.jetbrains.koog.workshop.screens.agentdemo.AgentDemoNavigationCallback
import com.jetbrains.koog.workshop.screens.settings.SettingsNavigationCallback
import com.jetbrains.koog.workshop.screens.start.StartNavigationCallback

internal class AppNavigation(private val backStack: MutableList<NavKey>) :
    AgentDemoNavigationCallback,
    SettingsNavigationCallback,
    StartNavigationCallback {

    override fun goBack() {
        backStack.removeLastOrNull()
    }

    override fun goSettings() {
        backStack.add(NavRoute.SettingsScreen)
    }

    override fun goAgentDemo(agentDemoRoute: NavRoute.AgentDemoRoute) {
        backStack.add(agentDemoRoute)
    }
}
