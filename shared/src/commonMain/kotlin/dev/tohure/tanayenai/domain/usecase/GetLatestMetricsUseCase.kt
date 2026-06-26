package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository

class GetLatestMetricsUseCase(
    private val healthMetricsRepository: HealthMetricsRepository,
) {
    suspend fun execute(userId: String): HealthMetrics? = healthMetricsRepository.getLatestMetrics(userId)
}
