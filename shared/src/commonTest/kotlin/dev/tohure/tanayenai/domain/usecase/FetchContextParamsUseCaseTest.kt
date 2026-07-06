package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.ActivityLevel
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.MetricsSource
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryLocation
import dev.tohure.tanayenai.domain.model.PantryUnit
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.RecommendationType
import dev.tohure.tanayenai.domain.model.Sex
import dev.tohure.tanayenai.domain.model.User
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import dev.tohure.tanayenai.domain.repository.ConversationMemoryRepository
import dev.tohure.tanayenai.domain.repository.DisplayNameProvider
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.PantryRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FetchContextParamsUseCaseTest {
    private val userId = "user_1"

    private val testUser =
        User(
            id = userId,
            name = "Carlo",
            birthDate = "1990-05-15",
            sex = Sex.MALE,
            heightCm = 175f,
            goal = NutritionGoal.EAT_HEALTHY,
            activityLevel = ActivityLevel.MODERATE,
        )

    private val testClinicalProfile =
        ClinicalProfile(
            userId = userId,
            cholesterolTotal = 215f,
            hdl = 42f,
            ldl = 148f,
        )

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeHealthMetricsRepository(
        private val metrics: List<HealthMetrics> = emptyList(),
    ) : HealthMetricsRepository {
        override suspend fun getMetricsForDateRange(
            userId: String,
            from: String,
            to: String,
        ) = metrics

        override suspend fun getLatestMetrics(userId: String) = metrics.firstOrNull()

        override fun getLatestMetricsFlow(userId: String): Flow<HealthMetrics?> = flowOf(metrics.firstOrNull())

        override suspend fun saveMetrics(metrics: HealthMetrics) {}
    }

    private class FakeRecommendationRepository(
        private val recommendations: List<Recommendation> = emptyList(),
    ) : RecommendationRepository {
        override suspend fun getRecentRecommendations(
            userId: String,
            days: Int,
        ) = recommendations

        override suspend fun saveRecommendation(recommendation: Recommendation) {}

        override suspend fun findSimilar(
            embedding: List<Float>,
            threshold: Float,
        ) = emptyList<Recommendation>()
    }

    private class FakeClinicalProfileRepository(
        private val profile: ClinicalProfile? = null,
    ) : ClinicalProfileRepository {
        override suspend fun getClinicalProfile(userId: String) = profile

        override suspend fun saveClinicalProfile(profile: ClinicalProfile) {}
    }

    private class FakeUserRepository(
        private val user: User? = null,
    ) : UserRepository {
        override suspend fun getUser(id: String) = user

        override suspend fun saveUser(user: User) {}

        override suspend fun updateUser(user: User) {}
    }

    private class FakeConversationMemoryRepository(
        private val summary: String? = null,
    ) : ConversationMemoryRepository {
        override suspend fun getSummary(userId: String) = summary

        override suspend fun saveSummary(
            userId: String,
            summary: String,
        ) {}
    }

    private class FakePantryRepository(
        private val items: List<PantryItem> = emptyList(),
        private val locations: List<PantryLocation> = emptyList(),
    ) : PantryRepository {
        override suspend fun getPantryItems(userId: String) = items

        override fun observeItems(
            userId: String,
            locationId: String,
        ): Flow<List<PantryItem>> = flowOf(items)

        override suspend fun getLocations(userId: String) = locations

        override suspend fun upsertItem(item: PantryItem) {}

        override suspend fun deleteItem(itemId: String) {}

        override suspend fun decrementQuantity(
            itemId: String,
            amount: Float,
        ) {}

        override suspend fun updateItem(item: PantryItem) {}
    }

    private fun makeUseCase(
        metrics: List<HealthMetrics> = emptyList(),
        recommendations: List<Recommendation> = emptyList(),
        clinicalProfile: ClinicalProfile? = null,
        user: User? = null,
        conversationSummary: String? = null,
        pantryItems: List<PantryItem> = emptyList(),
        pantryLocations: List<PantryLocation> = emptyList(),
        displayName: String? = null,
    ) = FetchContextParamsUseCase(
        healthMetricsRepository = FakeHealthMetricsRepository(metrics),
        recommendationRepository = FakeRecommendationRepository(recommendations),
        clinicalProfileRepository = FakeClinicalProfileRepository(clinicalProfile),
        userRepository = FakeUserRepository(user),
        conversationMemoryRepository = FakeConversationMemoryRepository(conversationSummary),
        pantryRepository = FakePantryRepository(pantryItems, pantryLocations),
        displayNameProvider = DisplayNameProvider { displayName },
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun fetchReturnsParamsWithUserFromRepository() =
        runTest {
            val useCase = makeUseCase(user = testUser)

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals(testUser, params.user)
            assertEquals("2026-03-21", params.today)
        }

    @Test
    fun fetchUsesPlaceholderUserWhenNoneInRepository() =
        runTest {
            // user = null → UseCase creates placeholder
            val useCase = makeUseCase(user = null)

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertNotNull(params.user)
            assertEquals(userId, params.user.id)
        }

    @Test
    fun fetchReturnsClinicalProfileFromRepository() =
        runTest {
            val useCase = makeUseCase(user = testUser, clinicalProfile = testClinicalProfile)

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals(testClinicalProfile, params.clinicalProfile)
        }

    @Test
    fun fetchReturnsNullClinicalProfileWhenNotFound() =
        runTest {
            val useCase = makeUseCase(user = testUser, clinicalProfile = null)

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertNull(params.clinicalProfile)
        }

    @Test
    fun fetchIncludesRecentMetricsFromRepository() =
        runTest {
            val metrics =
                listOf(
                    HealthMetrics(
                        id = "m1",
                        userId = userId,
                        date = "2026-03-20",
                        source = MetricsSource.HEALTH_CONNECT,
                    ),
                    HealthMetrics(
                        id = "m2",
                        userId = userId,
                        date = "2026-03-19",
                        source = MetricsSource.HEALTH_CONNECT,
                    ),
                )
            val useCase = makeUseCase(metrics = metrics, user = testUser)

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals(2, params.recentMetrics.size)
        }

    @Test
    fun fetchIncludesRecentRecommendationsFromRepository() =
        runTest {
            val recommendations =
                listOf(
                    Recommendation(
                        id = "r1",
                        userId = userId,
                        type = RecommendationType.MEAL,
                        title = "Avena con frutas",
                        content = "",
                        ingredientsUsed = emptyList(),
                        recommendedAt = "2026-03-20T10:00:00Z",
                    ),
                )
            val useCase = makeUseCase(recommendations = recommendations, user = testUser)

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals(1, params.recentRecommendations.size)
            assertEquals("Avena con frutas", params.recentRecommendations.first().title)
        }

    @Test
    fun fetchUsesDefaultWorkContextWhenNotProvided() =
        runTest {
            val useCase = makeUseCase(user = testUser)

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals("Sin especificar", params.workContext)
        }

    @Test
    fun fetchIncludesConversationSummaryFromRepository() =
        runTest {
            val useCase = makeUseCase(user = testUser, conversationSummary = "resumen previo de Carlo")

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals("resumen previo de Carlo", params.conversationSummary)
        }

    @Test
    fun fetchDisplayNameOverridesUserName() =
        runTest {
            // El usuario existe con nombre "Carlo", pero eligió mostrarse como "TOHURE".
            val useCase = makeUseCase(user = testUser, displayName = "TOHURE")

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals("TOHURE", params.user.name)
        }

    @Test
    fun fetchDisplayNameAppliesToPlaceholderWhenNoUser() =
        runTest {
            // No hay User guardado → placeholder, pero el nombre elegido prevalece.
            val useCase = makeUseCase(user = null, displayName = "TOHURE")

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals("TOHURE", params.user.name)
            assertEquals(userId, params.user.id)
        }

    @Test
    fun fetchBlankDisplayNameKeepsUserName() =
        runTest {
            val useCase = makeUseCase(user = testUser, displayName = "   ")

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals("Carlo", params.user.name)
        }

    @Test
    fun fetchIncludesPantryItemsAndLocationNames() =
        runTest {
            val item =
                PantryItem(
                    id = "item_1",
                    userId = userId,
                    locationId = "loc_home",
                    ingredient = "Avena",
                    quantity = 500f,
                    unit = PantryUnit.GRAMS,
                    updatedAt = "2026-03-21T08:00:00Z",
                )
            val location = PantryLocation(id = "loc_home", userId = userId, name = "Casa", isDefault = true)
            val useCase = makeUseCase(user = testUser, pantryItems = listOf(item), pantryLocations = listOf(location))

            val params = useCase.fetch(userId, today = "2026-03-21")

            assertEquals(1, params.pantryItems.size)
            assertEquals("Avena", params.pantryItems.first().ingredient)
            assertEquals("Casa", params.locationNames["loc_home"])
            assertTrue(params.pantryItems.isNotEmpty())
        }
}
