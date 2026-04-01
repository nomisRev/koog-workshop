package org.example.project.admin

import androidx.compose.runtime.Immutable
import org.example.project.domain.shared.OrderItemId
import org.example.project.domain.admin.AdminOrderDetail

@Immutable
sealed interface AdminOrderDetailUiState {
    data object Loading : AdminOrderDetailUiState

    data class Error(val message: String) : AdminOrderDetailUiState

    data class Ready(
        val detail: AdminOrderDetail,
        val selectedItemId: OrderItemId? = null
    ) : AdminOrderDetailUiState
}
