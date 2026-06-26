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

enum class NutritionGoal {
    LOSE_WEIGHT, // Bajar de peso
    GAIN_MUSCLE, // Ganar masa muscular
    MAINTAIN, // Mantener peso
    EAT_HEALTHY, // Salud general
    LOWER_CHOLESTEROL, // Bajar colesterol / triglicéridos
    CONTROL_GLUCOSE, // Controlar glucosa / prevenir diabetes
    REDUCE_INFLAMMATION, // Reducir inflamación (PCR alta / homocisteína)
    IMPROVE_ANEMIA, // Mejorar anemia / niveles de hierro
}

val NutritionGoal.displayName: String
    get() =
        when (this) {
            NutritionGoal.LOSE_WEIGHT -> "Bajar de peso"
            NutritionGoal.GAIN_MUSCLE -> "Ganar masa muscular"
            NutritionGoal.MAINTAIN -> "Mantener peso"
            NutritionGoal.EAT_HEALTHY -> "Salud general"
            NutritionGoal.LOWER_CHOLESTEROL -> "Bajar colesterol"
            NutritionGoal.CONTROL_GLUCOSE -> "Controlar glucosa / prevenir diabetes"
            NutritionGoal.REDUCE_INFLAMMATION -> "Reducir inflamación"
            NutritionGoal.IMPROVE_ANEMIA -> "Mejorar anemia / hierro bajo"
        }

enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }
