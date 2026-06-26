package dev.tohure.tanayenai.domain.model

data class PantryItem(
    val id: String,
    val userId: String,
    val locationId: String,
    val ingredient: String,
    val quantity: Float,
    val unit: PantryUnit,
    val expiryDate: String? = null,
    val updatedAt: String,
)

enum class PantryUnit { GRAMS, KG, ML, L, UNITS, TBSP, TSP, CUPS }
