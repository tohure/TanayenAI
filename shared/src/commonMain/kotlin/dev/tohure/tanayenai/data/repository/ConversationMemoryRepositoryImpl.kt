package dev.tohure.tanayenai.data.repository

import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.repository.ConversationMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConversationMemoryRepositoryImpl(
    private val database: TanayenDatabase,
) : ConversationMemoryRepository {
    private val queries = database.conversationSummaryQueries

    override suspend fun getSummary(userId: String): String? =
        withContext(Dispatchers.Default) {
            queries
                .getSummary(userId)
                .executeAsOneOrNull()
                ?.summary
        }

    override suspend fun saveSummary(
        userId: String,
        summary: String,
    ): Unit =
        withContext(Dispatchers.Default) {
            queries.upsertSummary(
                userId = userId,
                summary = summary,
                updatedAt = currentIsoDateTime(),
            )
        }
}
