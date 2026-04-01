@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.domain.admin.ProductActiveFilter
import org.example.project.domain.admin.ProductAdminService
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ProductId
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

class ProductAdminViewModel(
    private val productAdminService: ProductAdminService
) : ViewModel() {

    private val loadVersion = AtomicLong(0L)
    private val _uiState = MutableStateFlow(ProductAdminUiState())

    val uiState: StateFlow<ProductAdminUiState> = _uiState.asStateFlow()

    suspend fun load() {
        reload()
    }

    suspend fun refresh() {
        reload()
    }

    suspend fun updateNameQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(nameQuery = query)
        )
        reload()
    }

    suspend fun updateCategory(category: ProductCategory?) {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(category = category)
        )
        reload()
    }

    suspend fun updateMerchant(merchantId: MerchantId?) {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(merchantId = merchantId)
        )
        reload()
    }

    suspend fun updateActiveFilter(activeFilter: ProductActiveFilter) {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(activeFilter = activeFilter)
        )
        reload()
    }

    suspend fun selectProduct(productId: ProductId) {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(
            isLoading = true,
            errorMessage = null,
            selectedProductId = productId
        )

        val nextState = try {
            val detail = productAdminService.loadProductDetailOrNull(productId)
            if (detail == null) {
                current.copy(
                    isLoading = false,
                    errorMessage = "Product ${productId.value} was not found.",
                    selectedProductId = null,
                    selectedProduct = null
                )
            } else {
                current.copy(
                    isLoading = false,
                    errorMessage = null,
                    selectedProductId = productId,
                    selectedProduct = detail
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                isLoading = false,
                errorMessage = throwable.message ?: "Unable to load product details."
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    suspend fun adjustSelectedStock(quantityChange: Int) {
        val productId = _uiState.value.selectedProductId ?: return
        performMutation(
            failureMessage = "Unable to update stock for product ${productId.value}."
        ) {
            productAdminService.adjustStock(productId, quantityChange)
        }
    }

    suspend fun setSelectedProductActive(isActive: Boolean) {
        val productId = _uiState.value.selectedProductId ?: return
        performMutation(
            failureMessage = "Unable to update the product state for ${productId.value}."
        ) {
            productAdminService.setProductActive(productId, isActive)
        }
    }

    private suspend fun reload() {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        val nextState = try {
            val merchants = productAdminService.loadMerchantOptions().toPersistentList()
            val products = productAdminService.loadProducts(current.filter).toPersistentList()
            val selectedProductId = current.selectedProductId
                ?.takeIf { selectedId -> products.any { product -> product.id == selectedId } }
                ?: products.firstOrNull()?.id
            val selectedProduct = selectedProductId?.let { productAdminService.loadProductDetailOrNull(it) }

            current.copy(
                isLoading = false,
                errorMessage = null,
                merchants = merchants,
                products = products,
                selectedProductId = selectedProduct?.id ?: selectedProductId,
                selectedProduct = selectedProduct
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                isLoading = false,
                errorMessage = throwable.message ?: "Unable to load product operations."
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    private suspend fun performMutation(
        failureMessage: String,
        action: suspend () -> Boolean
    ) {
        val current = _uiState.value
        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        val success = try {
            action()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            _uiState.value = current.copy(
                isLoading = false,
                errorMessage = throwable.message ?: failureMessage
            )
            return
        }

        if (!success) {
            _uiState.value = current.copy(
                isLoading = false,
                errorMessage = failureMessage
            )
            return
        }

        reload()
    }

    companion object {
        fun factory(productAdminService: ProductAdminService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    if (modelClass == ProductAdminViewModel::class) {
                        return ProductAdminViewModel(productAdminService) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.simpleName ?: "unknown"}"
                    )
                }
            }
    }
}
