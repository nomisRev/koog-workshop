package org.example.project.domain.catalog

import kotlinx.coroutines.runBlocking
import org.example.project.db.createTables
import org.example.project.domain.currency.CurrencyService
import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class CatalogServiceTest {
    private lateinit var database: Database
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.random())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.random())

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_catalog_", ".db").apply { deleteOnExit() }
        database = Database.connect("jdbc:sqlite:${testDbFile.absolutePath}").createTables()
        currencyService = CurrencyService(database)
        catalogService = CatalogService(database)
        runBlocking {
            goldId = currencyService.createCurrency("GOLD", "Gold", "G")
            merchantId = catalogService.createMerchant("Test Merchant")
        }
    }

    private fun createWeapon(name: String = "Test Sword") = Product.Weapon(
        id = ProductId(kotlin.uuid.Uuid.random()),
        name = name,
        description = null,
        rarity = Rarity.COMMON,
        price = 100,
        currencyId = goldId,
        merchantId = merchantId,
        stock = 10,
        imageUrl = null,
        isActive = true,
        createdAt = kotlin.time.Instant.DISTANT_PAST,
        updatedAt = kotlin.time.Instant.DISTANT_PAST,
        damage = 5,
        damageType = DamageType.PHYSICAL,
        weaponSlot = WeaponSlot.MAIN_HAND
    )

    private fun createPotion(name: String = "Health Potion") = Product.Potion(
        id = ProductId(kotlin.uuid.Uuid.random()),
        name = name,
        description = null,
        rarity = Rarity.COMMON,
        price = 50,
        currencyId = goldId,
        merchantId = merchantId,
        stock = 20,
        imageUrl = null,
        isActive = true,
        createdAt = kotlin.time.Instant.DISTANT_PAST,
        updatedAt = kotlin.time.Instant.DISTANT_PAST,
        effect = "Restores health",
        duration = 10
    )

    @Test
    fun testCreateAndGetProduct() = runBlocking {
        val weapon = createWeapon()
        val id = catalogService.createProduct(weapon)
        val product = catalogService.getProduct(id)
        assertNotNull(product)
        assertIs<Product.Weapon>(product)
        assertEquals("Test Sword", product.name)
        assertEquals(5, product.damage)
        assertEquals(DamageType.PHYSICAL, product.damageType)
        assertEquals(WeaponSlot.MAIN_HAND, product.weaponSlot)
    }

    @Test
    fun testGetProductsByCategory() = runBlocking {
        catalogService.createProduct(createWeapon())
        catalogService.createProduct(createPotion())
        val weapons = catalogService.getProductsByCategory(ProductCategory.WEAPONS)
        assertEquals(1, weapons.size)
    }

    @Test
    fun testUpdateProduct() = runBlocking {
        val weapon = createWeapon()
        val id = catalogService.createProduct(weapon)
        val created = catalogService.getProduct(id)
        assertNotNull(created)
        assertIs<Product.Weapon>(created)
        val updated = catalogService.updateProduct(created.copy(name = "Epic Sword"))
        assertTrue(updated)
        val fetched = catalogService.getProduct(id)
        assertNotNull(fetched)
        assertEquals("Epic Sword", fetched.name)
    }

    @Test
    fun testDeleteProduct() = runBlocking {
        val id = catalogService.createProduct(createWeapon())
        val deleted = catalogService.deleteProduct(id)
        assertTrue(deleted)
        val product = catalogService.getProduct(id)
        assertNull(product)
    }

    @Test
    fun testCreateAndGetMerchant() = runBlocking {
        val id = catalogService.createMerchant("Magic Shop", description = "Sells magical items")
        val merchant = catalogService.getMerchant(id)
        assertNotNull(merchant)
        assertEquals("Magic Shop", merchant.name)
        assertEquals("Sells magical items", merchant.description)
    }

    @Test
    fun testUpdateMerchant() = runBlocking {
        val id = catalogService.createMerchant("Magic Shop")
        val updated = catalogService.updateMerchant(id, name = "Grand Magic Shop")
        assertTrue(updated)
        val merchant = catalogService.getMerchant(id)
        assertNotNull(merchant)
        assertEquals("Grand Magic Shop", merchant.name)
    }

    @Test
    fun testDeleteMerchant() = runBlocking {
        val id = catalogService.createMerchant("Magic Shop")
        val deleted = catalogService.deleteMerchant(id)
        assertTrue(deleted)
        val merchant = catalogService.getMerchant(id)
        assertNull(merchant)
    }

    @Test
    fun testGetMerchantsPagination() = runBlocking {
        catalogService.createMerchant("Shop A")
        catalogService.createMerchant("Shop B")
        catalogService.createMerchant("Shop C")
        val page = catalogService.getMerchants(offset = 0, limit = 2)
        assertEquals(2, page.items.size)
        assertEquals(4L, page.total) // 3 created + 1 from setup
    }
}
