package dev.tohure.tanayenai.data.repository

import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.ChatMessage
import dev.tohure.tanayenai.domain.model.ChatRole
import dev.tohure.tanayenai.domain.repository.ChatMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import db.ChatMessage as DbChatMessage

class ChatMessageRepositoryImpl(
    private val database: TanayenDatabase,
) : ChatMessageRepository {
    private val queries = database.chatMessageQueries

    override suspend fun getRecentMessages(
        userId: String,
        limit: Int,
    ): List<ChatMessage> =
        withContext(Dispatchers.Default) {
            queries
                .getRecentMessages(userId = userId, limit = limit.toLong())
                .executeAsList()
                .map { it.toDomain() }
                .reversed() // más recientes primero → orden cronológico ascendente
        }

    override suspend fun getOldestMessages(
        userId: String,
        limit: Int,
    ): List<ChatMessage> =
        withContext(Dispatchers.Default) {
            queries
                .getOldestMessages(userId = userId, limit = limit.toLong())
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun saveMessage(message: ChatMessage): Unit =
        withContext(Dispatchers.Default) {
            queries.insertMessage(
                id = message.id,
                userId = message.userId,
                role = message.role.geminiRole,
                content = message.content,
                createdAt = message.createdAt,
            )
        }

    override suspend fun countMessages(userId: String): Long =
        withContext(Dispatchers.Default) {
            queries.countMessages(userId).executeAsOne()
        }

    override suspend fun deleteMessages(ids: List<String>): Unit =
        withContext(Dispatchers.Default) {
            if (ids.isEmpty()) return@withContext
            queries.deleteMessages(ids)
        }

    override suspend fun clearMessages(userId: String): Unit =
        withContext(Dispatchers.Default) {
            queries.clearMessages(userId)
        }

    // ── Mapper ─────────────────────────────────────────────────────────────
    private fun DbChatMessage.toDomain() =
        ChatMessage(
            id = id,
            userId = user_id,
            role = ChatRole.fromDb(role),
            content = content,
            createdAt = created_at,
        )
}
