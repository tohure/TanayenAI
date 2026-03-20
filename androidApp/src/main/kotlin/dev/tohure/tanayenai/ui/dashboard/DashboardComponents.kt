package dev.tohure.tanayenai.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.tanayenai.ui.theme.AccentTerra
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SecondaryMint
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TextDark
import dev.tohure.tanayenai.ui.theme.TextMutedColor

@Composable
fun GreetingHeader(
    userName: String,
    greeting: String, // "Buenos días", "Buenas tardes", etc.
    subtitle: String, // "Hoy te ves bien 🌿" o alerta si VFC baja
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
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
            androidx.compose.foundation.Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp),
            ) {
                val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                // Fondo gradiente rojo -> amarillo -> verde (Rojo = bajo VFC = Alto estrés)
                drawRoundRect(
                    brush =
                        Brush.horizontalGradient(
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
    metrics: List<Triple<String, String, String>>, // (label, value, unit)
    emojis: List<String>,
    tints: List<Color>,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        metrics.forEachIndexed { index, (label, value, unit) ->
            MetricCard(
                label = label,
                value = value,
                unit = unit,
                emoji = emojis.getOrElse(index) { "📊" },
                tint = tints.getOrElse(index) { SecondaryMint },
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
    foodLogs: List<Pair<String, String>>, // (mealType label, foodName)
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
                foodLogs.forEach { (mealType, foodName) ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = mealType,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = foodName,
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
