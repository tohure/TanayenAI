package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.data.local.DatabaseDriverFactory
import dev.tohure.tanayenai.data.local.createDatabase
import dev.tohure.tanayenai.db.TanayenDatabase
import org.koin.dsl.module

val databaseModule =
    module {
        single<TanayenDatabase> {
            createDatabase(get<DatabaseDriverFactory>())
        }
    }
