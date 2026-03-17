package dev.tohure.tanayenai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.PantryRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import dev.tohure.tanayenai.domain.usecase.SyncHealthMetricsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "",
    val latestMetrics: HealthMetrics? = null,
    val todayFoodLogs: List<FoodLog> = emptyList(),
    val activeAlerts: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val geminiContext: String = "",
)

class DashboardViewModel(
    private val healthMetricsRepository: HealthMetricsRepository,
    private val pantryRepository: PantryRepository,
    private val recommendationRepository: RecommendationRepository,
    private val buildContextUseCase: BuildContextUseCase,
    private val syncHealthMetricsUseCase: SyncHealthMetricsUseCase,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    // En DashboardViewModel.kt, dentro de loadDashboard()
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Sincronizar datos de salud de hoy antes de mostrar
                syncHealthMetricsUseCase.syncToday()

                val latestMetrics = healthMetricsRepository.getLatestMetrics(userId)
                val alerts = buildAlerts(latestMetrics)

                _uiState.value =
                    _uiState.value.copy(
                        latestMetrics = latestMetrics,
                        activeAlerts = alerts,
                        isLoading = false,
                    )
                buildGeminiContext()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun buildGeminiContext() {
        try {
            val recentMetrics =
                healthMetricsRepository.getMetricsForDateRange(
                    userId,
                    currentIsoDate(),
                    currentIsoDate(),
                )
            val recentRecommendations =
                recommendationRepository
                    .getRecentRecommendations(userId, days = 7)

            val context =
                "MÉTRICAS: ${recentMetrics.size} registros recientes. " +
                    "RECOMENDACIONES: ${recentRecommendations.size} en los últimos 7 días."

            _uiState.value = _uiState.value.copy(geminiContext = context)
        } catch (e: Exception) {
            // Fallo silencioso — el contexto se construirá cuando el usuario abra el chat
        }
    }

    private fun buildAlerts(metrics: HealthMetrics?): List<String> {
        if (metrics == null) return emptyList()
        val alerts = mutableListOf<String>()
        metrics.sleepHours?.let {
            if (it < 6f) alerts.add("Dormiste ${it}h — evita cafeína después de las 14:00")
        }
        metrics.hrv?.let {
            if (it < 45f) alerts.add("VFC baja (${it}ms) — sistema nervioso bajo estrés")
        }
        return alerts
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
