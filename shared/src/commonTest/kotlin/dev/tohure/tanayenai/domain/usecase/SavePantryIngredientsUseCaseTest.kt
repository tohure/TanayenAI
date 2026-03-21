package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryLocation
import dev.tohure.tanayenai.domain.model.PantryUnit
import dev.tohure.tanayenai.domain.repository.PantryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SavePantryIngredientsUseCaseTest {
    private val userId = "user_1"
    private val locationId = "loc_1"

    // ── Fake PantryRepository ─────────────────────────────────────────────────

    private class FakePantryRepository : PantryRepository {
        val savedItems = mutableListOf<PantryItem>()
        var existingItems: List<PantryItem> = emptyList()

        override fun observeItems(
            userId: String,
            locationId: String,
        ): Flow<List<PantryItem>> = flowOf(existingItems)

        override suspend fun addItem(item: PantryItem) {
            savedItems.add(item)
        }

        override suspend fun getLocations(userId: String): List<PantryLocation> = emptyList()

        override suspend fun deleteItem(itemId: String) {}

        override suspend fun updateItem(item: PantryItem) {}

        override suspend fun decrementQuantity(
            itemId: String,
            amount: Float,
        ) {}
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun savesNewIngredientsThatAreNotInPantry() =
        runTest {
            val repo = FakePantryRepository()
            val useCase = SavePantryIngredientsUseCase(repo)

            useCase.saveIngredientsByName(
                names = listOf("avena", "almendras", "yogur griego"),
                locationId = locationId,
                userId = userId,
            )

            assertEquals(3, repo.savedItems.size)
            assertEquals(listOf("avena", "almendras", "yogur griego"), repo.savedItems.map { it.ingredient })
        }

    @Test
    fun skipsIngredientsAlreadyInPantry() =
        runTest {
            val repo = FakePantryRepository()
            repo.existingItems =
                listOf(
                    PantryItem("existing_1", userId, locationId, "avena", 1f, PantryUnit.UNITS, null, "2026-01-01"),
                )
            val useCase = SavePantryIngredientsUseCase(repo)

            useCase.saveIngredientsByName(
                names = listOf("avena", "almendras"),
                locationId = locationId,
                userId = userId,
            )

            assertEquals(1, repo.savedItems.size)
            assertEquals("almendras", repo.savedItems.first().ingredient)
        }

    @Test
    fun deduplicationIsCaseInsensitive() =
        runTest {
            val repo = FakePantryRepository()
            repo.existingItems =
                listOf(
                    PantryItem("existing_1", userId, locationId, "Avena", 1f, PantryUnit.UNITS, null, "2026-01-01"),
                )
            val useCase = SavePantryIngredientsUseCase(repo)

            useCase.saveIngredientsByName(
                names = listOf("avena"),
                locationId = locationId,
                userId = userId,
            )

            assertEquals(0, repo.savedItems.size)
        }

    @Test
    fun trimsWhitespaceFromIngredientNamesBeforeSaving() =
        runTest {
            val repo = FakePantryRepository()
            val useCase = SavePantryIngredientsUseCase(repo)

            useCase.saveIngredientsByName(
                names = listOf("  avena  ", " almendras"),
                locationId = locationId,
                userId = userId,
            )

            assertEquals(listOf("avena", "almendras"), repo.savedItems.map { it.ingredient })
        }

    @Test
    fun savesNothingWhenAllIngredientsAlreadyExist() =
        runTest {
            val repo = FakePantryRepository()
            repo.existingItems =
                listOf(
                    PantryItem("1", userId, locationId, "avena", 1f, PantryUnit.UNITS, null, "2026-01-01"),
                    PantryItem("2", userId, locationId, "almendras", 1f, PantryUnit.UNITS, null, "2026-01-01"),
                )
            val useCase = SavePantryIngredientsUseCase(repo)

            useCase.saveIngredientsByName(
                names = listOf("avena", "almendras"),
                locationId = locationId,
                userId = userId,
            )

            assertEquals(0, repo.savedItems.size)
        }
}
