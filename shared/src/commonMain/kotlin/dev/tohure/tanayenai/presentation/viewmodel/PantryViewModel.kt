package dev.tohure.tanayenai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryLocation
import dev.tohure.tanayenai.domain.model.PantryUnit
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.model.generateId
import dev.tohure.tanayenai.domain.repository.PantryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class PantryUiState(
    val locations: List<PantryLocation> = emptyList(),
    val items: List<PantryItem> = emptyList(),
    val selectedLocationId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class PantryViewModel(
    private val pantryRepository: PantryRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    init {
        loadLocations()
    }

    fun loadLocations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val locations = pantryRepository.getLocations(userId)
                _uiState.value =
                    _uiState.value.copy(
                        locations = locations,
                        isLoading = false,
                    )
                val defaultLocation = locations.firstOrNull { it.isDefault } ?: locations.firstOrNull()
                defaultLocation?.let { selectLocation(it.id) }
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = "Error cargando ubicaciones: ${e.message}",
                    )
            }
        }
    }

    fun selectLocation(locationId: String) {
        _uiState.value = _uiState.value.copy(selectedLocationId = locationId)
        observeItemsForLocation(locationId)
    }

    private fun observeItemsForLocation(locationId: String) {
        pantryRepository
            .observeItems(userId, locationId)
            .onEach { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }.catch { e ->
                _uiState.value =
                    _uiState.value.copy(
                        error = "Error observando items: ${e.message}",
                    )
            }.launchIn(viewModelScope)
    }

    fun addItem(
        ingredient: String,
        quantity: Float,
        unit: PantryUnit,
    ) {
        val locationId = _uiState.value.selectedLocationId ?: return
        viewModelScope.launch {
            val item =
                PantryItem(
                    id = generateId(),
                    userId = userId,
                    locationId = locationId,
                    ingredient = ingredient,
                    quantity = quantity,
                    unit = unit,
                    updatedAt = currentIsoDateTime(),
                )
            try {
                pantryRepository.addItem(item)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                pantryRepository.deleteItem(itemId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
