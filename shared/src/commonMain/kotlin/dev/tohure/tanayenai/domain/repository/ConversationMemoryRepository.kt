package dev.tohure.tanayenai.domain.repository

interface ConversationMemoryRepository {
    /** Resumen rodante acumulado de sesiones previas, o null si aún no existe. */
    suspend fun getSummary(userId: String): String?

    suspend fun saveSummary(
        userId: String,
        summary: String,
    )
}
