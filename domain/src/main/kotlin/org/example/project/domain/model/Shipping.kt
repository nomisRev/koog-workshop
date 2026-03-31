package org.example.project.domain.model

import kotlin.time.Instant
import org.example.project.domain.id.CurrencyId
import org.example.project.domain.id.ShippingMethodId

data class ShippingMethod(
    val id: ShippingMethodId,
    val name: String,
    val description: String?,
    val baseCost: Long,
    val currencyId: CurrencyId,
    val estimatedDays: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
