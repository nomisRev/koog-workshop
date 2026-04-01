package org.example.project.admin.products

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.example.project.domain.admin.ProductDetail
import org.example.project.domain.admin.ProductFilter
import org.example.project.domain.admin.ProductListItem
import org.example.project.domain.admin.ProductMerchantOption
import org.example.project.domain.shared.ProductId

@Immutable
data class ProductAdminUiState(
    val errorMessage: String? = null,
    val filter: ProductFilter = ProductFilter(),
    val merchants: PersistentList<ProductMerchantOption> = persistentListOf(),
    val products: PersistentList<ProductListItem> = persistentListOf(),
    val selectedProductId: ProductId? = null,
    val selectedProduct: ProductDetail? = null
)
