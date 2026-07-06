package dev.tohure.tanayenai.presentation.model

import dev.tohure.tanayenai.domain.model.IngredientCategory
import dev.tohure.tanayenai.domain.model.PantryItem
import kotlinx.collections.immutable.ImmutableList

/** A pantry item paired with its auto-classified category. */
data class CategorizedItem(
    val item: PantryItem,
    val category: IngredientCategory,
)

/**
 * A group of items belonging to the same ingredient category.
 *
 * Using a flat [List] of [CategoryGroup] instead of [Map]<[IngredientCategory], List> avoids
 * Kotlin/Native Map bridging complexity when consumed by the iOS wrapper. Arrays of data classes
 * bridge cleanly; Maps require runtime casts on the Swift side.
 */
data class CategoryGroup(
    val category: IngredientCategory,
    val items: ImmutableList<CategorizedItem>,
)
