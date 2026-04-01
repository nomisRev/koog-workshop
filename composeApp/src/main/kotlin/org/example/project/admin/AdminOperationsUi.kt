@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.example.project.domain.admin.AdminOrderDetail
import org.example.project.domain.admin.AdminOrderHistoryEvent
import org.example.project.domain.admin.AdminOrderItemDetail
import org.example.project.domain.admin.AdminSubOrderDetail
import org.example.project.domain.admin.OrderAdminService
import org.example.project.domain.admin.OrderListItem
import org.example.project.domain.admin.ProductActiveFilter
import org.example.project.domain.admin.ProductAdminService
import org.example.project.domain.admin.ProductDetail
import org.example.project.domain.admin.ProductListItem
import org.example.project.domain.admin.ProductReviewSummary
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.SubOrderId
import java.util.Locale
import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val adminDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

private val screenPadding = 16.dp
private val sectionSpacing = 10.dp
private val compactChromePadding = 12.dp
private val compactChromeButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
private val chromeSectionPadding = 12.dp

private enum class AdminWorkspaceTab(
    val title: String,
    val subtitle: String
) {
    Products(
        title = "Products",
        subtitle = "Catalog browsing, stock changes, and active-state control."
    ),
    Orders(
        title = "Orders",
        subtitle = "Operational triage, hierarchy inspection, and sub-order updates."
    )
}

