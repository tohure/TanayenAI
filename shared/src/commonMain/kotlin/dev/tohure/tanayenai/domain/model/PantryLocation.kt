package dev.tohure.tanayenai.domain.model

data class PantryLocation(
    val id: String,
    val userId: String,
    val name: String, // "Casa", "Trabajo", "Otros"
    val isDefault: Boolean,
)
