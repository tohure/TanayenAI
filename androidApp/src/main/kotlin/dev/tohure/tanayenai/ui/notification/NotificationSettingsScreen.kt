package dev.tohure.tanayenai.ui.notification

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.notification.NotificationScheduler
import dev.tohure.tanayenai.presentation.viewmodel.NotificationSettingsViewModel
import dev.tohure.tanayenai.ui.theme.BackgroundColor
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TextDark
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val TimeChipBackground = Color(0xFFE8F5EE)

/**
 * Contenido de la pantalla de notificaciones.
 * Diseñado para mostrarse dentro de un [ModalBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsContent(onDismiss: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: NotificationSettingsViewModel =
        koinViewModel {
            parametersOf(
                PROTOTYPE_USER_ID,
                { hour: Int, minute: Int, enabled: Boolean ->
                    NotificationScheduler.schedule(context, hour, minute, enabled)
                },
            )
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { /* resultado manejado por el switch */ }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.clearSaved()
            onDismiss()
        }
    }

    // ── Material3 TimePicker dialog ──────────────────────────────────────────
    if (showTimePicker) {
        // Se recrea con los valores actuales cada vez que se abre
        val timePickerState =
            rememberTimePickerState(
                initialHour = uiState.morningHour,
                initialMinute = uiState.morningMinute,
                is24Hour = false,
            )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = SurfaceColor,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Selecciona la hora",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedColor,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                    )
                    TimePicker(
                        state = timePickerState,
                        colors =
                            TimePickerDefaults.colors(
                                clockDialColor = BackgroundColor,
                                selectorColor = PrimaryGreen,
                                clockDialSelectedContentColor = Color.White,
                                clockDialUnselectedContentColor = TextDark,
                                periodSelectorSelectedContainerColor = PrimaryGreen,
                                periodSelectorUnselectedContainerColor = Color.Transparent,
                                periodSelectorSelectedContentColor = Color.White,
                                periodSelectorUnselectedContentColor = TextDark,
                                periodSelectorBorderColor = TextMutedColor,
                                timeSelectorSelectedContainerColor = Color(0xFFE8F5EE),
                                timeSelectorUnselectedContainerColor = BackgroundColor,
                                timeSelectorSelectedContentColor = PrimaryGreen,
                                timeSelectorUnselectedContentColor = TextDark,
                            ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancelar", color = TextMutedColor)
                        }
                        TextButton(
                            onClick = {
                                viewModel.setHour(timePickerState.hour)
                                viewModel.setMinute(timePickerState.minute)
                                showTimePicker = false
                            },
                        ) {
                            Text("OK", color = PrimaryGreen)
                        }
                    }
                }
            }
        }
    }

    // ── Contenido del bottom sheet ───────────────────────────────────────────
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Notificaciones",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextDark,
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // ── Toggle ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Consejo matutino",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextDark,
                        )
                        Text(
                            "Recibe un consejo nutricional cada mañana",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedColor,
                        )
                    }
                    Switch(
                        checked = uiState.morningEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.toggleEnabled(enabled)
                        },
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryGreen,
                            ),
                    )
                }

                // ── Hora (visible sólo cuando está habilitado) ────────────
                if (uiState.morningEnabled) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color(0xFFEEEEEE),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Hora del consejo",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextDark,
                        )

                        // Chip tappable — abre el TimePicker de Material3
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TimeChipBackground)
                                    .clickable { showTimePicker = true }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = formatTime12h(uiState.morningHour, uiState.morningMinute),
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = PrimaryGreen,
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.save() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
        ) {
            Text("Guardar", color = Color.White)
        }

        Spacer(Modifier.height(0.dp))
    }
}

/** Convierte hora 0-23 + minuto a "7:00 AM" / "1:30 PM". */
private fun formatTime12h(
    hour: Int,
    minute: Int,
): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour =
        when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
    return "$displayHour:%02d $period".format(minute)
}
