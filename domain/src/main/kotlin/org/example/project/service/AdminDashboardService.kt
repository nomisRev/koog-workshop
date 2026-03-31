package org.example.project.service

import org.example.project.db.repository.AdminDashboardRepository
import org.example.project.db.suspendTransaction
import org.example.project.domain.model.DashboardSnapshot
import org.jetbrains.exposed.v1.jdbc.Database

class AdminDashboardService(
    private val databaseProvider: () -> Database?,
    private val dashboardRepository: AdminDashboardRepository = AdminDashboardRepository()
) {
    suspend fun loadDashboard(): DashboardSnapshot {
        val database = databaseProvider() ?: error("No database selected.")
        return database.suspendTransaction(readOnly = true) {
            dashboardRepository.getDashboardSnapshot()
        }
    }
}
