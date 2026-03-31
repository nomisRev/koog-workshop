package org.example.project.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.collections.immutable.toPersistentList
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import org.example.project.domain.model.DashboardSnapshot
import org.example.project.service.AdminDashboardService

class DashboardViewModel(
    private val dashboardService: AdminDashboardService
) : ViewModel() {

    private val loadVersion = AtomicLong(0L)
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Uninitialized)

    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    suspend fun loadDashboard() {
        val version = loadVersion.incrementAndGet()
        _uiState.value = DashboardUiState.Loading

        val nextState = try {
            dashboardService.loadDashboard().toUiState()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            DashboardUiState.Error(throwable.message ?: "Unable to load the dashboard.")
        }

        if (loadVersion.get() == version) {
            _uiState.value = nextState
        }
    }

    fun reset() {
        loadVersion.incrementAndGet()
        _uiState.value = DashboardUiState.Uninitialized
    }

    companion object {
        fun factory(dashboardService: AdminDashboardService): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    if (modelClass == DashboardViewModel::class) {
                        return DashboardViewModel(dashboardService) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.simpleName ?: "unknown"}"
                    )
                }
            }
    }

    private fun DashboardSnapshot.toUiState(): DashboardUiState {
        val readyState = DashboardUiState.Ready(
            summary = summary,
            lowStockProducts = lowStockProducts.toPersistentList(),
            recentOrders = recentOrders.toPersistentList()
        )

        return if (readyState.isDashboardEmpty()) {
            DashboardUiState.Empty
        } else {
            readyState
        }
    }
}
