package org.example.project.domain.chat

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.features.persistence.jdbc.JdbcPersistenceStorageProvider
import ai.koog.prompt.message.Message

import org.example.project.domain.shared.CharacterId
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class ChatService(
    private val chatHistoryProvider: ChatHistoryProvider,
    private val chatRepository: ChatRepository,
    private val persistenceStorageProvider: JdbcPersistenceStorageProvider,
    private val askQuestionRepository: AskQuestionRepository
) {
    /**
     * Fetch all chats for the current user with the chat history
     */
    suspend fun getCharacterChatDetails(characterId: CharacterId): List<ChatDetails> {
        val chats = chatRepository.getCharacterChats(characterId)

        return chats.map {
            ChatDetails(
                characterId = it.characterId,
                conversationId = it.conversationId,
                messages = getChatHistory(it.conversationId)
            )
        }
    }

    /**
     * Create new character <-> conversation relationship or notify that it was updated (e.g. new messages)
     */
    fun updateChat(update: ChatUpdate) =
        chatRepository.updateChat(update)

    suspend fun getChatHistory(conversationId: String): List<Message> =
        chatHistoryProvider.load(conversationId)
            .ifEmpty {
                persistenceStorageProvider.getLatestCheckpoint(conversationId)?.messageHistory.orEmpty()
            }

    fun answerQuestion(characterId: CharacterId, sessionId: String, answer: String) =
        askQuestionRepository.answerQuestion(characterId, sessionId, answer)
}