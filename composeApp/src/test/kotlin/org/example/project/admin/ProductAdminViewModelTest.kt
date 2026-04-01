package org.example.project.admin

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.domain.admin.ProductActiveFilter
import org.example.project.domain.admin.ProductAdminService
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Potions
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.Products
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.catalog.Weapons
import org.example.project.domain.currency.Currencies
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ProductAdminViewModelTest {

    @Test
    fun `load selects the first product detail`() = runBlocking {
        val database = createDatabase()
        val fixture = seedProducts(database)
        val viewModel = ProductAdminViewModel(ProductAdminService(database))

        viewModel.load()

        val state = viewModel.uiState.value
        assertEquals(listOf(fixture.bronzeBladeId, fixture.moonwellDraughtId), state.products.map { it.id })
        assertEquals(fixture.bronzeBladeId, state.selectedProductId)
        assertNotNull(state.selectedProduct)
        assertEquals("Blackforge Armory", state.selectedProduct.merchantName)
    }

    @Test
    fun `active filter refreshes products and selection`() = runBlocking {
        val database = createDatabase()
        val fixture = seedProducts(database)
        val viewModel = ProductAdminViewModel(ProductAdminService(database))

        viewModel.load()
        viewModel.updateActiveFilter(ProductActiveFilter.INACTIVE)

        val state = viewModel.uiState.value
        assertEquals(listOf(fixture.moonwellDraughtId), state.products.map { it.id })
        assertEquals(fixture.moonwellDraughtId, state.selectedProductId)
        assertEquals("Moonwell Draught", state.selectedProduct?.name)
    }

    private fun createDatabase(): Database {
        val databaseFile = java.io.File.createTempFile("product_vm_", ".db").apply {
            deleteOnExit()
        }
        return Database.connect("jdbc:sqlite:${databaseFile.absolutePath}").createTables()
    }

    private fun seedProducts(database: Database): ProductFixture =
        transaction(database) {
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }
            val blackforgeId = Merchants.insertAndGetId {
                it[name] = "Blackforge Armory"
            }
            val moonwellId = Merchants.insertAndGetId {
                it[name] = "Moonwell Remedies"
            }

            val bronzeBladeId = Products.insertAndGetId {
                it[name] = "Bronze Blade"
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = Rarity.UNCOMMON.name
                it[price] = 320
                it[currency] = goldId
                it[merchant] = blackforgeId
                it[stock] = 12
                it[createdAt] = Instant.fromEpochMilliseconds(2_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
            }
            Weapons.insert {
                it[id] = bronzeBladeId
                it[damage] = 14
                it[damageType] = org.example.project.domain.catalog.DamageType.PHYSICAL.name
                it[weaponSlot] = org.example.project.domain.catalog.WeaponSlot.MAIN_HAND.name
            }

            val moonwellDraughtId = Products.insertAndGetId {
                it[name] = "Moonwell Draught"
                it[category] = ProductCategory.POTIONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 90
                it[currency] = goldId
                it[merchant] = moonwellId
                it[stock] = 6
                it[isActive] = false
                it[createdAt] = Instant.fromEpochMilliseconds(1_000)
                it[updatedAt] = Instant.fromEpochMilliseconds(1_000)
            }
            Potions.insert {
                it[id] = moonwellDraughtId
                it[effect] = "Restore stamina"
                it[duration] = 3
            }

            ProductFixture(
                bronzeBladeId = org.example.project.domain.shared.ProductId(bronzeBladeId.value),
                moonwellDraughtId = org.example.project.domain.shared.ProductId(moonwellDraughtId.value)
            )
        }

    private data class ProductFixture(
        val bronzeBladeId: org.example.project.domain.shared.ProductId,
        val moonwellDraughtId: org.example.project.domain.shared.ProductId
    )
}
