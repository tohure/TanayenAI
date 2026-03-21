package dev.tohure.tanayenai.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
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
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import dev.tohure.tanayenai.domain.usecase.ChatTagParser
import dev.tohure.tanayenai.domain.usecase.FetchContextParamsUseCase
import dev.tohure.tanayenai.domain.usecase.SavePantryIngredientsUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val log = Logger.withTag("ChatViewModel")

@Immutable
data class PantrySuggestion(
    val ingredients: ImmutableList<String>,
    val confirmed: Boolean = false,
)

@Immutable
data class UiChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val hasAttachedImage: Boolean = false,
    val pantrySuggestion: PantrySuggestion? = null,
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
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private var cachedContext: String = ""

    init {
        buildContext()
    }

    private fun buildContext() {
        viewModelScope.launch {
            try {
                val contextParams =
                    fetchContextParamsUseCase.fetch(
                        userId = userId,
                        user = placeholderUser(),
                        clinicalProfile = placeholderClinicalProfile(),
                        today = currentIsoDate(),
                    )
                cachedContext = buildContextUseCase.build(contextParams)
                _uiState.value = _uiState.value.copy(contextReady = true)
            } catch (e: Exception) {
                log.e(e) { "Failed to build context" }
                _uiState.value = _uiState.value.copy(contextReady = true)
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
                            val visibleContent = ChatTagParser.stripForStreaming(fullResponse)
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
                    buildContext()
                }
            } catch (e: Exception) {
                log.e(e) { "Unhandled Gemini Exception" }
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

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
                val defaultLocationId = "10000000-0000-0000-0000-000000000001"
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

    // ── Dummies (reemplazar en fase futura con UserRepository / ClinicalProfileRepository) ──
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
}
