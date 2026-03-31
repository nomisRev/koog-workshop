package org.example.project.domain.model

data class OrderDetails(
    val order: Order,
    val subOrders: List<SubOrderDetails>
)

data class SubOrderDetails(
    val subOrder: SubOrder,
    val items: List<OrderItem>
)
