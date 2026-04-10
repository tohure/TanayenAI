package dev.tohure.tanayenai.di

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.shreyaspatil.ai.client.generativeai.type.generationConfig
import dev.tohure.tanayenai.domain.model.GeminiConfig
import dev.tohure.tanayenai.domain.usecase.NUTRITION_SYSTEM_PROMPT
import org.koin.core.qualifier.named
import org.koin.dsl.module

val GEMINI_CHAT = named("gemini_chat")
val GEMINI_CLINICAL = named("gemini_clinical")

val geminiModule =
    module {
        single(GEMINI_CHAT) {
            val config = get<GeminiConfig>()
            GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = config.apiKey.trim(),
                systemInstruction = content { text(NUTRITION_SYSTEM_PROMPT) },
            )
        }
        single(GEMINI_CLINICAL) {
            val config = get<GeminiConfig>()
            GenerativeModel(
                modelName = "gemini-3.1-flash-lite-preview",
                apiKey = config.apiKey.trim(),
                generationConfig =
                    generationConfig {
                        temperature = 0f
                        responseMimeType = "application/json"
                    },
            )
        }
    }
