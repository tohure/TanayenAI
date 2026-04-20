package dev.tohure.tanayenai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.tohure.tanayenai.data.prefs.NotificationPrefs
import dev.tohure.tanayenai.domain.model.NotificationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

data class NotificationSettingsUiState(
    val morningEnabled: Boolean = false,
    val morningHour: Int = 7,
    val morningMinute: Int = 0,
    val saved: Boolean = false,
)

@OptIn(ExperimentalObjCName::class)
@ObjCName(name = "NotificationSettingsViewModel", exact = true)
class NotificationSettingsViewModel(
    private val prefs: NotificationPrefs,
    private val userId: String,
    private val onScheduleChanged: (hour: Int, minute: Int, enabled: Boolean) -> Unit,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationSettingsUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        val saved = prefs.load(userId)
        _uiState.value =
            NotificationSettingsUiState(
                morningEnabled = saved.morningEnabled,
                morningHour = saved.morningHour,
                morningMinute = saved.morningMinute,
            )
    }

    fun setHour(hour: Int) {
        _uiState.value = _uiState.value.copy(morningHour = hour.coerceIn(0, 23))
    }

    fun setMinute(minute: Int) {
        _uiState.value = _uiState.value.copy(morningMinute = minute.coerceIn(0, 59))
    }

    fun toggleEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(morningEnabled = enabled)
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            prefs.save(
                NotificationSettings(
                    userId = userId,
                    morningEnabled = state.morningEnabled,
                    morningHour = state.morningHour,
                    morningMinute = state.morningMinute,
                ),
            )
            onScheduleChanged(state.morningHour, state.morningMinute, state.morningEnabled)
            _uiState.value = state.copy(saved = true)
        }
    }

    fun clearSaved() {
        _uiState.value = _uiState.value.copy(saved = false)
    }
}
