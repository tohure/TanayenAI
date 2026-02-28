package dev.tohure.tanayenai.domain.model

data class FoodLog(
    val id: String,
    val userId: String,
    val foodName: String,
    val calories: Float? = null,
    val proteinG: Float? = null,
    val carbsG: Float? = null,
    val fatG: Float? = null,
    val mealType: MealType,
    val locationId: String? = null,
    val loggedAt: String,
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
