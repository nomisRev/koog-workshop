package org.example.project.service

import org.example.project.db.repository.OrderRepository
import org.example.project.db.repository.ReviewRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.OrderItemId
import org.example.project.domain.id.ProductId
import org.example.project.domain.id.ReviewId
import org.example.project.domain.model.Page
import org.example.project.domain.model.Review
import org.jetbrains.exposed.v1.jdbc.Database

class ReviewService(
    private val database: Database,
    private val reviewRepository: ReviewRepository = ReviewRepository(),
    private val orderRepository: OrderRepository = OrderRepository()
) {
    suspend fun createReview(
        characterId: CharacterId,
        productId: ProductId,
        orderItemId: OrderItemId,
        rating: Int,
        text: String? = null
    ): ReviewId {
        require(rating in 1..5) { "Rating must be between 1 and 5" }
        return database.suspendTransaction {
            reviewRepository.createReview(characterId, productId, orderItemId, rating, text)
        }
    }

    suspend fun updateReview(id: ReviewId, rating: Int? = null, text: String? = null): Boolean {
        if (rating != null) require(rating in 1..5) { "Rating must be between 1 and 5" }
        return database.suspendTransaction { reviewRepository.updateReview(id, rating, text) }
    }

    suspend fun deleteReview(id: ReviewId): Boolean =
        database.suspendTransaction { reviewRepository.deleteReview(id) }

    suspend fun getReview(id: ReviewId): Review? =
        database.suspendTransaction { reviewRepository.getReviewOrNull(id) }

    suspend fun getProductReviews(
        productId: ProductId,
        offset: Long = 0,
        limit: Long = 50
    ): Page<Review> =
        database.suspendTransaction {
            reviewRepository.getReviewsForProduct(productId, offset, limit)
        }

    suspend fun getAverageRating(productId: ProductId): Double? =
        database.suspendTransaction { reviewRepository.averageRatingForProductOrNull(productId) }
}
