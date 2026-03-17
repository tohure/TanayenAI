package dev.tohure.tanayenai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.HealthPermissionResult
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.usecase.SyncHealthMetricsUseCase
import dev.tohure.tanayenai.domain.usecase.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private val log = Logger.withTag("HealthViewModel")

data class HealthUiState(
    val latestMetrics: HealthMetrics? = null,
    val recentMetrics: List<HealthMetrics> = emptyList(),
    val permissionStatus: HealthPermissionResult? = null,
    val isSyncing: Boolean = false,
    val lastSyncMessage: String? = null,
    val error: String? = null,
)

class HealthViewModel(
    private val syncHealthMetricsUseCase: SyncHealthMetricsUseCase,
    private val healthMetricsRepository: HealthMetricsRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HealthUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        loadCachedMetrics()
        checkPermissionsAndSync()
    }

    private fun loadCachedMetrics() {
        viewModelScope.launch {
            try {
                val latest = healthMetricsRepository.getLatestMetrics(userId)
                val recent = healthMetricsRepository.getMetricsForDateRange(userId, daysAgo(7), today())
                _uiState.value =
                    _uiState.value.copy(
                        latestMetrics = latest,
                        recentMetrics = recent,
                    )
            } catch (e: Exception) {
                log.e(e) { "Failed to load cached metrics" }
            }
        }
    }

    fun checkPermissionsAndSync() {
        viewModelScope.launch {
            val permissionResult = syncHealthMetricsUseCase.checkAndRequestPermissions()
            _uiState.value = _uiState.value.copy(permissionStatus = permissionResult)

            if (permissionResult == HealthPermissionResult.Granted) {
                sync()
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, lastSyncMessage = null)

            val result = syncHealthMetricsUseCase.syncLastNDays(days = 7)

            _uiState.value =
                _uiState.value.copy(
                    isSyncing = false,
                    lastSyncMessage =
                        when (result) {
                            is SyncResult.Success -> "Sincronizados ${result.recordsSynced} días"
                            is SyncResult.NoData -> "No hay datos nuevos"
                            is SyncResult.Error -> "Error: ${result.message}"
                        },
                )

            // Recargar métricas del caché local después del sync
            if (result is SyncResult.Success) loadCachedMetrics()
        }
    }

    private fun today(): String =
        Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()

    private fun daysAgo(days: Int): String = today()
}
