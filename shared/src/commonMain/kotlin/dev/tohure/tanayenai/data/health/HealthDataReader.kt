package dev.tohure.tanayenai.data.health

import dev.tohure.tanayenai.domain.model.DailyHealthData
import dev.tohure.tanayenai.domain.model.HealthPermission
import dev.tohure.tanayenai.domain.model.HealthPermissionResult
import kotlinx.datetime.LocalDate

// El shared define el contrato — cada plataforma lo implementa
expect class HealthDataReader {
    // Verificar permisos
    suspend fun hasPermissions(permissions: Set<HealthPermission>): Boolean

    // Leer métricas del día especificado
    suspend fun readDailyData(date: LocalDate): DailyHealthData?

    // Leer los últimos N días
    suspend fun readRecentData(days: Int): List<DailyHealthData>
}
