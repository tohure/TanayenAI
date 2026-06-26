package dev.tohure.tanayenai

import android.app.Application
import dev.tohure.tanayenai.data.ApiKeyStore
import dev.tohure.tanayenai.data.health.HealthDataReader
import dev.tohure.tanayenai.data.local.DatabaseDriverFactory
import dev.tohure.tanayenai.data.pdf.PdfPicker
import dev.tohure.tanayenai.data.prefs.NotificationPrefs
import dev.tohure.tanayenai.di.sharedModules
import dev.tohure.tanayenai.domain.model.GeminiConfig
import dev.tohure.tanayenai.domain.model.PROTOTYPE_USER_ID
import dev.tohure.tanayenai.notification.NotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
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
                        single { ApiKeyStore(androidContext()) }
                        single {
                            val stored = get<ApiKeyStore>().getApiKey()
                            GeminiConfig(stored ?: BuildConfig.GEMINI_API_KEY)
                        }
                        single { HealthDataReader(androidContext()) }
                        single { NotificationPrefs(androidContext()) }
                        // PdfPicker se registra en MainActivity tras tener la activity
                    },
            )
        }

        // Re-schedule notification on app start (respects saved settings)
        val prefs =
            GlobalContext
                .get()
                .get<NotificationPrefs>()
        val settings = prefs.load(PROTOTYPE_USER_ID)
        NotificationScheduler.schedule(this, settings.morningHour, settings.morningMinute, settings.morningEnabled)
    }
}
