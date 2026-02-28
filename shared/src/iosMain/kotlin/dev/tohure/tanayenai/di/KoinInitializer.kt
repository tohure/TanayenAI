package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.data.local.DatabaseDriverFactory
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin() {
    startKoin {
        modules(
            sharedModules() +
                module {
                    single { DatabaseDriverFactory() }
                },
        )
    }
}
