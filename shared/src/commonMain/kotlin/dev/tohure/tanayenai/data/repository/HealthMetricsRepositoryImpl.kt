package dev.tohure.tanayenai.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.MetricsSource
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import db.HealthMetrics as DbHealthMetrics

class HealthMetricsRepositoryImpl(
    private val database: TanayenDatabase,
) : HealthMetricsRepository {
    private val queries = database.healthMetricsQueries

    override suspend fun getMetricsForDateRange(
        userId: String,
        from: String,
        to: String,
    ): List<HealthMetrics> =
        withContext(Dispatchers.Default) {
            queries
                .getMetricsByDateRange(userId = userId, fromDate = from, toDate = to)
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun getLatestMetrics(userId: String): HealthMetrics? =
        withContext(Dispatchers.Default) {
            queries.getLatestMetrics(userId).executeAsOneOrNull()?.toDomain()
        }

    override fun getLatestMetricsFlow(userId: String): Flow<HealthMetrics?> =
        queries
            .getLatestMetrics(userId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }

    suspend fun getContextWindowMetrics(userId: String): List<HealthMetrics> =
        withContext(Dispatchers.Default) {
            queries.getMetricsForContextWindow(userId).executeAsList().map { it.toDomain() }
        }

    override suspend fun saveMetrics(metrics: HealthMetrics): Unit =
        withContext(Dispatchers.Default) {
            queries.insertMetrics(
                id = metrics.id,
                userId = metrics.userId,
                date = metrics.date,
                weightKg = metrics.weightKg?.toDouble(),
                imc = metrics.imc?.toDouble(),
                sleepHours = metrics.sleepHours?.toDouble(),
                hrv = metrics.hrv?.toDouble(),
                restingHeartRate = metrics.restingHeartRate?.toLong(),
                steps = metrics.steps?.toLong(),
                source = metrics.source.name,
            )
        }

    // ── Mapper ─────────────────────────────────────────────────────────────
    private fun DbHealthMetrics.toDomain() =
        HealthMetrics(
            id = id,
            userId = user_id,
            date = date,
            weightKg = weight_kg?.toFloat(),
            imc = imc?.toFloat(),
            sleepHours = sleep_hours?.toFloat(),
            hrv = hrv?.toFloat(),
            restingHeartRate = resting_heart_rate?.toInt(),
            steps = steps?.toInt(),
            source = MetricsSource.valueOf(source),
        )
}
