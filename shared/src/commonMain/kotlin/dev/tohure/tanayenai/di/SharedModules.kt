package dev.tohure.tanayenai.di

import org.koin.core.module.Module

fun sharedModules(): List<Module> =
    listOf(
        databaseModule,
        repositoryModule,
        useCaseModule,
        viewModelModule,
        geminiModule,
        notificationModule,
    )
