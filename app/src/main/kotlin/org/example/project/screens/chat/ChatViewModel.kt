package org.example.project.screens.chat

import ai.koog.prompt.message.Message
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.example.project.domain.character.Character
import org.example.project.domain.chat.ChatUpdate
import org.example.project.shared.AgentState
import org.example.project.shared.ChatMessage
import kotlin.math.log
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@Serializable
private data class ToolMessage(val message: String)

class ChatViewModel(
    private val character: Character,
    initialConversationId: String,
    initialMessages: List<Message>?,
    private val chatService: ChatService,
    httpClient: HttpClient,
    private val onNavigateBack: () -> Unit,
) : ViewModel() {
    private val logger = LoggerFactory.getLogger(ChatViewModel::class.java)
    private val httpClient = httpClient.config {
        install(SSE)
        install(HttpTimeout) {
            // Entire SSE request should not time out
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            // Server sends heartbeat every 5 seconds
            socketTimeoutMillis = 30.seconds.inWholeMilliseconds
            connectTimeoutMillis = 60.seconds.inWholeMilliseconds
        }
    }
    val uiState: StateFlow<ChatUiState>
        field = MutableStateFlow(
            ChatUiState(
                title = character.name,
                chatMessages = buildList {
                    add(ChatMessage.SystemMessage("Hi ${character.name}! I'm the Fantasy Store assistant. How can I help?"))
                    initialMessages?.mapNotNull(::toChatMessage)?.let { addAll(it) }
                }
            )
        )

    private var sessionId: String = initialConversationId
    private var sseJob: Job? = null

    fun resumeIfRunning() {
        viewModelScope.launch {
            try {
                val state = chatService.getAgentState(sessionId)
                logger.info("Found state: $state")
                when (chatService.getAgentState(sessionId)) {
                    AgentState.None -> logger.info("No existing agent found!")
                    AgentState.Completed -> logger.info("Existing agent found but completed successfully!")
                    AgentState.Failed -> {
                        val lastMessage = uiState.value.chatMessages.lastOrNull()
                        logger.info("Agent is running for sessionId: $sessionId, resuming... askQuestion: $lastMessage")
                        uiState.update { it.copy(isLoading = true, isInputEnabled = true) }
                        runAgent("")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error checking agent state for sessionId: $sessionId", e)
            }
        }
    }

    fun onEvent(event: ChatUiEvents) {
        viewModelScope.launch {
            when (event) {
                is ChatUiEvents.UpdateInputText -> updateInputText(event.text)
                is ChatUiEvents.ToggleDebugEnabled -> toggleDebugEnabled()
                is ChatUiEvents.ToggleDebugOption -> toggleDebugOption(event.option)
                ChatUiEvents.SendMessage -> sendMessage()
                ChatUiEvents.RestartChat -> restartChat()
                ChatUiEvents.NavigateBack -> onNavigateBack()
            }
        }
    }

    private fun toChatMessage(message: Message): ChatMessage? = when (message) {
        is Message.User -> ChatMessage.UserMessage(message.content)
        is Message.Assistant -> ChatMessage.AgentMessage(message.content)
        is Message.Reasoning -> null
        is Message.System -> null

        is Message.Tool.Call if message.tool == "askQuestion" -> {
            val text = Json.decodeFromString<ToolMessage>(message.parts.single().text).message
            ChatMessage.AgentMessage(text)
        }

        is Message.Tool.Result if message.tool == "askQuestion" ->
            ChatMessage.UserMessage(Json.decodeFromString(String.serializer(), message.content))

        is Message.Tool.Call -> ChatMessage.ToolCallMessage(
            toolName = message.tool,
            args = mapOf("args" to message.content)
        )

        is Message.Tool.Result -> null
    }

    override fun onCleared() {
        super.onCleared()
        closeSession()
    }

    fun closeSession() {
        logger.info("Closing ChatViewModel session and cancelling SSE job for sessionId: $sessionId")
        sseJob?.cancel()
    }

    private fun updateInputText(text: String) {
        uiState.update { it.copy(inputText = text) }
    }

    private fun toggleDebugEnabled() {
        uiState.update {
            it.copy(debugView = it.debugView.copy(enabled = !it.debugView.enabled))
        }
    }

    private fun toggleDebugOption(option: DebugOption) {
        uiState.update {
            val current = it.debugView
            val newOptions = if (option in current.options) current.options - option else current.options + option
            it.copy(debugView = current.copy(options = newOptions))
        }
    }

    private fun sendMessage() {
        val userInput = uiState.value.inputText.trim()
        if (userInput.isEmpty()) return

        logger.info("Sending message for sessionId: $sessionId, length: ${userInput.length}")

        if (uiState.value.userResponseRequested) {
            uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.UserMessage(userInput),
                    inputText = "",
                    isInputEnabled = false,
                    isLoading = true,
                    userResponseRequested = false,
                    currentUserResponse = userInput,
                )
            }
            viewModelScope.launch {
                try {
                    logger.debug("Answering question for sessionId: $sessionId")
                    chatService.answerQuestion(character.id, sessionId, userInput)
                } catch (e: Exception) {
                    logger.error("Error sending answer for sessionId: $sessionId", e)
                    uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + ChatMessage.ErrorMessage("Error sending answer: ${e.message}"),
                            isInputEnabled = true,
                            isLoading = false,
                        )
                    }
                }
            }
        } else {
            uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage.UserMessage(userInput),
                    inputText = "",
                    isInputEnabled = false,
                    isLoading = true,
                )
            }

            runAgent(userInput)
        }
    }

    private fun runAgent(userInput: String) {
        logger.info("Starting agent run for sessionId: $sessionId")
        sseJob?.cancel()
        sseJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                logger.debug("Connecting to SSE at http://localhost:8080/chat for sessionId: $sessionId")
                httpClient.sse(
                    urlString = "http://localhost:8080/chat",
                    request = {
                        url {
                            parameter("characterId", character.id.value.toString())
                            parameter("input", userInput.ifBlank { null })
                            parameter("sessionId", sessionId)
                        }
                        method = HttpMethod.Post
                    }
                ) {
                    incoming
                        .mapNotNull { it.data }
                        .onEach { logger.debug("Received SSE event data: {}", it) }
                        .map { Json.decodeFromString<ChatMessage>(it) }
                        .onEach { logger.debug("Decoded ChatMessage: {}", it) }
                        .filterNot { it is ChatMessage.Heartbeat }
                        .collect { message ->
                            logger.info("Processing message: {}", message::class.simpleName)
                            uiState.update { state ->
                                val isAgentMessage =
                                    message is ChatMessage.AgentMessage || message is ChatMessage.ErrorMessage
                                val isAskQuestion =
                                    message is ChatMessage.AskQuestion || (message is ChatMessage.ToolCallMessage && message.toolName == "askQuestion")
                                state.copy(
                                    chatMessages = state.chatMessages + message,
                                    isInputEnabled = isAgentMessage || isAskQuestion,
                                    isLoading = !isAgentMessage && !isAskQuestion,
                                    userResponseRequested = isAskQuestion
                                )
                            }
                        }
                }
                // Write that a new chat was created
                logger.debug("Updating chat service for sessionId: $sessionId")
                chatService.updateChat(ChatUpdate(character.id, sessionId))
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                logger.error("SSE error for sessionId: $sessionId", e)
                uiState.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage.ErrorMessage("Error: ${e.message}"),
                        isInputEnabled = true,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun restartChat() {
        logger.info("Restarting chat, closing current session: $sessionId")
        sseJob?.cancel()
        sessionId = Uuid.random().toString()
        logger.info("New sessionId: $sessionId")
        uiState.update {
            ChatUiState(
                title = character.name,
                chatMessages = listOf(
                    ChatMessage.SystemMessage("Hi ${character.name}! I'm the Fantasy Store assistant. How can I help?")
                )
            )
        }
    }

    companion object {
        fun factory(
            character: Character,
            conversationId: String,
            initialMessages: List<Message>?,
            chatService: ChatService,
            httpClient: HttpClient,
            onNavigateBack: () -> Unit,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    return ChatViewModel(
                        character = character,
                        initialConversationId = conversationId,
                        initialMessages = initialMessages,
                        chatService = chatService,
                        httpClient = httpClient,
                        onNavigateBack = onNavigateBack,
                    ) as T
                }
            }
    }
}
