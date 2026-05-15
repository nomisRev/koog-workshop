package org.example.project.koog.tracking

import ai.koog.agents.features.eventHandler.feature.EventHandlerConfig
import kotlinx.serialization.json.Json
import org.example.project.shared.ChatMessage
import org.example.project.shared.ExecutionTraceItem
import org.example.project.shared.LlmCallData
import org.example.project.toHistoryItems
import org.example.project.toToolData
import org.springframework.http.MediaType
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseEmitterEventHandler(private val emitter: SseEmitter) {
    val config: EventHandlerConfig.() -> Unit = {
        onNodeExecutionStarting { ctx ->
            emitter.sendChatMessage(ChatMessage.ExecutionTraceMessage(ExecutionTraceItem.Node(ctx.node.name)))
        }
        onSubgraphExecutionStarting { ctx ->
            emitter.sendChatMessage(ChatMessage.ExecutionTraceMessage(ExecutionTraceItem.Subgraph(ctx.subgraph.name)))
        }
        onToolCallStarting { ctx ->
            emitter.sendChatMessage(
                ChatMessage.ToolCallMessage(
                    ctx.toolName,
                    ctx.toolArgs.entries.mapValues { it.value.toString() })
            )
        }
        onAgentExecutionFailed { ctx ->
            emitter.sendChatMessage(ChatMessage.ErrorMessage(ctx.error.message ?: ctx.error.toString()))
        }
        onLLMCallStarting { ctx ->
            emitter.sendChatMessage(
                ChatMessage.LLMCallMessage(
                    LlmCallData(
                        messageHistory = ctx.prompt.messages.toHistoryItems(),
                        availableTools = ctx.tools.toToolData()
                    )
                )
            )
        }
    }
}

fun SseEmitter.sendChatMessage(message: ChatMessage) {
    val data = Json.encodeToString(ChatMessage.serializer(), message)
    send(data, MediaType.APPLICATION_JSON)
}