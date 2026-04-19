package dev.tohure.tanayenai.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test de integración de la BD local.
 * Usa JdbcSqliteDriver (SQLite en memoria) — corre en JVM sin emulador.
 * Comando: ./gradlew :shared:testAndroidHostTest
 */
class PantryRepositoryIntegrationTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: TanayenDatabase
    private lateinit var repository: PantryRepositoryImpl

    private val testUserId = "user_test"
    private val testLocationId = "loc_default"

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TanayenDatabase.Schema.create(driver)
        database = TanayenDatabase(driver)
        repository = PantryRepositoryImpl(database)

        database.pantryLocationQueries.insertLocation(
            id = testLocationId,
            userId = testUserId,
            name = "Casa",
            isDefault = 1L,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    // ── Insert & Retrieve ───────────────────────────────────────────────────

    @Test
    fun `insert PantryItem and retrieve it from the same location`() =
        runTest {
            val item = buildItem(id = "item_1", ingredient = "Avena", quantity = 500f, unit = PantryUnit.GRAMS)

            repository.upsertItem(item)

            val result = repository.getLocations(testUserId)
            assertTrue(result.isNotEmpty(), "Debe haber al menos una ubicación")

            val items =
                database.pantryItemQueries
                    .getItemsByLocation(userId = testUserId, locationId = testLocationId)
                    .executeAsList()

            assertEquals(1, items.size)
            assertEquals("Avena", items.first().ingredient)
            assertEquals(500.0, items.first().quantity)
            assertEquals("GRAMS", items.first().unit)
        }

    @Test
    fun `insert multiple items and retrieve all by user`() =
        runTest {
            repository.upsertItem(buildItem("item_1", "Huevo", 12f, PantryUnit.UNITS))
            repository.upsertItem(buildItem("item_2", "Leche", 1f, PantryUnit.L))
            repository.upsertItem(buildItem("item_3", "Arroz", 2f, PantryUnit.KG))

            val items =
                database.pantryItemQueries
                    .getAllItemsByUser(userId = testUserId)
                    .executeAsList()

            assertEquals(3, items.size)
            val ingredients = items.map { it.ingredient }
            assertTrue("Huevo" in ingredients)
            assertTrue("Leche" in ingredients)
            assertTrue("Arroz" in ingredients)
        }

    // ── Update ──────────────────────────────────────────────────────────────

    @Test
    fun `update item quantity and verify change persists`() =
        runTest {
            val item = buildItem("item_1", "Avena", 500f, PantryUnit.GRAMS)
            repository.upsertItem(item)

            repository.updateItem(item.copy(quantity = 250f))

            val updated =
                database.pantryItemQueries
                    .getItemsByLocation(userId = testUserId, locationId = testLocationId)
                    .executeAsList()
                    .first()

            assertEquals(250.0, updated.quantity, "La cantidad debe haberse actualizado")
        }

    // ── Delete ──────────────────────────────────────────────────────────────

    @Test
    fun `delete item removes it from DB`() =
        runTest {
            val item = buildItem("item_1", "Avena", 500f, PantryUnit.GRAMS)
            repository.upsertItem(item)

            repository.deleteItem("item_1")

            val items =
                database.pantryItemQueries
                    .getItemsByLocation(userId = testUserId, locationId = testLocationId)
                    .executeAsList()

            assertTrue(items.isEmpty(), "El item debe haber sido eliminado")
        }

    // ── Decrement ───────────────────────────────────────────────────────────

    @Test
    fun `decrement quantity reduces stock correctly`() =
        runTest {
            repository.upsertItem(buildItem("item_1", "Nueces", 300f, PantryUnit.GRAMS))

            repository.decrementQuantity("item_1", 100f)

            val item =
                database.pantryItemQueries
                    .getItemsByLocation(userId = testUserId, locationId = testLocationId)
                    .executeAsList()
                    .first()

            assertEquals(200.0, item.quantity, "Debería quedar 200g después de decrementar 100g")
        }

    @Test
    fun `decrement quantity does not go below zero`() =
        runTest {
            repository.upsertItem(buildItem("item_1", "Nueces", 50f, PantryUnit.GRAMS))

            repository.decrementQuantity("item_1", 200f) // Más de lo que hay

            val item =
                database.pantryItemQueries
                    .getItemsByLocation(userId = testUserId, locationId = testLocationId)
                    .executeAsList()
                    .first()

            assertEquals(0.0, item.quantity, "La cantidad no debe ser negativa (MAX(0, ...))")
        }

    // ── Helper ──────────────────────────────────────────────────────────────

    private fun buildItem(
        id: String,
        ingredient: String,
        quantity: Float,
        unit: PantryUnit,
    ) = PantryItem(
        id = id,
        userId = testUserId,
        locationId = testLocationId,
        ingredient = ingredient,
        quantity = quantity,
        unit = unit,
        updatedAt = "2026-03-03T12:00:00Z",
    )
}
