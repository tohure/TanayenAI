package dev.tohure.tanayenai.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.tohure.tanayenai.db.TanayenDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for FoodLog SQL queries.
 * Inserts data via SQLDelight directly to test the read path in isolation
 * on a real in-memory SQLite database.
 *
 * Run with: ./gradlew :shared:testAndroidHostTest
 */
class FoodLogRepositoryIntegrationTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: TanayenDatabase

    private val userId = "user_test"
    private val today = "2026-04-21"

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TanayenDatabase.Schema.create(driver)
        database = TanayenDatabase(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun insertLog(
        id: String,
        loggedAt: String,
        foodName: String = "Avena",
    ) {
        database.foodLogQueries.insertFoodLog(
            id = id,
            userId = userId,
            foodName = foodName,
            mealType = "BREAKFAST",
            calories = 300.0,
            proteinG = 10.0,
            carbsG = 40.0,
            fatG = 5.0,
            fiberG = 3.0,
            sodiumMg = 50.0,
            sugarG = 8.0,
            source = "CHAT_DETECTED",
            loggedAt = loggedAt,
        )
    }

    // ── getTodayFoodLogs ────────────────────────────────────────────────────────

    @Test
    fun `getTodayFoodLogs returns all logs for date`() {
        insertLog("1", "${today}T08:00:00Z")
        insertLog("2", "${today}T12:00:00Z")
        insertLog("3", "2026-04-22T08:00:00Z")

        val result =
            database.foodLogQueries
                .getTodayFoodLogs(userId = userId, datePrefix = today)
                .executeAsList()

        assertEquals(2, result.size)
    }

    // ── getLatestTodayFoodLogs ──────────────────────────────────────────────────

    @Test
    fun `getLatestTodayFoodLogs returns only N most recent logs`() {
        repeat(6) { i ->
            insertLog("log_$i", "${today}T0${i + 1}:00:00Z", "Comida $i")
        }

        val result =
            database.foodLogQueries
                .getLatestTodayFoodLogs(userId = userId, datePrefix = today, limit = 4)
                .executeAsList()

        assertEquals(4, result.size)
    }

    @Test
    fun `getLatestTodayFoodLogs returns most recent entries first`() {
        insertLog("early", "${today}T07:00:00Z", "Desayuno")
        insertLog("late", "${today}T13:00:00Z", "Almuerzo")

        val result =
            database.foodLogQueries
                .getLatestTodayFoodLogs(userId = userId, datePrefix = today, limit = 4)
                .executeAsList()

        assertEquals("Almuerzo", result.first().food_name)
    }

    @Test
    fun `getLatestTodayFoodLogs returns empty list when no logs for date`() {
        insertLog("1", "2026-04-20T08:00:00Z")

        val result =
            database.foodLogQueries
                .getLatestTodayFoodLogs(userId = userId, datePrefix = today, limit = 4)
                .executeAsList()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getLatestTodayFoodLogs does not include logs from other users`() {
        insertLog("mine", "${today}T09:00:00Z", "Mío")
        database.foodLogQueries.insertFoodLog(
            id = "other",
            userId = "other_user",
            foodName = "Ajeno",
            mealType = "LUNCH",
            calories = 500.0,
            proteinG = 20.0,
            carbsG = 50.0,
            fatG = 10.0,
            fiberG = 5.0,
            sodiumMg = 100.0,
            sugarG = 5.0,
            source = "CHAT_DETECTED",
            loggedAt = "${today}T10:00:00Z",
        )

        val result =
            database.foodLogQueries
                .getLatestTodayFoodLogs(userId = userId, datePrefix = today, limit = 4)
                .executeAsList()

        assertEquals(1, result.size)
        assertEquals("Mío", result.first().food_name)
    }
}
