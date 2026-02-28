package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.domain.usecase.BuildContextUseCase
import org.koin.dsl.module

val useCaseModule =
    module {
        factory { BuildContextUseCase() }
    }
