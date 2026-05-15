package org.example.project

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.features.persistence.jdbc.JdbcPersistenceStorageProvider
import ai.koog.agents.snapshot.providers.PersistenceUtils
import ai.koog.prompt.message.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.example.project.domain.chat.ChatService
import org.example.project.domain.chat.ChatUpdate
import org.example.project.domain.shared.CharacterId
import org.example.project.koog.ChatAgentProvider
import org.example.project.koog.tracking.AgentExecutionTraceEvent
import org.example.project.shared.ChatMessage
import org.example.project.shared.AgentState
import org.example.project.shared.ExecutionTraceItem
import org.example.project.shared.LlmCallData
import org.example.project.shared.LlmCallHistoryItem
import org.example.project.shared.LlmCallToolData
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Controller
class AgentController(
    private val provider: ChatAgentProvider,
    private val chat: ChatHistoryProvider,
    private val persistence: JdbcPersistenceStorageProvider,
    private val chatService: ChatService
) {
    private val logger = KotlinLogging.logger {}
    private val sseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json {
        classDiscriminator = "type"
        encodeDefaults = true
    }

    private fun SseEmitter.sendChatMessage(message: ChatMessage) {
        val data = json.encodeToString(ChatMessage.serializer(), message)
        send(data, MediaType.APPLICATION_JSON)
    }

    @PostMapping("chat")
    fun chat(
        @RequestParam characterId: String,
        @RequestParam(required = false) input: String?,
        @RequestParam sessionId: String,
    ): SseEmitter {
        val jobScope = CoroutineScope(Dispatchers.IO + sseScope.coroutineContext[Job]!!)
        val emitter = SseEmitter(Long.MAX_VALUE)

        logger.info { "Creating SseEmitter for session: $sessionId, character: $characterId" }

        // Create chat if needed
        val charId = CharacterId(Uuid.parse(characterId))
        chatService.updateChat(ChatUpdate(charId, sessionId))

        emitter.onCompletion {
            logger.info { "SseEmitter completed going to close agent scope: $sessionId" }
            runBlocking { jobScope.coroutineContext[Job]!!.cancelAndJoin() }
            logger.info { "SseEmitter completed for session: $sessionId" }
        }
        emitter.onTimeout {
            logger.warn { "SseEmitter timed out for session: $sessionId" }
            emitter.complete()
        }
        emitter.onError {
            logger.error(it) { "SseEmitter error for session: $sessionId: ${it.message}" }
        }

        val agent = provider.provideAgent(
            characterId = charId,
            sessionId = sessionId,
            historyProvider = chat,
            onToolCallEvent = { toolName, args ->
                emitter.sendChatMessage(ChatMessage.ToolCallMessage(toolName, args))
            },
            onLLMCallEvent = { messages, tools ->
                emitter.sendChatMessage(
                    ChatMessage.LLMCallMessage(
                        LlmCallData(
                            messageHistory = messages.toHistoryItems(),
                            availableTools = tools.toToolData()
                        )
                    )
                )
            },
            onErrorEvent = { error ->
                emitter.sendChatMessage(ChatMessage.ErrorMessage(error))
            },
            onExecutionTraceEvent = { trace ->
                emitter.sendChatMessage(
                    ChatMessage.ExecutionTraceMessage(
                        when (trace) {
                            is AgentExecutionTraceEvent.Node -> ExecutionTraceItem.Node(trace.name)
                            is AgentExecutionTraceEvent.Subgraph -> ExecutionTraceItem.Subgraph(trace.name)
                        }
                    )
                )
            },
            onAskMessage = { message ->
                emitter.sendChatMessage(ChatMessage.AskQuestion(message))
            },
            persistence = persistence
        )

        val heartbeatJob = jobScope.launch {
            try {
                while (isActive) {
                    delay(5.seconds)
                    emitter.sendChatMessage(ChatMessage.Heartbeat)
                }
            } catch (e: Exception) {
                logger.debug { "Heartbeat failed for session $sessionId: ${e.message}" }
            }
        }

        jobScope.launch {
            try {
                logger.info { "Running agent for session: $sessionId" }
                val response = agent.run(input ?: "", sessionId)
                logger.info { "Agent finished for session: $sessionId" }
                emitter.sendChatMessage(ChatMessage.AgentMessage(response))
            } catch (e: Exception) {
                logger.error(e) { "Error running agent for session: $sessionId" }
                emitter.sendChatMessage(ChatMessage.ErrorMessage(e.message ?: "Unknown error occurred"))
            } finally {
                heartbeatJob.cancelAndJoin()
                emitter.complete()
            }
        }

        return emitter
    }

    @PostMapping("chat/answer")
    suspend fun answerQuestion(
        @RequestParam characterId: String,
        @RequestParam sessionId: String,
        @RequestParam answer: String
    ) {
        val charId = CharacterId(Uuid.parse(characterId))
        logger.info { "Answering question for session: $sessionId, character: $characterId" }
        chatService.answerQuestion(charId, sessionId, answer)
    }

    @ResponseBody
    @GetMapping("chat/state")
    fun getAgentState(@RequestParam sessionId: String): AgentState =
        when (persistence.getLatestCheckpointBlocking(sessionId)?.nodePath) {
            null -> AgentState.None
            PersistenceUtils.TOMBSTONE_CHECKPOINT_NAME -> AgentState.Completed
            else -> AgentState.Failed
        }
}

fun List<Message>.toHistoryItems(): List<LlmCallHistoryItem> =
    map { message ->
        when (message) {
            is Message.System -> LlmCallHistoryItem.System(message.content)
            is Message.User -> LlmCallHistoryItem.User(message.content)
            is Message.Assistant -> LlmCallHistoryItem.Assistant(message.content)
            is Message.Reasoning -> LlmCallHistoryItem.Reasoning(message.content)
            is Message.Tool.Call -> LlmCallHistoryItem.ToolCall(message.tool, message.content)
            is Message.Tool.Result -> LlmCallHistoryItem.ToolResult(message.tool, message.content)
        }
    }

fun List<ToolDescriptor>.toToolData(): List<LlmCallToolData> =
    map { tool ->
        LlmCallToolData(
            name = tool.name,
            requiredParameters = tool.requiredParameters.map { it.name },
            optionalParameters = tool.optionalParameters.map { it.name }
        )
    }
