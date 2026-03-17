package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.domain.usecase.SyncHealthMetricsUseCase
import dev.tohure.tanayenai.presentation.viewmodel.HealthViewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val healthModule =
    module {

        factory { (userId: String) ->
            SyncHealthMetricsUseCase(
                healthDataReader = get(),
                repository = get(),
                userId = userId,
            )
        }

        factory { (userId: String) ->
            HealthViewModel(
                syncHealthMetricsUseCase = get { parametersOf(userId) },
                healthMetricsRepository = get(),
                userId = userId,
            )
        }
    }
