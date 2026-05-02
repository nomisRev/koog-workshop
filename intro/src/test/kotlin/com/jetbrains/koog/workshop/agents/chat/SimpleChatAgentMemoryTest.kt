package com.jetbrains.koog.workshop.agents.chat

import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import dev.dokimos.core.JudgeLM
import dev.dokimos.core.conversation.ConversationTrajectory
import dev.dokimos.core.conversation.EvaluationCriterion
import dev.dokimos.core.conversation.Message.assistant
import dev.dokimos.core.conversation.Message.user
import dev.dokimos.kotlin.core.EvalTestCase
import dev.dokimos.kotlin.dsl.conversation.trajectoryEvaluator
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue

class SimpleChatAgentMemoryTest {

    private lateinit var apiKey: String
    private lateinit var judge: JudgeLM

    @Before
    fun setup() {
        val key = System.getenv("OPENAI_API_KEY") ?: ""
        Assume.assumeTrue("OPENAI_API_KEY is not set", key.isNotEmpty())
        apiKey = key
        val executor = simpleOpenAIExecutor(key)
        judge = JudgeLM { prompt ->
            runBlocking {
                AIAgent(
                    promptExecutor = executor,
                    llmModel = OpenAIModels.Chat.GPT4o,
                    maxIterations = 30
                ).run(prompt)
            }
        }
    }

    @Test
    fun `memory enabled - agent remembers favorite color`() {
        InMemoryChatHistoryProvider()
        val provider = SimpleChatAgentProvider { OpenAILLMClient(apiKey) to OpenAIModels.Chat.GPT4o }
        val agent = runBlocking {
            provider.provideAgent(
                historyProvider = InMemoryChatHistoryProvider(),
                onToolCallEvent = { _, _ -> },
                onLLMCallEvent = { _, _ -> },
                onErrorEvent = { },
                onExecutionTraceEvent = { },
            )
        }
        val sessionId = "test-memory-${"memory-enabled"}"
        val conversation = mutableListOf<Pair<String, String>>()
        val message1 = "My favorite color is green."
        val response1 = runBlocking { agent.run(message1, sessionId) }
        conversation.add("User" to message1)
        conversation.add("Assistant" to response1)
        System.out.println("[User] $message1")
        System.out.println("[Assistant] $response1")
        System.out.flush()
        val message2 = "What's my favorite color?"
        val response2 = runBlocking { agent.run(message2, sessionId) }
        conversation.add("User" to message2)
        conversation.add("Assistant" to response2)
        System.out.println("[User] $message2")
        System.out.println("[Assistant] $response2")
        System.out.flush()
        val trajectory = ConversationTrajectory(
            conversation.map { (role, content) ->
                if (role == "User") user(content) else assistant(content)
            },
            "Simple Chat Agent - Memory Test",
            emptyMap()
        )
        val colorRemembered = EvaluationCriterion(
            "Favorite Color Remembered",
            "The assistant correctly identifies 'green' as the user's favorite color when asked in the second message.",
            1.0
        )
        val result = trajectoryEvaluator(judge) {
            name = "Memory - Favorite Color (${"memory-enabled"})"
            threshold = 0.9
            criteria(listOf(colorRemembered))
        }.evaluate(EvalTestCase(actualOutputs = mapOf("trajectory" to trajectory)))
        println("memory-enabled score=${result.score()} passed=${result.success()} reason=${result.reason()}")
        assertTrue(result.success(), "Agent should remember the favorite color with memory enabled: ${result.reason()}")
    }

}
