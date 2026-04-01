package org.example.project.domain.chat

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import java.util.UUID

class ChatMemory {
    private val messages = mutableListOf<ChatMessage>()
    
    fun getMessages(): List<ChatMessage> = messages.toList()
    
    fun addMessage(content: String, role: MessageRole) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            role = role,
            timestamp = Clock.System.now()
        )
        messages.add(message)
    }
    
    suspend fun sendMessage(userMessage: String): ChatMessage {
        addMessage(userMessage, MessageRole.User)
        
        // Stub: simulate AI response delay
        delay(500)
        
        val response = generateStubResponse(userMessage)
        addMessage(response, MessageRole.Assistant)
        
        return messages.last()
    }
    
    private fun generateStubResponse(userMessage: String): String {
        return when {
            userMessage.contains("hello", ignoreCase = true) -> 
                "Hello! Welcome to Fantasy Store. How can I assist you today?"
            userMessage.contains("help", ignoreCase = true) -> 
                "I can help you with products, orders, and general inquiries about Fantasy Store."
            else -> 
                "I received your message: \"$userMessage\". This is a stub response."
        }
    }
}
