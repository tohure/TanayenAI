package dev.tohure.tanayenai.domain.model

import kotlinx.datetime.LocalDate

data class DailyHealthData(
    val date: LocalDate,
    val sleepHours: Float? = null,
    val hrv: Float? = null, // VFC en ms
    val caloriesBurned: Int? = null, // Calorías quemadas activas en kcal
    val weightKg: Float? = null,
    val steps: Int? = null,
    val source: MetricsSource,
)

// Permiso requerido por plataforma
enum class HealthPermission {
    SLEEP,
    HEART_RATE_VARIABILITY,
    CALORIES_BURNED,
    WEIGHT,
    STEPS,
}

// Resultado de la solicitud de permisos
sealed class HealthPermissionResult {
    data object Granted : HealthPermissionResult()

    data object Denied : HealthPermissionResult()

    data object NotAvailable : HealthPermissionResult() // Health Connect no instalado
}
