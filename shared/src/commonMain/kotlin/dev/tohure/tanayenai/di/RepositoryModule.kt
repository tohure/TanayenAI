package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.data.repository.HealthMetricsRepositoryImpl
import dev.tohure.tanayenai.data.repository.PantryRepositoryImpl
import dev.tohure.tanayenai.data.repository.RecommendationRepositoryImpl
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.PantryRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import org.koin.dsl.module

val repositoryModule =
    module {
        single<PantryRepository> { PantryRepositoryImpl(get()) }
        single<HealthMetricsRepository> { HealthMetricsRepositoryImpl(get()) }
        single<RecommendationRepository> { RecommendationRepositoryImpl(get()) }
    }
