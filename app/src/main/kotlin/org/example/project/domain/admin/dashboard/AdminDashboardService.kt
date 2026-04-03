package org.example.project.domain.admin.dashboard

import org.example.project.db.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.Database

class AdminDashboardService(
    private val database: Database,
    private val dashboardRepository: AdminDashboardRepository = AdminDashboardRepository()
) {
    suspend fun loadRecentOrders(limit: Int = 5): List<RecentOrderSummary> =
        database.suspendTransaction {
            dashboardRepository.getRecentOrders(limit)
        }

    suspend fun loadOrderHistory(): List<RecentOrderSummary> =
        database.suspendTransaction {
            dashboardRepository.getOrderSummaries(limit = null)
        }
}
