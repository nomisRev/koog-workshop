package org.example.project.domain.model

import kotlin.time.Instant
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.id.OrderId

data class RecentOrderSummary(
    val orderId: OrderId,
    val status: OrderStatus,
    val characterName: String,
    val totalPrice: Long,
    val totalCurrencyCode: String,
    val createdAt: Instant
)
