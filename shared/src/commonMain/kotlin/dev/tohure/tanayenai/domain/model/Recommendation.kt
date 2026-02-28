package dev.tohure.tanayenai.domain.model

data class Recommendation(
    val id: String,
    val userId: String,
    val type: RecommendationType,
    val title: String,
    val content: String, // JSON detail
    val ingredientsUsed: List<String>,
    val recommendedAt: String,
)

enum class RecommendationType { MEAL, RECIPE, PLAN, SNACK, ALERT }
