package org.example.project.domain.repository

import org.example.project.domain.enums.OrderStatus
import org.example.project.domain.id.CharacterId
import org.example.project.domain.id.OrderId
import org.example.project.domain.id.SubOrderId
import org.example.project.domain.model.Order
import org.example.project.domain.model.OrderItem
import org.example.project.domain.model.SubOrder

interface OrderRepository {
    suspend fun getOrderOrNull(id: OrderId): Order?
    suspend fun getOrderHistory(characterId: CharacterId): List<Order>
    suspend fun getSubOrders(orderId: OrderId): List<SubOrder>
    suspend fun getOrderItems(subOrderId: SubOrderId): List<OrderItem>
    suspend fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean
}
