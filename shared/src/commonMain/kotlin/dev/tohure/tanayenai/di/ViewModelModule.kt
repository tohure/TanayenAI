package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.presentation.viewmodel.ChatViewModel
import dev.tohure.tanayenai.presentation.viewmodel.DashboardViewModel
import dev.tohure.tanayenai.presentation.viewmodel.PantryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { params ->
            DashboardViewModel(
                healthMetricsRepository = get(),
                pantryRepository = get(),
                recommendationRepository = get(),
                buildContextUseCase = get(),
                syncHealthMetricsUseCase = get { parametersOf(params.get()) },
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
                healthMetricsRepository = get(),
                pantryRepository = get(),
                recommendationRepository = get(),
                userId = params.get(),
            )
        }
    }
