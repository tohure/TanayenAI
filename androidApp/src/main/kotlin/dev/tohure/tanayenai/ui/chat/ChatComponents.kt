package dev.tohure.tanayenai.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tohure.tanayenai.ui.theme.BackgroundColor
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SecondaryMint
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TextDark
import dev.tohure.tanayenai.ui.theme.TextMutedColor

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val isLoading: Boolean = false, // Indicador de "Gemini está escribiendo"
)

@Composable
fun MessageBubble(
    modifier: Modifier = Modifier,
    message: ChatMessage,
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

/** Indicador "escribiendo..." */
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
                targetValue = 6f, // ← positivo, sin warning
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            keyframes {
                                durationMillis = 900
                                0f at 0
                                6f at 200 // ← mismo rango que targetValue
                                0f at 400
                                0f at 900
                            },
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = StartOffset(delayMs),
                    ),
                label = "dot_$delayMs",
            )
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .offset(y = (-offsetY).dp) // ← negado aquí para subir
                        .clip(CircleShape)
                        .background(TextMutedColor),
            )
        }
    }
}

/** Input field */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    onCameraClick: () -> Unit,
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
            // Botón de cámara (foto alacena)
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

            // Campo de texto
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Escribe o pregunta algo...",
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
                enabled = value.isNotBlank() && !isLoading,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (value.isNotBlank() && !isLoading) {
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
