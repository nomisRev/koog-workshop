package org.example.project.service

import org.example.project.db.repository.MerchantRepository
import org.example.project.db.repository.ProductRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.enums.ProductCategory
import org.example.project.domain.id.MerchantId
import org.example.project.domain.id.ProductId
import org.example.project.domain.model.Merchant
import org.example.project.domain.model.Page
import org.example.project.domain.model.Product
import org.jetbrains.exposed.v1.jdbc.Database

class CatalogService(
    private val database: Database,
    private val productRepository: ProductRepository = ProductRepository(),
    private val merchantRepository: MerchantRepository = MerchantRepository()
) {
    // --- Products ---

    suspend fun getProducts(offset: Long = 0, limit: Long = 50): Page<Product> =
        database.suspendTransaction { productRepository.getProducts(offset, limit) }

    suspend fun getProduct(id: ProductId): Product? =
        database.suspendTransaction { productRepository.getProductOrNull(id) }

    suspend fun getProductsByCategory(category: ProductCategory): List<Product> =
        database.suspendTransaction { productRepository.getProductsByCategory(category) }

    suspend fun createProduct(product: Product): ProductId =
        database.suspendTransaction { productRepository.createProduct(product) }

    suspend fun updateProduct(product: Product): Boolean =
        database.suspendTransaction { productRepository.updateProduct(product) }

    suspend fun deleteProduct(id: ProductId): Boolean =
        database.suspendTransaction { productRepository.deleteProduct(id) }

    // --- Merchants ---

    suspend fun getMerchants(offset: Long = 0, limit: Long = 50): Page<Merchant> =
        database.suspendTransaction { merchantRepository.getMerchants(offset, limit) }

    suspend fun getMerchant(id: MerchantId): Merchant? =
        database.suspendTransaction { merchantRepository.getMerchantOrNull(id) }

    suspend fun createMerchant(
        name: String,
        description: String? = null,
        location: String? = null,
        theme: String? = null,
        iconPath: String? = null
    ): MerchantId =
        database.suspendTransaction {
            merchantRepository.createMerchant(name, description, location, theme, iconPath)
        }

    suspend fun updateMerchant(
        id: MerchantId,
        name: String? = null,
        description: String? = null,
        location: String? = null,
        theme: String? = null,
        iconPath: String? = null,
        isActive: Boolean? = null
    ): Boolean =
        database.suspendTransaction {
            merchantRepository.updateMerchant(id, name, description, location, theme, iconPath, isActive)
        }

    suspend fun deleteMerchant(id: MerchantId): Boolean =
        database.suspendTransaction { merchantRepository.deleteMerchant(id) }
}
