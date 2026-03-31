package org.example.project.domain.model

import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.example.project.domain.enums.TransactionType
import org.example.project.domain.id.*

data class Currency(
    val id: CurrencyId,
    val code: String,
    val name: String,
    val symbol: String,
    val iconPath: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CurrencyConversion(
    val id: CurrencyConversionId,
    val fromCurrencyId: CurrencyId,
    val toCurrencyId: CurrencyId,
    val rate: Double,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class WalletBalance(
    val currencyId: CurrencyId,
    val currencyCode: String,
    val currencyName: String,
    val balance: Long
)

data class Transaction(
    val id: TransactionId,
    val characterId: CharacterId,
    val currencyId: CurrencyId,
    val amount: Long,
    val type: TransactionType,
    val referenceId: Uuid?,
    val referenceType: String?,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
