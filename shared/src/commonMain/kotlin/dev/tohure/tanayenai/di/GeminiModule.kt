package dev.tohure.tanayenai.di

import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.domain.usecase.NUTRITION_SYSTEM_PROMPT
import org.koin.core.qualifier.named
import org.koin.dsl.module

val geminiModule =
    module {
        single {
            GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = get(named("GEMINI_API_KEY")),
                systemInstruction = content { text(NUTRITION_SYSTEM_PROMPT) },
            )
        }
    }
