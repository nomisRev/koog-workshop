package org.example.project.domain.repository

import org.example.project.domain.enums.TransactionType
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.CurrencyId
import org.example.project.domain.id.TransactionId
import org.example.project.domain.model.Character
import kotlin.uuid.Uuid

interface CharacterRepository {
    suspend fun getCharacterOrNull(id: CharacterId): Character?
    suspend fun createCharacter(name: String): CharacterId
    suspend fun getWalletBalance(characterId: CharacterId): Map<CurrencyId, Long>
    suspend fun addTransaction(
        characterId: CharacterId,
        currencyId: CurrencyId,
        amount: Long,
        type: TransactionType,
        referenceId: Uuid? = null,
        referenceType: String? = null,
        description: String? = null
    ): TransactionId
}
