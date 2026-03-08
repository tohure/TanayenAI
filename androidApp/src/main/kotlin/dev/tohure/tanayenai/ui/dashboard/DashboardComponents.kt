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
import androidx.compose.ui.graphics.Color
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
