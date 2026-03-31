package org.example.project.service

import org.example.project.db.repository.CharacterRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.enums.TransactionType
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.CurrencyId
import org.example.project.domain.id.TransactionId
import org.example.project.domain.model.Character
import org.example.project.domain.model.Page
import org.example.project.domain.model.Transaction
import org.jetbrains.exposed.v1.jdbc.Database

class CharacterService(
    private val database: Database,
    private val characterRepository: CharacterRepository = CharacterRepository()
) {
    suspend fun getCharacter(id: CharacterId): Character? =
        database.suspendTransaction { characterRepository.getCharacterOrNull(id) }

    suspend fun createCharacter(name: String): CharacterId =
        database.suspendTransaction { characterRepository.createCharacter(name) }

    suspend fun updateCharacter(id: CharacterId, name: String): Boolean =
        database.suspendTransaction { characterRepository.updateCharacter(id, name) }

    suspend fun deleteCharacter(id: CharacterId): Boolean =
        database.suspendTransaction { characterRepository.deleteCharacter(id) }

    suspend fun getWalletBalance(characterId: CharacterId): Map<CurrencyId, Long> =
        database.suspendTransaction { characterRepository.getWalletBalance(characterId) }

    suspend fun deposit(
        characterId: CharacterId,
        currencyId: CurrencyId,
        amount: Long,
        description: String? = null
    ): TransactionId {
        require(amount > 0) { "Deposit amount must be positive" }
        return database.suspendTransaction {
            characterRepository.addTransaction(
                characterId = characterId,
                currencyId = currencyId,
                amount = amount,
                type = TransactionType.DEPOSIT,
                description = description
            )
        }
    }

    suspend fun getTransactionHistory(
        characterId: CharacterId,
        offset: Long = 0,
        limit: Long = 50
    ): Page<Transaction> =
        database.suspendTransaction {
            characterRepository.getTransactionHistory(characterId, offset, limit)
        }
}
