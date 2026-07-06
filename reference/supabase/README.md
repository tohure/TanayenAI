# Supabase — código de referencia (dormido)

Este directorio conserva la integración con **Supabase** que estuvo activa hasta la
Fase 5. Se desconectó del app en la **Fase B** (memoria/optimización) porque hoy todo
el estado del usuario se trabaja en la **BD local del dispositivo** (SQLDelight) y no
se necesita backend.

> ⚠️ **Estos archivos NO se compilan.** Están fuera de cualquier *source set* de Gradle,
> a propósito, para que Supabase no afecte el build ni el runtime ni el tamaño del APK.

## Qué contiene

| Archivo | Rol |
|---|---|
| `SupabaseClient.kt` | Factory `createSupabaseClient(...)` (Postgrest + Auth + Realtime). |
| `SupabaseDataSource.kt` | Upsert/fetch de `pantry_items` y `health_metrics` + mappers DTO→dominio. |
| `SyncManager.kt` | Orquestación de pull (y stubs de push) contra Supabase. |
| `NetworkModule.kt` | Módulo Koin que cableaba `SupabaseClient`/`SupabaseDataSource`/`SyncManager`. |

Los DTOs (`FoodLogDto`, `HealthMetricsDto`, `PantryItemDto`, `RecommendationDto`) siguen
en `shared/src/commonMain/.../data/remote/dto/` porque son `@Serializable` puros (no
importan Supabase) y `ClinicalExtractionDto` además provee `buildClinicalSummaryFromJson`,
usado por el chat.

## Cómo re-activarlo (Fase 6 — Auth + multi-usuario)

1. **Gradle** — restaurar dependencias en `shared/build.gradle.kts`:
   ```kotlin
   implementation(project.dependencies.platform(libs.supabase.bom))
   implementation(libs.supabase.postgrest)
   implementation(libs.supabase.auth)
   implementation(libs.supabase.realtime)
   ```
   y las entradas `supabase*` en `gradle/libs.versions.toml`.
2. **Mover de vuelta** estos 4 archivos a sus paquetes originales
   (`data/remote/` y `di/`).
3. **DI** — volver a agregar `networkModule` a `sharedModules()` y reinyectar
   `SupabaseClient` en `FoodLogRepositoryImpl` (más el `syncToSupabase(...)`).
4. **Secrets** — reponer `SUPABASE_URL`/`SUPABASE_ANON_KEY`:
   - Android: `buildConfigField` en `androidApp/build.gradle.kts` + singles nombrados en `App.kt`.
   - iOS: params de `initKoin(...)` en `KoinInitializer.kt` y `iOSApp.swift`.
5. **RLS** — re-habilitar Row Level Security en Supabase (quedó desactivado en el prototipo).
