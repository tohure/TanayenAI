package dev.tohure.tanayenai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoodLogDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("food_name") val foodName: String,
    @SerialName("meal_type") val mealType: String,
    val calories: Float,
    @SerialName("protein_g") val proteinG: Float = 0f,
    @SerialName("carbs_g") val carbsG: Float = 0f,
    @SerialName("fat_g") val fatG: Float = 0f,
    @SerialName("fiber_g") val fiberG: Float = 0f,
    @SerialName("sodium_mg") val sodiumMg: Float = 0f,
    @SerialName("sugar_g") val sugarG: Float = 0f,
    val source: String = "CHAT_DETECTED",
    @SerialName("logged_at") val loggedAt: String,
)
