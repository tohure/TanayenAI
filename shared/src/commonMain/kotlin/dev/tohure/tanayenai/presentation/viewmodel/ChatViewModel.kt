package dev.tohure.tanayenai.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.data.remote.dto.buildClinicalSummaryFromJson
import dev.tohure.tanayenai.domain.model.DEFAULT_LOCATION_ID
import dev.tohure.tanayenai.domain.model.FoodLogSource
import dev.tohure.tanayenai.domain.model.MealType
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.RecommendationType
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.model.generateId
import dev.tohure.tanayenai.domain.parser.ChatTagParser
import dev.tohure.tanayenai.domain.repository.FoodLogRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import dev.tohure.tanayenai.domain.usecase.EstimateFoodNutritionUseCase
import dev.tohure.tanayenai.domain.usecase.ExtractClinicalProfileUseCase
import dev.tohure.tanayenai.domain.usecase.FetchContextParamsUseCase
import dev.tohure.tanayenai.domain.usecase.SavePantryIngredientsUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val log = Logger.withTag("ChatViewModel")
private val jsonParser =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

@Immutable
data class PantrySuggestion(
    val ingredients: ImmutableList<String>,
    val confirmed: Boolean = false,
)

@Immutable
data class ClinicalSuggestion(
    val rawJson: String,
    val summary: String,
    val confirmed: Boolean = false,
)

@Immutable
data class FoodLogSuggestion(
    val description: String,
    val confirmed: Boolean = false,
)

@Immutable
data class CheckInSuggestion(
    val mealType: String,
    val recommendedFood: String,
    val userResponse: CheckInResponse = CheckInResponse.PENDING,
)

enum class CheckInResponse { PENDING, YES, NO }

@Immutable
data class UiChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val hasAttachedImage: Boolean = false,
    val pantrySuggestion: PantrySuggestion? = null,
    val clinicalSuggestion: ClinicalSuggestion? = null,
    val foodLogSuggestion: FoodLogSuggestion? = null,
    val checkInSuggestion: CheckInSuggestion? = null,
)

@Immutable
data class PendingImage(
    val base64Data: String,
    val mimeType: String = "image/jpeg",
)

data class ChatUiState(
    val messages: List<UiChatMessage> =
        listOf(
            UiChatMessage(
                id = "welcome",
                content =
                    "Hola 🌿 Soy Tanayen. Puedes escribirme sobre nutrición, enviarme " +
                        "fotos del super, de algún menú o de tu alacena, y yo te ayudaré.",
                isUser = false,
            ),
        ),
    val isLoading: Boolean = false,
    val error: String? = null,
    val contextReady: Boolean = false,
    val pendingImage: PendingImage? = null,
)

