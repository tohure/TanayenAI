package dev.tohure.tanayenai.domain.usecase

object ChatTagParser {
    val REC_TAG_REGEX = Regex("""\[REC:([A-Z]+):([^:]+):([^\]]+)\]""", RegexOption.IGNORE_CASE)
    val PANTRY_TAG_REGEX = Regex("""\[PANTRY:([^\]]+)\]""", RegexOption.IGNORE_CASE)

    // Detecta tags incompletos al final del texto durante el streaming.
    // [^\]]* captura cualquier carácter excepto "]", incluyendo minúsculas y acentos de los ingredientes.
    // Ancla al final ($) para no eliminar "[" legítimos en el cuerpo del texto.
    private val PARTIAL_TAG_REGEX = Regex("""\[[A-Z][^\]]*$""")

    /** Extrae ingredientes del tag [PANTRY:a|b|...]. Retorna null si no hay tag. */
    fun extractPantryIngredients(response: String): List<String>? {
        val match = PANTRY_TAG_REGEX.find(response) ?: return null
        val ingredients =
            match.groupValues[1]
                .split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        return ingredients.ifEmpty { null }
    }

    /** Extrae tipo, título e ingredientes del tag [REC:TIPO:título:a|b|...]. Retorna null si no hay tag. */
    fun extractRecAction(response: String): RecAction? {
        val match = REC_TAG_REGEX.find(response) ?: return null
        return RecAction(
            type = match.groupValues[1].uppercase(),
            title = match.groupValues[2].trim(),
            ingredients =
                match.groupValues[3]
                    .split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
        )
    }

    /** Elimina tags completos del texto para mostrarlo al usuario. */
    fun stripTags(text: String): String =
        text
            .replace(REC_TAG_REGEX, "")
            .replace(PANTRY_TAG_REGEX, "")
            .trim()

    /** Oculta tags completos Y parciales durante el streaming, evitando parpadeos. */
    fun stripForStreaming(text: String): String =
        text
            .replace(REC_TAG_REGEX, "")
            .replace(PANTRY_TAG_REGEX, "")
            .replace(PARTIAL_TAG_REGEX, "")
            .trim()
}

data class RecAction(
    val type: String,
    val title: String,
    val ingredients: List<String>,
)
