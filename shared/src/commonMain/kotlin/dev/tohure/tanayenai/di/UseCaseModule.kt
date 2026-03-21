package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import dev.tohure.tanayenai.domain.usecase.FetchContextParamsUseCase
import dev.tohure.tanayenai.domain.usecase.GetLatestMetricsUseCase
import dev.tohure.tanayenai.domain.usecase.SavePantryIngredientsUseCase
import dev.tohure.tanayenai.domain.usecase.SyncHealthMetricsUseCase
import org.koin.dsl.module

val useCaseModule =
    module {
        factory { BuildContextUseCase() }
        factory { SavePantryIngredientsUseCase(get()) }
        factory { FetchContextParamsUseCase(get(), get()) }
        factory { GetLatestMetricsUseCase(get()) }
        factory { (userId: String) ->
            SyncHealthMetricsUseCase(
                healthDataReader = get(),
                repository = get(),
                userId = userId,
            )
        }
    }
