package dev.tohure.tanayenai.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.ChatMessage
import dev.tohure.tanayenai.domain.model.ChatRole
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test de integración de la memoria de chat (ventana reciente + compactación).
 * Usa JdbcSqliteDriver (SQLite en memoria) — corre en JVM sin emulador.
 * Comando: ./gradlew :shared:testAndroidHostTest
 */
class ChatMessageRepositoryIntegrationTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: TanayenDatabase
    private lateinit var repository: ChatMessageRepositoryImpl
    private lateinit var memoryRepository: ConversationMemoryRepositoryImpl

    private val testUserId = "user_test"

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TanayenDatabase.Schema.create(driver)
        database = TanayenDatabase(driver)
        repository = ChatMessageRepositoryImpl(database)
        memoryRepository = ConversationMemoryRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    // ── Ventana reciente ──────────────────────────────────────────────────────

    @Test
    fun `getRecentMessages returns messages in ascending chronological order`() =
        runTest {
            repository.saveMessage(msg("m1", ChatRole.USER, "hola", "2026-03-03T10:00:00Z"))
            repository.saveMessage(msg("m2", ChatRole.MODEL, "buenas", "2026-03-03T10:00:01Z"))
            repository.saveMessage(msg("m3", ChatRole.USER, "¿qué ceno?", "2026-03-03T10:05:00Z"))

            val recent = repository.getRecentMessages(testUserId, limit = 10)

            assertEquals(listOf("m1", "m2", "m3"), recent.map { it.id })
            assertEquals(ChatRole.USER, recent.first().role)
        }

    @Test
    fun `getRecentMessages respects the limit keeping the newest`() =
        runTest {
            repeat(5) { i ->
                repository.saveMessage(msg("m$i", ChatRole.USER, "msg $i", "2026-03-03T10:0$i:00Z"))
            }

            val recent = repository.getRecentMessages(testUserId, limit = 2)

            assertEquals(listOf("m3", "m4"), recent.map { it.id })
        }

    // ── Compactación (los más antiguos salen de la ventana) ─────────────────────

    @Test
    fun `getOldestMessages returns oldest first and deleteMessages removes them`() =
        runTest {
            repeat(4) { i ->
                repository.saveMessage(msg("m$i", ChatRole.USER, "msg $i", "2026-03-03T10:0$i:00Z"))
            }

            val oldest = repository.getOldestMessages(testUserId, limit = 2)
            assertEquals(listOf("m0", "m1"), oldest.map { it.id })

            repository.deleteMessages(oldest.map { it.id })

            assertEquals(2L, repository.countMessages(testUserId))
            assertEquals(listOf("m2", "m3"), repository.getRecentMessages(testUserId, 10).map { it.id })
        }

    @Test
    fun `deleteMessages with empty list is a no-op`() =
        runTest {
            repository.saveMessage(msg("m1", ChatRole.USER, "hola", "2026-03-03T10:00:00Z"))

            repository.deleteMessages(emptyList())

            assertEquals(1L, repository.countMessages(testUserId))
        }

    // ── Resumen rodante ─────────────────────────────────────────────────────────

    @Test
    fun `upsert summary overwrites previous value for same user`() =
        runTest {
            assertTrue(memoryRepository.getSummary(testUserId) == null)

            memoryRepository.saveSummary(testUserId, "resumen v1")
            assertEquals("resumen v1", memoryRepository.getSummary(testUserId))

            memoryRepository.saveSummary(testUserId, "resumen v2 actualizado")
            assertEquals("resumen v2 actualizado", memoryRepository.getSummary(testUserId))
        }

    // ── Helper ──────────────────────────────────────────────────────────────

    private fun msg(
        id: String,
        role: ChatRole,
        content: String,
        createdAt: String,
    ) = ChatMessage(
        id = id,
        userId = testUserId,
        role = role,
        content = content,
        createdAt = createdAt,
    )
}
