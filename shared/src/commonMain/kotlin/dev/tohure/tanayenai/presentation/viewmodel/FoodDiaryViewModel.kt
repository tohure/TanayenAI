package dev.tohure.tanayenai.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.currentIsoDate
import dev.tohure.tanayenai.domain.model.daysAgo
import dev.tohure.tanayenai.domain.repository.FoodLogRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val log = Logger.withTag("FoodDiaryViewModel")

@Immutable
data class FoodDiaryDay(
    val label: String,
    val date: String,
    val entries: ImmutableList<FoodLog>,
    val totalCalories: Float,
    val mealCount: Int,
)

@Immutable
data class FoodDiaryUiState(
    val days: ImmutableList<FoodDiaryDay> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && days.isEmpty()
}

class FoodDiaryViewModel(
    private val foodLogRepository: FoodLogRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FoodDiaryUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<FoodDiaryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                // Reutiliza getTodayFoodLogs (match por prefijo de fecha) para hoy y ayer.
                val dayDefs = listOf("Hoy" to currentIsoDate(), "Ayer" to daysAgo(1))
                val days =
                    dayDefs
                        .mapNotNull { (label, date) ->
                            val entries = foodLogRepository.getTodayFoodLogs(userId, date)
                            if (entries.isEmpty()) {
                                null
                            } else {
                                FoodDiaryDay(
                                    label = label,
                                    date = date,
                                    entries = entries.toImmutableList(),
                                    totalCalories = entries.sumOf { it.calories.toDouble() }.toFloat(),
                                    mealCount = entries.size,
                                )
                            }
                        }.toImmutableList()

                _uiState.value = FoodDiaryUiState(days = days, isLoading = false)
            } catch (e: Exception) {
                log.e(e) { "Error cargando el diario" }
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Borra un registro y recarga: los subtotales del día se recalculan solos.
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            try {
                foodLogRepository.deleteFoodLog(id)
                load()
            } catch (e: Exception) {
                log.e(e) { "Error borrando registro $id" }
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
