package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.data.repository.ClinicalProfileRepositoryImpl
import dev.tohure.tanayenai.data.repository.HealthMetricsRepositoryImpl
import dev.tohure.tanayenai.data.repository.PantryRepositoryImpl
import dev.tohure.tanayenai.data.repository.RecommendationRepositoryImpl
import dev.tohure.tanayenai.data.repository.UserRepositoryImpl
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.PantryRepository
import dev.tohure.tanayenai.domain.repository.RecommendationRepository
import dev.tohure.tanayenai.domain.repository.UserRepository
import org.koin.dsl.module

val repositoryModule =
    module {
        single<PantryRepository> { PantryRepositoryImpl(get()) }
        single<HealthMetricsRepository> { HealthMetricsRepositoryImpl(get()) }
        single<RecommendationRepository> { RecommendationRepositoryImpl(get()) }
        single<ClinicalProfileRepository> { ClinicalProfileRepositoryImpl(get()) }
        single<UserRepository> { UserRepositoryImpl(get()) }
    }