@Composable
fun AdminRoute(
    productAdminService: ProductAdminService,
    orderAdminService: OrderAdminService
) {
    val productViewModel: ProductAdminViewModel = viewModel(factory = ProductAdminViewModel.factory(productAdminService))
    val orderViewModel: OrderAdminViewModel = viewModel(factory = OrderAdminViewModel.factory(orderAdminService))
    val productState by productViewModel.uiState.collectAsState()
    val orderState by orderViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        launch { productViewModel.load() }
        launch { orderViewModel.load() }
    }

    var selectedTab by rememberSaveable { mutableStateOf(AdminWorkspaceTab.Products) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AdminTitleBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab }
            )

            when (selectedTab) {
                AdminWorkspaceTab.Products -> ProductOperationsScreen(
                    uiState = productState,
                    onRefresh = {
                        coroutineScope.launch { productViewModel.refresh() }
                    },
                    onNameQueryChange = { query ->
                        coroutineScope.launch { productViewModel.updateNameQuery(query) }
                    },
                    onCategoryChange = { category ->
                        coroutineScope.launch { productViewModel.updateCategory(category) }
                    },
                    onMerchantChange = { merchantId ->
                        coroutineScope.launch { productViewModel.updateMerchant(merchantId) }
                    },
                    onActiveFilterChange = { activeFilter ->
                        coroutineScope.launch { productViewModel.updateActiveFilter(activeFilter) }
                    },
                    onSelectProduct = { productId ->
                        coroutineScope.launch { productViewModel.selectProduct(productId) }
                    },
                    onAdjustStock = { quantityChange ->
                        coroutineScope.launch { productViewModel.adjustSelectedStock(quantityChange) }
                    },
                    onSetActive = { isActive ->
                        coroutineScope.launch { productViewModel.setSelectedProductActive(isActive) }
                    }
                )

                AdminWorkspaceTab.Orders -> OrderOperationsScreen(
                    uiState = orderState,
                    onRefresh = {
                        coroutineScope.launch { orderViewModel.refresh() }
                    },
                    onOrderIdQueryChange = { query ->
                        coroutineScope.launch { orderViewModel.updateOrderIdQuery(query) }
                    },
                    onOrderStatusChange = { status ->
                        coroutineScope.launch { orderViewModel.updateOrderStatus(status) }
                    },
                    onSubOrderStatusChange = { status ->
                        coroutineScope.launch { orderViewModel.updateSubOrderStatusFilter(status) }
                    },
                    onMerchantChange = { merchantId ->
                        coroutineScope.launch { orderViewModel.updateMerchant(merchantId) }
                    },
                    onSelectOrder = { orderId ->
                        coroutineScope.launch { orderViewModel.selectOrder(orderId) }
                    },
                    onUpdateSubOrderStatus = { subOrderId, status ->
                        coroutineScope.launch { orderViewModel.updateSubOrderStatus(subOrderId, status) }
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminTitleBar(
    selectedTab: AdminWorkspaceTab,
    onTabSelected: (AdminWorkspaceTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = screenPadding, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Fantasy Store Admin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = selectedTab.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminWorkspaceTab.entries.forEach { tab ->
                    FilterChip(
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductOperationsScreen(
    uiState: ProductAdminUiState,
    onRefresh: () -> Unit,
    onNameQueryChange: (String) -> Unit,
    onCategoryChange: (ProductCategory?) -> Unit,
    onMerchantChange: (MerchantId?) -> Unit,
    onActiveFilterChange: (ProductActiveFilter) -> Unit,
    onSelectProduct: (ProductId) -> Unit,
    onAdjustStock: (Int) -> Unit,
    onSetActive: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = screenPadding, vertical = chromeSectionPadding),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        ProductFilterCard(
            uiState = uiState,
            onRefresh = onRefresh,
            onNameQueryChange = onNameQueryChange,
            onCategoryChange = onCategoryChange,
            onMerchantChange = onMerchantChange,
            onActiveFilterChange = onActiveFilterChange
        )

        uiState.errorMessage?.let { message ->
            InlineErrorCard(message = message)
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            ProductListPanel(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                isLoading = uiState.isLoading,
                products = uiState.products,
                selectedProductId = uiState.selectedProductId,
                onSelectProduct = onSelectProduct
            )

            ProductDetailPanel(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                isLoading = uiState.isLoading,
                product = uiState.selectedProduct,
                onAdjustStock = onAdjustStock,
                onSetActive = onSetActive
            )
        }
    }
}

@Composable
private fun ProductFilterCard(
    uiState: ProductAdminUiState,
    onRefresh: () -> Unit,
    onNameQueryChange: (String) -> Unit,
    onCategoryChange: (ProductCategory?) -> Unit,
    onMerchantChange: (MerchantId?) -> Unit,
    onActiveFilterChange: (ProductActiveFilter) -> Unit
) {
    CompactFilterToolbar(
        title = "Product filters",
        onRefresh = onRefresh,
        summaryLabels = productSecondaryFilterSummaries(uiState),
        primaryContent = {
            ToolbarTextFilter(
                value = uiState.filter.nameQuery,
                onValueChange = onNameQueryChange,
                placeholder = "Search products"
            )

            FilterGroup(
                title = "Active state",
                options = ProductActiveFilter.entries.map { filter ->
                    filter.labelize() to filter
                },
                selected = uiState.filter.activeFilter,
                onSelect = onActiveFilterChange
            )
        },
        advancedContent = {
            FilterGroup(
                title = "Category",
                options = listOf("All" to null) + ProductCategory.entries.map { category ->
                    category.labelize() to category
                },
                selected = uiState.filter.category,
                onSelect = onCategoryChange
            )

            FilterGroup(
                title = "Merchant",
                options = listOf("All" to null) + uiState.merchants.map { merchant ->
                    merchant.name to merchant.id
                },
                selected = uiState.filter.merchantId,
                onSelect = onMerchantChange
            )
        }
    )
}

@Composable
private fun ProductListPanel(
    modifier: Modifier,
    isLoading: Boolean,
    products: List<ProductListItem>,
    selectedProductId: ProductId?,
    onSelectProduct: (ProductId) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelHeader(
                title = "Catalog",
                subtitle = "${products.size} matching products"
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (products.isEmpty() && isLoading) {
                PanelLoadingState()
            } else if (products.isEmpty()) {
                PanelEmptyState(message = "No products match the current filters.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = products,
                        key = { product -> product.id.value }
                    ) { product ->
                        ProductRow(
                            product = product,
                            selected = product.id == selectedProductId,
                            onClick = { onSelectProduct(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductListItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${product.category.labelize()} · ${product.merchantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(if (product.isActive) "Active" else "Inactive")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.price.formatAmount(product.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Stock ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = product.reviewSummary.toDisplayText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductDetailPanel(
    modifier: Modifier,
    isLoading: Boolean,
    product: ProductDetail?,
    onAdjustStock: (Int) -> Unit,
    onSetActive: (Boolean) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        when {
            product == null && isLoading -> PanelLoadingState(modifier = Modifier.fillMaxSize())
            product == null -> PanelEmptyState(
                modifier = Modifier.fillMaxSize(),
                message = "Select a product to inspect its details and operations."
            )

            else -> {
                val imageUrl = product.imageUrl

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(screenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PanelHeader(
                        title = product.name,
                        subtitle = product.category.labelize()
                    )

                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = product.id.value.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            product.description?.takeIf { description -> description.isNotBlank() }?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(if (product.isActive) "Active" else "Inactive")
                            }
                        )
                    }

                    DetailMetricsRow(
                        DetailMetric("Price", product.price.formatAmount(product.currencyCode)),
                        DetailMetric("Stock", product.stock.toString()),
                        DetailMetric("Rarity", product.rarity.labelize())
                    )
                    DetailMetricsRow(
                        DetailMetric("Merchant", product.merchantName),
                        DetailMetric("Currency", "${product.currencyCode} (${product.currencySymbol})"),
                        DetailMetric("Reviews", product.reviewSummary.toDisplayText())
                    )
                    DetailMetricsRow(
                        DetailMetric("Created", product.createdAt.formatAdminInstant()),
                        DetailMetric("Updated", product.updatedAt.formatAdminInstant())
                    )

                    if (imageUrl != null) {
                        DetailBlock(
                            title = "Image URL",
                            body = imageUrl
                        )
                    }

                    if (product.categoryAttributes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Category fields",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            product.categoryAttributes.forEach { attribute ->
                                DetailBlock(
                                    title = attribute.label,
                                    body = attribute.value
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(-5, -1, 1, 5).forEach { quantityChange ->
                                FilledTonalButton(onClick = { onAdjustStock(quantityChange) }) {
                                    Text(
                                        text = if (quantityChange > 0) "+$quantityChange stock" else "$quantityChange stock"
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onSetActive(!product.isActive) }
                        ) {
                            Text(if (product.isActive) "Deactivate product" else "Activate product")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderOperationsScreen(
    uiState: OrderAdminUiState,
    onRefresh: () -> Unit,
    onOrderIdQueryChange: (String) -> Unit,
    onOrderStatusChange: (OrderStatus?) -> Unit,
    onSubOrderStatusChange: (OrderStatus?) -> Unit,
    onMerchantChange: (MerchantId?) -> Unit,
    onSelectOrder: (OrderId) -> Unit,
    onUpdateSubOrderStatus: (SubOrderId, OrderStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = screenPadding, vertical = chromeSectionPadding),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        OrderFilterCard(
            uiState = uiState,
            onRefresh = onRefresh,
            onOrderIdQueryChange = onOrderIdQueryChange,
            onOrderStatusChange = onOrderStatusChange,
            onSubOrderStatusChange = onSubOrderStatusChange,
            onMerchantChange = onMerchantChange
        )

        uiState.errorMessage?.let { message ->
            InlineErrorCard(message = message)
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            OrderListPanel(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight(),
                isLoading = uiState.isLoading,
                orders = uiState.orders,
                selectedOrderId = uiState.selectedOrderId,
                onSelectOrder = onSelectOrder
            )

            OrderDetailPanel(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                isLoading = uiState.isLoading,
                order = uiState.selectedOrder,
                onUpdateSubOrderStatus = onUpdateSubOrderStatus
            )
        }
    }
}

@Composable
private fun OrderFilterCard(
    uiState: OrderAdminUiState,
    onRefresh: () -> Unit,
    onOrderIdQueryChange: (String) -> Unit,
    onOrderStatusChange: (OrderStatus?) -> Unit,
    onSubOrderStatusChange: (OrderStatus?) -> Unit,
    onMerchantChange: (MerchantId?) -> Unit
) {
    CompactFilterToolbar(
        title = "Order filters",
        onRefresh = onRefresh,
        summaryLabels = orderSecondaryFilterSummaries(uiState),
        primaryContent = {
            ToolbarTextFilter(
                value = uiState.filter.orderIdQuery,
                onValueChange = onOrderIdQueryChange,
                placeholder = "Filter by order ID"
            )

            FilterGroup(
                title = "Order status",
                options = listOf("All" to null) + OrderStatus.entries.map { status ->
                    status.labelize() to status
                },
                selected = uiState.filter.orderStatus,
                onSelect = onOrderStatusChange
            )
        },
        advancedContent = {
            FilterGroup(
                title = "Sub-order status",
                options = listOf("All" to null) + OrderStatus.entries.map { status ->
                    status.labelize() to status
                },
                selected = uiState.filter.subOrderStatus,
                onSelect = onSubOrderStatusChange
            )

            FilterGroup(
                title = "Merchant",
                options = listOf("All" to null) + uiState.merchants.map { merchant ->
                    merchant.name to merchant.id
                },
                selected = uiState.filter.merchantId,
                onSelect = onMerchantChange
            )
        }
    )
}

@Composable
private fun OrderListPanel(
    modifier: Modifier,
    isLoading: Boolean,
    orders: List<OrderListItem>,
    selectedOrderId: OrderId?,
    onSelectOrder: (OrderId) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelHeader(
                title = "Orders",
                subtitle = "${orders.size} matching orders"
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (orders.isEmpty() && isLoading) {
                PanelLoadingState()
            } else if (orders.isEmpty()) {
                PanelEmptyState(message = "No orders match the current filters.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = orders,
                        key = { order -> order.orderId.value }
                    ) { order ->
                        OrderRow(
                            order = order,
                            selected = order.orderId == selectedOrderId,
                            onClick = { onSelectOrder(order.orderId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(
    order: OrderListItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Order ${order.orderId.value}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = order.characterName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = onClick,
                    label = { Text(order.status.labelize()) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.totalPrice.formatAmount(order.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${order.merchantCount} merchants",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = order.createdAt.formatAdminInstant(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OrderDetailPanel(
    modifier: Modifier,
    isLoading: Boolean,
    order: AdminOrderDetail?,
    onUpdateSubOrderStatus: (SubOrderId, OrderStatus) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        when {
            order == null && isLoading -> PanelLoadingState(modifier = Modifier.fillMaxSize())
            order == null -> PanelEmptyState(
                modifier = Modifier.fillMaxSize(),
                message = "Select an order to inspect the hierarchy and update sub-order status."
            )

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(screenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PanelHeader(
                        title = "Order ${order.order.id.value}",
                        subtitle = order.characterName
                    )

                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    DetailMetricsRow(
                        DetailMetric("Status", order.order.status.labelize()),
                        DetailMetric("Total", order.order.totalPrice.formatAmount(order.currencyCode)),
                        DetailMetric("Sub-orders", order.subOrders.size.toString())
                    )
                    DetailMetricsRow(
                        DetailMetric("Created", order.order.createdAt.formatAdminInstant()),
                        DetailMetric("Updated", order.order.updatedAt.formatAdminInstant())
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Sub-orders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        order.subOrders.forEach { subOrder ->
                            SubOrderCard(
                                detail = subOrder,
                                onUpdateStatus = onUpdateSubOrderStatus
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (order.history.isEmpty()) {
                            DetailBlock(
                                title = "No events",
                                body = "No history events were recorded for this order."
                            )
                        } else {
                            order.history.forEach { event ->
                                HistoryEventCard(event = event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubOrderCard(
    detail: AdminSubOrderDetail,
    onUpdateStatus: (SubOrderId, OrderStatus) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = detail.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sub-order ${detail.subOrder.id.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(detail.subOrder.status.labelize()) }
                )
            }

            DetailMetricsRow(
                DetailMetric("Shipping", detail.shippingMethodName),
                DetailMetric(
                    "Shipping cost",
                    detail.subOrder.shippingCost.formatAmount(detail.shippingCostCurrencyCode)
                ),
                DetailMetric(
                    "Merchant total",
                    detail.subOrder.merchantTotalPrice.formatAmount(detail.shippingCostCurrencyCode)
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Update status",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrderStatus.entries.forEach { status ->
                        FilterChip(
                            selected = detail.subOrder.status == status,
                            onClick = { onUpdateStatus(detail.subOrder.id, status) },
                            label = { Text(status.labelize()) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                detail.items.forEach { item ->
                    OrderItemRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: AdminOrderItemDetail) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${item.productCategory.labelize()} · ${item.merchantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.subtotal.formatAmount(item.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "${item.item.quantity} x ${item.unitPrice.formatAmount(item.currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.productDescription?.takeIf { description -> description.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryEventCard(event: AdminOrderHistoryEvent) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = event.timestamp.formatAdminInstant(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PanelHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PanelLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PanelEmptyState(
    modifier: Modifier = Modifier,
    message: String
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InlineErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun DetailBlock(
    title: String,
    body: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private data class DetailMetric(
    val label: String,
    val value: String
)

@Composable
private fun CompactFilterToolbar(
    title: String,
    onRefresh: () -> Unit,
    summaryLabels: List<String>,
    modifier: Modifier = Modifier,
    primaryContent: @Composable ColumnScope.() -> Unit,
    advancedContent: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(compactChromePadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!expanded && summaryLabels.isNotEmpty()) {
                        Text(
                            text = "${summaryLabels.size} advanced filter${if (summaryLabels.size == 1) "" else "s"} active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onRefresh,
                    contentPadding = compactChromeButtonPadding
                ) {
                    Text("Refresh")
                }

                FilledTonalButton(
                    onClick = { expanded = !expanded },
                    contentPadding = compactChromeButtonPadding
                ) {
                    Text(if (expanded) "Hide advanced" else "Advanced")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                primaryContent()
            }

            AnimatedVisibility(
                visible = !expanded && summaryLabels.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SummaryChipRow(labels = summaryLabels)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    advancedContent()
                }
            }
        }
    }
}

@Composable
private fun ToolbarTextFilter(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) }
    )
}

@Composable
private fun SummaryChipRow(labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DetailMetricsRow(vararg metrics: DetailMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        metrics.forEach { metric ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun <T> FilterGroup(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (label, value) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

private fun Long.formatAmount(currencyCode: String): String = "$this $currencyCode"

private fun Enum<*>.labelize(): String = name.labelize()

private fun ProductActiveFilter.labelize(): String =
    when (this) {
        ProductActiveFilter.ALL -> "All"
        ProductActiveFilter.ACTIVE -> "Active"
        ProductActiveFilter.INACTIVE -> "Inactive"
    }

private fun productSecondaryFilterSummaries(uiState: ProductAdminUiState): List<String> =
    listOfNotNull(
        uiState.filter.category?.let { category -> "Category: ${category.labelize()}" },
        uiState.filter.merchantId?.let { merchantId ->
            val merchantName = uiState.merchants.firstOrNull { merchant -> merchant.id == merchantId }?.name
                ?: "Selected merchant"
            "Merchant: $merchantName"
        }
    )

private fun orderSecondaryFilterSummaries(uiState: OrderAdminUiState): List<String> =
    listOfNotNull(
        uiState.filter.subOrderStatus?.let { status -> "Sub-order: ${status.labelize()}" },
        uiState.filter.merchantId?.let { merchantId ->
            val merchantName = uiState.merchants.firstOrNull { merchant -> merchant.id == merchantId }?.name
                ?: "Selected merchant"
            "Merchant: $merchantName"
        }
    )

private fun String.labelize(): String =
    lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { segment ->
            segment.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase()
                } else {
                    char.toString()
                }
            }
        }

private fun ProductReviewSummary.toDisplayText(): String =
    if (reviewCount == 0 || averageRating == null) {
        "No reviews yet"
    } else {
        "${String.format(Locale.US, "%.1f", averageRating)} / 5 from $reviewCount reviews"
    }

private fun kotlin.time.Instant.formatAdminInstant(): String =
    adminDateFormatter.format(JavaInstant.ofEpochMilli(toEpochMilliseconds()))
