package dev.tohure.tanayenai.ui.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.tanayenai.R
import dev.tohure.tanayenai.domain.model.DailyNutritionSummary
import dev.tohure.tanayenai.ui.theme.AccentTerra
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SecondaryMint
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TanayenTheme
import dev.tohure.tanayenai.ui.theme.TextDark
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class MetricItem(
    val label: String,
    val value: String,
    val unit: String,
    val emoji: String,
    val tint: Color = SecondaryMint,
)

@Immutable
data class FoodLogItem(
    val mealType: String,
    val foodName: String,
)

@Composable
fun GreetingHeader(
    userName: String,
    greeting: String, // "Buenos días", "Buenas tardes", etc.
    subtitle: String, // "Hoy te ves bien 🌿" o alerta si VFC baja
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedColor,
            )
        }
        if (onSettingsClick != null) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = "Notificaciones",
                    tint = TextMutedColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Métrica individual*/
@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    emoji: String,
    tint: Color = SecondaryMint,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(color = tint),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** Tarjeta exclusiva para el Estrés (VFC) con un Gauge de semáforo */
@Composable
fun StressLevelCard(
    modifier: Modifier = Modifier,
    hrvValue: String, // "40" o "--"
) {
    val hrv = hrvValue.toFloatOrNull() ?: 0f

    // Si la VFC es baja (<40), el estrés es Alto (Rojo).
    // Si la VFC está entre 40 y 60, estrés es Medio (Amarillo).
    // Si la VFC es alta (>60), el estrés es Bajo (Verde).
    val (stressText, stressColor) =
        when {
            hrvValue == "--" -> "--" to TextMutedColor
            hrv < 40f -> "Alto" to Color(0xFFE63946)
            hrv < 60f -> "Medio" to Color(0xFFFFB703)
            else -> "Bajo" to SecondaryMint
        }

    // Normalizamos la VFC en un rango de 0 a 100 para pintar el indicador en la barra
    // Limitado a 100 para que el punto no se salga de la tarjeta si la VFC > 100.
    val fraction = (hrv / 100f).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Nivel de estrés",
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stressText,
                style = MaterialTheme.typography.headlineMedium.copy(color = stressColor),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            // Barra de Semáforo (Gauge horizontal)
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp),
            ) {
                val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                // Fondo gradiente rojo -> amarillo -> verde (Rojo = bajo VFC = Alto estrés)
                drawRoundRect(
                    brush =
                        horizontalGradient(
                            colors = listOf(Color(0xFFE63946), Color(0xFFFFB703), SecondaryMint),
                        ),
                    size = size,
                    cornerRadius = cornerRadius,
                )

                // Si hay datos, dibujar el marcador circular
                if (hrvValue != "--") {
                    val markerX = size.width * fraction
                    val safeX = markerX.coerceIn(6.dp.toPx(), size.width - 6.dp.toPx())

                    // Círculo blanco interno
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = Offset(safeX, size.height / 2),
                    )
                    // Borde oscuro del círculo para que resalte
                    drawCircle(
                        color = Color.DarkGray,
                        radius = 6.dp.toPx(),
                        center = Offset(safeX, size.height / 2),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "VFC: $hrvValue ms",
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedColor,
            )
        }
    }
}

/**Fila de 2 métricas*/
@Composable
fun MetricsRow(
    modifier: Modifier = Modifier,
    metrics: ImmutableList<MetricItem>,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        metrics.forEach { item ->
            MetricCard(
                label = item.label,
                value = item.value,
                unit = item.unit,
                emoji = item.emoji,
                tint = item.tint,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**Alerta contextual (VFC baja, sueño insuficiente, etc.)*/
@Composable
fun AlertBanner(
    modifier: Modifier = Modifier,
    message: String,
    emoji: String = "⚠️",
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF3E0))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = AccentTerra),
        )
    }
}

/**Lo que comiste hoy*/
@Composable
fun TodayFoodCard(
    foodLogs: ImmutableList<FoodLogItem>,
    onAddManuallyClick: () -> Unit, // Botón dummy por ahora
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Hoy comiste",
                    style = MaterialTheme.typography.titleMedium,
                )
                // TODO
                TextButton(onClick = onAddManuallyClick) {
                    Text(
                        text = "+ Agregar",
                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryGreen),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (foodLogs.isEmpty()) {
                Text(
                    text = "Cuéntale al asistente qué comiste 🍽️",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                foodLogs.forEach { item ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.mealType,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = item.foodName,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextDark),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AskAssistantButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
    ) {
        Text(
            text = "¿Qué como hoy? 🌿",
            style = MaterialTheme.typography.titleMedium.copy(color = SurfaceColor),
        )
    }
}

/** Banner específico para solicitar permisos de Health Connect */
@Composable
fun HealthPermissionBanner(
    modifier: Modifier = Modifier,
    onGrantClick: () -> Unit,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Faltan permisos de salud 🛡️",
                style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFF9B1C1C)),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text =
                    "No podemos calcular tus métricas de hoy ni darte recomendaciones precisas" +
                        " sin acceso a Health Connect.",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9B1C1C)),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE02424)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Conceder permisos", color = Color.White)
            }
        }
    }
}

