package dev.tohure.tanayenai

import android.app.Application
import dev.tohure.tanayenai.data.local.DatabaseDriverFactory
import dev.tohure.tanayenai.di.sharedModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(
                sharedModules() +
                    module {
                        single { DatabaseDriverFactory(androidContext()) }
                    },
            )
        }
    }
}
