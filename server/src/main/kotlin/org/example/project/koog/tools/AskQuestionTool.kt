package org.example.project.koog.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.domain.chat.AskQuestionRepository
import org.example.project.domain.shared.CharacterId

// FIXME similar to "intro", the class name is confusing since it doesn't define a tool, but a set of tools.
//  Rename to smth like "CommunicationTools" or convert to a class-based tool.
class AskQuestionTool(
    private val characterId: CharacterId,
    private val sessionId: String,
    private val repository: AskQuestionRepository,
    private val onAskMessage: (String) -> Unit
) : ToolSet {
    @Tool
    @LLMDescription("Ask a question to customer of the Fantasy Store assistant.")
    suspend fun askQuestion(message: String): String = withContext(Dispatchers.IO) {
        repository.askQuestion(characterId, sessionId, message, onAskMessage).await()
    }
}