// ── Card de nutrición del día ─────────────────────────────────────────────────
@Composable
fun NutritionSummaryCard(
    summary: DailyNutritionSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Nutrición hoy", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${summary.mealCount} comida${if (summary.mealCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${summary.totalCalories.toInt()} kcal",
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    Text(
                        "/${summary.calorieGoal.toInt()} objetivo",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                LinearProgressIndicator(
                    progress = { summary.calorieProgress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    color =
                        when {
                            summary.calorieProgress < 0.5f -> SecondaryMint
                            summary.calorieProgress < 0.9f -> PrimaryGreen
                            else -> AccentTerra
                        },
                    trackColor = Color(0xFFEEEEEE),
                )
                if (summary.remainingCalories > 0) {
                    Text(
                        "Quedan ${summary.remainingCalories.toInt()} kcal para hoy",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MacroItem("Proteína", "${summary.totalProteinG.toInt()}g", SecondaryMint)
                MacroItem("Carbos", "${summary.totalCarbsG.toInt()}g", Color(0xFF74B9FF))
                MacroItem("Grasa", "${summary.totalFatG.toInt()}g", AccentTerra)
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MicroItem("Fibra", "${summary.totalFiberG.toInt()}g")
                MicroItem("Sodio", "${summary.totalSodiumMg.toInt()}mg")
                MicroItem("Azúcar", "${summary.totalSugarG.toInt()}g")
            }
        }
    }
}

@Composable
private fun MacroItem(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MicroItem(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun GreetingHeaderPreview() {
    TanayenTheme {
        GreetingHeader(
            userName = "Carlo",
            greeting = "Buenos días ☀️",
            subtitle = "Todo bien por ahora 🌿",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun GreetingHeaderWithAlertPreview() {
    TanayenTheme {
        GreetingHeader(
            userName = "Carlo",
            greeting = "Buenas noches 🌙",
            subtitle = "Dormiste 5.0h — evita cafeína después de las 14:00",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun MetricCardSleepPreview() {
    TanayenTheme {
        MetricCard(
            label = "Sueño",
            value = "7.5",
            unit = "h",
            emoji = "🌙",
            tint = AccentTerra,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun MetricCardEmptyPreview() {
    TanayenTheme {
        MetricCard(
            label = "Peso",
            value = "--",
            unit = "kg",
            emoji = "⚖️",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun StressLevelCardLowPreview() {
    TanayenTheme {
        StressLevelCard(hrvValue = "72")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun StressLevelCardMediumPreview() {
    TanayenTheme {
        StressLevelCard(hrvValue = "48")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun StressLevelCardHighPreview() {
    TanayenTheme {
        StressLevelCard(hrvValue = "32")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun StressLevelCardNoDataPreview() {
    TanayenTheme {
        StressLevelCard(hrvValue = "--")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun MetricsRowPreview() {
    TanayenTheme {
        MetricsRow(
            metrics =
                persistentListOf(
                    MetricItem("Peso", "74.2", "kg", "⚖️", SecondaryMint),
                    MetricItem("Calorías activas", "320", "kcal", "🔥", Color(0xFFE63946)),
                ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun AlertBannerPreview() {
    TanayenTheme {
        AlertBanner(message = "Dormiste 5.0h — evita cafeína después de las 14:00")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun TodayFoodCardEmptyPreview() {
    TanayenTheme {
        TodayFoodCard(
            foodLogs = persistentListOf(),
            onAddManuallyClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun TodayFoodCardWithDataPreview() {
    TanayenTheme {
        TodayFoodCard(
            foodLogs =
                persistentListOf(
                    FoodLogItem("Desayuno", "Avena con frutas"),
                    FoodLogItem("Almuerzo", "Ensalada de pollo"),
                    FoodLogItem("Cena", "Salmón con verduras"),
                ),
            onAddManuallyClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun AskAssistantButtonPreview() {
    TanayenTheme {
        AskAssistantButton(onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun HealthPermissionBannerPreview() {
    TanayenTheme {
        HealthPermissionBanner(onGrantClick = {})
    }
}
