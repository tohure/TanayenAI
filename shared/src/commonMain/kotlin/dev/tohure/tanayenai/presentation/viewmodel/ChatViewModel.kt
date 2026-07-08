package dev.tohure.tanayenai.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.data.remote.dto.buildClinicalSummaryFromJson
import dev.tohure.tanayenai.domain.model.ChatMessage
import dev.tohure.tanayenai.domain.model.ChatRole
import dev.tohure.tanayenai.domain.model.DEFAULT_LOCATION_ID
import dev.tohure.tanayenai.domain.model.FoodLogSource
import dev.tohure.tanayenai.domain.model.MealType
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.RecommendationType
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.model.generateId
import dev.tohure.tanayenai.domain.parser.ChatTagParser
import dev.tohure.tanayenai.domain.repository.ChatMessageRepository
import dev.tohure.tanayenai.domain.repository.FoodLogRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.repository.UserRepository
import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import dev.tohure.tanayenai.domain.usecase.ContextParams
import dev.tohure.tanayenai.domain.usecase.EstimateFoodNutritionUseCase
import dev.tohure.tanayenai.domain.usecase.ExtractClinicalProfileUseCase
import dev.tohure.tanayenai.domain.usecase.FetchContextParamsUseCase
import dev.tohure.tanayenai.domain.usecase.SavePantryIngredientsUseCase
import dev.tohure.tanayenai.domain.usecase.SummarizeConversationUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
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

// Tope de fotos por envío: evita reventar el tamaño del request a Gemini y la memoria.
const val MAX_PENDING_IMAGES = 6

@Immutable
data class PantrySuggestion(
    val ingredients: ImmutableList<String>,
    val confirmed: Boolean = false,
    val isLoading: Boolean = false,
)

@Immutable
data class ClinicalSuggestion(
    val rawJson: String,
    val summary: String,
    val confirmed: Boolean = false,
    val isLoading: Boolean = false,
)

@Immutable
data class FoodLogSuggestion(
    val description: String,
    val confirmed: Boolean = false,
    val isLoading: Boolean = false,
)

