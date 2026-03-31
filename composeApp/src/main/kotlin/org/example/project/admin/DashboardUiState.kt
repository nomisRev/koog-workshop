package org.example.project.admin

import kotlinx.collections.immutable.PersistentList
import org.example.project.domain.model.DashboardSummary
import org.example.project.domain.model.LowStockProductSummary
import org.example.project.domain.model.RecentOrderSummary

sealed interface DashboardUiState {
    data object Uninitialized : DashboardUiState
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Ready(
        val summary: DashboardSummary,
        val lowStockProducts: PersistentList<LowStockProductSummary>,
        val recentOrders: PersistentList<RecentOrderSummary>
    ) : DashboardUiState
}

fun DashboardUiState.Ready.isDashboardEmpty(): Boolean =
    summary.totalProducts == 0L &&
        summary.totalMerchants == 0L &&
        summary.totalOrders == 0L &&
        summary.totalCharacters == 0L &&
        summary.totalShippingMethods == 0L &&
        lowStockProducts.isEmpty() &&
        recentOrders.isEmpty()
