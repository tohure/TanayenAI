package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.MealType
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.User

/**
 * Construcción del contexto estructurado para inyectar a Gemini.
 *
 * Capas de contexto (de permanente a más reciente):
 * 1. Perfil clínico — siempre completo, nunca se trunca
 * 2. Métricas — solo los últimos 7 días
 * 3. Alacena — filtrada por tipo de comida (mealType) de la query cuando sea posible
 * 4. Historial de recomendaciones — solo los últimos 7 días
 * 5. Resumen nutricional — agregado de los últimos 14 días (no raw)
 */
class BuildContextUseCase {
    fun build(params: ContextParams): String =
        buildString {
            appendLine("=== CONTEXTO DEL SISTEMA ===")
            appendLine()

            // ── Capa 1: Perfil (siempre completo) ──────────────────────────────
            appendLine("── PERFIL DEL USUARIO ──")
            with(params.user) {
                appendLine("Nombre: $name | Objetivo: ${goal.name} | Actividad: ${activityLevel.name}")
                appendLine("Altura: ${heightCm}cm | Sexo: ${sex.name}")
            }
            appendLine()

            // ── Capa 1b: Restricciones clínicas ────────────────────────────────
            params.clinicalProfile?.let { clinical ->
                appendLine("── PERFIL CLÍNICO ──")
                appendClinicalValues(clinical)
                appendLine()
                appendLine("── RESTRICCIONES ACTIVAS ──")
                appendClinicalConstraints(clinical)
                appendLine()
            }

            // ── Capa 2: Métricas recientes (ventana de 7 días) ─────────────────
            if (params.recentMetrics.isNotEmpty()) {
                appendLine("── MÉTRICAS RECIENTES (últimos 7 días) ──")
                params.recentMetrics.take(7).forEach { metrics ->
                    append("${metrics.date}: ")
                    metrics.weightKg?.let { append("Peso ${it}kg ") }
                    metrics.sleepHours?.let { append("| Sueño ${it}h ") }
                    metrics.hrv?.let { hrv ->
                        val alert = if (hrv < 50) " ⚠ VFC BAJA" else ""
                        append("| VFC ${hrv}ms$alert ")
                    }
                    metrics.caloriesBurned?.let { append("| Calorías ${it}kcal ") }
                    appendLine()
                }
                appendLine()
            }

            // ── Capa 3: Alacena (filtrada por contexto de la query) ────────────
            appendLine("── ALACENA DISPONIBLE ──")
            if (params.pantryItems.isEmpty()) {
                appendLine("Sin items registrados.")
            } else {
                // Agrupar por ubicación
                params.pantryItems
                    .groupBy { it.locationId }
                    .forEach { (locationId, items) ->
                        val locationName = params.locationNames[locationId] ?: locationId
                        appendLine("$locationName:")
                        items.forEach { item ->
                            val lowStockWarning = if (item.quantity <= 1) " ⚠ STOCK BAJO" else ""
                            appendLine("  - ${item.ingredient}: ${item.quantity} ${item.unit.name}$lowStockWarning")
                        }
                    }
            }
            appendLine()

            // ── Capa 4: Contexto del día actual ────────────────────────────────
            appendLine("── HOY ──")
            appendLine("Fecha: ${params.today}")
            appendLine("Contexto laboral: ${params.workContext}")
            if (params.todayFoodLogs.isNotEmpty()) {
                appendLine("Ya consumido hoy:")
                params.todayFoodLogs.forEach { log ->
                    appendLine("  - ${log.mealType.name}: ${log.foodName}")
                }
            }
            appendLine()

            // ── Capa 5: Recomendaciones recientes (evitar repetición) ──────────
            if (params.recentRecommendations.isNotEmpty()) {
                appendLine("── RECOMENDACIONES RECIENTES (últimos 7 días) ──")
                appendLine("NO repetir estos platos:")
                params.recentRecommendations.take(14).forEach { rec ->
                    appendLine("  - ${rec.recommendedAt.take(10)}: ${rec.title}")
                }
                appendLine()
            }

            // ── Capa 6: Alertas activas ─────────────────────────────────────────
            val alerts = buildAlerts(params)
            if (alerts.isNotEmpty()) {
                appendLine("── ⚠ ALERTAS ACTIVAS ──")
                alerts.forEach { appendLine("  • $it") }
                appendLine()
            }

            appendLine("=== FIN CONTEXTO ===")
        }

