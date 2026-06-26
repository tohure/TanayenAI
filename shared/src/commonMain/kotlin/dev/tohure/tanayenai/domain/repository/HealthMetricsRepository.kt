package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.HealthMetrics
import kotlinx.coroutines.flow.Flow

interface HealthMetricsRepository {
    suspend fun getMetricsForDateRange(
        userId: String,
        from: String,
        to: String,
    ): List<HealthMetrics>

    suspend fun getLatestMetrics(userId: String): HealthMetrics?

    fun getLatestMetricsFlow(userId: String): Flow<HealthMetrics?>

    suspend fun saveMetrics(metrics: HealthMetrics)
}
