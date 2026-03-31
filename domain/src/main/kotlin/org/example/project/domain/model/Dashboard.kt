package org.example.project.domain.model

import kotlin.time.Instant
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.id.OrderId
import org.example.project.domain.id.ProductId

data class DashboardSummary(
    val totalProducts: Long,
    val totalMerchants: Long,
    val totalOrders: Long,
    val totalCharacters: Long,
    val totalShippingMethods: Long
)

data class LowStockProductSummary(
    val productId: ProductId,
    val productName: String,
    val merchantName: String,
    val stock: Int
)

data class RecentOrderSummary(
    val orderId: OrderId,
    val status: OrderStatus,
    val characterName: String,
    val totalPrice: Long,
    val totalCurrencyCode: String,
    val createdAt: Instant
)

data class DashboardSnapshot(
    val summary: DashboardSummary,
    val lowStockProducts: List<LowStockProductSummary>,
    val recentOrders: List<RecentOrderSummary>
)
