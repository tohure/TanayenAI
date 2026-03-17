package dev.tohure.tanayenai.data.remote

import co.touchlab.kermit.Logger
import dev.tohure.tanayenai.domain.repository.HealthMetricsRepository
import dev.tohure.tanayenai.domain.repository.PantryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val log = Logger.withTag("SyncManager")

class SyncManager(
    private val dataSource: SupabaseDataSource,
    private val pantryRepository: PantryRepository,
    private val healthMetricsRepository: HealthMetricsRepository,
) {
    suspend fun syncPendingChanges(userId: String) =
        withContext(Dispatchers.Default) {
            // TODO: En Fase futura, esto leerá los pendientes del repositorio local
            // y llamará a dataSource.upsert(...)
            // Por ahora, como es SQLDelight puro, no lo tocamos hasta la etapa Push.
            log.d { "=== SYNCMANAGER: syncPendingChanges not fully refactored yet ===" }
        }

    suspend fun pullRemoteData(userId: String) =
        withContext(Dispatchers.Default) {
            log.d { "=== SYNCMANAGER: pullRemoteData started ===" }
            pullPantryItems(userId)
            log.d { "=== SYNCMANAGER: pullPantryItems finished ===" }
            pullHealthMetrics(userId)
            log.d { "=== SYNCMANAGER: pullHealthMetrics finished ===" }
        }

    private suspend fun syncPendingPantryItems() {
        // TODO: Next phase
    }

    private suspend fun syncPendingHealthMetrics() {
        // TODO: Next phase
    }

    private suspend fun pullPantryItems(userId: String) {
        val items = dataSource.fetchPantryItems(userId)
        log.d { "=== SYNCMANAGER: fetched ${items.size} pantry items from Supabase" }
        items.forEach { dto ->
            try {
                val domain = with(dataSource) { dto.toDomain() }
                pantryRepository.addItem(domain)
                // TODO: En fase futura, la base de datos marcará is_synced = 1 adentro del repo
            } catch (e: Exception) {
                log.e(e) { "Failed saving pulled PantryItem ${dto.id}" }
            }
        }
    }

    private suspend fun pullHealthMetrics(userId: String) {
        val metrics = dataSource.fetchHealthMetrics(userId)
        log.d { "=== SYNCMANAGER: fetched ${metrics.size} health metrics from Supabase" }
        metrics.forEach { dto ->
            try {
                val domain = with(dataSource) { dto.toDomain() }
                healthMetricsRepository.saveMetrics(domain)
                // TODO: En fase futura, la base de datos marcará is_synced = 1 adentro del repo
            } catch (e: Exception) {
                log.e(e) { "Failed saving pulled HealthMetrics ${dto.id}" }
            }
        }
    }
}
