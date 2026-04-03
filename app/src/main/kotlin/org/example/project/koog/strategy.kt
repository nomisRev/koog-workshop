package org.example.project.koog

import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.ModeratedMessage
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolFromCallable
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.ext.agent.CriticResult
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.agent.subgraphWithVerification
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.serialization.Serializable
import org.example.project.domain.order.OrderService
import org.example.project.domain.order.OrderStatus
import kotlin.uuid.Uuid
import org.example.project.domain.shared.CharacterId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId
import org.example.project.koog.tools.AskQuestionTool
import org.example.project.koog.tools.CustomerSupportTools
import org.example.project.koog.tools.ReadOrderTools
import org.example.project.koog.tools.UpdateOrderTools
import org.example.project.koog.tools.plus
import kotlin.time.Instant

@Serializable
data class OrderDetails(
    val orderId: String,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class IssueSolution(val actionsTaken: String)

fun orderCustomerSupportStrategy(tools: CustomerSupportTools) = strategy<String, String>("order-customer-support") {
    val moderate by nodeLLMModerateString(
        moderatingModel = OpenAIModels.Moderation.Omni
    )

    val summarize by subgraphWithTask<String, OrderDetails>(tools = tools.askQuestionTool + tools.readOrderTools) { input ->
        """
            $input
        """.trimIndent()
    }

    val resolve by subgraphWithTask<OrderDetails, IssueSolution>(tools = tools.readOrderTools + tools.updateOrderTools) { details ->
        """
            Resolve the problem ${agentInput<String>()} given the details: $details
            Tell us what actions you took to fix the problem when you're done.
        """.trimIndent()
    }

    val verify by subgraphWithVerification<IssueSolution>(tools = tools.readOrderTools + tools.askQuestionTool) { solution ->
        "Verify if $solution fixed the problem. Also verify its actually fixed by checking the system state if necessary."
    }

    val adjust by subgraphWithTask<CriticResult<IssueSolution>, IssueSolution>(tools = tools.readOrderTools + tools.updateOrderTools) { critique ->
        """
            We need to solve the user problem: ${agentInput<String>()}.
            Following steps didn't solve the problem: ${critique.input}
            These problems still presists: ${critique.feedback}
        """.trimIndent()
    }

    val describe by subgraphWithTask<IssueSolution, String>(tools = emptyList()) { solution ->
        """
            Provide a nice and friendly summarizing message for the user how we solved their problem.
            Problem: ${agentInput<String>()}
            Actions Taken: ${solution.actionsTaken}
        """.trimIndent()
    }

    nodeStart then moderate
    edge(moderate forwardTo summarize onCondition { !it.moderationResult.isHarmful } transformed { it.message.content })
    edge(
        moderate forwardTo nodeFinish
                onCondition { it.moderationResult.isHarmful }
                transformed { "We cannot help you with this kind-of requests." }
    )
    summarize then resolve then verify
    edge(verify forwardTo describe onCondition { it.successful } transformed { it.input })
    edge(verify forwardTo adjust onCondition { !it.successful })
    adjust then verify
    describe then nodeFinish
}

// Avoids String -> Message.User wrapping
fun nodeLLMModerateString(
    name: String? = null,
    moderatingModel: LLModel? = null,
    includeCurrentPrompt: Boolean = false,
): AIAgentNodeDelegate<String, ModeratedMessage> =
    node<String, ModeratedMessage>(name) { prompt ->
        val message = Message.User(prompt, RequestMetaInfo.Empty)
        val moderationPrompt = if (includeCurrentPrompt) {
            prompt(llm.prompt) { message(message) }
        } else {
            prompt("single-message-moderation") { message(message) }
        }

        val moderationResult = llm.promptExecutor.moderate(moderationPrompt, moderatingModel ?: llm.model)

        ModeratedMessage(message, moderationResult)
    }
