package org.example.project.chat

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import org.example.project.domain.order.OrderService
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.tools.AskQuestionTool
import org.example.project.koog.orderCustomerSupportStrategy
import org.example.project.koog.tools.CustomerSupportTools
import org.example.project.koog.tools.ReadOrderTools
import org.example.project.koog.tools.UpdateOrderTools
import kotlin.uuid.Uuid

class ChatAgent(
    private val executor: PromptExecutor,
    private val orderService: OrderService,
    private val chatHistoryProvider: ChatHistoryProvider,
) {
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
                this.chatHistoryProvider = this@ChatAgent.chatHistoryProvider
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
                this.chatHistoryProvider = this@ChatAgent.chatHistoryProvider
                windowSize(50)
            }
            install(Tracing) {
                addMessageProcessor(TraceFeatureMessageLogWriter(KotlinLogging.logger { }))
            }
        }

        ChatUi.Message.CustomerSupport(agent.run(userMessage, sessionId.toString()))
    }
}