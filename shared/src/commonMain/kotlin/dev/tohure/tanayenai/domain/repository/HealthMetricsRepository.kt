package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.HealthMetrics

interface HealthMetricsRepository {
    suspend fun getMetricsForDateRange(
        userId: String,
        from: String,
        to: String,
    ): List<HealthMetrics>

    suspend fun getLatestMetrics(userId: String): HealthMetrics?

    suspend fun saveMetrics(metrics: HealthMetrics)
}
