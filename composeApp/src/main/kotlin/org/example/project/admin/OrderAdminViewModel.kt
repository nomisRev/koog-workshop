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
import org.example.project.domain.admin.OrderAdminService
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.SubOrderId
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

class OrderAdminViewModel(
    private val orderAdminService: OrderAdminService
) : ViewModel() {

    private val loadVersion = AtomicLong(0L)
    private val _uiState = MutableStateFlow(OrderAdminUiState())

    val uiState: StateFlow<OrderAdminUiState> = _uiState.asStateFlow()

    suspend fun load() {
        reload()
    }

    suspend fun refresh() {
        reload()
    }

    suspend fun updateOrderIdQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(orderIdQuery = query)
        )
        reload()
    }

    suspend fun updateOrderStatus(status: OrderStatus?) {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(orderStatus = status)
        )
        reload()
    }

    suspend fun updateSubOrderStatusFilter(status: OrderStatus?) {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(subOrderStatus = status)
        )
        reload()
    }

    suspend fun updateMerchant(merchantId: MerchantId?) {
        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(merchantId = merchantId)
        )
        reload()
    }

    suspend fun selectOrder(orderId: OrderId) {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(
            isLoading = true,
            errorMessage = null,
            selectedOrderId = orderId
        )

        val nextState = try {
            val detail = orderAdminService.loadOrderDetailOrNull(orderId)
            if (detail == null) {
                current.copy(
                    isLoading = false,
                    errorMessage = "Order ${orderId.value} was not found.",
                    selectedOrderId = null,
                    selectedOrder = null
                )
            } else {
                current.copy(
                    isLoading = false,
                    errorMessage = null,
                    selectedOrderId = orderId,
                    selectedOrder = detail
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                isLoading = false,
                errorMessage = throwable.message ?: "Unable to load order details."
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    suspend fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus) {
        val current = _uiState.value
        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        val success = try {
            orderAdminService.updateSubOrderStatus(subOrderId, status)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            _uiState.value = current.copy(
                isLoading = false,
                errorMessage = throwable.message ?: "Unable to update sub-order status."
            )
            return
        }

        if (!success) {
            _uiState.value = current.copy(
                isLoading = false,
                errorMessage = "Unable to update sub-order status."
            )
            return
        }

        reload()
    }

    private suspend fun reload() {
        val version = loadVersion.incrementAndGet()
        val current = _uiState.value
        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        val nextState = try {
            val merchants = orderAdminService.loadMerchantOptions().toPersistentList()
            val orders = orderAdminService.loadOrders(current.filter).toPersistentList()
            val selectedOrderId = current.selectedOrderId
                ?.takeIf { selectedId -> orders.any { order -> order.orderId == selectedId } }
                ?: orders.firstOrNull()?.orderId
            val selectedOrder = selectedOrderId?.let { orderAdminService.loadOrderDetailOrNull(it) }

            current.copy(
                isLoading = false,
                errorMessage = null,
                merchants = merchants,
                orders = orders,
                selectedOrderId = selectedOrder?.order?.id ?: selectedOrderId,
                selectedOrder = selectedOrder
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            current.copy(
                isLoading = false,
                errorMessage = throwable.message ?: "Unable to load order operations."
            )
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    companion object {
        fun factory(orderAdminService: OrderAdminService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    if (modelClass == OrderAdminViewModel::class) {
                        return OrderAdminViewModel(orderAdminService) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.simpleName ?: "unknown"}"
                    )
                }
            }
    }
}
