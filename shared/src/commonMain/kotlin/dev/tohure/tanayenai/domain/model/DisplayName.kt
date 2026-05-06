package dev.tohure.tanayenai.domain.model

fun resolveDisplayName(stored: String): String {
    val trimmed = stored.trim()
    if (trimmed.isBlank()) return "Hola"
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 3 -> "${parts[0]} ${parts.last()[0].uppercaseChar()}."
        parts.size == 2 && trimmed.length > 14 -> "${parts[0]} ${parts[1][0].uppercaseChar()}."
        else -> trimmed
    }
}
