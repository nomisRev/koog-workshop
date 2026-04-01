@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.example.project.domain.admin.AdminOrderItemDetail
import org.example.project.domain.shared.OrderId

@Composable
fun AdminMasterDetailScreen(
    dashboardState: DashboardUiState,
    orderDetailState: AdminOrderDetailUiState,
    onRefreshHistory: suspend () -> Unit,
    onLoadOrderDetail: suspend (OrderId) -> Unit,
    onRefreshOrderDetail: suspend (OrderId) -> Unit,
    onOrderClick: () -> Unit,
    onItemClick: (AdminOrderItemDetail) -> Unit
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = AdminDetailRoute.orderHistory
        ) {
            composable(route = AdminDetailRoute.orderHistory) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        AdminTopBar(
                            title = "Order history",
                            onRefresh = {
                                coroutineScope.launch {
                                    onRefreshHistory()
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        OrderHistoryScreen(
                            uiState = dashboardState,
                            onRefresh = {
                                coroutineScope.launch {
                                    onRefreshHistory()
                                }
                            },
                            onOrderClick = { order ->
                                onOrderClick()
                                navController.navigate(AdminDetailRoute.forOrder(order.orderId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }

            composable(
                route = AdminDetailRoute.orderDetail,
                arguments = listOf(
                    navArgument(AdminDetailRoute.orderIdArg) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val orderId = requireNotNull(backStackEntry.selectedOrderIdOrNull()) {
                    "Missing order ID for order detail route."
                }

                LaunchedEffect(orderId) {
                    onLoadOrderDetail(orderId)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        AdminTopBar(
                            title = "Order details",
                            onBack = {
                                navController.popBackStack()
                            },
                            onRefresh = {
                                coroutineScope.launch {
                                    onRefreshOrderDetail(orderId)
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        OrderDetailScreen(
                            uiState = orderDetailState,
                            onRefresh = {
                                coroutineScope.launch {
                                    onRefreshOrderDetail(orderId)
                                }
                            },
                            onItemClick = onItemClick
                        )
                    }
                }
            }
        }
    }
}
