package dev.tohure.tanayenai.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tohure.tanayenai.ui.theme.TextMutedColor

/**
 * Cabecera estándar para pantallas principales.
 * Provee tipografía y espaciado consistente; el caller agrega padding posicional vía [modifier].
 *
 * Uso típico:
 *   ScreenHeader(
 *       title = "Alacena",
 *       subtitle = "34 ingredientes",
 *       modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
 *   )
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMutedColor)
    }
}
