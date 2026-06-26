package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.domain.usecase.GenerateMorningAdviceUseCase
import dev.tohure.tanayenai.presentation.viewmodel.NotificationSettingsViewModel
import org.koin.dsl.module

val notificationModule =
    module {
        factory { (userId: String) ->
            GenerateMorningAdviceUseCase(
                generativeModel = get(GEMINI_ADVICE),
                healthMetricsRepository = get(),
                clinicalProfileRepository = get(),
                pantryRepository = get(),
                userId = userId,
            )
        }
        factory { (userId: String, onScheduleChanged: (Int, Int, Boolean) -> Unit) ->
            NotificationSettingsViewModel(
                prefs = get(),
                userId = userId,
                onScheduleChanged = onScheduleChanged,
            )
        }
    }
