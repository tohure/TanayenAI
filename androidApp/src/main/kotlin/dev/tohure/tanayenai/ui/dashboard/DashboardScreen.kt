package dev.tohure.tanayenai.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.tohure.tanayenai.presentation.viewmodel.DashboardViewModel
import dev.tohure.tanayenai.ui.theme.AccentTerra
import dev.tohure.tanayenai.ui.theme.SecondaryMint
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Calendar

@Composable
fun DashboardScreen(onNavigateToChat: () -> Unit = {}) {
    val viewModel: DashboardViewModel =
        koinViewModel {
            parametersOf("00000000-0000-0000-0000-000000000001")
        }
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = PermissionController.createRequestPermissionResultContract(),
        ) {
            // Ya sea que aceptó algo o nada, recargamos para reevaluar e intentar sincronizar
            viewModel.loadDashboard()
        }

    LifecycleResumeEffect(Unit) {
        viewModel.loadDashboard()
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        GreetingHeader(
            userName = "Carlo",
            greeting = greetingByHour(),
            subtitle =
                if (uiState.activeAlerts.isEmpty()) {
                    "Todo bien por ahora 🌿"
                } else {
                    uiState.activeAlerts.first()
                },
        )

        // Mostrar alerta si existe
        uiState.activeAlerts.firstOrNull()?.let { alert ->
            AlertBanner(message = alert)
        }

        if (uiState.showPermissionAlert) {
            HealthPermissionBanner(onGrantClick = {
                permissionLauncher.launch(
                    setOf(
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                            androidx.health.connect.client.records.SleepSessionRecord::class,
                        ),
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                            androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord::class,
                        ),
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                            androidx.health.connect.client.records.TotalCaloriesBurnedRecord::class,
                        ),
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                            androidx.health.connect.client.records.WeightRecord::class,
                        ),
                        androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                            androidx.health.connect.client.records.StepsRecord::class,
                        ),
                    ),
                )
            })
        }

        // Métricas reales o skeleton si está cargando
        if (uiState.isLoading) {
            // Skeleton simple mientras cargan los datos
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(2) {
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFEEEEEE)),
                        )
                    }
                }
            }
        } else {
            val metrics = uiState.latestMetrics
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    label = "Sueño",
                    value = metrics?.sleepHours?.let { "%.1f".format(it) } ?: "--",
                    unit = "h",
                    emoji = "🌙",
                    tint = AccentTerra,
                    modifier = Modifier.weight(1f),
                )

                StressLevelCard(
                    hrvValue = metrics?.hrv?.toInt()?.toString() ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }
            MetricsRow(
                metrics =
                    listOf(
                        Triple("Peso", metrics?.weightKg?.let { "%.1f".format(it) } ?: "--", "kg"),
                        Triple("Calorías activas", metrics?.caloriesBurned?.toString() ?: "--", "kcal"),
                    ),
                emojis = listOf("⚖️", "🔥"),
                tints = listOf(SecondaryMint, Color(0xFFE63946)),
            )
        }

        TodayFoodCard(
            foodLogs =
                uiState.todayFoodLogs.map {
                    it.mealType.name
                        .lowercase()
                        .replaceFirstChar { c -> c.uppercase() } to it.foodName
                },
            onAddManuallyClick = { /* TODO: Fase futura */ },
        )

        AskAssistantButton(onClick = onNavigateToChat)
        Spacer(Modifier.height(16.dp))
    }
}

/**Saludo contextual (hora del día)*/
private fun greetingByHour(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Buenos días ☀️"
        in 12..17 -> "Buenas tardes 🌤️"
        else -> "Buenas noches 🌙"
    }
}
