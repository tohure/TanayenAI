package dev.tohure.tanayenai.ui.chat

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.tanayenai.presentation.viewmodel.PantrySuggestion
import dev.tohure.tanayenai.presentation.viewmodel.PendingImage
import dev.tohure.tanayenai.presentation.viewmodel.UiChatMessage
import dev.tohure.tanayenai.ui.theme.BackgroundColor
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SecondaryMint
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TanayenTheme
import dev.tohure.tanayenai.ui.theme.TextDark
import dev.tohure.tanayenai.ui.theme.TextMutedColor
import java.io.ByteArrayOutputStream

@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    message: UiChatMessage,
) {
    val isUser = message.isUser
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            // Avatar del asistente
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5EE)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🌿", fontSize = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp,
                        ),
                    ).background(if (isUser) PrimaryGreen else SurfaceColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (message.isLoading) {
                TypingIndicator()
            } else {
                Column {
                    // Indicador de foto procesada en lugar de mostrar la imagen enorme
                    if (message.hasAttachedImage) {
                        Text(
                            text = "📷 Imagen adjunta",
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    color = if (isUser) Color.White.copy(alpha = 0.8f) else TextMutedColor,
                                ),
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    if (message.content.isNotBlank()) {
                        Text(
                            text = message.content,
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = if (isUser) SurfaceColor else TextDark,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        listOf(0, 150, 300).forEach { delayMs ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            keyframes {
                                durationMillis = 900
                                0f at 0
                                6f at 200
                                0f at 400
                                0f at 900
                            },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(delayMs),
                    ),
                label = "dot",
            )
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .offset(y = (-offsetY).dp)
                        .clip(CircleShape)
                        .background(TextMutedColor),
            )
        }
    }
}

@Composable
fun PendingImagePreview(
    pendingImage: PendingImage,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap =
        remember(pendingImage.base64Data) {
            val bytes = Base64.decode(pendingImage.base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }

    Box(
        modifier =
            modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(72.dp),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Imagen adjunta",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .width(72.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
            )
        }

        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE63946))
                    .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                color = Color.White,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        lineHeight = 7.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle =
                            LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both,
                            ),
                    ),
            )
        }
    }
}

@Composable
fun PantrySuggestionChip(
    suggestion: PantrySuggestion,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestion.confirmed) {
        Text(
            text = "✓ Guardado en tu alacena",
            style = MaterialTheme.typography.labelSmall.copy(color = SecondaryMint),
            modifier = modifier.padding(start = 48.dp, top = 4.dp, bottom = 8.dp),
        )
        return
    }

    Row(
        modifier =
            modifier
                .padding(start = 48.dp, top = 4.dp, end = 16.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE8F5EE))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "¿Agrego ${suggestion.ingredients.size} ingrediente(s) a tu alacena?",
            style = MaterialTheme.typography.labelSmall.copy(color = PrimaryGreen),
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onDismiss,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("No", style = MaterialTheme.typography.labelSmall.copy(color = TextMutedColor))
        }
        Button(
            onClick = onConfirm,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
        ) {
            Text("Sí", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    hasPendingImage: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceColor,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onCameraClick,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5EE)),
            ) {
                Text("📷", fontSize = 18.sp)
            }

            IconButton(
                onClick = onGalleryClick,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5EE)),
            ) {
                Text("🖼️", fontSize = 18.sp)
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Escribe aquí...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryMint,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = BackgroundColor,
                        unfocusedContainerColor = BackgroundColor,
                    ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (!isLoading) onSend() }),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge,
            )

            // Botón enviar
            IconButton(
                onClick = onSend,
                enabled = (value.isNotBlank() || hasPendingImage) && !isLoading,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if ((value.isNotBlank() || hasPendingImage) && !isLoading) {
                                PrimaryGreen
                            } else {
                                Color(0xFFE0E0E0)
                            },
                        ),
            ) {
                Text("↑", fontSize = 20.sp, color = SurfaceColor)
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun PendingImagePreviewPreview() {
    TanayenTheme {
        // Genera un bitmap celeste de 72×72 para visualizar el tamaño real del thumbnail
        val base64 =
            remember {
                val bmp =
                    createBitmap(72, 72, android.graphics.Bitmap.Config.ARGB_8888)
                bmp.eraseColor(0xFF4FC3F7.toInt())
                val out = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                Base64.encodeToString(out.toByteArray(), android.util.Base64.DEFAULT)
            }
        PendingImagePreview(
            pendingImage = PendingImage(base64Data = base64),
            onRemove = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun PantrySuggestionChipPreview() {
    TanayenTheme {
        PantrySuggestionChip(
            suggestion = PantrySuggestion(ingredients = listOf("avena", "almendras", "yogur griego")),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun PantrySuggestionChipConfirmedPreview() {
    TanayenTheme {
        PantrySuggestionChip(
            suggestion =
                PantrySuggestion(
                    ingredients = listOf("avena", "almendras"),
                    confirmed = true,
                ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubbleUserPreview() {
    TanayenTheme {
        MessageBubble(
            message =
                UiChatMessage(
                    id = "1",
                    content = "¿Cuál cereal es mejor para mi colesterol?",
                    isUser = true,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubbleAssistantPreview() {
    TanayenTheme {
        MessageBubble(
            message =
                UiChatMessage(
                    id = "2",
                    content = "El cereal A tiene menos azúcar y más fibra, ideal para tu perfil. 🌿",
                    isUser = false,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubbleWithImagePreview() {
    TanayenTheme {
        MessageBubble(
            message =
                UiChatMessage(
                    id = "3",
                    content = "Veo que compraste avena, almendras y yogur griego.",
                    isUser = false,
                    hasAttachedImage = true,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputBarPreview() {
    TanayenTheme {
        ChatInputBar(
            value = "",
            onValueChange = {},
            onSend = {},
            isLoading = false,
            hasPendingImage = false,
            onCameraClick = {},
            onGalleryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputBarWithImagePreview() {
    TanayenTheme {
        ChatInputBar(
            value = "¿Es bueno para mi salud?",
            onValueChange = {},
            onSend = {},
            isLoading = false,
            hasPendingImage = true,
            onCameraClick = {},
            onGalleryClick = {},
        )
    }
}
