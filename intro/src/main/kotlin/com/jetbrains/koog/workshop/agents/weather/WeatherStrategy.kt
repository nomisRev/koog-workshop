package com.jetbrains.koog.workshop.agents.weather

import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.prompt.message.Message

/**
 * Low-level implementation of the basic loop strategy by hand.
 */
fun basicSingleRunStrategyByHandSimplerVersion() = strategy("single-run-strategy-by-hand") {
    val callLLm: AIAgentNodeBase<Message.User, Message.Assistant> by nodeLLMRequest()
    val executeTools: AIAgentNodeBase<ToolCalls, Message.User> by nodeExecuteTools()

    edge(nodeStart forwardTo callLLm asUserMessage { it })
    edge(callLLm forwardTo executeTools onToolCalls { true })
    edge(callLLm forwardTo nodeFinish onTextMessage { true })
    edge(executeTools forwardTo callLLm)
}

/**
 * Low-level implementation of the basic loop strategy by hand.
 */
fun basicSingleRunStrategyByHand() = strategy("single-run-strategy-by-hand") {
    val nodeCallLLM: AIAgentNodeBase<Message.User, Message.Assistant> by nodeLLMRequest()
    val nodeExecuteTool: AIAgentNodeBase<ToolCalls, ReceivedToolResults> by nodeExecuteToolsAndGetResults()
    val nodeSendToolResult: AIAgentNodeBase<ReceivedToolResults, Message.Assistant> by nodeLLMSendToolResults()

    edge(nodeStart forwardTo nodeCallLLM asUserMessage { it })
    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCalls { true })
    edge(nodeCallLLM forwardTo nodeFinish onTextMessage { true })

    // Task: Complete the strategy implementation.
    edge(nodeExecuteTool forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeFinish onTextMessage { true })
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCalls { true })
}
