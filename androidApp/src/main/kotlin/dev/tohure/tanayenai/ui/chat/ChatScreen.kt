package dev.tohure.tanayenai.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatScreen() {
    val viewModel: ChatViewModel = koinViewModel { parametersOf(PROTOTYPE_USER_ID) }
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Estado del uri temporal cuando se abre la camara
    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                uri?.let {
                    val base64 = ImageUtils.resizeAndEncodeImage(context, it)
                    if (base64 != null) {
                        viewModel.attachImage(base64)
                    }
                }
            },
        )

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { success ->
                if (success) {
                    currentCameraUri?.let { uri ->
                        val base64 = ImageUtils.resizeAndEncodeImage(context, uri)
                        if (base64 != null) {
                            viewModel.attachImage(base64)
                        }
                    }
                }
            },
        )

    // Auto-scroll al último mensaje al cambiar el contenido textual (ideal para el stream)
    val lastMessageContent = uiState.messages.lastOrNull()?.content
    LaunchedEffect(lastMessageContent) {
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Header
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
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
                MessageBubble(message = message)

                // Chip de alacena (verde)
                message.pantrySuggestion?.let { suggestion ->
                    PantrySuggestionChip(
                        suggestion = suggestion,
                        onConfirm = { viewModel.confirmPantrySuggestion(message.id) },
                        onDismiss = { viewModel.dismissPantrySuggestion(message.id) },
                    )
                }

                // Chip clínico (azul)
                message.clinicalSuggestion?.let { suggestion ->
                    ClinicalSuggestionChip(
                        suggestion = suggestion,
                        onConfirm = { viewModel.confirmClinicalSuggestion(message.id) },
                        onDismiss = { viewModel.dismissClinicalSuggestion(message.id) },
                    )
                }
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

        // Vista previa de la imagen a enviar
        uiState.pendingImage?.let { pending ->
            PendingImagePreview(
                pendingImage = pending,
                onRemove = { viewModel.clearPendingImage() },
            )
        }

        // Input
        ChatInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            isLoading = uiState.isLoading,
            hasPendingImage = uiState.pendingImage != null,
            onCameraClick = {
                val uri = ImageUtils.createImageUri(context)
                currentCameraUri = uri
                uri?.let { cameraLauncher.launch(it) }
            },
            onGalleryClick = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSend = {
                viewModel.sendMessage(inputText)
                inputText = ""
            },
        )
    }
}
