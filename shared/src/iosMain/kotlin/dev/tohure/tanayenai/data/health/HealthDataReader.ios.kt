package dev.tohure.tanayenai.data.health

import co.touchlab.kermit.Logger
import dev.tohure.tanayenai.domain.model.DailyHealthData
import dev.tohure.tanayenai.domain.model.HealthPermissionResult
import dev.tohure.tanayenai.domain.model.MetricsSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSinceDate
import platform.HealthKit.HKCategoryType
import platform.HealthKit.HKCategoryTypeIdentifierSleepAnalysis
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifier
import platform.HealthKit.HKQuantityTypeIdentifierActiveEnergyBurned
import platform.HealthKit.HKQuantityTypeIdentifierBodyMass
import platform.HealthKit.HKQuantityTypeIdentifierHeartRateVariabilitySDNN
import platform.HealthKit.HKQuantityTypeIdentifierStepCount
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKStatistics
import platform.HealthKit.HKStatisticsCollectionQuery
import platform.HealthKit.HKStatisticsOptionCumulativeSum
import platform.HealthKit.HKStatisticsOptionDiscreteAverage
import platform.HealthKit.HKUnit
import platform.HealthKit.countUnit
import platform.HealthKit.gramUnitWithMetricPrefix
import platform.HealthKit.kilocalorieUnit
import platform.HealthKit.predicateForSamplesWithStartDate
import platform.HealthKit.secondUnit
import kotlin.coroutines.resume
import dev.tohure.tanayenai.domain.model.HealthPermission as TanayenPermission

private val log = Logger.withTag("HealthDataReader.iOS")

