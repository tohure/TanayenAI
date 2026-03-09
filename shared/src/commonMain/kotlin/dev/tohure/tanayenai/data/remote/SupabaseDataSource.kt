package dev.tohure.tanayenai.data.remote

import co.touchlab.kermit.Logger
import dev.tohure.tanayenai.data.remote.dto.HealthMetricsDto
import dev.tohure.tanayenai.data.remote.dto.PantryItemDto
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.MetricsSource
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryUnit
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

private val log = Logger.withTag("SupabaseDataSource")

// ── Data Source ───────────────────────────────────────────────────────────
class SupabaseDataSource(
    private val client: SupabaseClient,
) {
    // ── Pantry Items ──────────────────────────────────────────────────────
    suspend fun upsertPantryItem(item: PantryItem) {
        try {
            client.from("pantry_items").upsert(
                PantryItemDto(
                    id = item.id,
                    userId = item.userId,
                    locationId = item.locationId,
                    ingredient = item.ingredient,
                    quantity = item.quantity.toDouble(),
                    unit = item.unit.name,
                    expiryDate = item.expiryDate,
                    updatedAt = item.updatedAt,
                ),
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to upsert pantry item ${item.id}" }
        }
    }

    suspend fun fetchPantryItems(userId: String): List<PantryItemDto> =
        client
            .from("pantry_items")
            .select {
                filter { eq("user_id", userId) }
            }.decodeList()

    // ── Health Metrics ────────────────────────────────────────────────────
    suspend fun upsertHealthMetrics(metrics: HealthMetrics) {
        try {
            client.from("health_metrics").upsert(
                HealthMetricsDto(
                    id = metrics.id,
                    userId = metrics.userId,
                    date = metrics.date,
                    weightKg = metrics.weightKg?.toDouble(),
                    imc = metrics.imc?.toDouble(),
                    sleepHours = metrics.sleepHours?.toDouble(),
                    hrv = metrics.hrv?.toDouble(),
                    restingHeartRate = metrics.restingHeartRate,
                    steps = metrics.steps,
                    source = metrics.source.name,
                ),
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to upsert health metrics ${metrics.id}" }
        }
    }

    suspend fun fetchHealthMetrics(
        userId: String,
        limit: Int = 30,
    ): List<HealthMetricsDto> =
        client
            .from("health_metrics")
            .select {
                filter { eq("user_id", userId) }
                order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList()

    // ── Mapper helpers ────────────────────────────────────────────────────
    fun HealthMetricsDto.toDomain() =
        HealthMetrics(
            id = id,
            userId = userId,
            date = date,
            weightKg = weightKg?.toFloat(),
            imc = imc?.toFloat(),
            sleepHours = sleepHours?.toFloat(),
            hrv = hrv?.toFloat(),
            restingHeartRate = restingHeartRate,
            steps = steps,
            source = MetricsSource.valueOf(source),
        )

    fun PantryItemDto.toDomain() =
        PantryItem(
            id = id,
            userId = userId,
            locationId = locationId,
            ingredient = ingredient,
            quantity = quantity.toFloat(),
            unit = PantryUnit.valueOf(unit),
            expiryDate = expiryDate,
            updatedAt = updatedAt,
        )
}
