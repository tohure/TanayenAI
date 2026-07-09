package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel
import dev.tohure.tanayenai.presentation.viewmodel.ClinicalProfileViewModel
import dev.tohure.tanayenai.presentation.viewmodel.DashboardViewModel
import dev.tohure.tanayenai.presentation.viewmodel.FoodDiaryViewModel
import dev.tohure.tanayenai.presentation.viewmodel.HealthViewModel
import dev.tohure.tanayenai.presentation.viewmodel.PantryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { params ->
            DashboardViewModel(
                getLatestMetricsUseCase = get(),
                fetchContextParamsUseCase = get(),
                buildContextUseCase = get(),
                syncHealthMetricsUseCase = get { parametersOf(params.get<String>()) },
                foodLogRepository = get(),
                notificationPrefs = get(),
                userId = params.get(),
            )
        }
        viewModel { params ->
            PantryViewModel(
                pantryRepository = get(),
                userId = params.get(),
            )
        }
        viewModel { params ->
            FoodDiaryViewModel(
                foodLogRepository = get(),
                userId = params.get(),
            )
        }
        viewModel { params ->
            val userId = params.get<String>()
            ChatViewModel(
                generativeModel = get(GEMINI_CHAT),
                buildContextUseCase = get(),
                fetchContextParamsUseCase = get(),
                savePantryIngredientsUseCase = get(),
                recommendationRepository = get(),
                extractClinicalProfileUseCase = get { parametersOf(userId) },
                estimateFoodNutritionUseCase = get { parametersOf(userId) },
                foodLogRepository = get(),
                userRepository = get(),
                chatMessageRepository = get(),
                summarizeConversationUseCase = get { parametersOf(userId) },
                userId = userId,
            )
        }
        viewModel { params ->
            val userId = params.get<String>()
            ClinicalProfileViewModel(
                pdfPicker = get(),
                extractUseCase = get { parametersOf(userId) },
                repository = get(),
                userId = userId,
            )
        }
        // factory en lugar de viewModel para compatibilidad con iOS (KoinPlatform.getKoin().get())
        factory { (userId: String) ->
            HealthViewModel(
                syncHealthMetricsUseCase = get { parametersOf(userId) },
                healthMetricsRepository = get(),
                userId = userId,
            )
        }
    }
