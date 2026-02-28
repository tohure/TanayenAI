package dev.tohure.tanayenai.domain.model

data class User(
    val id: String,
    val name: String,
    val birthDate: String,
    val sex: Sex,
    val heightCm: Float,
    val goal: NutritionGoal,
    val activityLevel: ActivityLevel,
)

enum class Sex { MALE, FEMALE }

enum class NutritionGoal { LOSE_WEIGHT, GAIN_MUSCLE, MAINTAIN, EAT_HEALTHY }

enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }
