package dev.tohure.tanayenai.domain.usecase

import co.touchlab.kermit.Logger
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.FoodLogSource
import dev.tohure.tanayenai.domain.model.MealType
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.model.generateId
import dev.tohure.tanayenai.domain.repository.FoodLogRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val log = Logger.withTag("EstimateFoodNutritionUseCase")
private val jsonParser =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

@Serializable
private data class NutritionEstimationDto(
    val food_name: String,
    val meal_type: String,
    val calories: Float,
    val protein_g: Float,
    val carbs_g: Float,
    val fat_g: Float,
    val fiber_g: Float,
    val sodium_mg: Float,
    val sugar_g: Float,
)

class EstimateFoodNutritionUseCase(
    private val generativeModel: GenerativeModel,
    private val repository: FoodLogRepository,
    private val userId: String,
) {
    suspend fun estimateAndSave(
        foodDescription: String,
        source: FoodLogSource,
        mealTypeHint: MealType? = null,
    ): EstimationResult {
        return try {
            val raw = callGemini(foodDescription, mealTypeHint)
            val dto = parseResponse(raw) ?: return EstimationResult.ParseError(foodDescription)

            val foodLog =
                FoodLog(
                    id = generateId(),
                    userId = userId,
                    foodName = dto.food_name,
                    mealType = parseMealType(dto.meal_type) ?: mealTypeHint ?: MealType.SNACK,
                    calories = dto.calories,
                    proteinG = dto.protein_g,
                    carbsG = dto.carbs_g,
                    fatG = dto.fat_g,
                    fiberG = dto.fiber_g,
                    sodiumMg = dto.sodium_mg,
                    sugarG = dto.sugar_g,
                    source = source,
                    loggedAt = currentIsoDateTime(),
                )

            repository.insertFoodLog(foodLog)
            EstimationResult.Success(foodLog)
        } catch (e: Exception) {
            log.e(e) { "Estimation failed for: $foodDescription" }
            EstimationResult.Error(e.message ?: "Error desconocido")
        }
    }

    private suspend fun callGemini(
        description: String,
        mealTypeHint: MealType?,
    ): String {
        val mealHint = mealTypeHint?.let { "La comida es un ${it.displayName}." } ?: ""

        val prompt =
            """
            Estima la información nutricional de esta comida: "$description"
            $mealHint

            Responde ÚNICAMENTE con JSON válido sin texto adicional:
            {
              "food_name": "nombre descriptivo de la comida en español",
              "meal_type": "BREAKFAST|LUNCH|DINNER|SNACK",
              "calories": número,
              "protein_g": número,
              "carbs_g": número,
              "fat_g": número,
              "fiber_g": número,
              "sodium_mg": número,
              "sugar_g": número
            }

            Reglas:
            - Usa porciones estándar si no se especifica cantidad
            - Si hay múltiples alimentos, suma los valores nutricionales
            - Infiere el meal_type por el contexto (avena → BREAKFAST, etc.)
            - No uses decimales con más de 1 dígito
            - Valores aproximados son aceptables — no inventes cifras exactas
            """.trimIndent()

        return generativeModel
            .generateContent(content { text(prompt) })
            .text ?: throw Exception("Sin respuesta")
    }

    private fun parseResponse(raw: String): NutritionEstimationDto? {
        val cleaned =
            raw
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
        return try {
            jsonParser.decodeFromString<NutritionEstimationDto>(cleaned)
        } catch (e: Exception) {
            log.e(e) { "Parse failed: $cleaned" }
            null
        }
    }

    private fun parseMealType(raw: String): MealType? =
        runCatching {
            MealType.valueOf(raw.uppercase())
        }.getOrNull()
}

sealed class EstimationResult {
    data class Success(
        val log: FoodLog,
    ) : EstimationResult()

    data class ParseError(
        val originalDescription: String,
    ) : EstimationResult()

    data class Error(
        val message: String,
    ) : EstimationResult()
}
