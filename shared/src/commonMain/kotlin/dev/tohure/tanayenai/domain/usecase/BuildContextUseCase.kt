package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.FoodLog
import dev.tohure.tanayenai.domain.model.HealthMetrics
import dev.tohure.tanayenai.domain.model.MealType
import dev.tohure.tanayenai.domain.model.NutritionGoal
import dev.tohure.tanayenai.domain.model.PantryItem
import dev.tohure.tanayenai.domain.model.Recommendation
import dev.tohure.tanayenai.domain.model.RecommendationType
import dev.tohure.tanayenai.domain.model.User
import dev.tohure.tanayenai.domain.model.displayName

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

            // ── Capa 0: Meta del usuario (guía todo lo demás) ──────────────────
            if (!params.userExistsInDb) {
                appendLine("── ⚠ META NO DEFINIDA ──")
                appendLine(
                    "El usuario NO ha establecido su meta todavía. " +
                        "ANTES de responder cualquier otra cosa, pregúntale por su meta de salud. " +
                        "Presenta las opciones numeradas y espera su elección.",
                )
                appendLine()
            } else {
                appendLine("── META DEL USUARIO ──")
                appendLine("Meta actual: ${params.user.goal.displayName} (${params.user.goal.name})")
                appendLine("TODAS las recomendaciones deben estar orientadas a esta meta.")
                appendLine()
            }

            // ── Capa 1: Perfil (siempre completo) ──────────────────────────────
            appendLine("── PERFIL DEL USUARIO ──")
            with(params.user) {
                appendLine("Nombre: $name | Actividad: ${activityLevel.name}")
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
                        append("| VFC ${hrv}ms ${hrv.toHrvLabel()} ")
                    }
                    metrics.caloriesBurned?.let { append("| Calorías ${it}kcal ") }
                    metrics.steps?.let { append("| Pasos $it ") }
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
                appendLine("Lo que ha consumido hoy:")
                params.todayFoodLogs.forEach { log ->
                    val rawTime = log.loggedAt.substringAfter("T", "")
                    val time = if (rawTime.length >= 5) rawTime.take(5) else "?"
                    appendLine(
                        "  - $time ${log.mealType.displayName}: ${log.foodName} " +
                            "(${log.calories.toInt()} kcal | " +
                            "P:${log.proteinG.toInt()}g C:${log.carbsG.toInt()}g G:${log.fatG.toInt()}g)",
                    )
                }
                val totalCals = params.todayFoodLogs.sumOf { it.calories.toDouble() }.toInt()
                appendLine("  Total: $totalCals kcal consumidas hoy")
            }
            appendLine()

            // ── Capa 5: Memoria de sesiones anteriores ─────────────────────────
            val memorySection = formatRecommendationsAsMemory(params.recentRecommendations, params.today)
            if (memorySection.isNotEmpty()) {
                append(memorySection)
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
        clinical.homaIr?.let { appendLine("HOMA-IR: $it") }
        clinical.systolicPressure?.let { sys ->
            clinical.diastolicPressure?.let { dia -> appendLine("Presión: $sys/$dia mmHg") }
        }
        clinical.uricAcid?.let { appendLine("Ácido úrico: $it mg/dL") }
        clinical.creatinine?.let { appendLine("Creatinina: $it mg/dL") }
        clinical.gfr?.let { appendLine("FG estimado: ${it.toInt()} mL/min") }
        clinical.tsh?.let { appendLine("TSH: $it µU/mL") }
        clinical.hemoglobin?.let { appendLine("Hemoglobina: $it g/dL") }
        clinical.vitaminD?.let { appendLine("Vitamina D: $it ng/mL") }
        clinical.vitaminB12?.let { appendLine("Vitamina B12: $it pg/mL") }
        clinical.crpUltraSensitive?.let { appendLine("PCR ultrasensible: $it mg/L") }
    }

    private fun StringBuilder.appendClinicalConstraints(clinical: ClinicalProfile) {
        val restrictions = clinical.activeRestrictions
        if (restrictions.isEmpty()) {
            appendLine("Sin restricciones clínicas activas.")
        } else {
            restrictions.forEach { appendLine("• $it") }
        }
    }

    private fun formatRecommendationsAsMemory(
        recommendations: List<Recommendation>,
        today: String,
    ): String {
        if (recommendations.isEmpty()) return ""

        val todayRecs =
            recommendations
                .filter { it.recommendedAt.startsWith(today) }
                .sortedByDescending { it.recommendedAt }
        val pastRecs =
            recommendations
                .filter { !it.recommendedAt.startsWith(today) }
                .sortedByDescending { it.recommendedAt }
                .take(5)

        return buildString {
            appendLine("── MEMORIA DE SESIONES ANTERIORES ──")
            appendLine("Úsala para dar seguimiento natural — conecta, no repitas.")

            if (todayRecs.isNotEmpty()) {
                appendLine("Hoy:")
                todayRecs.forEach { rec ->
                    val time = rec.recommendedAt.substringAfter("T", "").take(5)
                    val label = rec.type.toMealLabel()
                    append("  - $time $label: ${rec.title}")
                    if (rec.ingredientsUsed.isNotEmpty()) {
                        append(" (${rec.ingredientsUsed.joinToString(", ")})")
                    }
                    appendLine()
                }
            }

            if (pastRecs.isNotEmpty()) {
                appendLine("Días anteriores:")
                pastRecs.forEach { rec ->
                    val date = rec.recommendedAt.take(10)
                    val time = rec.recommendedAt.substringAfter("T", "").take(5)
                    appendLine("  - $date $time: ${rec.title}")
                }
            }

            appendLine()
        }
    }

    private fun RecommendationType.toMealLabel(): String =
        when (this) {
            RecommendationType.MEAL -> "Comida"
            RecommendationType.SNACK -> "Snack"
            RecommendationType.RECIPE -> "Receta"
            RecommendationType.PLAN -> "Plan"
            RecommendationType.ALERT -> "Alerta"
        }

    private fun Float.toHrvLabel(): String =
        when {
            this >= 60 -> "(buena)"
            this >= 40 -> "(moderada)"
            else -> "(baja — priorizar recuperación)"
        }

    private fun buildAlerts(params: ContextParams): List<String> {
        val alerts = mutableListOf<String>()
        val latestMetrics = params.recentMetrics.firstOrNull()

        latestMetrics?.sleepHours?.let { sleep ->
            if (sleep < 6f) alerts.add("Sueño insuficiente (${sleep}h) — evitar cafeína después de las 14:00.")
        }
        latestMetrics?.hrv?.let { hrv ->
            when {
                hrv < 30f -> {
                    alerts.add(
                        "VFC MUY BAJA (${hrv}ms) — estrés severo. APLICA PROTOCOLO DE ESTRÉS ALTO. " +
                            "Prioriza hidratación y descanso. No recomiendes comidas pesadas.",
                    )
                }

                hrv < 45f -> {
                    alerts.add(
                        "VFC baja (${hrv}ms) — estrés moderado-alto. APLICA PROTOCOLO DE ESTRÉS ALTO. " +
                            "Sugiere intervención no alimentaria antes que comida.",
                    )
                }
            }
        }
        params.clinicalProfile?.let { clinical ->
            if (clinical.hasHyperglycemia) alerts.add("Glucosa elevada — evitar carbohidratos simples hoy.")

            // Conflictos meta vs. clínica — Gemini debe advertir si el usuario quiere cambiar de meta
            detectGoalConflicts(params.user.goal, clinical, alerts)
        }

        return alerts
    }

    private fun detectGoalConflicts(
        goal: NutritionGoal,
        clinical: ClinicalProfile,
        alerts: MutableList<String>,
    ) {
        when (goal) {
            NutritionGoal.GAIN_MUSCLE -> {
                clinical.hemoglobin?.let { hb ->
                    if (hb < 12f) {
                        alerts.add(
                            "CONFLICTO DE META: El usuario quiere ganar músculo pero tiene hemoglobina baja " +
                                "($hb g/dL). Advertir que sin corregir la anemia primero, ganar músculo será " +
                                "difícil y arriesgado. Sugerir priorizar IMPROVE_ANEMIA.",
                        )
                    }
                }
                clinical.ferritin?.let { fe ->
                    if (fe < 20f) {
                        alerts.add(
                            "CONFLICTO DE META: Ferritina baja ($fe ng/mL) limita la recuperación muscular. " +
                                "Recomendar suplementación de hierro antes de intensificar entrenamiento.",
                        )
                    }
                }
            }

            NutritionGoal.LOSE_WEIGHT -> {
                clinical.hemoglobin?.let { hb ->
                    if (hb < 12f) {
                        alerts.add(
                            "PRECAUCIÓN: Déficit calórico agresivo con hemoglobina baja ($hb g/dL) puede " +
                                "empeorar la anemia. Recomendar pérdida de peso gradual (≤500 kcal/día de déficit).",
                        )
                    }
                }
            }

            NutritionGoal.CONTROL_GLUCOSE -> {
                clinical.hba1c?.let { hba1c ->
                    if (hba1c >= 6.5f) {
                        alerts.add(
                            "HbA1c elevada ($hba1c%) — nivel de diabetes confirmado. " +
                                "Esta meta es crítica. Todas las recomendaciones deben priorizar índice glucémico bajo.",
                        )
                    }
                }
            }

            else -> { /* Sin conflictos conocidos para las demás metas */ }
        }
    }
}

data class ContextParams(
    val user: User,
    val userExistsInDb: Boolean = true, // false = primera vez, meta no definida aún
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
