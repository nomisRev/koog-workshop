package org.example.project.admin

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.db.tables.Characters
import org.example.project.db.tables.Currencies
import org.example.project.db.tables.Orders
import org.example.project.domain.enums.OrderStatus
import org.example.project.service.AdminDashboardService
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class DashboardViewModelTest {

    @Test
    fun `loadRecentOrders maps an empty database to Ready with no orders`() = runBlocking {
        val database = createDatabase()
        val viewModel = DashboardViewModel(AdminDashboardService(database))

        viewModel.loadRecentOrders()

        val state = viewModel.uiState.value
        assertIs<DashboardUiState.Ready>(state)
        assertTrue(state.recentOrders.isEmpty())
    }

    @Test
    fun `loadRecentOrders maps dashboard data to Ready`() = runBlocking {
        val database = createDatabase()
        seedDashboardData(database)
        val viewModel = DashboardViewModel(AdminDashboardService(database))

        viewModel.loadRecentOrders()

        val state = viewModel.uiState.value
        assertIs<DashboardUiState.Ready>(state)
        assertEquals(
            listOf(Instant.fromEpochMilliseconds(2_000), Instant.fromEpochMilliseconds(1_000)),
            state.recentOrders.map { it.createdAt }
        )
        assertEquals(
            listOf(OrderStatus.DELIVERED, OrderStatus.PENDING),
            state.recentOrders.map { it.status }
        )
        assertEquals(listOf(2_000L, 1_000L), state.recentOrders.map { it.totalPrice })
        assertTrue(state.recentOrders.all { it.totalCurrencyCode == "GOLD" })
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("dashboard_viewmodel_", ".db").apply {
            deleteOnExit()
        }
        return Database.connect("jdbc:sqlite:${databaseFile.absolutePath}").createTables()
    }

    private fun seedDashboardData(database: Database) {
        transaction(database) {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }

            val characterId = Characters.insertAndGetId {
                it[name] = "Aldric"
            }

            Orders.insertAndGetId {
                it[character] = characterId
                it[status] = OrderStatus.PENDING.name
                it[totalPrice] = 1_000
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(1_000)
            }
            Orders.insertAndGetId {
                it[character] = characterId
                it[status] = OrderStatus.DELIVERED.name
                it[totalPrice] = 2_000
                it[totalCurrency] = goldId
                it[createdAt] = Instant.fromEpochMilliseconds(2_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
            }
        }
    }
}
