package org.example.project.domain.repository

import org.example.project.domain.id.CurrencyId
import org.example.project.domain.model.Currency

interface CurrencyRepository {
    suspend fun getAllCurrencies(): List<Currency>
    suspend fun getCurrencyOrNull(id: CurrencyId): Currency?
    suspend fun getConversionRateOrNull(fromId: CurrencyId, toId: CurrencyId): Double?
}
