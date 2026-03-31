package org.example.project.db.repository

import org.example.project.db.tables.Characters
import org.example.project.db.tables.Currencies
import org.example.project.db.tables.Merchants
import org.example.project.db.tables.Orders
import org.example.project.db.tables.Products
import org.example.project.db.tables.ShippingMethods
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.id.OrderId
import org.example.project.domain.id.ProductId
import org.example.project.domain.model.DashboardSnapshot
import org.example.project.domain.model.DashboardSummary
import org.example.project.domain.model.LowStockProductSummary
import org.example.project.domain.model.RecentOrderSummary
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.selectAll

class AdminDashboardRepository {

    private val lowStockJoin = Products.innerJoin(Merchants)
    private val recentOrdersJoin = Orders
        .innerJoin(Characters)
        .innerJoin(Currencies)

    context(_: Transaction)
    fun getDashboardSummary(): DashboardSummary = DashboardSummary(
        totalProducts = Products.selectAll().count(),
        totalMerchants = Merchants.selectAll().count(),
        totalOrders = Orders.selectAll().count(),
        totalCharacters = Characters.selectAll().count(),
        totalShippingMethods = ShippingMethods.selectAll().count()
    )

    context(_: Transaction)
    fun getLowStockProducts(
        threshold: Int = 10,
        limit: Int = 5
    ): List<LowStockProductSummary> {
        if (limit <= 0) return emptyList()

        return lowStockJoin.selectAll()
            .where { Products.stock lessEq threshold }
            .orderBy(Products.stock to SortOrder.ASC, Products.name to SortOrder.ASC)
            .limit(limit)
            .map(::mapToLowStockProductSummary)
    }

    context(_: Transaction)
    fun getRecentOrders(limit: Int = 5): List<RecentOrderSummary> {
        if (limit <= 0) return emptyList()

        return recentOrdersJoin.selectAll()
            .orderBy(Orders.createdAt to SortOrder.DESC, Orders.id to SortOrder.DESC)
            .limit(limit)
            .map(::mapToRecentOrderSummary)
    }

    context(_: Transaction)
    fun getDashboardSnapshot(
        lowStockThreshold: Int = 10,
        lowStockLimit: Int = 5,
        recentOrderLimit: Int = 5
    ): DashboardSnapshot = DashboardSnapshot(
        summary = getDashboardSummary(),
        lowStockProducts = getLowStockProducts(lowStockThreshold, lowStockLimit),
        recentOrders = getRecentOrders(recentOrderLimit)
    )

    private fun mapToLowStockProductSummary(row: ResultRow) = LowStockProductSummary(
        productId = ProductId(row[Products.id].value),
        productName = row[Products.name],
        merchantName = row[Merchants.name],
        stock = row[Products.stock]
    )

    private fun mapToRecentOrderSummary(row: ResultRow) = RecentOrderSummary(
        orderId = OrderId(row[Orders.id].value),
        status = OrderStatus.valueOf(row[Orders.status]),
        characterName = row[Characters.name],
        totalPrice = row[Orders.totalPrice],
        totalCurrencyCode = row[Currencies.code],
        createdAt = row[Orders.createdAt]
    )
}
