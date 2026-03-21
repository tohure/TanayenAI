package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.User
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.model.daysAgo
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository

/**
 * Obtiene los datos necesarios para construir el contexto de Gemini.
 * Los ViewModels ya no acceden directamente a los repositorios para este fin.
 *
 * User y ClinicalProfile se reciben como parámetros hasta que UserRepository
 * y ClinicalProfileRepository sean implementados en una fase futura.
 */
class FetchContextParamsUseCase(
    private val healthMetricsRepository: HealthMetricsRepository,
    private val recommendationRepository: RecommendationRepository,
) {
    suspend fun fetch(
        userId: String,
        user: User,
        clinicalProfile: ClinicalProfile?,
        today: String = currentIsoDate(),
        workContext: String = "Sin especificar",
    ): ContextParams {
        val recentMetrics =
            healthMetricsRepository.getMetricsForDateRange(
                userId,
                daysAgo(7),
                today,
            )
        val recentRecommendations =
            recommendationRepository.getRecentRecommendations(
                userId,
                days = 7,
            )
        return ContextParams(
            user = user,
            clinicalProfile = clinicalProfile,
            recentMetrics = recentMetrics,
            pantryItems = emptyList(), // TODO: PantryRepository en fase futura
            locationNames = emptyMap(),
            recentRecommendations = recentRecommendations,
            todayFoodLogs = emptyList(),
            today = today,
            workContext = workContext,
        )
    }
}
