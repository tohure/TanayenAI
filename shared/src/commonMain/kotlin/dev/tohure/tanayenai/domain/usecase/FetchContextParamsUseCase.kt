package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.ActivityLevel
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.Sex
import dev.tohure.tanayenai.domain.model.User
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.model.daysAgo
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.repository.UserRepository

/**
 * Obtiene los datos necesarios para construir el contexto de Gemini.
 * Lee User y ClinicalProfile internamente desde sus repositorios.
 * Si no existe un User guardado, usa un placeholder temporal (hasta Fase 5D).
 */
class FetchContextParamsUseCase(
    private val healthMetricsRepository: HealthMetricsRepository,
    private val recommendationRepository: RecommendationRepository,
    private val clinicalProfileRepository: ClinicalProfileRepository,
    private val userRepository: UserRepository,
) {
    suspend fun fetch(
        userId: String,
        today: String = currentIsoDate(),
        workContext: String = "Sin especificar",
    ): ContextParams {
        val storedUser = userRepository.getUser(userId)
        val userExistsInDb = storedUser != null
        val user = storedUser ?: placeholderUser(userId)
        val clinicalProfile = clinicalProfileRepository.getClinicalProfile(userId)

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
            userExistsInDb = userExistsInDb,
            clinicalProfile = clinicalProfile,
            recentMetrics = recentMetrics,
            pantryItems = emptyList(), // TODO: Conectar PantryRepository cuando la pantalla Dispensa esté lista
            locationNames = emptyMap(),
            recentRecommendations = recentRecommendations,
            todayFoodLogs = emptyList(),
            today = today,
            workContext = workContext,
        )
    }

    // Placeholder temporal hasta que exista onboarding de usuario (Fase 5D)
    private fun placeholderUser(userId: String) =
        User(
            id = userId,
            name = "Carlo",
            birthDate = "1990-05-15",
            sex = Sex.MALE,
            heightCm = 175f,
            goal = NutritionGoal.EAT_HEALTHY,
            activityLevel = ActivityLevel.MODERATE,
        )
}
