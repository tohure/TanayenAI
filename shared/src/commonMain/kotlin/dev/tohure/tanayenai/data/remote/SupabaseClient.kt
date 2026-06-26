package dev.tohure.tanayenai.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

fun createSupabaseClient(
    supabaseUrl: String,
    supabaseAnonKey: String,
): SupabaseClient =
    createSupabaseClient(
        supabaseUrl = supabaseUrl,
        supabaseKey = supabaseAnonKey,
    ) {
        defaultSerializer =
            KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )

        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
