package org.example.project.domain.repository

import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.OrderItemId
import org.example.project.domain.id.ProductId
import org.example.project.domain.id.ReviewId
import org.example.project.domain.model.Review

interface ReviewRepository {
    suspend fun createReview(
        characterId: CharacterId,
        productId: ProductId,
        orderItemId: OrderItemId,
        rating: Int,
        text: String?
    ): ReviewId
    suspend fun getReviewsForProduct(productId: ProductId): List<Review>
    suspend fun getAverageRatingForProduct(productId: ProductId): Double
}
