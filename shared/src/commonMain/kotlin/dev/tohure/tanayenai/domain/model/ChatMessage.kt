package dev.tohure.tanayenai.domain.model

data class ChatMessage(
    val id: String,
    val userId: String,
    val role: ChatRole,
    val content: String, // texto crudo del turno (con tags si es del modelo)
    val createdAt: String,
)

enum class ChatRole {
    USER,
    MODEL,
    ;

    /** Rol tal como lo espera la API de Gemini (`content(role) { ... }`). */
    val geminiRole: String
        get() = if (this == USER) "user" else "model"

    companion object {
        fun fromDb(raw: String): ChatRole = if (raw == USER.geminiRole) USER else MODEL
    }
}
