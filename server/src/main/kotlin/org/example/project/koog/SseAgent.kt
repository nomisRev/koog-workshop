package org.example.project.koog

import ai.koog.agents.core.agent.AIAgent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.koog.tracking.sendChatMessage
import org.example.project.shared.ChatMessage
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Utility that uses KotlinX Coroutines for automatic sse heartbeat.
 */
fun CoroutineScope.sse(
    heartbeat: Duration = 5.seconds,
    block: suspend (emitter: SseEmitter) -> Unit
): SseEmitter {
    val job = Job(coroutineContext[Job])
    val scope = CoroutineScope(Dispatchers.IO + job)
    val heartbeatJob = Job(job)
    val heartbeatScope = CoroutineScope(Dispatchers.IO + heartbeatJob)

    val emitter = SseEmitter(Long.MAX_VALUE).apply {
        onCompletion {
            logger.info { "SseEmitter completed" }
            heartbeatJob.cancel()
        }
        onTimeout { logger.warn { "SseEmitter timed out" } }
        onError { logger.error(it) { "SseEmitter error: ${it.message}" } }
    }

    heartbeatScope.launch {
        while (true) {
            delay(heartbeat)
            emitter.sendChatMessage(ChatMessage.Heartbeat)
        }
    }

    scope.launch {
        try {
            block(emitter)
        } finally {
            heartbeatJob.cancelAndJoin()
            emitter.complete()
        }
    }
    return emitter
}