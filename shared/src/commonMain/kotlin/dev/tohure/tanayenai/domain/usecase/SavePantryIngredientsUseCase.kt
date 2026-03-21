package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryUnit
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.model.generateId
import dev.tohure.tanayenai.domain.repository.PantryRepository
import kotlinx.coroutines.flow.first

class SavePantryIngredientsUseCase(
    private val pantryRepository: PantryRepository,
) {
    suspend fun saveIngredientsByName(
        names: List<String>,
        locationId: String,
        userId: String,
    ) {
        val currentPantry = pantryRepository.observeItems(userId, locationId).first()

        names.forEach { name ->
            val existing =
                currentPantry.find {
                    it.ingredient.equals(name.trim(), ignoreCase = true)
                }
            if (existing == null) {
                pantryRepository.addItem(
                    PantryItem(
                        id = generateId(),
                        userId = userId,
                        locationId = locationId,
                        ingredient = name.trim(),
                        quantity = 1f,
                        unit = PantryUnit.UNITS,
                        expiryDate = null,
                        updatedAt = currentIsoDateTime(),
                    ),
                )
            }
        }
    }
}
