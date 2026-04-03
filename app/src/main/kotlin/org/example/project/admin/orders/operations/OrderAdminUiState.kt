package org.example.project.admin.orders.operations

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.example.project.domain.admin.orders.AdminOrderDetail
import org.example.project.domain.admin.orders.OrderFilter
import org.example.project.domain.admin.orders.OrderListItem
import org.example.project.domain.admin.orders.OrderMerchantOption
import org.example.project.domain.shared.OrderId

@Immutable
data class OrderAdminUiState(
    val errorMessage: String? = null,
    val filter: OrderFilter = OrderFilter(),
    val merchants: PersistentList<OrderMerchantOption> = persistentListOf(),
    val orders: PersistentList<OrderListItem> = persistentListOf(),
    val selectedOrderId: OrderId? = null,
    val selectedOrder: AdminOrderDetail? = null
)
