package dev.tohure.tanayenai.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.tohure.tanayenai.data.prefs.NotificationPrefs
import dev.tohure.tanayenai.domain.model.DailyNutritionSummary
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.model.resolveDisplayName
import dev.tohure.tanayenai.domain.repository.FoodLogRepository
import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import dev.tohure.tanayenai.domain.usecase.FetchContextParamsUseCase
import dev.tohure.tanayenai.domain.usecase.GetLatestMetricsUseCase
import dev.tohure.tanayenai.domain.usecase.SyncHealthMetricsUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class DashboardUiState(
    val userName: String = "",
    val rawDisplayName: String = "",
    val showNameDialog: Boolean = false,
    val latestMetrics: HealthMetrics? = null,
    val todayFoodLogs: ImmutableList<FoodLog> = persistentListOf(),
    val todayNutrition: DailyNutritionSummary? = null,
    val activeAlerts: ImmutableList<String> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val geminiContext: String = "",
    val showPermissionAlert: Boolean = false,
)

private val log = Logger.withTag("DashboardViewModel")

class DashboardViewModel(
    private val getLatestMetricsUseCase: GetLatestMetricsUseCase,
    private val fetchContextParamsUseCase: FetchContextParamsUseCase,
    private val buildContextUseCase: BuildContextUseCase,
    private val syncHealthMetricsUseCase: SyncHealthMetricsUseCase,
    private val foodLogRepository: FoodLogRepository,
    private val notificationPrefs: NotificationPrefs,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            if (_uiState.value.latestMetrics == null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }

            val storedName = notificationPrefs.loadDisplayName()
            val displayName =
                if (storedName.isNullOrBlank()) "Hola" else resolveDisplayName(storedName)
            _uiState.value =
                _uiState.value.copy(
                    userName = displayName,
                    rawDisplayName = storedName.orEmpty(),
                    showNameDialog = storedName.isNullOrBlank(),
                )

            try {
                val isGranted = syncHealthMetricsUseCase.hasPermissions()
                if (isGranted) {
                    try {
                        syncHealthMetricsUseCase.syncToday()
                    } catch (e: Exception) {
                        log.e(e) { "Health sync failed, continuing with local data" }
                    }
                }

                val latestMetrics = getLatestMetricsUseCase.execute(userId)
                val alerts = buildAlerts(latestMetrics)
                val today = currentIsoDate()
                val todayNutrition = foodLogRepository.getDailySummary(userId, today)
                val todayFoodLogs =
                    foodLogRepository.getLatestTodayFoodLogs(userId, today, limit = 4).toImmutableList()

                _uiState.value =
                    _uiState.value.copy(
                        latestMetrics = latestMetrics,
                        activeAlerts = alerts,
                        todayNutrition = todayNutrition,
                        todayFoodLogs = todayFoodLogs,
                        isLoading = false,
                        showPermissionAlert = !isGranted,
                    )
                buildGeminiContext()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Recarga solo la nutrición del día (comidas + resumen), sin re-sincronizar salud.
    // Se llama al volver al Inicio (p. ej. tras borrar un registro en el Diario) para que
    // el card refleje las calorías recalculadas.
    fun refreshNutrition() {
        viewModelScope.launch {
            val today = currentIsoDate()
            val todayNutrition = foodLogRepository.getDailySummary(userId, today)
            val todayFoodLogs =
                foodLogRepository.getLatestTodayFoodLogs(userId, today, limit = 4).toImmutableList()
            _uiState.value =
                _uiState.value.copy(
                    todayNutrition = todayNutrition,
                    todayFoodLogs = todayFoodLogs,
                )
        }
    }

    fun saveDisplayName(rawName: String) {
        val trimmed = rawName.trim()
        notificationPrefs.saveDisplayName(trimmed)
        _uiState.value =
            _uiState.value.copy(
                userName = if (trimmed.isBlank()) "Hola" else resolveDisplayName(trimmed),
                rawDisplayName = trimmed,
                showNameDialog = false,
            )
    }

    fun dismissNameDialog() {
        // No guarda nada: si es primera vez, el diálogo reaparece en el próximo lanzamiento.
        // Si es una edición cancelada, el nombre anterior se preserva en prefs.
        _uiState.value = _uiState.value.copy(showNameDialog = false)
    }

    fun requestEditName() {
        _uiState.value = _uiState.value.copy(showNameDialog = true)
    }

    private suspend fun buildGeminiContext() {
        try {
            val params =
                fetchContextParamsUseCase.fetch(userId = userId, today = currentIsoDate())
            val context = buildContextUseCase.build(params)
            _uiState.value = _uiState.value.copy(geminiContext = context)
        } catch (e: Exception) {
            // Fallo silencioso — el contexto se construirá cuando el usuario abra el chat
        }
    }

    private fun buildAlerts(metrics: HealthMetrics?): ImmutableList<String> {
        if (metrics == null) return persistentListOf()
        val alerts = mutableListOf<String>()
        metrics.sleepHours?.let {
            if (it < 6f) alerts.add("Dormiste ${it}h — evita cafeína después de las 14:00")
        }
        metrics.hrv?.let {
            if (it < 45f) alerts.add("VFC baja (${it}ms) — sistema nervioso bajo estrés")
        }
        return alerts.toImmutableList()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
