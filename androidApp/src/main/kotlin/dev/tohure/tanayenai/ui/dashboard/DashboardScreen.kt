package dev.tohure.tanayenai.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.tohure.tanayenai.ui.theme.AccentTerra
import dev.tohure.tanayenai.ui.theme.SecondaryMint
import java.util.Calendar

@Composable
fun DashboardScreen(onNavigateToChat: () -> Unit = {}) {
    // TODO Fase 3: conectar con ViewModel y datos reales de Supabase/SQLDelight

    val scrollState = rememberScrollState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Saludo contextual
        GreetingHeader(
            userName = "Carlo",
            greeting = greetingByHour(),
            subtitle = "Tu VFC está baja hoy — tómatelo con calma 🌿",
        )

        // Alerta activa (solo visible si hay alertas)
        AlertBanner(
            message = "Dormiste 5.1h. Evita cafeína después de las 14:00.",
            emoji = "😴",
        )

        Spacer(Modifier.height(4.dp))

        // Fila de métricas: sueño + VFC
        MetricsRow(
            metrics =
                listOf(
                    Triple("Sueño", "5.1", "h"),
                    Triple("VFC", "42", "ms"),
                ),
            emojis = listOf("🌙", "💚"),
            tints = listOf(AccentTerra, SecondaryMint),
        )

        // Fila de métricas: peso + FC reposo
        MetricsRow(
            metrics =
                listOf(
                    Triple("Peso", "78.2", "kg"),
                    Triple("FC reposo", "68", "bpm"),
                ),
            emojis = listOf("⚖️", "❤️"),
            tints = listOf(SecondaryMint, Color(0xFFE63946)),
        )

        Spacer(Modifier.height(4.dp))

        // Lo que comiste hoy
        TodayFoodCard(
            foodLogs =
                listOf(
                    "Desayuno" to "Avena con almendras",
                    "Almuerzo" to "Ensalada con atún",
                ),
            onAddManuallyClick = { /* TODO: Fase futura */ },
        )

        Spacer(Modifier.height(4.dp))

        // CTA principal
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