@OptIn(ExperimentalForeignApi::class)
actual class HealthDataReader {
    private val store = HKHealthStore()

    actual suspend fun hasPermissions(permissions: Set<TanayenPermission>): Boolean {
        // En iOS por restricciones de privacidad de Apple, no podemos consultar el estado
        // de autorización para LEER datos, solo para escribir.
        // Asumimos true por defecto en la capa Data para intentar leer.
        // La solicitud real de UI se hace vía Swift directamente.
        return HKHealthStore.isHealthDataAvailable()
    }

    suspend fun requestPermissionsIos(permissions: Set<TanayenPermission>): Boolean {
        if (!HKHealthStore.isHealthDataAvailable()) return false

        val readTypes = permissions.mapNotNull { it.toHKType() }.toSet()

        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            store.requestAuthorizationToShareTypes(
                typesToShare = emptySet<platform.HealthKit.HKObjectType>(),
                readTypes = readTypes,
            ) { success, error ->
                if (error != null) {
                    log.e { "HealthKit permission error: ${error.localizedDescription}" }
                    cont.resume(false)
                } else {
                    cont.resume(success)
                }
            }
        }
    }

    actual suspend fun readDailyData(date: LocalDate): DailyHealthData? {
        val calendar = NSCalendar.currentCalendar
        val startDate = date.toNSDate(calendar)
        val endDate = date.plus(1, DateTimeUnit.DAY).toNSDate(calendar)

        return try {
            val sleepHours = readSleep(startDate, endDate)?.let { it / 60.0 / 60.0 }
            val hrv =
                readQuantity(
                    HKQuantityTypeIdentifierHeartRateVariabilitySDNN,
                    HKUnit.secondUnit(),
                    startDate,
                    endDate,
                )?.let { it * 1000f }
            val calories = readCalories(startDate, endDate)?.toInt()
            val weight =
                readQuantity(
                    HKQuantityTypeIdentifierBodyMass,
                    HKUnit.gramUnitWithMetricPrefix(platform.HealthKit.HKMetricPrefixKilo),
                    startDate,
                    endDate,
                )
            val steps = readSteps(startDate, endDate)?.toInt()

            if (sleepHours == null && hrv == null && calories == null && weight == null && steps == null) return null

            DailyHealthData(
                date = date,
                sleepHours = sleepHours?.toFloat(),
                hrv = hrv?.toFloat(),
                caloriesBurned = calories,
                weightKg = weight?.toFloat(),
                steps = steps,
                source = MetricsSource.HEALTH_KIT,
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to read HealthKit data for $date" }
            null
        }
    }

    actual suspend fun readRecentData(days: Int): List<DailyHealthData> {
        val calendar = NSCalendar.currentCalendar
        val components =
            calendar.components(
                platform.Foundation.NSCalendarUnitYear or platform.Foundation.NSCalendarUnitMonth or
                    platform.Foundation.NSCalendarUnitDay,
                fromDate = NSDate(),
            )
        val today = LocalDate(components.year.toInt(), components.month.toInt(), components.day.toInt())
        return (0 until days).mapNotNull { offset ->
            readDailyData(today.minus(offset, DateTimeUnit.DAY))
        }
    }

    // ── Lecturas individuales ─────────────────────────────────────────────────

    private suspend fun readSleep(
        start: NSDate,
        end: NSDate,
    ): Double? =
        suspendCancellableCoroutine { cont ->
            val sleepType =
                HKCategoryType.categoryTypeForIdentifier(HKCategoryTypeIdentifierSleepAnalysis)
                    ?: return@suspendCancellableCoroutine cont.resume(null)

            val predicate = HKSampleQuery.predicateForSamplesWithStartDate(start, end, 0u)

            val query =
                HKSampleQuery(
                    sampleType = sleepType,
                    predicate = predicate,
                    limit = platform.HealthKit.HKObjectQueryNoLimit,
                    sortDescriptors = null,
                ) { _, samples, _ ->
                    if (samples == null) {
                        cont.resume(null)
                        return@HKSampleQuery
                    }
                    var totalSleepSeconds = 0.0
                    for (sample in samples) {
                        val categorySample = sample as? platform.HealthKit.HKCategorySample ?: continue
                        if (categorySample.value == platform.HealthKit.HKCategoryValueSleepAnalysisAsleep) {
                            totalSleepSeconds += categorySample.endDate.timeIntervalSinceDate(categorySample.startDate)
                        }
                    }
                    if (totalSleepSeconds > 0) cont.resume(totalSleepSeconds) else cont.resume(null)
                }
            store.executeQuery(query)
        }

    private suspend fun readQuantity(
        identifier: HKQuantityTypeIdentifier,
        unit: HKUnit,
        start: NSDate,
        end: NSDate,
    ): Double? =
        suspendCancellableCoroutine { cont ->
            val type =
                HKQuantityType.quantityTypeForIdentifier(identifier)
                    ?: return@suspendCancellableCoroutine cont.resume(null)

            val predicate = HKSampleQuery.predicateForSamplesWithStartDate(start, end, 0u)
            val query =
                HKStatisticsCollectionQuery(
                    type,
                    predicate,
                    HKStatisticsOptionDiscreteAverage,
                    anchorDate = start,
                    intervalComponents = NSDateComponents().apply { day = 1 },
                )

            query.initialResultsHandler = { _, results, _ ->
                val statistics = results?.statistics() as? List<HKStatistics>
                val average = statistics?.firstOrNull()?.averageQuantity()?.doubleValueForUnit(unit)
                cont.resume(average)
            }
            store.executeQuery(query)
        }

    private suspend fun readSteps(
        start: NSDate,
        end: NSDate,
    ): Double? =
        suspendCancellableCoroutine { cont ->
            val stepsType =
                HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierStepCount)
                    ?: return@suspendCancellableCoroutine cont.resume(null)
            val predicate = HKSampleQuery.predicateForSamplesWithStartDate(start, end, 0u)
            val query =
                HKStatisticsCollectionQuery(
                    stepsType,
                    predicate,
                    HKStatisticsOptionCumulativeSum,
                    anchorDate = start,
                    intervalComponents = NSDateComponents().apply { day = 1 },
                )
            query.initialResultsHandler = { _, results, _ ->
                val statistics = results?.statistics() as? List<HKStatistics>
                val total = statistics?.firstOrNull()?.sumQuantity()?.doubleValueForUnit(HKUnit.countUnit())
                cont.resume(total)
            }
            store.executeQuery(query)
        }

    private suspend fun readCalories(
        start: NSDate,
        end: NSDate,
    ): Double? =
        suspendCancellableCoroutine { cont ->
            val caloriesType =
                HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierActiveEnergyBurned)
                    ?: return@suspendCancellableCoroutine cont.resume(null)
            val predicate = HKSampleQuery.predicateForSamplesWithStartDate(start, end, 0u)
            val query =
                HKStatisticsCollectionQuery(
                    caloriesType,
                    predicate,
                    HKStatisticsOptionCumulativeSum,
                    anchorDate = start,
                    intervalComponents = NSDateComponents().apply { day = 1 },
                )
            query.initialResultsHandler = { _, results, _ ->
                val statistics = results?.statistics() as? List<HKStatistics>
                val total = statistics?.firstOrNull()?.sumQuantity()?.doubleValueForUnit(HKUnit.kilocalorieUnit())
                cont.resume(total)
            }
            store.executeQuery(query)
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun TanayenPermission.toHKType(): HKObjectType? =
        when (this) {
            TanayenPermission.SLEEP -> {
                HKCategoryType.categoryTypeForIdentifier(HKCategoryTypeIdentifierSleepAnalysis)
            }

            TanayenPermission.HEART_RATE_VARIABILITY -> {
                HKQuantityType.quantityTypeForIdentifier(
                    HKQuantityTypeIdentifierHeartRateVariabilitySDNN,
                )
            }

            TanayenPermission.CALORIES_BURNED -> {
                HKQuantityType.quantityTypeForIdentifier(
                    HKQuantityTypeIdentifierActiveEnergyBurned,
                )
            }

            TanayenPermission.WEIGHT -> {
                HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierBodyMass)
            }

            TanayenPermission.STEPS -> {
                HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierStepCount)
            }
        }

    private fun LocalDate.toNSDate(calendar: NSCalendar): NSDate {
        val components =
            NSDateComponents().apply {
                year = this@toNSDate.year.toLong()
                month = this@toNSDate.month.number.toLong()
                day = this@toNSDate.day.toLong()
            }
        return calendar.dateFromComponents(components)!!
    }
}
