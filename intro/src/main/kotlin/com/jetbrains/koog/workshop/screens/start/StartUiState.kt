package com.jetbrains.koog.workshop.screens.start

import com.jetbrains.koog.workshop.NavRoute

data class StartUiState(
    val demoCards: List<CardItem> = listOf(
        CardItem(
            title = "Weather Forecast",
            description = "Get forecasts, conditions, and weather insights for any location.",
            imageRes = "images/weatherAgent.png",
            agentDemoRoute = NavRoute.AgentDemoRoute.WeatherScreen
        ),
        CardItem(
            title = "Home Services",
            description = "Schedule plumbing, electrical, HVAC, or handyman service.",
            imageRes = "images/homeServicesAgent.png",
            agentDemoRoute = NavRoute.AgentDemoRoute.HomeServicesScreen
        ),
    )
)

data class CardItem(
    val title: String,
    val description: String,
    val imageRes: String? = null,
    val agentDemoRoute: NavRoute.AgentDemoRoute? = null,
)
