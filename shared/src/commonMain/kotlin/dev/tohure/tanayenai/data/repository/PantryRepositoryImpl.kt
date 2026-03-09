package dev.tohure.tanayenai.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryLocation
import dev.tohure.tanayenai.domain.model.PantryUnit
import dev.tohure.tanayenai.domain.repository.PantryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import db.PantryItem as DbPantryItem
import db.PantryLocation as DbPantryLocation

class PantryRepositoryImpl(
    private val database: TanayenDatabase,
) : PantryRepository {
    private val itemQueries = database.pantryItemQueries
    private val locationQueries = database.pantryLocationQueries

    override fun observeItems(
        userId: String,
        locationId: String,
    ): Flow<List<PantryItem>> =
        itemQueries
            .getItemsByLocation(userId = userId, locationId = locationId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { dbItems -> dbItems.map { it.toDomain() } }

    override suspend fun getLocations(userId: String): List<PantryLocation> =
        withContext(Dispatchers.Default) {
            locationQueries.getLocationsByUser(userId = userId).executeAsList().map { it.toDomain() }
        }

    override suspend fun addItem(item: PantryItem): Unit =
        withContext(Dispatchers.Default) {
            itemQueries.insertItem(
                id = item.id,
                userId = item.userId,
                locationId = item.locationId,
                ingredient = item.ingredient,
                quantity = item.quantity.toDouble(),
                unit = item.unit.name,
                expiryDate = item.expiryDate,
                updatedAt = Clock.System.now().toString(),
            )
        }

    override suspend fun updateItem(item: PantryItem): Unit =
        withContext(Dispatchers.Default) {
            itemQueries.updateItem(
                quantity = item.quantity.toDouble(),
                unit = item.unit.name,
                expiryDate = item.expiryDate,
                updatedAt = Clock.System.now().toString(),
                id = item.id,
            )
        }

    override suspend fun deleteItem(itemId: String): Unit =
        withContext(Dispatchers.Default) {
            itemQueries.deleteItem(itemId)
        }

    override suspend fun decrementQuantity(
        itemId: String,
        amount: Float,
    ): Unit =
        withContext(Dispatchers.Default) {
            itemQueries.decrementQuantity(
                amount = amount.toDouble(),
                updatedAt = Clock.System.now().toString(),
                id = itemId,
            )
        }

    // ── Mappers ────────────────────────────────────────────────────────────
    private fun DbPantryItem.toDomain() =
        PantryItem(
            id = id,
            userId = user_id,
            locationId = location_id,
            ingredient = ingredient,
            quantity = quantity.toFloat(),
            unit = PantryUnit.valueOf(unit),
            expiryDate = expiry_date,
            updatedAt = updated_at,
        )

    private fun DbPantryLocation.toDomain() =
        PantryLocation(
            id = id,
            userId = user_id,
            name = name,
            isDefault = is_default == 1L,
        )
}
