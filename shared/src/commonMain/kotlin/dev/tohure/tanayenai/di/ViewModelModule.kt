package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.presentation.viewmodel.DashboardViewModel
import dev.tohure.tanayenai.presentation.viewmodel.PantryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { params ->
            DashboardViewModel(
                healthMetricsRepository = get(),
                pantryRepository = get(),
                recommendationRepository = get(),
                buildContextUseCase = get(),
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
            dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel(
                generativeModel = get(),
                buildContextUseCase = get(),
                healthMetricsRepository = get(),
                pantryRepository = get(),
                recommendationRepository = get(),
                userId = params.get(),
            )
        }
    }
