package dev.tohure.tanayenai.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen() {
    val messages =
        remember {
            mutableStateListOf(
                ChatMessage(
                    id = "welcome",
                    content = "Hola 🌿 Soy tu asistente nutricional. Puedes contarme qué comiste, mandarme fotos de tu alacena, o preguntarme qué comer hoy.",
                    isUser = false,
                ),
            )
        }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
    ) {
        // Header mínimo
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Asistente", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Contexto actualizado · hace 2 min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedColor,
                )
            }
        }

        // Lista de mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        // Input bar
        ChatInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            isLoading = isLoading,
            onCameraClick = {
                // TODO: abrir cámara para escanear alacena
            },
            onSend = {
                if (inputText.isBlank() || isLoading) return@ChatInputBar

                val userMessage =
                    ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        content = inputText.trim(),
                        isUser = true,
                    )
                messages.add(userMessage)
                val sentText = inputText
                inputText = ""
                isLoading = true

                // Indicador "escribiendo"
                val loadingId = "loading_${System.currentTimeMillis()}"
                messages.add(
                    ChatMessage(id = loadingId, content = "", isUser = false, isLoading = true),
                )

                scope.launch {
                    // TODO Fase 4: reemplazar con llamada real a Gemini API + ContextBuilder
                    delay(1500)
                    messages.removeIf { it.id == loadingId }
                    messages.add(
                        ChatMessage(
                            id = System.currentTimeMillis().toString(),
                            content = mockGeminiResponse(sentText),
                            isUser = false,
                        ),
                    )
                    isLoading = false
                }
            },
        )
    }
}

// Respuestas mock hasta conectar Gemini
private fun mockGeminiResponse(input: String): String =
    when {
        input.contains(
            "comer",
            ignoreCase = true,
        ) -> "Basándome en tu alacena y métricas de hoy, te recomiendo una ensalada de atún con aguacate para el almuerzo. Tienes todos los ingredientes en casa 🥗"

        input.contains("sueño", ignoreCase = true) ||
            input.contains(
                "dormí",
                ignoreCase = true,
            )
        -> "Veo que dormiste poco. Hoy evita cafeína después de las 14:00 y prefiere una cena ligera. El magnesio de las almendras puede ayudarte esta noche 🌙"

        input.contains("foto", ignoreCase = true) ||
            input.contains(
                "alacena",
                ignoreCase = true,
            )
        -> "Recibido 📦 Estoy actualizando tu alacena con los nuevos ingredientes. Te aviso cuando termine."

        else -> "Entendido. Tengo en cuenta eso para tus próximas recomendaciones. ¿Quieres saber qué puedes comer hoy con lo que tienes en casa? 🌿"
    }
