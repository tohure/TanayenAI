package dev.tohure.tanayenai.data.remote

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val log = Logger.withTag("GeminiClient")

private val json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

// ── DTOs de la API de Gemini ──────────────────────────────────────────────────

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction") val systemInstruction: GeminiContent? = null,
    @SerialName("generation_config") val generationConfig: GenerationConfig? = null,
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
    val text: String? = null,
)

@Serializable
data class GenerationConfig(
    val temperature: Double = 0.7,
    @SerialName("max_output_tokens") val maxOutputTokens: Int = 2048,
    @SerialName("top_p") val topP: Double = 0.95,
)

@Serializable
data class GeminiStreamResponse(
    val candidates: List<GeminiCandidate>? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

// ── Cliente ───────────────────────────────────────────────────────────────────

class GeminiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = "gemini-2.5-pro",
) {
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model"

    /**
     * Envía un mensaje a Gemini y retorna un Flow que emite chunks de texto
     * a medida que el modelo los genera (streaming).
     *
     * @param systemPrompt  El contexto estructurado del ContextBuilder
     * @param history       Mensajes anteriores de la conversación (máximo últimos 10)
     * @param userMessage   El mensaje nuevo del usuario
     */
    fun sendMessageStream(
        systemPrompt: String,
        history: List<Pair<String, String>>, // (role, content): "user" o "model"
        userMessage: String,
    ): Flow<String> =
        flow {
            try {
                // Construir el historial de conversación
                val contents =
                    buildList {
                        history.forEach { (role, content) ->
                            add(
                                GeminiContent(
                                    role = role,
                                    parts = listOf(GeminiPart(text = content)),
                                ),
                            )
                        }
                        // Agregar el mensaje nuevo del usuario
                        add(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart(text = userMessage)),
                            ),
                        )
                    }

                val request =
                    GeminiRequest(
                        contents = contents,
                        systemInstruction =
                            GeminiContent(
                                parts = listOf(GeminiPart(text = systemPrompt)),
                            ),
                        generationConfig =
                            GenerationConfig(
                                temperature = 0.7,
                                maxOutputTokens = 2048,
                            ),
                    )

                // Llamada SSE al endpoint de streaming
                httpClient.sse(
                    urlString = "$baseUrl:streamGenerateContent?alt=sse&key=$apiKey",
                    request = {
                        headers {
                            append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        }
                        method = io.ktor.http.HttpMethod.Post
                        setBody(json.encodeToString(GeminiRequest.serializer(), request))
                    },
                ) {
                    incoming.collect { event ->
                        val data = event.data ?: return@collect
                        if (data == "[DONE]") return@collect

                        try {
                            val response = json.decodeFromString<GeminiStreamResponse>(data)
                            val chunk =
                                response.candidates
                                    ?.firstOrNull()
                                    ?.content
                                    ?.parts
                                    ?.firstOrNull()
                                    ?.text
                                    ?: return@collect

                            emit(chunk)
                        } catch (e: Exception) {
                            // Chunk malformado — ignorar y continuar
                            log.d { "Skipping malformed SSE chunk: $data" }
                        }
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "Gemini streaming failed" }
                throw e
            }
        }
}
