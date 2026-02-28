package dev.tohure.tanayenai.data.repository

import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.RecommendationType
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import db.Recommendation as DbRecommendation

class RecommendationRepositoryImpl(
    private val database: TanayenDatabase,
) : RecommendationRepository {
    private val queries = database.recommendationQueries

    override suspend fun getRecentRecommendations(
        userId: String,
        days: Int,
    ): List<Recommendation> =
        withContext(Dispatchers.Default) {
            val fromDate =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .minus(days, DateTimeUnit.DAY)
                    .toString()

            queries
                .getRecentRecommendations(userId = userId, fromDate = fromDate)
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun saveRecommendation(recommendation: Recommendation): Unit =
        withContext(Dispatchers.Default) {
            queries.insertRecommendation(
                id = recommendation.id,
                userId = recommendation.userId,
                type = recommendation.type.name,
                title = recommendation.title,
                content = recommendation.content,
                ingredientsUsed = Json.encodeToString(recommendation.ingredientsUsed),
                recommendedAt = recommendation.recommendedAt,
            )
        }

    override suspend fun findSimilar(
        embedding: List<Float>,
        threshold: Float,
    ): List<Recommendation> =
        withContext(Dispatchers.Default) {
            queries
                .getLastNRecommendations(userId = "local_user", limit = 10L)
                .executeAsList()
                .map { it.toDomain() }
        }

    // ── Mapper ─────────────────────────────────────────────────────────────
    private fun DbRecommendation.toDomain() =
        Recommendation(
            id = id,
            userId = user_id,
            type = RecommendationType.valueOf(type),
            title = title,
            content = content,
            ingredientsUsed = Json.decodeFromString(ingredients_used),
            recommendedAt = recommended_at,
        )
}
