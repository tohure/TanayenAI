package dev.tohure.tanayenai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PantryItemDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("location_id") val locationId: String,
    val ingredient: String,
    val quantity: Double,
    val unit: String,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)
