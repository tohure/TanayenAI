package dev.tohure.tanayenai

import android.app.Application
import dev.tohure.tanayenai.data.health.HealthDataReader
import dev.tohure.tanayenai.data.local.DatabaseDriverFactory
import dev.tohure.tanayenai.di.GeminiConfig
import dev.tohure.tanayenai.di.sharedModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
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
                        single(named("SUPABASE_URL")) { BuildConfig.SUPABASE_URL }
                        single(named("SUPABASE_ANON_KEY")) { BuildConfig.SUPABASE_ANON_KEY }
                        single { GeminiConfig(BuildConfig.GEMINI_API_KEY) }
                        single { HealthDataReader(androidContext()) }
                    },
            )
        }
    }
}
