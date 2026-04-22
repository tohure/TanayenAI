package dev.tohure.tanayenai.domain.repository

import dev.tohure.tanayenai.domain.model.DailyNutritionSummary
import dev.tohure.tanayenai.domain.model.FoodLog

interface FoodLogRepository {
    suspend fun insertFoodLog(log: FoodLog)

    suspend fun getTodayFoodLogs(
        userId: String,
        datePrefix: String,
    ): List<FoodLog>

    suspend fun getDailySummary(
        userId: String,
        datePrefix: String,
    ): DailyNutritionSummary?
}
