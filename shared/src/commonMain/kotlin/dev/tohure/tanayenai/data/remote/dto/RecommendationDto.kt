package dev.tohure.tanayenai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecommendationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val content: String,
    @SerialName("ingredients_used") val ingredientsUsed: List<String>,
    @SerialName("recommended_at") val recommendedAt: String,
)