@Immutable
data class CheckInSuggestion(
    val mealType: String,
    val recommendedFood: String,
    val userResponse: CheckInResponse = CheckInResponse.PENDING,
    val isLoading: Boolean = false,
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

@Immutable
data class ChatUiState(
    val messages: ImmutableList<UiChatMessage> =
        persistentListOf(
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
    val pendingImages: ImmutableList<PendingImage> = persistentListOf(),
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
    private val userRepository: UserRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val summarizeConversationUseCase: SummarizeConversationUseCase,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private var cachedContext: String = ""
    private var cachedContextParams: ContextParams? = null
    private var contextJob: Job? = null

    companion object {
        val FOODLOG_TAG_REGEX = Regex("""\[FOODLOG:(\{[^\]]+\})\]""", RegexOption.IGNORE_CASE)
        val CHECKIN_TAG_REGEX = Regex("""\[CHECKIN:(\{[^\]]+\})\]""", RegexOption.IGNORE_CASE)

        /** Efecto typewriter: caracteres revelados por tick y pausa entre ticks. */
        private const val TYPEWRITER_STEP = 3
        private const val TYPEWRITER_DELAY_MS = 12L

        /** Mensajes recientes que se recargan al reabrir el chat (≈15 turnos). */
        private const val PERSISTED_WINDOW = 30

        /** Máximo de mensajes crudos que se envían a Gemini como historial vivo (10 turnos). */
        private const val GEMINI_HISTORY_LIMIT = 20
    }

    init {
        loadPersistedConversation()
        buildContext()
    }

    /**
     * Recarga la ventana reciente de mensajes desde la BD al abrir el chat, para que el
     * asistente retome donde se quedó. El resumen rodante de sesiones más antiguas se
     * inyecta aparte vía [buildContext] → [ContextParams.conversationSummary].
     */
    private fun loadPersistedConversation() {
        viewModelScope.launch {
            val recent =
                runCatching { chatMessageRepository.getRecentMessages(userId, PERSISTED_WINDOW) }
                    .getOrElse {
                        log.e(it) { "Failed to load persisted conversation" }
                        return@launch
                    }
            if (recent.isEmpty()) return@launch

            // Historial vivo para Gemini (crudo, con tags), acotado a los últimos N.
            conversationHistory.clear()
            recent.takeLast(GEMINI_HISTORY_LIMIT).forEach { msg ->
                conversationHistory.add(msg.role.geminiRole to msg.content)
            }

            // Mensajes visibles en la UI (tags ocultos en los del asistente).
            val uiMessages =
                recent.map { msg ->
                    UiChatMessage(
                        id = msg.id,
                        content =
                            if (msg.role == ChatRole.USER) msg.content else buildVisibleContent(msg.content),
                        isUser = msg.role == ChatRole.USER,
                    )
                }
            _uiState.update { state -> state.copy(messages = uiMessages.toImmutableList()) }
        }
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
                    cachedContextParams = enrichedParams
                    cachedContext = buildContextUseCase.build(enrichedParams)
                    _uiState.update { it.copy(contextReady = true) }
                } catch (e: Exception) {
                    log.e(e) { "Failed to build context" }
                }
            }
    }

    // ── Preselección de Imágenes ──────────────────────────────────────────────
    fun attachImage(
        base64: String,
        mimeType: String = "image/jpeg",
    ) {
        _uiState.update { state ->
            if (state.pendingImages.size >= MAX_PENDING_IMAGES) {
                state
            } else {
                state.copy(
                    pendingImages = (state.pendingImages + PendingImage(base64, mimeType)).toImmutableList(),
                )
            }
        }
    }

    fun removePendingImage(index: Int) {
        _uiState.update { state ->
            if (index !in state.pendingImages.indices) {
                state
            } else {
                state.copy(
                    pendingImages =
                        state.pendingImages
                            .filterIndexed { i, _ -> i != index }
                            .toImmutableList(),
                )
            }
        }
    }

    fun clearPendingImage() {
        _uiState.update { it.copy(pendingImages = persistentListOf()) }
    }

    // ── Envío Inteligente ─────────────────────────────────────────────────────
    @OptIn(ExperimentalEncodingApi::class)
    fun sendMessage(userText: String) {
        val pendingImages = _uiState.value.pendingImages
        if (userText.isBlank() && pendingImages.isEmpty()) return
        if (_uiState.value.isLoading) return

        val safeText = userText.trim()
        val sentAt = currentIsoDateTime()

        val userMessage =
            UiChatMessage(
                id = generateId(),
                content = safeText,
                isUser = true,
                hasAttachedImage = pendingImages.isNotEmpty(),
            )
        val loadingId = generateId()
        val loadingMessage = UiChatMessage(id = loadingId, content = "", isUser = false, isLoading = true)

        _uiState.update { state ->
            state.copy(
                messages = (state.messages + userMessage + loadingMessage).toImmutableList(),
                isLoading = true,
                pendingImages = persistentListOf(),
                error = null,
            )
        }

        viewModelScope.launch {
            var fullResponse = ""
            var firstChunk = true
            var shownLength = 0 // caracteres del contenido visible ya revelados (typewriter)
            val assistantMessageId = generateId()

            try {
                val contents =
                    buildList {
                        conversationHistory.forEach { (role, msg) ->
                            add(content(role) { text(msg) })
                        }
                        add(
                            content("user") {
                                pendingImages.forEach { pending ->
                                    val decoded = Base64.decode(pending.base64Data.replace("\\s".toRegex(), ""))
                                    image(decoded)
                                }
                                val finalPrompt =
                                    if (safeText.isBlank() && pendingImages.isNotEmpty()) {
                                        if (pendingImages.size == 1) {
                                            "El usuario te acaba de enviar solo esta imagen, sin texto."
                                        } else {
                                            "El usuario te acaba de enviar ${pendingImages.size} imágenes, sin texto."
                                        }
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
                        _uiState.update { state ->
                            state.copy(
                                messages =
                                    (
                                        state.messages.filter { it.id != assistantMessageId } +
                                            UiChatMessage(
                                                id = assistantMessageId,
                                                content =
                                                    "Hubo un error al conectar con el asistente. " +
                                                        "Intenta de nuevo. (${e.message})",
                                                isUser = false,
                                            )
                                    ).toImmutableList(),
                                isLoading = false,
                                error = e.message,
                            )
                        }
                    }.collect { chunk ->
                        val text = chunk.text ?: return@collect

                        if (firstChunk) {
                            firstChunk = false
                            _uiState.update { state ->
                                state.copy(
                                    messages =
                                        (
                                            state.messages.filter { it.id != loadingId } +
                                                UiChatMessage(id = assistantMessageId, content = "", isUser = false)
                                        ).toImmutableList(),
                                )
                            }
                        }

                        // Efecto typewriter: acumula el texto crudo y revela el contenido visible
                        // en pasos pequeños a cadencia fija (no todo el chunk de golpe, ni por
                        // carácter-emisión). buildVisibleContent corre una vez por chunk, no por tick.
                        fullResponse += text
                        val targetVisible = buildVisibleContent(fullResponse)

                        // Un tag que se completa puede acortar el visible; no dejar que lo mostrado lo supere.
                        if (shownLength > targetVisible.length) {
                            shownLength = targetVisible.length
                            updateMessage(assistantMessageId) { it.copy(content = targetVisible) }
                        }

                        while (shownLength < targetVisible.length) {
                            shownLength = minOf(shownLength + TYPEWRITER_STEP, targetVisible.length)
                            val revealed = targetVisible.take(shownLength)
                            updateMessage(assistantMessageId) { it.copy(content = revealed) }
                            delay(TYPEWRITER_DELAY_MS)
                        }
                    }

                if (fullResponse.isNotEmpty()) {
                    conversationHistory.add("user" to safeText)
                    conversationHistory.add("model" to fullResponse)
                    if (conversationHistory.size > GEMINI_HISTORY_LIMIT) {
                        conversationHistory.removeAt(0)
                        conversationHistory.removeAt(0)
                    }
                    persistTurn(userMessage.id, safeText, sentAt, assistantMessageId, fullResponse)
                    extractAndSaveRecommendation(fullResponse)
                    extractPantrySuggestion(fullResponse, assistantMessageId)
                    extractClinicalSuggestion(fullResponse, assistantMessageId)
                    extractFoodLogSuggestion(fullResponse, assistantMessageId)
                    extractCheckInSuggestion(fullResponse, assistantMessageId)
                    extractGoalSet(fullResponse)
                    extractGoalChange(fullResponse)
                    summarizeConversationUseCase.compactIfNeeded()
                    buildContext()
                }
            } catch (e: Exception) {
                log.e(e) { "Unhandled Gemini Exception" }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun buildVisibleContent(fullResponse: String): String =
        ChatTagParser
            .stripForStreaming(fullResponse)
            .replace(FOODLOG_TAG_REGEX, "")
            .replace(CHECKIN_TAG_REGEX, "")
            .replace(ChatTagParser.GOAL_SET_TAG_REGEX, "")
            .replace(ChatTagParser.GOAL_CHANGE_TAG_REGEX, "")
            .trim()

    /**
     * Actualiza (por id) un único mensaje del estado, preservando la inmutabilidad de la lista.
     * Punto único que garantiza que [ChatUiState.messages] siga siendo una [ImmutableList].
     */
    private fun updateMessage(
        messageId: String,
        transform: (UiChatMessage) -> UiChatMessage,
    ) {
        _uiState.update { state ->
            state.copy(
                messages =
                    state.messages
                        .map { if (it.id == messageId) transform(it) else it }
                        .toImmutableList(),
            )
        }
    }

    /**
     * Persiste el turno (mensaje del usuario + respuesta cruda del modelo) en la BD local.
     * Se guarda el texto crudo del modelo (con tags) para que el historial vivo y el resumen
     * rodante reflejen exactamente lo que se le envió a Gemini. Silencioso ante fallos.
     */
    private suspend fun persistTurn(
        userMessageId: String,
        userText: String,
        userSentAt: String,
        assistantMessageId: String,
        assistantResponse: String,
    ) {
        try {
            if (userText.isNotBlank()) {
                chatMessageRepository.saveMessage(
                    ChatMessage(userMessageId, userId, ChatRole.USER, userText, userSentAt),
                )
            }
            chatMessageRepository.saveMessage(
                ChatMessage(assistantMessageId, userId, ChatRole.MODEL, assistantResponse, currentIsoDateTime()),
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to persist chat turn" }
        }
    }

    // ── Alacena Sugerencias ───────────────────────────────────────────────────
    private fun extractPantrySuggestion(
        response: String,
        messageId: String,
    ) {
        val ingredients = ChatTagParser.extractPantryIngredients(response) ?: return
        updateMessage(messageId) { msg ->
            msg.copy(pantrySuggestion = PantrySuggestion(ingredients = ingredients.toImmutableList()))
        }
    }

    fun confirmPantrySuggestion(messageId: String) {
        updateMessage(messageId) { msg ->
            msg.copy(pantrySuggestion = msg.pantrySuggestion?.copy(isLoading = true))
        }
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
                updateMessage(messageId) { msg ->
                    msg.copy(pantrySuggestion = suggestion.copy(confirmed = true))
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to save pantry suggestion" }
            }
        }
    }

    fun dismissPantrySuggestion(messageId: String) {
        updateMessage(messageId) { msg ->
            msg.copy(pantrySuggestion = null)
        }
    }

    // ── Perfil Clínico Sugerencias ────────────────────────────────────────────
    private fun extractClinicalSuggestion(
        response: String,
        messageId: String,
    ) {
        val rawJson = ChatTagParser.extractClinicalJson(response) ?: return
        val summary = buildClinicalSummaryFromJson(rawJson).ifEmpty { return }

        updateMessage(messageId) { msg ->
            msg.copy(clinicalSuggestion = ClinicalSuggestion(rawJson, summary))
        }
    }

    fun confirmClinicalSuggestion(messageId: String) {
        updateMessage(messageId) { msg ->
            msg.copy(clinicalSuggestion = msg.clinicalSuggestion?.copy(isLoading = true))
        }
        viewModelScope.launch {
            val msg = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val suggestion = msg.clinicalSuggestion ?: return@launch
            try {
                extractClinicalProfileUseCase.saveFromJson(suggestion.rawJson)
                updateMessage(messageId) { it.copy(clinicalSuggestion = suggestion.copy(confirmed = true)) }
                buildContext()
            } catch (e: Exception) {
                log.e(e) { "Failed to save clinical suggestion" }
            }
        }
    }

    fun dismissClinicalSuggestion(messageId: String) {
        updateMessage(messageId) { it.copy(clinicalSuggestion = null) }
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

        updateMessage(messageId) { msg ->
            msg.copy(foodLogSuggestion = FoodLogSuggestion(description))
        }
    }

    fun confirmFoodLogSuggestion(messageId: String) {
        updateMessage(messageId) { msg ->
            msg.copy(foodLogSuggestion = msg.foodLogSuggestion?.copy(isLoading = true))
        }
        viewModelScope.launch {
            val msg = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val suggestion = msg.foodLogSuggestion ?: return@launch
            try {
                estimateFoodNutritionUseCase.estimateAndSave(
                    foodDescription = suggestion.description,
                    source = FoodLogSource.CHAT_DETECTED,
                )
                updateMessage(messageId) { it.copy(foodLogSuggestion = suggestion.copy(confirmed = true)) }
                buildContext()
            } catch (e: Exception) {
                log.e(e) { "Failed to save food log" }
            }
        }
    }

    fun dismissFoodLogSuggestion(messageId: String) {
        updateMessage(messageId) { it.copy(foodLogSuggestion = null) }
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

            updateMessage(messageId) { msg ->
                msg.copy(checkInSuggestion = CheckInSuggestion(mealType, recommendedFood))
            }
        }
    }

    fun confirmCheckInYes(messageId: String) {
        updateMessage(messageId) { msg ->
            msg.copy(checkInSuggestion = msg.checkInSuggestion?.copy(isLoading = true))
        }
        viewModelScope.launch {
            val msg = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val checkIn = msg.checkInSuggestion ?: return@launch

            val mealType = runCatching { MealType.valueOf(checkIn.mealType) }.getOrDefault(MealType.SNACK)

            estimateFoodNutritionUseCase.estimateAndSave(
                foodDescription = checkIn.recommendedFood,
                source = FoodLogSource.PROACTIVE_CHECKIN,
                mealTypeHint = mealType,
            )

            updateMessage(messageId) {
                it.copy(checkInSuggestion = checkIn.copy(userResponse = CheckInResponse.YES))
            }
            buildContext()
            sendMessage("Registrado, gracias")
        }
    }

    fun confirmCheckInNo(messageId: String) {
        updateMessage(messageId) {
            it.copy(checkInSuggestion = it.checkInSuggestion?.copy(userResponse = CheckInResponse.NO))
        }
        viewModelScope.launch {
            val followUp =
                UiChatMessage(
                    id = generateId(),
                    content = "Está bien, ¿qué comiste en su lugar?",
                    isUser = false,
                )
            _uiState.update { state ->
                state.copy(messages = (state.messages + followUp).toImmutableList())
            }
        }
    }

    // ── Meta del usuario (GOAL_SET / GOAL_CHANGE) ────────────────────────────
    private suspend fun extractGoalSet(response: String) {
        val json = ChatTagParser.extractGoalSetJson(response) ?: return
        val goal = parseGoalFromJson(json) ?: return
        val user = cachedContextParams?.user ?: return
        try {
            userRepository.saveUser(user.copy(goal = goal))
            log.i { "Goal set: $goal" }
        } catch (e: Exception) {
            log.e(e) { "Failed to save user goal" }
        }
    }

    private suspend fun extractGoalChange(response: String) {
        val json = ChatTagParser.extractGoalChangeJson(response) ?: return
        val goal = parseGoalFromJson(json) ?: return
        val user = cachedContextParams?.user ?: return
        try {
            userRepository.updateUser(user.copy(goal = goal))
            log.i { "Goal changed: $goal" }
        } catch (e: Exception) {
            log.e(e) { "Failed to update user goal" }
        }
    }

    private fun parseGoalFromJson(json: String): NutritionGoal? =
        runCatching {
            val goalValue =
                jsonParser
                    .parseToJsonElement(json)
                    .jsonObject["goal"]
                    ?.toString()
                    ?.trim('"')
                    ?: return null
            NutritionGoal.valueOf(goalValue)
        }.getOrNull()

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
        _uiState.update { it.copy(error = null) }
    }
}
