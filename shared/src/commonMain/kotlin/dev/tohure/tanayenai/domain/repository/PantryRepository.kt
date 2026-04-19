package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryLocation
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    /** Returns all items for the user across all locations. */
    suspend fun getPantryItems(userId: String): List<PantryItem>

    /** Observes items for a specific location (used by [SavePantryIngredientsUseCase]). */
    fun observeItems(
        userId: String,
        locationId: String,
    ): Flow<List<PantryItem>>

    suspend fun getLocations(userId: String): List<PantryLocation>

    /** Insert or replace a pantry item (upsert). Covers both new items and edits. */
    suspend fun upsertItem(item: PantryItem)

    suspend fun deleteItem(itemId: String)

    suspend fun decrementQuantity(
        itemId: String,
        amount: Float,
    )

    suspend fun updateItem(item: PantryItem)
}
