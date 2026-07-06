package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.ActivityLevel
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.Sex
import dev.tohure.tanayenai.domain.model.User
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.model.daysAgo
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import dev.tohure.tanayenai.domain.repository.ConversationMemoryRepository
import dev.tohure.tanayenai.domain.repository.DisplayNameProvider
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.PantryRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.repository.UserRepository

/**
 * Obtiene los datos necesarios para construir el contexto de Gemini.
 * Lee User, perfil clínico, alacena y memoria internamente desde sus repositorios.
 *
 * El nombre visible lo aporta [DisplayNameProvider] (elegido por el usuario en el
 * onboarding); si no hay User guardado se usa un placeholder con datos demográficos
 * por defecto — pero el nombre elegido siempre prevalece.
 */
class FetchContextParamsUseCase(
    private val healthMetricsRepository: HealthMetricsRepository,
    private val recommendationRepository: RecommendationRepository,
    private val clinicalProfileRepository: ClinicalProfileRepository,
    private val userRepository: UserRepository,
    private val conversationMemoryRepository: ConversationMemoryRepository,
    private val pantryRepository: PantryRepository,
    private val displayNameProvider: DisplayNameProvider,
) {
    suspend fun fetch(
        userId: String,
        today: String = currentIsoDate(),
        workContext: String = "Sin especificar",
    ): ContextParams {
        val storedUser = userRepository.getUser(userId)
        val userExistsInDb = storedUser != null
        val baseUser = storedUser ?: placeholderUser(userId)
        // El nombre elegido por el usuario (prefs) prevalece sobre el del placeholder/registro.
        val displayName = displayNameProvider.getDisplayName()?.trim()?.takeIf { it.isNotBlank() }
        val user = if (displayName != null) baseUser.copy(name = displayName) else baseUser

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
        val conversationSummary = conversationMemoryRepository.getSummary(userId)

        val pantryItems = pantryRepository.getPantryItems(userId)
        val locationNames = pantryRepository.getLocations(userId).associate { it.id to it.name }

        return ContextParams(
            user = user,
            userExistsInDb = userExistsInDb,
            clinicalProfile = clinicalProfile,
            recentMetrics = recentMetrics,
            pantryItems = pantryItems,
            locationNames = locationNames,
            recentRecommendations = recentRecommendations,
            todayFoodLogs = emptyList(),
            today = today,
            workContext = workContext,
            conversationSummary = conversationSummary,
        )
    }

    // Placeholder de datos demográficos hasta que exista onboarding completo (Fase 6).
    // El nombre real lo sobrescribe [DisplayNameProvider] en fetch().
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
