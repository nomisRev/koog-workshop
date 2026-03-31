package org.example.project.domain.repository

import org.example.project.domain.enums.ProductCategory
import org.example.project.domain.id.ProductId
import org.example.project.domain.model.Product

interface ProductRepository {
    suspend fun getAllProducts(): List<Product>
    suspend fun getProductOrNull(id: ProductId): Product?
    suspend fun getProductsByCategory(category: ProductCategory): List<Product>
    suspend fun updateStock(productId: ProductId, quantityChange: Int): Boolean
}
