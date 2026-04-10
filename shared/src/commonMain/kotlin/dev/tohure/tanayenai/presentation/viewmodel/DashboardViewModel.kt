package dev.tohure.tanayenai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import dev.tohure.tanayenai.domain.usecase.FetchContextParamsUseCase
import dev.tohure.tanayenai.domain.usecase.GetLatestMetricsUseCase
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
    val showPermissionAlert: Boolean = false,
)

class DashboardViewModel(
    private val getLatestMetricsUseCase: GetLatestMetricsUseCase,
    private val fetchContextParamsUseCase: FetchContextParamsUseCase,
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

    fun loadDashboard() {
        viewModelScope.launch {
            if (_uiState.value.latestMetrics == null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            try {
                val isGranted = syncHealthMetricsUseCase.hasPermissions()
                if (isGranted) syncHealthMetricsUseCase.syncToday()

                val latestMetrics = getLatestMetricsUseCase.execute(userId)
                val alerts = buildAlerts(latestMetrics)

                _uiState.value =
                    _uiState.value.copy(
                        latestMetrics = latestMetrics,
                        activeAlerts = alerts,
                        isLoading = false,
                        showPermissionAlert = !isGranted,
                    )
                buildGeminiContext()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
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
