package dev.tohure.tanayenai.ui.chat

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val boldRegex = Regex("""\*\*(.+?)\*\*""")
private val bulletLineRegex = Regex("""(?m)^[ \t]*[-*][ \t]+""")

/**
 * Convierte el markdown básico que emite Gemini a AnnotatedString:
 * **negrita** y viñetas ("- " / "* " al inicio de línea) → "• ".
 *
 * Robusto ante streaming: un "**" sin cerrar todavía queda como texto literal
 * hasta que llega su par, sin romper el render.
 */
fun markdownToAnnotatedString(raw: String): AnnotatedString {
    val normalized = raw.replace(bulletLineRegex, "• ")
    return buildAnnotatedString {
        var last = 0
        for (match in boldRegex.findAll(normalized)) {
            append(normalized.substring(last, match.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            last = match.range.last + 1
        }
        append(normalized.substring(last))
    }
}
