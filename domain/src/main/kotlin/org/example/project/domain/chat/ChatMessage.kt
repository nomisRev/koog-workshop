package org.example.project.domain.chat

import kotlinx.datetime.Instant

data class ChatMessage(
    val id: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Instant
)

enum class MessageRole {
    User,
    Assistant
}
