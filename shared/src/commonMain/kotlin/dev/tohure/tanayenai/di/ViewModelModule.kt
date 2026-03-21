package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel
import dev.tohure.tanayenai.presentation.viewmodel.DashboardViewModel
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
            ChatViewModel(
                generativeModel = get(),
                buildContextUseCase = get(),
                fetchContextParamsUseCase = get(),
                savePantryIngredientsUseCase = get(),
                recommendationRepository = get(),
                userId = params.get(),
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
