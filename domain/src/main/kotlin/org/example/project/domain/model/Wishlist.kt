package org.example.project.domain.model

import kotlin.time.Instant
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.ProductId
import org.example.project.domain.id.WishlistItemId

data class WishlistItem(
    val id: WishlistItemId,
    val characterId: CharacterId,
    val productId: ProductId,
    val addedAt: Instant
)
