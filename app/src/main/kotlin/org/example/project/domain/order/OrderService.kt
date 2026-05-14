package org.example.project.domain.order

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.example.project.domain.shared.*
import org.example.project.admin.orders.operations.UpdateOrderStatusRequest

class OrderService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) {
    suspend fun checkout(
        characterId: CharacterId,
        shippingSelections: Map<MerchantId, ShippingMethodId>
    ): OrderId {
        val request = CheckoutRequest(
            shippingSelections = shippingSelections.map { (k, v) -> k.value.toString() to v.value.toString() }.toMap()
        )
        return httpClient.post("$baseUrl/orders/checkout/${characterId.value}") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getOrderOrNull(id: OrderId): Order? =
        httpClient.get("$baseUrl/orders/${id.value}").body()

    suspend fun getOrderHistory(
        characterId: CharacterId,
        offset: Long = 0,
        limit: Long = 50
    ): Page<Order> =
        httpClient.get("$baseUrl/orders/history/${characterId.value}") {
            parameter("offset", offset)
            parameter("limit", limit)
        }.body()

    suspend fun getOrderDetailsOrNull(orderId: OrderId): OrderDetails? =
        httpClient.get("$baseUrl/orders/${orderId.value}/details").body()

    suspend fun updateSubOrderStatus(subOrderId: SubOrderId, status: OrderStatus): Boolean {
        return httpClient.patch("$baseUrl/admin/orders/sub-orders/${subOrderId.value}/status") {
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status))
        }.body()
    }

    suspend fun cancelOrder(orderId: OrderId): Boolean =
        httpClient.post("$baseUrl/orders/${orderId.value}/cancel").body()
}
