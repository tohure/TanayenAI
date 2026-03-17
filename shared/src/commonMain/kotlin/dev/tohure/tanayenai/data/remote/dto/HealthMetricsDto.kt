package dev.tohure.tanayenai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthMetricsDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    @SerialName("weight_kg") val weightKg: Double? = null,
    val imc: Double? = null,
    @SerialName("sleep_hours") val sleepHours: Double? = null,
    val hrv: Double? = null,
    @SerialName("calories_burned") val caloriesBurned: Int? = null,
    val steps: Int? = null,
    val source: String,
)
