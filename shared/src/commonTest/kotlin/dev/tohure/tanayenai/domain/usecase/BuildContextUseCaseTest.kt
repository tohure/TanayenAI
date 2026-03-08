package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.ActivityLevel
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.MetricsSource
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryUnit
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.RecommendationType
import dev.tohure.tanayenai.domain.model.Sex
import dev.tohure.tanayenai.domain.model.User
import kotlin.test.Test
import kotlin.test.assertContains

class BuildContextUseCaseTest {
    private val useCase = BuildContextUseCase()

    private val testUser =
        User(
            id = "user_1",
            name = "Carlo",
            birthDate = "1990-05-15",
            sex = Sex.MALE,
            heightCm = 175f,
            goal = NutritionGoal.EAT_HEALTHY,
            activityLevel = ActivityLevel.MODERATE,
        )

    @Test
    fun `context includes clinical constraints for dyslipidemia`() {
        val profile =
            ClinicalProfile(
                userId = "user_1",
                cholesterolTotal = 215f,
                ldl = 148f,
                hdl = 42f,
            )

        val params =
            ContextParams(
                user = testUser,
                clinicalProfile = profile,
                recentMetrics = emptyList(),
                pantryItems = emptyList(),
                locationNames = emptyMap(),
                recentRecommendations = emptyList(),
                todayFoodLogs = emptyList(),
                today = "2026-02-27",
                workContext = "Remoto",
            )

        val context = useCase.build(params)

        assertContains(context, "DISLIPIDEMIA")
        assertContains(context, "Evitar: mantequilla")
        assertContains(context, "avena")
    }

    @Test
    fun `context includes low sleep alert when sleep under 6 hours`() {
        val metrics =
            HealthMetrics(
                id = "m1",
                userId = "user_1",
                date = "2026-02-27",
                sleepHours = 4.5f,
                hrv = 55f,
                source = MetricsSource.FITBIT,
            )

        val params =
            ContextParams(
                user = testUser,
                clinicalProfile = null,
                recentMetrics = listOf(metrics),
                pantryItems = emptyList(),
                locationNames = emptyMap(),
                recentRecommendations = emptyList(),
                todayFoodLogs = emptyList(),
                today = "2026-02-27",
                workContext = "Remoto",
            )

        val context = useCase.build(params)

        assertContains(context, "Sueño insuficiente")
        assertContains(context, "evitar cafeína")
    }

    @Test
    fun `context shows low stock warning for items with quantity 1 or less`() {
        val pantryItem =
            PantryItem(
                id = "item_1",
                userId = "user_1",
                locationId = "loc_1",
                ingredient = "Barras proteicas",
                quantity = 1f,
                unit = PantryUnit.UNITS,
                updatedAt = "2026-02-27T08:00:00Z",
            )

        val params =
            ContextParams(
                user = testUser,
                clinicalProfile = null,
                recentMetrics = emptyList(),
                pantryItems = listOf(pantryItem),
                locationNames = mapOf("loc_1" to "Casa"),
                recentRecommendations = emptyList(),
                todayFoodLogs = emptyList(),
                today = "2026-02-27",
                workContext = "Remoto",
            )

        val context = useCase.build(params)

        assertContains(context, "STOCK BAJO")
    }

    @Test
    fun `context lists recent recommendations to avoid repetition`() {
        val rec =
            Recommendation(
                id = "rec_1",
                userId = "user_1",
                type = RecommendationType.MEAL,
                title = "Huevo sancochado con avena",
                content = "{}",
                ingredientsUsed = listOf("huevo", "avena"),
                recommendedAt = "2026-02-26T12:00:00Z",
            )

        val params =
            ContextParams(
                user = testUser,
                clinicalProfile = null,
                recentMetrics = emptyList(),
                pantryItems = emptyList(),
                locationNames = emptyMap(),
                recentRecommendations = listOf(rec),
                todayFoodLogs = emptyList(),
                today = "2026-02-27",
                workContext = "Remoto",
            )

        val context = useCase.build(params)

        assertContains(context, "Huevo sancochado con avena")
        assertContains(context, "NO repetir")
    }
}
