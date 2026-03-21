package dev.tohure.tanayenai.di

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.domain.usecase.NUTRITION_SYSTEM_PROMPT
import org.koin.core.qualifier.named
import org.koin.dsl.module

data class GeminiConfig(
    val apiKey: String,
)

val geminiModule =
    module {
        single {
            val config = get<GeminiConfig>()
            GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = config.apiKey.trim(),
                systemInstruction = content { text(NUTRITION_SYSTEM_PROMPT) },
            )
        }
    }
