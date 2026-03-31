package org.example.project.domain.model

import kotlin.time.Instant
import org.example.project.domain.id.CartItemId
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.ProductId

data class CartItem(
    val id: CartItemId,
    val characterId: CharacterId,
    val productId: ProductId,
    val quantity: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