class ChatViewModel(
    private val generativeModel: GenerativeModel,
    private val buildContextUseCase: BuildContextUseCase,
    private val fetchContextParamsUseCase: FetchContextParamsUseCase,
    private val savePantryIngredientsUseCase: SavePantryIngredientsUseCase,
    private val recommendationRepository: RecommendationRepository,
    private val extractClinicalProfileUseCase: ExtractClinicalProfileUseCase,
    private val estimateFoodNutritionUseCase: EstimateFoodNutritionUseCase,
    private val foodLogRepository: FoodLogRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private var cachedContext: String = ""
    private var contextJob: Job? = null

    companion object {
        val FOODLOG_TAG_REGEX = Regex("""\[FOODLOG:(\{[^\]]+\})\]""", RegexOption.IGNORE_CASE)
        val CHECKIN_TAG_REGEX = Regex("""\[CHECKIN:(\{[^\]]+\})\]""", RegexOption.IGNORE_CASE)
    }

    init {
        buildContext()
    }

    private fun buildContext() {
        contextJob?.cancel()
        contextJob =
            viewModelScope.launch {
                try {
                    val today = currentIsoDate()
                    val contextParams =
                        fetchContextParamsUseCase.fetch(userId = userId, today = today)
                    val todayFoodLogs = foodLogRepository.getTodayFoodLogs(userId, today)
                    val enrichedParams = contextParams.copy(todayFoodLogs = todayFoodLogs)
                    cachedContext = buildContextUseCase.build(enrichedParams)
                    _uiState.value = _uiState.value.copy(contextReady = true)
                } catch (e: Exception) {
                    log.e(e) { "Failed to build context" }
                }
            }
    }

    // ── Preselección de Imagen ────────────────────────────────────────────────
    fun attachImage(
        base64: String,
        mimeType: String = "image/jpeg",
    ) {
        _uiState.value = _uiState.value.copy(pendingImage = PendingImage(base64, mimeType))
    }

    fun clearPendingImage() {
        _uiState.value = _uiState.value.copy(pendingImage = null)
    }

    // ── Envío Inteligente ─────────────────────────────────────────────────────
    @OptIn(ExperimentalEncodingApi::class)
    fun sendMessage(userText: String) {
        val pendingImage = _uiState.value.pendingImage
        if (userText.isBlank() && pendingImage == null) return
        if (_uiState.value.isLoading) return

        val safeText = userText.trim()

        val userMessage =
            UiChatMessage(
                id = generateId(),
                content = safeText,
                isUser = true,
                hasAttachedImage = pendingImage != null,
            )
        val loadingId = generateId()
        val loadingMessage = UiChatMessage(id = loadingId, content = "", isUser = false, isLoading = true)

        _uiState.value =
            _uiState.value.copy(
                messages = _uiState.value.messages + userMessage + loadingMessage,
                isLoading = true,
                pendingImage = null,
                error = null,
            )

        viewModelScope.launch {
            var fullResponse = ""
            var firstChunk = true
            val assistantMessageId = generateId()

            try {
                val contents =
                    buildList {
                        conversationHistory.forEach { (role, msg) ->
                            add(content(role) { text(msg) })
                        }
                        add(
                            content("user") {
                                if (pendingImage != null) {
                                    val decoded = Base64.decode(pendingImage.base64Data.replace("\\s".toRegex(), ""))
                                    image(decoded)
                                }
                                val finalPrompt =
                                    if (safeText.isBlank() && pendingImage != null) {
                                        "El usuario te acaba de enviar solo esta imagen, sin texto."
                                    } else {
                                        safeText
                                    }
                                text(
                                    "Contexto actualizado de salud y alacena:\n$cachedContext\n\n" +
                                        "Mensaje del usuario:\n$finalPrompt",
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
                                            content =
                                                "Hubo un error al conectar con el asistente. " +
                                                    "Intenta de nuevo. (${e.message})",
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
                                        _uiState.value.messages.filter { it.id != loadingId } +
                                            UiChatMessage(id = assistantMessageId, content = "", isUser = false),
                                )
                        }

                        for (char in text) {
                            fullResponse += char
                            val visibleContent = buildVisibleContent(fullResponse)
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
                            delay(8)
                        }
                    }

                if (fullResponse.isNotEmpty()) {
                    conversationHistory.add("user" to safeText)
                    conversationHistory.add("model" to fullResponse)
                    if (conversationHistory.size > 20) {
                        conversationHistory.removeAt(0)
                        conversationHistory.removeAt(0)
                    }
                    extractAndSaveRecommendation(fullResponse)
                    extractPantrySuggestion(fullResponse, assistantMessageId)
                    extractClinicalSuggestion(fullResponse, assistantMessageId)
                    extractFoodLogSuggestion(fullResponse, assistantMessageId)
                    extractCheckInSuggestion(fullResponse, assistantMessageId)
                    buildContext()
                }
            } catch (e: Exception) {
                log.e(e) { "Unhandled Gemini Exception" }
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun buildVisibleContent(fullResponse: String): String =
        ChatTagParser
            .stripForStreaming(fullResponse)
            .replace(FOODLOG_TAG_REGEX, "")
            .replace(CHECKIN_TAG_REGEX, "")
            .trim()

    // ── Alacena Sugerencias ───────────────────────────────────────────────────
    private fun extractPantrySuggestion(
        response: String,
        messageId: String,
    ) {
        val ingredients = ChatTagParser.extractPantryIngredients(response) ?: return
        _uiState.value =
            _uiState.value.copy(
                messages =
                    _uiState.value.messages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(pantrySuggestion = PantrySuggestion(ingredients = ingredients.toImmutableList()))
                        } else {
                            msg
                        }
                    },
            )
    }

    fun confirmPantrySuggestion(messageId: String) {
        viewModelScope.launch {
            val message = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val suggestion = message.pantrySuggestion ?: return@launch
            try {
                val defaultLocationId = DEFAULT_LOCATION_ID
                savePantryIngredientsUseCase.saveIngredientsByName(
                    names = suggestion.ingredients,
                    locationId = defaultLocationId,
                    userId = userId,
                )
                _uiState.value =
                    _uiState.value.copy(
                        messages =
                            _uiState.value.messages.map { msg ->
                                if (msg.id == messageId) {
                                    msg.copy(pantrySuggestion = suggestion.copy(confirmed = true))
                                } else {
                                    msg
                                }
                            },
                    )
            } catch (e: Exception) {
                log.e(e) { "Failed to save pantry suggestion" }
            }
        }
    }

    fun dismissPantrySuggestion(messageId: String) {
        _uiState.value =
            _uiState.value.copy(
                messages =
                    _uiState.value.messages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(pantrySuggestion = null)
                        } else {
                            msg
                        }
                    },
            )
    }

    // ── Perfil Clínico Sugerencias ────────────────────────────────────────────
    private fun extractClinicalSuggestion(
        response: String,
        messageId: String,
    ) {
        val rawJson = ChatTagParser.extractClinicalJson(response) ?: return
        val summary = buildClinicalSummaryFromJson(rawJson).ifEmpty { return }

        _uiState.value =
            _uiState.value.copy(
                messages =
                    _uiState.value.messages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(clinicalSuggestion = ClinicalSuggestion(rawJson, summary))
                        } else {
                            msg
                        }
                    },
            )
    }

    fun confirmClinicalSuggestion(messageId: String) {
        viewModelScope.launch {
            val msg = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val suggestion = msg.clinicalSuggestion ?: return@launch
            try {
                extractClinicalProfileUseCase.saveFromJson(suggestion.rawJson)
                _uiState.value =
                    _uiState.value.copy(
                        messages =
                            _uiState.value.messages.map {
                                if (it.id == messageId) {
                                    it.copy(clinicalSuggestion = suggestion.copy(confirmed = true))
                                } else {
                                    it
                                }
                            },
                    )
                buildContext()
            } catch (e: Exception) {
                log.e(e) { "Failed to save clinical suggestion" }
            }
        }
    }

    fun dismissClinicalSuggestion(messageId: String) {
        _uiState.value =
            _uiState.value.copy(
                messages =
                    _uiState.value.messages.map {
                        if (it.id == messageId) it.copy(clinicalSuggestion = null) else it
                    },
            )
    }

    // ── Food Log detectado del chat ───────────────────────────────────────────
    private fun extractFoodLogSuggestion(
        response: String,
        messageId: String,
    ) {
        val match = FOODLOG_TAG_REGEX.find(response) ?: return
        val rawJson = match.groupValues[1]
        val description =
            runCatching {
                jsonParser
                    .parseToJsonElement(rawJson)
                    .jsonObject["description"]
                    ?.toString()
                    ?.trim('"')
            }.getOrNull() ?: return

        _uiState.value =
            _uiState.value.copy(
                messages =
                    _uiState.value.messages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(foodLogSuggestion = FoodLogSuggestion(description))
                        } else {
                            msg
                        }
                    },
            )
    }

    fun confirmFoodLogSuggestion(messageId: String) {
        viewModelScope.launch {
            val msg = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val suggestion = msg.foodLogSuggestion ?: return@launch
            try {
                estimateFoodNutritionUseCase.estimateAndSave(
                    foodDescription = suggestion.description,
                    source = FoodLogSource.CHAT_DETECTED,
                )
                _uiState.value =
                    _uiState.value.copy(
                        messages =
                            _uiState.value.messages.map {
                                if (it.id == messageId) {
                                    it.copy(foodLogSuggestion = suggestion.copy(confirmed = true))
                                } else {
                                    it
                                }
                            },
                    )
                buildContext()
            } catch (e: Exception) {
                log.e(e) { "Failed to save food log" }
            }
        }
    }

    fun dismissFoodLogSuggestion(messageId: String) {
        _uiState.value =
            _uiState.value.copy(
                messages =
                    _uiState.value.messages.map {
                        if (it.id == messageId) it.copy(foodLogSuggestion = null) else it
                    },
            )
    }

    // ── Check-in proactivo ────────────────────────────────────────────────────
    private fun extractCheckInSuggestion(
        response: String,
        messageId: String,
    ) {
        val match = CHECKIN_TAG_REGEX.find(response) ?: return
        val rawJson = match.groupValues[1]
        runCatching {
            val obj = jsonParser.parseToJsonElement(rawJson).jsonObject
            val mealType = obj["meal_type"]?.toString()?.trim('"') ?: return
            val recommendedFood = obj["recommended_food"]?.toString()?.trim('"') ?: return

            _uiState.value =
                _uiState.value.copy(
                    messages =
                        _uiState.value.messages.map { msg ->
                            if (msg.id == messageId) {
                                msg.copy(checkInSuggestion = CheckInSuggestion(mealType, recommendedFood))
                            } else {
                                msg
                            }
                        },
                )
        }
    }

    fun confirmCheckInYes(messageId: String) {
        viewModelScope.launch {
            val msg = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val checkIn = msg.checkInSuggestion ?: return@launch

            val mealType = runCatching { MealType.valueOf(checkIn.mealType) }.getOrDefault(MealType.SNACK)

            estimateFoodNutritionUseCase.estimateAndSave(
                foodDescription = checkIn.recommendedFood,
                source = FoodLogSource.PROACTIVE_CHECKIN,
                mealTypeHint = mealType,
            )

            _uiState.value =
                _uiState.value.copy(
                    messages =
                        _uiState.value.messages.map {
                            if (it.id == messageId) {
                                it.copy(checkInSuggestion = checkIn.copy(userResponse = CheckInResponse.YES))
                            } else {
                                it
                            }
                        },
                )
            buildContext()
            sendMessage("Registrado, gracias")
        }
    }

    fun confirmCheckInNo(messageId: String) {
        _uiState.value =
            _uiState.value.copy(
                messages =
                    _uiState.value.messages.map {
                        if (it.id == messageId) {
                            it.copy(
                                checkInSuggestion =
                                    it.checkInSuggestion
                                        ?.copy(userResponse = CheckInResponse.NO),
                            )
                        } else {
                            it
                        }
                    },
            )
        viewModelScope.launch {
            val followUp =
                UiChatMessage(
                    id = generateId(),
                    content = "Está bien, ¿qué comiste en su lugar?",
                    isUser = false,
                )
            _uiState.value =
                _uiState.value.copy(
                    messages = _uiState.value.messages + followUp,
                )
        }
    }

    // ── REC Tag → Recomendaciones ─────────────────────────────────────────────
    private suspend fun extractAndSaveRecommendation(response: String) {
        val rec = ChatTagParser.extractRecAction(response) ?: return
        try {
            val recommendation =
                Recommendation(
                    id = generateId(),
                    userId = userId,
                    type = runCatching { RecommendationType.valueOf(rec.type) }.getOrDefault(RecommendationType.MEAL),
                    title = rec.title,
                    content = response,
                    ingredientsUsed = rec.ingredients,
                    recommendedAt = currentIsoDateTime(),
                )
            recommendationRepository.saveRecommendation(recommendation)
        } catch (e: Exception) {
            log.e(e) { "Failed to save recommendation: ${rec.title}" }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
