package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryLocation
import kotlinx.coroutines.flow.Flow

interface PantryRepository {
    fun observeItems(
        userId: String,
        locationId: String,
    ): Flow<List<PantryItem>>

    suspend fun getLocations(userId: String): List<PantryLocation>

    suspend fun addItem(item: PantryItem)

    suspend fun updateItem(item: PantryItem)

    suspend fun deleteItem(itemId: String)

    suspend fun decrementQuantity(
        itemId: String,
        amount: Float,
    )
}
