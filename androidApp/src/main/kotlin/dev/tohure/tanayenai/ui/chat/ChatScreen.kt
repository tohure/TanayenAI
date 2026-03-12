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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatScreen() {
    val viewModel: ChatViewModel =
        koinViewModel {
            parametersOf("00000000-0000-0000-0000-000000000001")
        }
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje al cambiar el contenido textual (ideal para el stream)
    val lastMessageContent = uiState.messages.lastOrNull()?.content
    LaunchedEffect(lastMessageContent) {
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
    ) {
        // Header
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
                    text = if (uiState.contextReady) "Contexto listo 🌿" else "Cargando contexto...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedColor,
                )
            }
        }

        // Mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(
                    message =
                        ChatMessage(
                            id = message.id,
                            content = message.content,
                            isUser = message.isUser,
                            isLoading = message.isLoading,
                        ),
                )
            }
        }

        // Error banner
        uiState.error?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }

        // Input
        ChatInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            isLoading = uiState.isLoading,
            onCameraClick = { /* TODO Fase 5B */ },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            },
        )
    }
}
