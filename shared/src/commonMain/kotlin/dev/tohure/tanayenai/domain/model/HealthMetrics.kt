package dev.tohure.tanayenai.domain.model

data class HealthMetrics(
    val id: String,
    val userId: String,
    val date: String,
    val weightKg: Float? = null,
    val imc: Float? = null,
    val sleepHours: Float? = null,
    val hrv: Float? = null, // VFC en ms
    val restingHeartRate: Int? = null,
    val steps: Int? = null,
    val source: MetricsSource,
)

enum class MetricsSource { FITBIT, HEALTH_CONNECT, HEALTH_KIT, MANUAL }
