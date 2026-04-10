package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.ActivityLevel
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.MetricsSource
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.RecommendationType
import dev.tohure.tanayenai.domain.model.Sex
import dev.tohure.tanayenai.domain.model.User
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    private fun makeUseCase(
        metrics: List<HealthMetrics> = emptyList(),
        recommendations: List<Recommendation> = emptyList(),
        clinicalProfile: ClinicalProfile? = null,
        user: User? = null,
    ) = FetchContextParamsUseCase(
        healthMetricsRepository = FakeHealthMetricsRepository(metrics),
        recommendationRepository = FakeRecommendationRepository(recommendations),
        clinicalProfileRepository = FakeClinicalProfileRepository(clinicalProfile),
        userRepository = FakeUserRepository(user),
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
}
