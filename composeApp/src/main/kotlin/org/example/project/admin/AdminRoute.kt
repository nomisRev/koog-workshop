package org.example.project.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.domain.admin.AdminDashboardService

@Composable
fun AdminRoute(dashboardService: AdminDashboardService) {
    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(dashboardService))
    val orderDetailViewModel: OrderDetailViewModel = viewModel(factory = OrderDetailViewModel.factory(dashboardService))
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val orderDetailState by orderDetailViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        dashboardViewModel.loadOrderHistory()
    }

    AdminMasterDetailScreen(
        dashboardState = dashboardState,
        orderDetailState = orderDetailState,
        onRefreshHistory = {
            dashboardViewModel.loadOrderHistory()
        },
        onLoadOrderDetail = { orderId ->
            orderDetailViewModel.loadOrderDetail(orderId)
        },
        onRefreshOrderDetail = { orderId ->
            dashboardViewModel.loadOrderHistory()
            orderDetailViewModel.loadOrderDetail(orderId)
        },
        onOrderClick = {
            orderDetailViewModel.startLoading()
        },
        onItemClick = { item ->
            orderDetailViewModel.selectItem(item.item.id)
        }
    )
}