    private fun StringBuilder.appendClinicalValues(clinical: ClinicalProfile) {
        clinical.cholesterolTotal?.let { appendLine("Colesterol total: $it mg/dL") }
        clinical.hdl?.let { appendLine("HDL: $it mg/dL") }
        clinical.ldl?.let { appendLine("LDL: $it mg/dL") }
        clinical.triglycerides?.let { appendLine("Triglicéridos: $it mg/dL") }
        clinical.fastingGlucose?.let { appendLine("Glucosa en ayunas: $it mg/dL") }
        clinical.hba1c?.let { appendLine("HbA1c: $it%") }
        clinical.systolicPressure?.let { sys ->
            clinical.diastolicPressure?.let { dia -> appendLine("Presión: $sys/$dia mmHg") }
        }
    }

    private fun StringBuilder.appendClinicalConstraints(clinical: ClinicalProfile) {
        if (clinical.hasDyslipidemia) {
            appendLine("DISLIPIDEMIA → Evitar: mantequilla, embutidos, frituras.")
            appendLine("  Limitar: carnes rojas, yema de huevo.")
            appendLine("  Priorizar: avena, aguacate, nueces, aceite de oliva, pescado azul.")
        }
        if (clinical.hasHyperglycemia) {
            appendLine("GLUCOSA ELEVADA → Evitar: azúcar, harinas refinadas, bebidas azucaradas.")
            appendLine("  Limitar: arroz blanco, pan blanco, papa.")
            appendLine("  Priorizar: vegetales verdes, proteína magra, fibra.")
        }
        if (clinical.hasHypertension) {
            appendLine("HIPERTENSIÓN → Evitar: exceso de sal, embutidos, enlatados.")
            appendLine("  Priorizar: potasio, magnesio, vegetales, frutas.")
        }
        if (!clinical.hasDyslipidemia && !clinical.hasHyperglycemia && !clinical.hasHypertension) {
            appendLine("Sin restricciones clínicas activas.")
        }
    }

    private fun buildAlerts(params: ContextParams): List<String> {
        val alerts = mutableListOf<String>()
        val latestMetrics = params.recentMetrics.firstOrNull()

        latestMetrics?.sleepHours?.let { sleep ->
            if (sleep < 6f) alerts.add("Sueño insuficiente (${sleep}h) — evitar cafeína después de las 14:00.")
        }
        latestMetrics?.hrv?.let { hrv ->
            if (hrv <
                45f
            ) {
                alerts.add(
                    "VFC baja (${hrv}ms) — sistema nervioso bajo estrés. Priorizar alimentos antiinflamatorios.",
                )
            }
        }
        params.clinicalProfile?.let { clinical ->
            if (clinical.hasHyperglycemia) alerts.add("Glucosa elevada — evitar carbohidratos simples hoy.")
        }

        return alerts
    }
}

data class ContextParams(
    val user: User,
    val clinicalProfile: ClinicalProfile?,
    val recentMetrics: List<HealthMetrics>, // Últimos 7 días
    val pantryItems: List<PantryItem>,
    val locationNames: Map<String, String>, // locationId → "Casa", "Trabajo"
    val recentRecommendations: List<Recommendation>, // Últimos 7 días
    val todayFoodLogs: List<FoodLog>,
    val today: String, // ISO-8601: "2026-02-27"
    val workContext: String, // "Oficina", "Remoto", "Sin especificar"
    val mealTypeHint: MealType? = null, // Para filtrar alacena si se detecta
)
