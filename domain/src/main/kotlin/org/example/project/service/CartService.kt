package org.example.project.service

import org.example.project.db.repository.CartRepository
import org.example.project.db.repository.ProductRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.id.CartItemId
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.ProductId
import org.example.project.domain.model.CartItem
import org.jetbrains.exposed.v1.jdbc.Database

class CartService(
    private val database: Database,
    private val cartRepository: CartRepository = CartRepository(),
    private val productRepository: ProductRepository = ProductRepository()
) {
    suspend fun getCart(characterId: CharacterId): List<CartItem> =
        database.suspendTransaction { cartRepository.getCartItems(characterId) }

    suspend fun addToCart(
        characterId: CharacterId,
        productId: ProductId,
        quantity: Int = 1
    ): CartItemId {
        require(quantity > 0) { "Quantity must be positive" }
        return database.suspendTransaction {
            val product = productRepository.getProductOrNull(productId)
                ?: throw IllegalArgumentException("Product not found: $productId")
            require(product.isActive) { "Product is not active: $productId" }
            require(product.stock >= quantity) { "Insufficient stock for product: $productId" }
            cartRepository.addToCart(characterId, productId, quantity)
        }
    }

    suspend fun updateQuantity(cartItemId: CartItemId, quantity: Int): Boolean {
        require(quantity > 0) { "Quantity must be positive" }
        return database.suspendTransaction {
            cartRepository.updateQuantity(cartItemId, quantity)
        }
    }

    suspend fun removeFromCart(cartItemId: CartItemId): Boolean =
        database.suspendTransaction { cartRepository.removeFromCart(cartItemId) }

    suspend fun clearCart(characterId: CharacterId): Int =
        database.suspendTransaction { cartRepository.clearCart(characterId) }
}
