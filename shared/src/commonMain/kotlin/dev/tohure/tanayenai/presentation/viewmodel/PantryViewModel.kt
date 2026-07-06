package dev.tohure.tanayenai.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.tohure.tanayenai.domain.model.DEFAULT_LOCATION_ID
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.PantryUnit
import dev.tohure.tanayenai.domain.model.classifyIngredient
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.model.generateId
import dev.tohure.tanayenai.domain.repository.PantryRepository
import dev.tohure.tanayenai.presentation.model.CategorizedItem
import dev.tohure.tanayenai.presentation.model.CategoryGroup
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class PantryUiState(
    /**
     * Items grouped and sorted by category.
     *
     * Exposed as [List]<[CategoryGroup]> instead of Map to bridge cleanly through the
     * Kotlin/Native ObjC layer to Swift without runtime casts.
     */
    val categoryGroups: ImmutableList<CategoryGroup> = persistentListOf(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val error: String? = null,
    /** Non-null while the edit bottom sheet is open. */
    val editingItem: PantryItem? = null,
    /** True while a save/delete operation is in-flight. */
    val isSaving: Boolean = false,
)

class PantryViewModel(
    private val pantryRepository: PantryRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PantryUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    /** In-memory cache — avoids re-querying the DB on every search keystroke. */
    private var allItems: List<PantryItem> = emptyList()

    init {
        loadItems()
    }

    // ── Load & group ──────────────────────────────────────────────────────────

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching { pantryRepository.getPantryItems(userId) }
                .onSuccess { items ->
                    allItems = items
                    _uiState.value =
                        _uiState.value.copy(
                            categoryGroups = buildGroups(allItems, _uiState.value.searchQuery),
                            isLoading = false,
                        )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun search(query: String) {
        _uiState.value =
            _uiState.value.copy(
                searchQuery = query,
                categoryGroups = buildGroups(allItems, query),
            )
    }

    // ── Edit existing item ────────────────────────────────────────────────────

    fun openEdit(item: PantryItem) {
        _uiState.value = _uiState.value.copy(editingItem = item)
    }

    fun closeEdit() {
        _uiState.value = _uiState.value.copy(editingItem = null)
    }

    fun saveEdit(
        itemId: String,
        newQuantity: Float,
        newUnit: PantryUnit,
        newExpiryDate: String?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            runCatching {
                val existing =
                    allItems.find { it.id == itemId }
                        ?: error("Item $itemId not found in cache")
                existing
                    .copy(
                        quantity = newQuantity,
                        unit = newUnit,
                        expiryDate = newExpiryDate,
                        updatedAt = currentIsoDateTime(),
                    ).also { pantryRepository.upsertItem(it) }
            }.onSuccess { updated ->
                // Update in-memory cache immediately to avoid UI flash
                allItems = allItems.map { if (it.id == itemId) updated else it }
                _uiState.value =
                    _uiState.value.copy(
                        categoryGroups = buildGroups(allItems, _uiState.value.searchQuery),
                        isSaving = false,
                        editingItem = null,
                    )
                // Background DB refresh to confirm DB consistency
                loadItems()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    // ── Add manually ──────────────────────────────────────────────────────────

    fun addItem(
        ingredient: String,
        quantity: Float,
        unit: PantryUnit,
        expiryDate: String?,
        locationId: String = DEFAULT_LOCATION_ID,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            runCatching {
                PantryItem(
                    id = generateId(),
                    userId = userId,
                    locationId = locationId,
                    ingredient = ingredient.trim(),
                    quantity = quantity,
                    unit = unit,
                    expiryDate = expiryDate,
                    updatedAt = currentIsoDateTime(),
                ).also { pantryRepository.upsertItem(it) }
            }.onSuccess { newItem ->
                // Update in-memory cache immediately
                allItems = allItems + newItem
                _uiState.value =
                    _uiState.value.copy(
                        categoryGroups = buildGroups(allItems, _uiState.value.searchQuery),
                        isSaving = false,
                    )
                loadItems()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            runCatching { pantryRepository.deleteItem(itemId) }
                .onSuccess {
                    allItems = allItems.filter { it.id != itemId }
                    _uiState.value =
                        _uiState.value.copy(
                            categoryGroups = buildGroups(allItems, _uiState.value.searchQuery),
                        )
                    loadItems()
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildGroups(
        items: List<PantryItem>,
        query: String,
    ): ImmutableList<CategoryGroup> {
        val filtered =
            if (query.isBlank()) {
                items
            } else {
                items.filter { it.ingredient.contains(query, ignoreCase = true) }
            }

        return filtered
            .map { CategorizedItem(it, classifyIngredient(it.ingredient)) }
            .groupBy { it.category }
            .entries
            .sortedBy { it.key.displayName }
            .map { (category, categorized) -> CategoryGroup(category, categorized.toImmutableList()) }
            .toImmutableList()
    }
}
