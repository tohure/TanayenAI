package dev.tohure.tanayenai.domain.usecase

import co.touchlab.kermit.Logger
import dev.tohure.tanayenai.data.health.HealthDataReader
import dev.tohure.tanayenai.domain.model.DailyHealthData
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.HealthPermission
import dev.tohure.tanayenai.domain.model.generateId
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private val log = Logger.withTag("SyncHealthMetricsUseCase")

class SyncHealthMetricsUseCase(
    private val healthDataReader: HealthDataReader,
    private val repository: HealthMetricsRepository,
    private val userId: String,
) {
    // Permisos que necesita la app (Público para que Android UI los lea)
    val requiredPermissions =
        setOf(
            HealthPermission.SLEEP,
            HealthPermission.HEART_RATE_VARIABILITY,
            HealthPermission.CALORIES_BURNED,
            HealthPermission.WEIGHT,
            HealthPermission.STEPS,
        )

    suspend fun hasPermissions(): Boolean = healthDataReader.hasPermissions(requiredPermissions)

    suspend fun syncLastNDays(days: Int = 7): SyncResult {
        return try {
            val recentData = healthDataReader.readRecentData(days)
            if (recentData.isEmpty()) return SyncResult.NoData

            recentData.forEach { dailyData ->
                val metrics = dailyData.toHealthMetrics(userId)
                repository.saveMetrics(metrics)
            }

            log.d { "Synced ${recentData.size} days of health data" }
            SyncResult.Success(recentData.size)
        } catch (e: Exception) {
            log.e(e) { "Health sync failed" }
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun syncToday(): SyncResult {
        return try {
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            val data = healthDataReader.readDailyData(today)
            log.d { "readDailyData returned: $data" }
            if (data == null) return SyncResult.NoData

            val metrics = data.toHealthMetrics(userId)
            log.d { "Saving metrics to DB: $metrics" }
            repository.saveMetrics(metrics)

            val saved = repository.getLatestMetrics(userId)
            log.d { "Verified saved metrics from DB: $saved" }

            SyncResult.Success(1)
        } catch (e: Exception) {
            log.e(e) { "Today health sync failed" }
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun DailyHealthData.toHealthMetrics(userId: String) =
        HealthMetrics(
            id = generateId(),
            userId = userId,
            date = date.toString(),
            weightKg = weightKg,
            imc = weightKg?.let { calculateImc(it) },
            sleepHours = sleepHours,
            hrv = hrv,
            caloriesBurned = caloriesBurned,
            steps = steps,
            source = source,
        )

    // IMC = peso(kg) / altura(m)² — altura hardcoded por ahora, viene del perfil en Fase 5C
    private fun calculateImc(
        weightKg: Float,
        heightM: Float = 1.75f,
    ): Float = weightKg / (heightM * heightM)
}

sealed class SyncResult {
    data class Success(
        val recordsSynced: Int,
    ) : SyncResult()

    data object NoData : SyncResult()

    data class Error(
        val message: String,
    ) : SyncResult()
}
