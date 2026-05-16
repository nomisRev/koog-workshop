package com.jetbrains.koog.workshop.settings

object ApiKeyService {
    private const val KOOG_WORKSHOP_ANTHROPIC_API_KEY = "KOOG_WORKSHOP_ANTHROPIC_API_KEY"

    val anthropicApiKey: String
        get() = System.getenv(KOOG_WORKSHOP_ANTHROPIC_API_KEY)
            ?: throw IllegalArgumentException("$KOOG_WORKSHOP_ANTHROPIC_API_KEY env is not set")
}