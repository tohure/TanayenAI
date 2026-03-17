package dev.tohure.tanayenai.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import co.touchlab.kermit.Logger
import dev.tohure.tanayenai.domain.model.DailyHealthData
import dev.tohure.tanayenai.domain.model.HealthPermissionResult
import dev.tohure.tanayenai.domain.model.MetricsSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Instant
import dev.tohure.tanayenai.domain.model.HealthPermission as TanayenPermission
import java.time.Instant as JavaInstant

private val log = Logger.withTag("HealthDataReader.Android")

actual class HealthDataReader(
    private val context: Context,
) {
    private val client: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Exception) {
            log.e(e) { "Health Connect not available" }
            null
        }
    }

    actual suspend fun requestPermissions(permissions: Set<TanayenPermission>): HealthPermissionResult {
        val hcClient = client ?: return HealthPermissionResult.NotAvailable

        val hcPermissions = permissions.mapNotNull { it.toHealthConnectPermission() }.toSet()

        return try {
            val granted = hcClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(hcPermissions)) {
                HealthPermissionResult.Granted
            } else {
                HealthPermissionResult.Denied
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to check permissions" }
            HealthPermissionResult.Denied
        }
    }

    actual suspend fun readDailyData(date: LocalDate): DailyHealthData? {
        val hcClient = client ?: return null
        val tz = TimeZone.currentSystemDefault()
        val start = date.atStartOfDayIn(tz)
        val end = date.plus(1, kotlinx.datetime.DateTimeUnit.DAY).atStartOfDayIn(tz)

        val timeRange =
            TimeRangeFilter.between(
                start.toJavaInstant(),
                end.toJavaInstant(),
            )

        // Sleep and Resting HR usually starts the night before.
        // We look back 12 hours (midday yesterday) to catch the entire session
        val sleepTimeRange =
            TimeRangeFilter.between(
                start.toJavaInstant().minus(12, java.time.temporal.ChronoUnit.HOURS),
                end.toJavaInstant(),
            )

        // Weight is not logged every day, we look back 30 days to get the latest known
        val weightTimeRange =
            TimeRangeFilter.between(
                start.toJavaInstant().minus(30, java.time.temporal.ChronoUnit.DAYS),
                end.toJavaInstant(),
            )

        return try {
            val granted = hcClient.permissionController.getGrantedPermissions()

            val sleepHours =
                if (granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
                    readSleepHours(hcClient, sleepTimeRange)
                } else {
                    null
                }

            val hrv =
                if (granted.contains(HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class))) {
                    readHrv(hcClient, timeRange)
                } else {
                    null
                }

            val calories =
                if (granted.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))) {
                    readCaloriesBurned(hcClient, timeRange)
                } else {
                    null
                }

            val weight =
                if (granted.contains(HealthPermission.getReadPermission(WeightRecord::class))) {
                    readWeight(hcClient, weightTimeRange)
                } else {
                    null
                }

            val steps =
                if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
                    readSteps(hcClient, timeRange)
                } else {
                    null
                }

            if (sleepHours == null && hrv == null && calories == null && weight == null && steps == null) return null

            DailyHealthData(
                date = date,
                sleepHours = sleepHours,
                hrv = hrv,
                caloriesBurned = calories,
                weightKg = weight,
                steps = steps,
                source = MetricsSource.HEALTH_CONNECT,
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to read daily data for $date" }
            null
        }
    }

    actual suspend fun readRecentData(days: Int): List<DailyHealthData> {
        val javaToday = java.time.LocalDate.now()
        val today = LocalDate(javaToday.year, javaToday.monthValue, javaToday.dayOfMonth)
        return (0 until days).mapNotNull { offset ->
            readDailyData(today.minus(offset, kotlinx.datetime.DateTimeUnit.DAY))
        }
    }

    private suspend fun readSleepHours(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter,
    ): Float? =
        try {
            val response =
                client.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, timeRange),
                )
            val totalMinutes =
                response.records.sumOf { session ->
                    (session.endTime.epochSecond - session.startTime.epochSecond) / 60.0
                }
            val result = if (totalMinutes > 0) (totalMinutes / 60).toFloat() else null
            log.d { "HealthConnect Sleep: $result hours" }
            result
        } catch (e: Exception) {
            log.e(e) { "Failed to read SleepSessionRecord: ${e.message}" }
            null
        }

    private suspend fun readHrv(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter,
    ): Float? =
        try {
            val response =
                client.readRecords(
                    ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRange),
                )
            val result =
                response.records
                    .lastOrNull()
                    ?.heartRateVariabilityMillis
                    ?.toFloat()
            log.d { "HealthConnect HRV: $result ms" }
            result
        } catch (e: Exception) {
            log.e(e) { "Failed to read HeartRateVariabilityRmssdRecord: ${e.message}" }
            null
        }

    private suspend fun readCaloriesBurned(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter,
    ): Int? =
        try {
            val response =
                client.readRecords(
                    ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRange),
                )
            val result = response.records.sumOf { it.energy.inKilocalories }.toInt()
            val total = if (result > 0) result else null
            log.d { "HealthConnect Calories: $total kcal" }
            total
        } catch (e: Exception) {
            log.e(e) { "Failed to read TotalCaloriesBurnedRecord: ${e.message}" }
            null
        }

    private suspend fun readWeight(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter,
    ): Float? =
        try {
            val response =
                client.readRecords(
                    ReadRecordsRequest(WeightRecord::class, timeRange),
                )
            val result =
                response.records
                    .lastOrNull()
                    ?.weight
                    ?.inKilograms
                    ?.toFloat()
            log.d { "HealthConnect Weight: $result kg" }
            result
        } catch (e: Exception) {
            log.e(e) { "Failed to read WeightRecord: ${e.message}" }
            null
        }

    private suspend fun readSteps(
        client: HealthConnectClient,
        timeRange: TimeRangeFilter,
    ): Int? =
        try {
            val response =
                client.readRecords(
                    ReadRecordsRequest(StepsRecord::class, timeRange),
                )
            val total = response.records.sumOf { it.count }
            val result = if (total > 0) total.toInt() else null
            log.d { "HealthConnect Steps: $result count" }
            result
        } catch (e: Exception) {
            log.e(e) { "Failed to read StepsRecord: ${e.message}" }
            null
        }

    private fun TanayenPermission.toHealthConnectPermission(): String? =
        when (this) {
            TanayenPermission.SLEEP -> {
                HealthPermission.getReadPermission(SleepSessionRecord::class)
            }

            TanayenPermission.HEART_RATE_VARIABILITY -> {
                HealthPermission.getReadPermission(
                    HeartRateVariabilityRmssdRecord::class,
                )
            }

            TanayenPermission.CALORIES_BURNED -> {
                HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
            }

            TanayenPermission.WEIGHT -> {
                HealthPermission.getReadPermission(WeightRecord::class)
            }

            TanayenPermission.STEPS -> {
                HealthPermission.getReadPermission(StepsRecord::class)
            }
        }
}

// Extension para compatibilidad con java.time que usa Health Connect
private fun Instant.toJavaInstant() = JavaInstant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())
