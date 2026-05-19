package com.jetbrains.koog.workshop.agents.util

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart

fun Message.Assistant.textContent() = parts.filterIsInstance<MessagePart.Text>().joinToString(separator = "\n") { it.text }