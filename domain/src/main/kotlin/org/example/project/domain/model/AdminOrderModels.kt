package org.example.project.domain.model

import kotlin.time.Instant

data class AdminOrderDetail(
    val order: Order,
    val characterName: String,
    val currencyCode: String,
    val subOrders: List<AdminSubOrderDetail>,
    val history: List<AdminOrderHistoryEvent>
)

data class AdminSubOrderDetail(
    val subOrder: SubOrder,
    val merchantName: String,
    val shippingMethodName: String,
    val shippingCostCurrencyCode: String,
    val items: List<AdminOrderItemDetail>
)

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

data class AdminOrderHistoryEvent(
    val timestamp: Instant,
    val title: String,
    val description: String
)
