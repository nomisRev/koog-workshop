@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.compose.runtime.Composable
import org.example.project.domain.admin.AdminOrderItemDetail
import org.example.project.domain.admin.RecentOrderSummary

@Composable
fun OrderHistoryScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onOrderClick: (RecentOrderSummary) -> Unit
) {
    when (uiState) {
        DashboardUiState.Uninitialized,
        DashboardUiState.Loading -> LoadingCard(
            title = "Loading order history",
            body = "Reading the latest order activity."
        )

        is DashboardUiState.Error -> ErrorCard(
            title = "Order history failed to load",
            message = uiState.message,
            onRefresh = onRefresh
        )

        is DashboardUiState.Ready -> OrderHistoryCard(
            orders = uiState.recentOrders,
            onOrderClick = onOrderClick
        )
    }
}

@Composable
fun OrderDetailScreen(
    uiState: AdminOrderDetailUiState,
    onRefresh: () -> Unit,
    onItemClick: (AdminOrderItemDetail) -> Unit
) {
    when (uiState) {
        AdminOrderDetailUiState.Loading -> LoadingCard(
            title = "Loading order details",
            body = "Reading the full history for this order."
        )

        is AdminOrderDetailUiState.Error -> ErrorCard(
            title = "Order details failed to load",
            message = uiState.message,
            onRefresh = onRefresh
        )

        is AdminOrderDetailUiState.Ready -> OrderDetailContent(
            detail = uiState.detail,
            selectedItemId = uiState.selectedItemId,
            onItemClick = onItemClick
        )
    }
}
