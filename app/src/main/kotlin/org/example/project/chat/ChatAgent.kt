package org.example.project.chat

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.project.domain.order.OrderService
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.tools.AskQuestionTool
import org.example.project.koog.orderCustomerSupportStrategy
import org.example.project.koog.tools.CustomerSupportTools
import org.example.project.koog.tools.ReadOrderTools
import org.example.project.koog.tools.UpdateOrderTools
import kotlin.uuid.Uuid

@Serializable
private data class ToolMessage(val message: String)

class ChatAgent(
    private val executor: PromptExecutor,
    private val orderService: OrderService,
    val history: ChatHistoryProvider,
) {
    suspend fun loadChat(sessionId: Uuid): PersistentList<ChatUi.Message> {
        val history = history.load(sessionId.toString())
        return history.mapNotNull { message ->
            when (message) {
                is Message.User -> ChatUi.Message.User(message.content)
                is Message.Assistant -> ChatUi.Message.CustomerSupport(message.content)
                is Message.Reasoning -> ChatUi.Message.CustomerSupport(message.content)
                is Message.System -> ChatUi.Message.CustomerSupport(message.content)

                is Message.Tool.Call if message.tool == "askQuestion" -> {
                    val message = Json.decodeFromString<ToolMessage>(message.parts.single().text).message
                    ChatUi.Message.CustomerSupport(message)
                }

                is Message.Tool.Result if message.tool == "askQuestion" ->
                    ChatUi.Message.User(Json.decodeFromString(String.serializer(), message.content))

                is Message.Tool.Result,
                is Message.Tool.Call -> null
            }
        }.toPersistentList()
    }

    suspend fun sendMessage(
        characterId: CharacterId?,
        sessionId: Uuid,
        userMessage: String,
        askQuestion: suspend (message: String) -> String
    ): ChatUi.Message.CustomerSupport = if (characterId == null) {
        val agent = AIAgent(
            promptExecutor = executor,
            systemPrompt = """
                | You are a helpful Fantasy Store assistant. Help customers with products, orders, and general inquiries.
                | Use the askQuestion in case you're unsure or there is any missing data for solve the issue.
            """.trimMargin(),
            llmModel = OpenAIModels.Chat.GPT5_4,
            toolRegistry = ToolRegistry {
                tools(AskQuestionTool(askQuestion))

            }
        ) {
            install(ChatMemory) {
                chatHistoryProvider = history
                windowSize(50)
            }
            install(Tracing) {
                addMessageProcessor(TraceFeatureMessageLogWriter(KotlinLogging.logger { }))
            }
        }

        ChatUi.Message.CustomerSupport(agent.run(userMessage, sessionId.toString()))
    } else {
        val tools = CustomerSupportTools(
            askQuestionTool = AskQuestionTool(askQuestion),
            readOrderTools = ReadOrderTools(characterId, orderService),
            updateOrderTools = UpdateOrderTools(characterId, orderService)
        )
        val agent = AIAgent(
            promptExecutor = executor,
            strategy = orderCustomerSupportStrategy(tools),
            systemPrompt = """
                | You are a helpful Fantasy Store assistant. Help customers with products, orders, and general inquiries.
                | Use the askQuestion in case you're unsure or there is any missing data for solve the issue.
            """.trimMargin(),
            llmModel = OpenAIModels.Chat.GPT5_4,
            toolRegistry = ToolRegistry {
                tools(tools.askQuestionTool)
                tools(tools.readOrderTools)
                tools(tools.updateOrderTools)
            }
        ) {
            install(ChatMemory) {
                chatHistoryProvider = history
                windowSize(50)
            }
            install(Tracing) {
                addMessageProcessor(TraceFeatureMessageLogWriter(KotlinLogging.logger { }))
            }
        }

        ChatUi.Message.CustomerSupport(agent.run(userMessage, sessionId.toString()))
    }
}