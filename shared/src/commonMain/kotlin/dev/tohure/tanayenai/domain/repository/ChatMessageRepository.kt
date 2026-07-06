package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.ChatMessage

interface ChatMessageRepository {
    /** Últimos [limit] mensajes en orden cronológico ascendente (más antiguo primero). */
    suspend fun getRecentMessages(
        userId: String,
        limit: Int,
    ): List<ChatMessage>

    /** Los [limit] mensajes más antiguos (para compactar en el resumen rodante). */
    suspend fun getOldestMessages(
        userId: String,
        limit: Int,
    ): List<ChatMessage>

    suspend fun saveMessage(message: ChatMessage)

    suspend fun countMessages(userId: String): Long

    suspend fun deleteMessages(ids: List<String>)

    suspend fun clearMessages(userId: String)
}
