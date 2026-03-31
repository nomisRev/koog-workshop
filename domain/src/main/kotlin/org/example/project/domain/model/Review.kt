package org.example.project.domain.model

import kotlin.time.Instant
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.OrderItemId
import org.example.project.domain.id.ProductId
import org.example.project.domain.id.ReviewId

data class Review(
    val id: ReviewId,
    val characterId: CharacterId,
    val productId: ProductId,
    val orderItemId: OrderItemId,
    val rating: Int,
    val text: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
