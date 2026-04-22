package dev.tohure.tanayenai.data.repository

import co.touchlab.kermit.Logger
import dev.tohure.tanayenai.data.remote.dto.FoodLogDto
import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.DailyNutritionSummary
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.FoodLogSource
import dev.tohure.tanayenai.domain.model.MealType
import dev.tohure.tanayenai.domain.repository.FoodLogRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import db.FoodLog as DbFoodLog

private val log = Logger.withTag("FoodLogRepository")

class FoodLogRepositoryImpl(
    private val database: TanayenDatabase,
    private val supabase: SupabaseClient,
) : FoodLogRepository {
    private val queries = database.foodLogQueries

    override suspend fun insertFoodLog(foodLog: FoodLog): Unit =
        withContext(Dispatchers.Default) {
            queries.insertFoodLog(
                id = foodLog.id,
                userId = foodLog.userId,
                foodName = foodLog.foodName,
                mealType = foodLog.mealType.name,
                calories = foodLog.calories.toDouble(),
                proteinG = foodLog.proteinG.toDouble(),
                carbsG = foodLog.carbsG.toDouble(),
                fatG = foodLog.fatG.toDouble(),
                fiberG = foodLog.fiberG.toDouble(),
                sodiumMg = foodLog.sodiumMg.toDouble(),
                sugarG = foodLog.sugarG.toDouble(),
                source = foodLog.source.name,
                loggedAt = foodLog.loggedAt,
            )
            syncToSupabase(foodLog)
        }

    override suspend fun getTodayFoodLogs(
        userId: String,
        datePrefix: String,
    ): List<FoodLog> =
        withContext(Dispatchers.Default) {
            queries
                .getTodayFoodLogs(userId = userId, datePrefix = datePrefix)
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun getLatestTodayFoodLogs(
        userId: String,
        datePrefix: String,
        limit: Int,
    ): List<FoodLog> =
        withContext(Dispatchers.Default) {
            queries
                .getLatestTodayFoodLogs(userId = userId, datePrefix = datePrefix, limit = limit.toLong())
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun getDailySummary(
        userId: String,
        datePrefix: String,
    ): DailyNutritionSummary? =
        withContext(Dispatchers.Default) {
            val row =
                queries
                    .getDailySummary(userId = userId, datePrefix = datePrefix)
                    .executeAsOneOrNull() ?: return@withContext null

            val mealCount = row.meal_count?.toInt() ?: 0
            if (mealCount == 0) return@withContext null

            DailyNutritionSummary(
                date = datePrefix,
                totalCalories = (row.total_calories ?: 0.0).toFloat(),
                totalProteinG = (row.total_protein ?: 0.0).toFloat(),
                totalCarbsG = (row.total_carbs ?: 0.0).toFloat(),
                totalFatG = (row.total_fat ?: 0.0).toFloat(),
                totalFiberG = (row.total_fiber ?: 0.0).toFloat(),
                totalSodiumMg = (row.total_sodium ?: 0.0).toFloat(),
                totalSugarG = (row.total_sugar ?: 0.0).toFloat(),
                mealCount = mealCount,
            )
        }

    private suspend fun syncToSupabase(foodLog: FoodLog) {
        try {
            supabase.from("food_logs").upsert(
                FoodLogDto(
                    id = foodLog.id,
                    userId = foodLog.userId,
                    foodName = foodLog.foodName,
                    mealType = foodLog.mealType.name,
                    calories = foodLog.calories,
                    proteinG = foodLog.proteinG,
                    carbsG = foodLog.carbsG,
                    fatG = foodLog.fatG,
                    fiberG = foodLog.fiberG,
                    sodiumMg = foodLog.sodiumMg,
                    sugarG = foodLog.sugarG,
                    source = foodLog.source.name,
                    loggedAt = foodLog.loggedAt,
                ),
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to sync food log ${foodLog.id} to Supabase" }
        }
    }

    // ── Mapper ─────────────────────────────────────────────────────────────
    private fun DbFoodLog.toDomain() =
        FoodLog(
            id = id,
            userId = user_id,
            foodName = food_name,
            mealType = runCatching { MealType.valueOf(meal_type) }.getOrDefault(MealType.SNACK),
            calories = calories.toFloat(),
            proteinG = protein_g.toFloat(),
            carbsG = carbs_g.toFloat(),
            fatG = fat_g.toFloat(),
            fiberG = fiber_g.toFloat(),
            sodiumMg = sodium_mg.toFloat(),
            sugarG = sugar_g.toFloat(),
            source = runCatching { FoodLogSource.valueOf(source) }.getOrDefault(FoodLogSource.CHAT_DETECTED),
            loggedAt = logged_at,
        )
}
