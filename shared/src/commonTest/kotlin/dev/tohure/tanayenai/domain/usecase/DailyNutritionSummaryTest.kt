package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.DailyNutritionSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyNutritionSummaryTest {
    private fun summary(
        calories: Float,
        goal: Float = 2000f,
    ) = DailyNutritionSummary(
        date = "2026-04-20",
        totalCalories = calories,
        totalProteinG = 0f,
        totalCarbsG = 0f,
        totalFatG = 0f,
        totalFiberG = 0f,
        totalSodiumMg = 0f,
        totalSugarG = 0f,
        mealCount = 1,
        calorieGoal = goal,
    )

    @Test
    fun `progress clamps to 1 when over goal`() {
        assertEquals(1f, summary(2500f).calorieProgress)
    }

    @Test
    fun `remaining is 0 when over goal`() {
        assertEquals(0f, summary(2500f).remainingCalories)
    }

    @Test
    fun `progress is 0_5 at half goal`() {
        assertEquals(0.5f, summary(1000f).calorieProgress)
    }

    @Test
    fun `remaining is correct below goal`() {
        assertEquals(1200f, summary(800f).remainingCalories)
    }
}
