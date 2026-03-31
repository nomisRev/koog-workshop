package org.example.project.db.repository

import org.example.project.db.tables.Characters
import org.example.project.db.tables.Currencies
import org.example.project.db.tables.Orders
import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.id.OrderId
import org.example.project.domain.model.RecentOrderSummary
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.selectAll

class AdminDashboardRepository {

    private val recentOrdersJoin = Orders
        .innerJoin(Characters)
        .innerJoin(Currencies)

    context(_: Transaction)
    fun getRecentOrders(limit: Int = 5): List<RecentOrderSummary> =
        getOrderSummaries(limit)

    context(_: Transaction)
    fun getOrderSummaries(limit: Int? = null): List<RecentOrderSummary> {
        if (limit != null && limit <= 0) return emptyList()

        val query = recentOrdersJoin.selectAll()
            .orderBy(Orders.createdAt to SortOrder.DESC, Orders.id to SortOrder.DESC)

        val limitedQuery = limit?.let { query.limit(it) } ?: query

        return limitedQuery.map(::mapToRecentOrderSummary)
    }

    private fun mapToRecentOrderSummary(row: ResultRow) = RecentOrderSummary(
        orderId = OrderId(row[Orders.id].value),
        status = OrderStatus.valueOf(row[Orders.status]),
        characterName = row[Characters.name],
        totalPrice = row[Orders.totalPrice],
        totalCurrencyCode = row[Currencies.code],
        createdAt = row[Orders.createdAt]
    )
}
