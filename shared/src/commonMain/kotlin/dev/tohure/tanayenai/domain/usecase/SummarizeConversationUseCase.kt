package dev.tohure.tanayenai.domain.usecase

import co.touchlab.kermit.Logger
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.domain.model.ChatMessage
import dev.tohure.tanayenai.domain.model.ChatRole
import dev.tohure.tanayenai.domain.repository.ChatMessageRepository
import dev.tohure.tanayenai.domain.repository.ConversationMemoryRepository

private val log = Logger.withTag("SummarizeConversationUseCase")

/**
 * Memoria híbrida de conversación:
 *  - Ventana reciente: se conservan [RECENT_WINDOW] mensajes verbatim en la BD.
 *  - Resumen rodante: cuando el total supera [COMPACT_TRIGGER], los mensajes más
 *    antiguos que exceden la ventana se funden en un único resumen de texto plano
 *    vía Gemini y luego se eliminan. El resumen "rueda hacia adelante".
 *
 * Así el contexto inyectado siempre es acotado: resumen breve + últimos turnos exactos.
 */
class SummarizeConversationUseCase(
    private val generativeModel: GenerativeModel,
    private val chatMessageRepository: ChatMessageRepository,
    private val conversationMemoryRepository: ConversationMemoryRepository,
    private val userId: String,
) {
    companion object {
        /** Mensajes recientes que se conservan verbatim (≈15 turnos). */
        const val RECENT_WINDOW = 30

        /** Umbral a partir del cual se dispara la compactación (evita llamar a Gemini cada turno). */
        const val COMPACT_TRIGGER = 40
    }

    /**
     * Compacta los mensajes viejos en el resumen rodante si se superó el umbral.
     * Es idempotente y silencioso ante fallos: nunca bloquea el flujo del chat.
     */
    suspend fun compactIfNeeded(): Boolean {
        return try {
            val total = chatMessageRepository.countMessages(userId)
            if (total < COMPACT_TRIGGER) return false

            val toCompactCount = (total - RECENT_WINDOW).toInt()
            if (toCompactCount <= 0) return false

            val oldMessages = chatMessageRepository.getOldestMessages(userId, toCompactCount)
            if (oldMessages.isEmpty()) return false

            val previousSummary = conversationMemoryRepository.getSummary(userId)
            val updatedSummary = callGemini(previousSummary, oldMessages)

            if (updatedSummary.isNotBlank()) {
                conversationMemoryRepository.saveSummary(userId, updatedSummary)
                chatMessageRepository.deleteMessages(oldMessages.map { it.id })
                log.i { "Compacted ${oldMessages.size} messages into rolling summary" }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            log.e(e) { "Conversation compaction failed" }
            false
        }
    }

    private suspend fun callGemini(
        previousSummary: String?,
        messages: List<ChatMessage>,
    ): String {
        val transcript =
            messages.joinToString("\n") { msg ->
                val who = if (msg.role == ChatRole.USER) "Usuario" else "Asistente"
                "$who: ${msg.content}"
            }

        val previousBlock =
            if (previousSummary.isNullOrBlank()) {
                "No hay resumen previo."
            } else {
                previousSummary
            }

        val prompt =
            """
            Eres el asistente nutricional Tanayen. Mantén una MEMORIA compacta de la conversación
            con el usuario para retomar sesiones futuras con continuidad.

            Resumen previo acumulado:
            $previousBlock

            Nuevos mensajes a integrar (los más antiguos, que van a archivarse):
            $transcript

            Devuelve ÚNICAMENTE el resumen actualizado en español, en texto plano (sin markdown,
            sin viñetas), máximo ~150 palabras. Integra lo nuevo con lo previo, conserva:
            - preferencias, gustos y rechazos del usuario
            - metas y restricciones mencionadas
            - recomendaciones dadas y qué le funcionó o no
            - temas o dudas que quedaron pendientes
            Descarta small talk irrelevante. No inventes datos que no estén en el texto.
            """.trimIndent()

        return generativeModel
            .generateContent(content { text(prompt) })
            .text
            .orEmpty()
            .trim()
    }
}
