package org.example.project.domain.chat

import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.example.project.domain.shared.CharacterId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

@Repository
@Transactional
class AskQuestionRepository {
    typealias Key = Pair<CharacterId, String>
    typealias State = ConcurrentMap<Key, CompletableDeferred<String>>

    @OptIn(ExperimentalAtomicApi::class)
    private val state: State = ConcurrentMap()

    fun askQuestion(
        characterId: CharacterId,
        sessionId: String,
        message: String,
        onAskMessage: (String) -> Unit
    ): Deferred<String> {
        val existing = getQuestion(characterId, sessionId)
        if (existing == null || existing.answer == null) {
            upsertQuestion(characterId, sessionId, message)
            val updated = getQuestion(characterId, sessionId)
            if (updated != null && !updated.isAsked) {
                onAskMessage(message) // message is send over SSE
                markAsAsked(characterId, sessionId)
            }
        }

        val current = getQuestion(characterId, sessionId)
        if (current?.answer != null) return CompletableDeferred(current.answer)

        return subscribeToAnswer(characterId, sessionId)
    }

    fun getQuestion(characterId: CharacterId, sessionId: String): AskQuestionRecord? =
        AskQuestions.selectAll()
            .where { (AskQuestions.character eq characterId.value) and (AskQuestions.sessionId eq sessionId) }
            .map {
                AskQuestionRecord(
                    characterId = CharacterId(it[AskQuestions.character].value),
                    sessionId = it[AskQuestions.sessionId],
                    question = it[AskQuestions.question],
                    answer = it[AskQuestions.answer],
                    isAsked = it[AskQuestions.isAsked]
                )
            }
            .singleOrNull()

    fun upsertQuestion(characterId: CharacterId, sessionId: String, question: String) {
        val existing = getQuestion(characterId, sessionId)
        if (existing == null) {
            AskQuestions.insert {
                it[character] = characterId.value
                it[this.sessionId] = sessionId
                it[this.question] = question
                it[isAsked] = false
            }
        } else {
            AskQuestions.update({ (AskQuestions.character eq characterId.value) and (AskQuestions.sessionId eq sessionId) }) {
                it[this.question] = question
            }
        }
    }

    fun markAsAsked(characterId: CharacterId, sessionId: String) =
        AskQuestions.update({ (AskQuestions.character eq characterId.value) and (AskQuestions.sessionId eq sessionId) }) {
            it[isAsked] = true
        }

    fun answerQuestion(characterId: CharacterId, sessionId: String, answer: String) {
        AskQuestions.update({ (AskQuestions.character eq characterId.value) and (AskQuestions.sessionId eq sessionId) }) {
            it[this.answer] = answer
        }
        val deferred = requireNotNull(
            state.remove(
                Pair(
                    characterId,
                    sessionId
                )
            )
        ) { "Session not found for character $characterId and session $sessionId" }
        deferred.complete(answer)
    }

    fun subscribeToAnswer(characterId: CharacterId, sessionId: String): CompletableDeferred<String> =
        state.computeIfAbsent(Pair(characterId, sessionId)) { CompletableDeferred() }
}

data class AskQuestionRecord(
    val characterId: CharacterId,
    val sessionId: String,
    val question: String,
    val answer: String?,
    val isAsked: Boolean
)
