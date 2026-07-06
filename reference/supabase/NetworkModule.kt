package dev.tohure.tanayenai.di

import dev.tohure.tanayenai.data.remote.SupabaseDataSource
import dev.tohure.tanayenai.data.remote.SyncManager
import dev.tohure.tanayenai.data.remote.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule =
    module {
        single<SupabaseClient> {
            createSupabaseClient(
                supabaseUrl = get(named("SUPABASE_URL")),
                supabaseAnonKey = get(named("SUPABASE_ANON_KEY")),
            )
        }

        single {
            SupabaseDataSource(
                client = get(),
            )
        }

        single {
            SyncManager(
                dataSource = get(),
                pantryRepository = get(),
                healthMetricsRepository = get(),
            )
        }
    }
