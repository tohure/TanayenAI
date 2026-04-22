package dev.tohure.tanayenai.domain.model

data class FoodLog(
    val id: String,
    val userId: String,
    val foodName: String,
    val mealType: MealType,
    val calories: Float = 0f,
    // Macros
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    // Micronutrientes clave
    val fiberG: Float = 0f,
    val sodiumMg: Float = 0f,
    val sugarG: Float = 0f,
    // Origen del registro
    val source: FoodLogSource = FoodLogSource.MANUAL,
    val loggedAt: String,
)

enum class MealType(
    val displayName: String,
) {
    BREAKFAST("Desayuno"),
    LUNCH("Almuerzo"),
    DINNER("Cena"),
    SNACK("Snack"),
}

enum class FoodLogSource {
    CHAT_DETECTED,
    PROACTIVE_CHECKIN,
    MANUAL,
}

// Resumen del día para el Dashboard
data class DailyNutritionSummary(
    val date: String,
    val totalCalories: Float,
    val totalProteinG: Float,
    val totalCarbsG: Float,
    val totalFatG: Float,
    val totalFiberG: Float,
    val totalSodiumMg: Float,
    val totalSugarG: Float,
    val mealCount: Int,
    val calorieGoal: Float = 2000f,
) {
    val calorieProgress: Float get() = (totalCalories / calorieGoal).coerceIn(0f, 1f)
    val remainingCalories: Float get() = (calorieGoal - totalCalories).coerceAtLeast(0f)
}
