package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.Recommendation

interface RecommendationRepository {
    suspend fun getRecentRecommendations(
        userId: String,
        days: Int = 7,
    ): List<Recommendation>

    suspend fun saveRecommendation(recommendation: Recommendation)

    suspend fun findSimilar(
        embedding: List<Float>,
        threshold: Float = 0.85f,
    ): List<Recommendation>
}
