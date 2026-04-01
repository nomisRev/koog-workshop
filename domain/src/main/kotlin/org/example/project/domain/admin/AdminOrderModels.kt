package org.example.project.domain.admin

import androidx.compose.runtime.Immutable
import org.example.project.domain.order.Order
import org.example.project.domain.order.OrderItem
import org.example.project.domain.order.SubOrder
import kotlin.time.Instant

@Immutable
data class AdminOrderDetail(
    val order: Order,
    val characterName: String,
    val currencyCode: String,
    val subOrders: List<AdminSubOrderDetail>,
    val history: List<AdminOrderHistoryEvent>
)

@Immutable
data class AdminSubOrderDetail(
    val subOrder: SubOrder,
    val merchantName: String,
    val shippingMethodName: String,
    val shippingCostCurrencyCode: String,
    val items: List<AdminOrderItemDetail>
)

@Immutable
data class AdminOrderItemDetail(
    val item: OrderItem,
    val productName: String,
    val productCategory: String,
    val productDescription: String?,
    val merchantName: String,
    val currencyCode: String,
    val unitPrice: Long,
    val subtotal: Long
)

@Immutable
data class AdminOrderHistoryEvent(
    val timestamp: Instant,
    val title: String,
    val description: String
)
