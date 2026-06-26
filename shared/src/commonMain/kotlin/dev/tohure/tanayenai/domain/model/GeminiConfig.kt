package dev.tohure.tanayenai.domain.model

/**
 * Wrapper fuerte para la API key de Gemini.
 * Evita que Koin confunda Strings primitivos entre sí
 * (bug descubierto en Fase 5B donde PROTOTYPE_USER_ID sobreescribía la key).
 */
data class GeminiConfig(
    val apiKey: String,
)
