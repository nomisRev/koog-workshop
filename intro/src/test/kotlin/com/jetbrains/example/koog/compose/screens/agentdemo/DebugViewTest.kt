package com.jetbrains.example.koog.compose.screens.agentdemo

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugViewTest {
    @Test
    fun `off shows only user agent and system messages`() {
        assertVisible(
            debugView = DebugView.Off,
            visible = listOf(
                ChatMessageType.User,
                ChatMessageType.Agent,
                ChatMessageType.System
            ),
            hidden = listOf(
                ChatMessageType.Error,
                ChatMessageType.ToolCall,
                ChatMessageType.LlmCall
            )
        )
    }

    @Test
    fun `tools hides llm call messages only`() {
        assertVisible(
            debugView = DebugView.Tools,
            visible = listOf(
                ChatMessageType.User,
                ChatMessageType.Agent,
                ChatMessageType.System,
                ChatMessageType.Error,
                ChatMessageType.ToolCall
            ),
            hidden = listOf(ChatMessageType.LlmCall)
        )
    }

    @Test
    fun `full trace shows every message type`() {
        ChatMessageType.entries.forEach { type ->
            assertTrue(DebugView.FullTrace.shows(type), "Expected $type to be visible")
        }
    }

    private fun assertVisible(
        debugView: DebugView,
        visible: List<ChatMessageType>,
        hidden: List<ChatMessageType>
    ) {
        visible.forEach { type ->
            assertTrue(debugView.shows(type), "Expected $type to be visible for $debugView")
        }
        hidden.forEach { type ->
            assertFalse(debugView.shows(type), "Expected $type to be hidden for $debugView")
        }
    }
}
