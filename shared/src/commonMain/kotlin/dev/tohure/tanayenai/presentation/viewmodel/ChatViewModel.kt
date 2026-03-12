package dev.tohure.tanayenai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.domain.model.ActivityLevel
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.RecommendationType
import dev.tohure.tanayenai.domain.model.Sex
import dev.tohure.tanayenai.domain.model.User
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.model.generateId
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.PantryRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import dev.tohure.tanayenai.domain.usecase.ContextParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val log = Logger.withTag("ChatViewModel")

data class UiChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
)

data class ChatUiState(
    val messages: List<UiChatMessage> =
        listOf(
            UiChatMessage(
                id = "welcome",
                content =
                    "Hola 🌿 Soy Tanayen AI. Puedes contarme qué comiste, " +
                        "mandarme fotos de tu alacena, o preguntarme qué comer hoy.",
                isUser = false,
            ),
        ),
    val isLoading: Boolean = false,
    val error: String? = null,
    val contextReady: Boolean = false,
)

@Serializable
data class RecommendationExtraction(
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("ingredients") val ingredients: List<String> = emptyList(),
)

class ChatViewModel(
    private val generativeModel: GenerativeModel,
    private val buildContextUseCase: BuildContextUseCase,
    private val healthMetricsRepository: HealthMetricsRepository,
    private val pantryRepository: PantryRepository,
    private val recommendationRepository: RecommendationRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Historial de conversación para Gemini (máximo últimos 10 turnos)
    // Cada par es (role, content): role = "user" | "model"
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    // Contexto pre-armado — se reconstruye si los datos cambian
    private var cachedContext: String = ""

    init {
        buildContext()
    }

    // ── Construir contexto ────────────────────────────────────────────────────
    private fun buildContext() {
        viewModelScope.launch {
            try {
                val recentMetrics =
                    healthMetricsRepository
                        .getMetricsForDateRange(userId, daysAgo(7), currentIsoDate())
                val recentRecommendations =
                    recommendationRepository
                        .getRecentRecommendations(userId, days = 7)

                // TODO: cargar user y clinicalProfile reales desde repositorios
                // Por ahora usamos placeholders
                val contextParams =
                    ContextParams(
                        user = placeholderUser(),
                        clinicalProfile = placeholderClinicalProfile(),
                        recentMetrics = recentMetrics,
                        pantryItems = emptyList(), // TODO: alacena real
                        locationNames = emptyMap(),
                        recentRecommendations = recentRecommendations,
                        todayFoodLogs = emptyList(),
                        today = currentIsoDate(),
                        workContext = "Sin especificar",
                    )

                cachedContext = buildContextUseCase.build(contextParams)
                _uiState.value = _uiState.value.copy(contextReady = true)
                log.d { "Context built successfully (${cachedContext.length} chars)" }
            } catch (e: Exception) {
                log.e(e) { "Failed to build context" }
                cachedContext = "Sin datos de contexto disponibles en este momento."
                _uiState.value = _uiState.value.copy(contextReady = true)
            }
        }
    }

    // ── Enviar mensaje ────────────────────────────────────────────────────────
    fun sendMessage(userText: String) {
        if (userText.isBlank() || _uiState.value.isLoading) return

        val userMessage =
            UiChatMessage(
                id = generateId(),
                content = userText.trim(),
                isUser = true,
            )
        val loadingId = generateId()
        val loadingMessage =
            UiChatMessage(
                id = loadingId,
                content = "",
                isUser = false,
                isLoading = true,
            )

        _uiState.value =
            _uiState.value.copy(
                messages = _uiState.value.messages + userMessage + loadingMessage,
                isLoading = true,
                error = null,
            )

        viewModelScope.launch {
            var fullResponse = ""
            var firstChunk = true
            val assistantMessageId = generateId()

            try {
                // Preparamos los mensajes de la historia usando la DSL content {} del SDK oficial
                val contents =
                    buildList {
                        // Historial previo
                        conversationHistory.forEach { (role, msg) ->
                            add(content(role) { text(msg) })
                        }
                        // Nuevo mensaje inyectando el contexto dinámicamente de forma invisible
                        add(
                            content("user") {
                                text(
                                    "Contexto actualizado de salud y alacena:\n$cachedContext\n\nResponde al siguiente mensaje del usuario:\n${userText.trim()}",
                                )
                            },
                        )
                    }

                generativeModel
                    .generateContentStream(*contents.toTypedArray())
                    .catch { e ->
                        log.e(e) { "Streaming error" }
                        _uiState.value =
                            _uiState.value.copy(
                                messages =
                                    _uiState.value.messages.filter { it.id != assistantMessageId } +
                                        UiChatMessage(
                                            id = assistantMessageId,
                                            content = "Hubo un error al conectar con el asistente. Intenta de nuevo.",
                                            isUser = false,
                                        ),
                                isLoading = false,
                                error = e.message,
                            )
                    }.collect { chunk ->
                        val text = chunk.text ?: return@collect

                        if (firstChunk) {
                            firstChunk = false
                            _uiState.value =
                                _uiState.value.copy(
                                    messages =
                                        _uiState.value.messages
                                            .filter { it.id != loadingId } +
                                            UiChatMessage(id = assistantMessageId, content = "", isUser = false),
                                )
                        }

                        for (char in text) {
                            fullResponse += char
                            val visibleContent =
                                fullResponse
                                    .replace(Regex("```json[\\s\\S]*?```"), "")
                                    .trim()
                            _uiState.value =
                                _uiState.value.copy(
                                    messages =
                                        _uiState.value.messages.map { msg ->
                                            if (msg.id == assistantMessageId) {
                                                msg.copy(content = visibleContent)
                                            } else {
                                                msg
                                            }
                                        },
                                )
                            delay(12)
                        }
                    }

                if (fullResponse.isNotEmpty()) {
                    conversationHistory.add("user" to userText.trim())
                    conversationHistory.add("model" to fullResponse)

                    if (conversationHistory.size > 20) {
                        conversationHistory.removeAt(0)
                        conversationHistory.removeAt(0)
                    }

                    extractAndSaveRecommendation(fullResponse)
                    buildContext() // Actualizamos contexto por si ya recomendó algo y se debe evitar repetir
                }
            } catch (e: Exception) {
                log.e(e) { "Unhandled Gemini Exception" }
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // ── Extraer y guardar recomendaciones via JSON ───────────────────────────
    private suspend fun extractAndSaveRecommendation(response: String) {
        val jsonRegex = Regex("```json([\\s\\S]*?)```")
        val match = jsonRegex.find(response) ?: return // No dictó receta

        try {
            val jsonStr: String = match.groupValues[1].trim()
            val format =
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            val dto = format.decodeFromString<RecommendationExtraction>(jsonStr)

            val recommendation =
                Recommendation(
                    id = generateId(),
                    userId = userId,
                    type =
                        runCatching {
                            RecommendationType.valueOf(
                                dto.type.uppercase(),
                            )
                        }.getOrDefault(RecommendationType.MEAL),
                    title = dto.title.trim(),
                    content = Json.encodeToString(mapOf("raw_response" to response)),
                    ingredientsUsed = dto.ingredients,
                    recommendedAt = currentIsoDateTime(),
                )
            recommendationRepository.saveRecommendation(recommendation)
            log.d { "Saved recommendation from JSON: ${recommendation.title}" }
        } catch (e: Exception) {
            log.e(e) { "Failed to parse recommendation JSON structure: ${match.groupValues[1]}" }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Placeholders ────────────────────────────────────────────
    private fun placeholderUser() =
        User(
            id = userId,
            name = "Carlo",
            birthDate = "1990-05-15",
            sex = Sex.MALE,
            heightCm = 175f,
            goal = NutritionGoal.EAT_HEALTHY,
            activityLevel = ActivityLevel.MODERATE,
        )

    private fun placeholderClinicalProfile() =
        ClinicalProfile(
            userId = userId,
            cholesterolTotal = 215f,
            hdl = 42f,
            ldl = 148f,
            triglycerides = 180f,
            fastingGlucose = 102f,
            hba1c = 5.8f,
            systolicPressure = 125,
            diastolicPressure = 82,
        )

    private fun daysAgo(days: Int): String = currentIsoDate()

    // ── iOS Helpers  ────────────────────────────────────────────────
    fun observeUiState(onChange: (ChatUiState) -> Unit) {
        viewModelScope.launch {
            uiState.collect { onChange(it) }
        }
    }
}
