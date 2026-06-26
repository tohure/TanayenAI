package dev.tohure.tanayenai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.tohure.tanayenai.data.ApiKeyStore
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import org.koin.compose.koinInject

@Composable
fun GeminiTokenScreen(onBack: () -> Unit) {
    val store: ApiKeyStore = koinInject()
    var tokenText by remember { mutableStateOf(store.getApiKey() ?: "") }
    var showToken by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Volver", color = PrimaryGreen, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "🌿 API Token",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5EE)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Text(
                text =
                    "Cada usuario puede usar su propio API Key de Google AI Studio. " +
                        "Al guardar, se usará en el próximo arranque de la app. " +
                        "Déjalo vacío para usar el token por defecto.",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryGreen,
                modifier = Modifier.padding(14.dp),
            )
        }

        // ── Campo ─────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = tokenText,
            onValueChange = {
                tokenText = it
                saved = false
            },
            label = { Text("Gemini API Key") },
            placeholder = { Text("AIza...", color = TextMutedColor) },
            visualTransformation =
                if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Text(if (showToken) "🙈" else "👁️", style = MaterialTheme.typography.bodyLarge)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    focusedLabelColor = PrimaryGreen,
                ),
        )

        if (saved) {
            Text(
                text = "✓ Guardado. Se usará al reiniciar la app.",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryGreen,
            )
        }

        // ── Botones ───────────────────────────────────────────────────────────
        Button(
            onClick = {
                val trimmed = tokenText.trim()
                if (trimmed.isNotEmpty()) {
                    store.saveApiKey(trimmed)
                } else {
                    store.clearApiKey()
                }
                saved = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
        ) {
            Text("Guardar", style = MaterialTheme.typography.titleSmall.copy(color = SurfaceColor))
        }

        OutlinedButton(
            onClick = {
                store.clearApiKey()
                tokenText = ""
                saved = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Usar token por defecto", color = TextMutedColor)
        }

        Spacer(Modifier.height(16.dp))
    }
}
