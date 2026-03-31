package org.example.project.admin

import kotlinx.collections.immutable.PersistentList
import org.example.project.domain.model.RecentOrderSummary

sealed interface DashboardUiState {
    data object Uninitialized : DashboardUiState
    data object Loading : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Ready(
        val recentOrders: PersistentList<RecentOrderSummary>
    ) : DashboardUiState
}
