package dev.tohure.tanayenai.domain.usecase

import co.touchlab.kermit.Logger
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.PantryRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

private val log = Logger.withTag("GenerateMorningAdviceUseCase")

data class MorningAdvice(
    val title: String,
    val body: String,
)

class GenerateMorningAdviceUseCase(
    private val generativeModel: GenerativeModel,
    private val healthMetricsRepository: HealthMetricsRepository,
    private val clinicalProfileRepository: ClinicalProfileRepository,
    private val pantryRepository: PantryRepository,
    private val userId: String,
) {
    suspend fun generate(): MorningAdvice? {
        return try {
            val latestMetrics = healthMetricsRepository.getLatestMetrics(userId)
            val clinicalProfile = clinicalProfileRepository.getClinicalProfile(userId)
            val pantryItems =
                pantryRepository
                    .getPantryItems(userId)
                    .take(15)
                    .joinToString(", ") { it.ingredient }

            val restrictions =
                clinicalProfile
                    ?.activeRestrictions
                    ?.joinToString("; ")
                    ?: "Sin restricciones conocidas"

            val metricsContext =
                buildString {
                    latestMetrics?.let { m ->
                        m.sleepHours?.let { append("Sueño: ${it}h. ") }
                        m.hrv?.let { append("VFC: ${it.toInt()}ms. ") }
                        m.caloriesBurned?.let { append("Calorías quemadas ayer: ${it.toInt()} kcal. ") }
                    } ?: append("Sin métricas de hoy disponibles.")
                }

            val prompt =
                """
                Eres un asistente nutricional. Genera UN consejo de nutrición o hábito
                para este usuario basándote en sus datos de hoy.

                DATOS:
                - Métricas: $metricsContext
                - Restricciones clínicas activas: $restrictions
                - Alacena disponible: $pantryItems

                FORMATO DE RESPUESTA — JSON exacto, sin texto adicional:
                {
                  "title": "título corto máximo 50 caracteres",
                  "body": "consejo específico máximo 120 caracteres, menciona un alimento concreto de la alacena si aplica"
                }

                REGLAS:
                - Si el sueño fue < 6h, enfócate en recuperación y evitar cafeína tarde
                - Si la VFC es < 40ms, sugiere algo antiinflamatorio
                - Respeta las restricciones clínicas activas sin excepción
                - Sé concreto — menciona alimentos reales, no consejos genéricos
                - NO uses signos de exclamación ni lenguaje de marketing
                """.trimIndent()

            val response =
                generativeModel
                    .generateContent(content { text(prompt) })
                    .text ?: return defaultAdvice()

            parseAdvice(response) ?: defaultAdvice()
        } catch (e: Exception) {
            log.e(e) { "Failed to generate morning advice" }
            null
        }
    }

    private fun parseAdvice(raw: String): MorningAdvice? {
        return try {
            val cleaned =
                raw
                    .replace(Regex("```json\\s*"), "")
                    .replace(Regex("```\\s*"), "")
                    .trim()
            val json =
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            val obj = json.parseToJsonElement(cleaned).jsonObject
            MorningAdvice(
                title = obj["title"]?.toString()?.trim('"') ?: return null,
                body = obj["body"]?.toString()?.trim('"') ?: return null,
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to parse advice: $raw" }
            null
        }
    }

    private fun defaultAdvice() =
        MorningAdvice(
            title = "Buenos días 🌿",
            body = "Recuerda hidratarte bien antes de desayunar.",
        )
}
