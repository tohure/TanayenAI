package dev.tohure.tanayenai.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel
import dev.tohure.tanayenai.presentation.viewmodel.MAX_PENDING_IMAGES
import dev.tohure.tanayenai.ui.ScreenHeader
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatScreen() {
    val viewModel: ChatViewModel = koinViewModel { parametersOf(PROTOTYPE_USER_ID) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Estado del uri temporal cuando se abre la camara
    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_PENDING_IMAGES),
            onResult = { uris ->
                uris.forEach { uri ->
                    val base64 = ImageUtils.resizeAndEncodeImage(context, uri)
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

    // Auto-scroll al último mensaje. Se re-dispara al añadirse un mensaje (size) y mientras
    // crece el texto en streaming (length) — usando claves Int baratas, no el string completo.
    val messageCount = uiState.messages.size
    val lastMessageLength =
        uiState.messages
            .lastOrNull()
            ?.content
            ?.length ?: 0
    LaunchedEffect(messageCount, lastMessageLength) {
        if (messageCount > 0) {
            listState.scrollToItem(messageCount - 1)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),
    ) {
        // Header
        ScreenHeader(
            title = "Asistente",
            subtitle = if (uiState.contextReady) "Contexto listo 🌿" else "Cargando contexto...",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )

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

                // Chips food log detectados (naranja) — uno por plato distinto
                message.foodLogSuggestions.forEach { suggestion ->
                    FoodLogSuggestionChip(
                        suggestion = suggestion,
                        onConfirm = { viewModel.confirmFoodLogSuggestion(message.id, suggestion.id) },
                        onDismiss = { viewModel.dismissFoodLogSuggestion(message.id, suggestion.id) },
                    )
                }

                // Chip check-in proactivo (naranja)
                message.checkInSuggestion?.let { suggestion ->
                    CheckInChip(
                        suggestion = suggestion,
                        onYes = { viewModel.confirmCheckInYes(message.id) },
                        onNo = { viewModel.confirmCheckInNo(message.id) },
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

        // Vista previa de las imágenes a enviar
        if (uiState.pendingImages.isNotEmpty()) {
            PendingImagesPreview(
                pendingImages = uiState.pendingImages,
                maxImages = MAX_PENDING_IMAGES,
                onRemove = { index -> viewModel.removePendingImage(index) },
            )
        }

        // Input
        ChatInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            isLoading = uiState.isLoading,
            hasPendingImage = uiState.pendingImages.isNotEmpty(),
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
