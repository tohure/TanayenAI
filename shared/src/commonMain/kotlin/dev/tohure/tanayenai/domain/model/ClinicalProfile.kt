package dev.tohure.tanayenai.domain.model

data class ClinicalProfile(
    val userId: String,
    // ── Perfil lipídico ───────────────────────────────────────────────────────
    val cholesterolTotal: Float? = null, // mg/dL — alto >200
    val hdl: Float? = null, // mg/dL — bajo <40H / <50M
    val ldl: Float? = null, // mg/dL — alto >130
    val triglycerides: Float? = null, // mg/dL — alto >150, muy alto >500
    val vldl: Float? = null, // mg/dL — calculado: triglicéridos/5
    // ── Metabolismo de glucosa ────────────────────────────────────────────────
    val fastingGlucose: Float? = null, // mg/dL — normal <100, prediabetes 100-125
    val hba1c: Float? = null, // % — normal <5.7, prediabetes 5.7-6.4
    val fastingInsulin: Float? = null, // µU/mL — resistencia si >10 con glucosa normal
    val homaIr: Float? = null, // HOMA-IR = (glucosa × insulina) / 405
    // ── Función renal ─────────────────────────────────────────────────────────
    val creatinine: Float? = null, // mg/dL — alto >1.2H / >1.0M
    val urea: Float? = null, // mg/dL — alto >45
    val gfr: Float? = null, // mL/min/1.73m² — <60 = daño renal
    val uricAcid: Float? = null, // mg/dL — alto >7H / >6M → gota
    // ── Función hepática ──────────────────────────────────────────────────────
    val alt: Float? = null, // U/L — ALT/TGP, alto >40
    val ast: Float? = null, // U/L — AST/TGO, alto >40
    val ggt: Float? = null, // U/L — sensible a alcohol y esteatosis
    val totalBilirubin: Float? = null, // mg/dL — alto >1.2
    // ── Tiroides ─────────────────────────────────────────────────────────────
    val tsh: Float? = null, // µU/mL — hipotiroidismo >4.5
    val t3Free: Float? = null, // pg/mL
    val t4Free: Float? = null, // ng/dL
    // ── Hemograma / Hierro ────────────────────────────────────────────────────
    val hemoglobin: Float? = null, // g/dL — anemia <13H / <12M
    val hematocrit: Float? = null, // %
    val ferritin: Float? = null, // ng/mL — reservas hierro, bajo <30
    val serumIron: Float? = null, // µg/dL
    val transferrinSaturation: Float? = null, // %
    // ── Vitaminas y minerales ─────────────────────────────────────────────────
    val vitaminD: Float? = null, // ng/mL — deficiencia <20
    val vitaminB12: Float? = null, // pg/mL — deficiencia <200
    val folate: Float? = null, // ng/mL — deficiencia <3
    val zinc: Float? = null, // µg/dL — deficiencia <70
    val magnesium: Float? = null, // mg/dL — deficiencia <1.7
    // ── Inflamación y riesgo cardiovascular ───────────────────────────────────
    val crpUltraSensitive: Float? = null, // mg/L — PCR-us, alto >3
    val homocysteine: Float? = null, // µmol/L — alto >15
    // ── Presión arterial ──────────────────────────────────────────────────────
    val systolicPressure: Int? = null, // mmHg — elevada >130
    val diastolicPressure: Int? = null, // mmHg — elevada >80
    val recordedAt: String? = null,
) {
    val hasDyslipidemia: Boolean get() =
        (cholesterolTotal ?: 0f) > 200f || (ldl ?: 0f) > 130f ||
            (hdl ?: 999f) < 40f || (triglycerides ?: 0f) > 150f

    val hasHyperglycemia: Boolean get() =
        (fastingGlucose ?: 0f) > 100f || (hba1c ?: 0f) > 5.7f

    val hasInsulinResistance: Boolean get() =
        (homaIr ?: 0f) > 2.5f ||
            ((fastingInsulin ?: 0f) > 10f && (fastingGlucose ?: 0f) > 95f)

    val hasHypertension: Boolean get() =
        (systolicPressure ?: 0) > 130 || (diastolicPressure ?: 0) > 80

    val hasHyperuricemia: Boolean get() = (uricAcid ?: 0f) > 7f

    val hasRenalImpairment: Boolean get() =
        (gfr ?: 999f) < 60f || (creatinine ?: 0f) > 1.3f

    val hasHepaticAlteration: Boolean get() =
        (alt ?: 0f) > 40f || (ast ?: 0f) > 40f || (ggt ?: 0f) > 50f

    val hasHypothyroidism: Boolean get() = (tsh ?: 0f) > 4.5f

    val hasAnemia: Boolean get() = (hemoglobin ?: 999f) < 13f

    val hasVitaminDDeficiency: Boolean get() = (vitaminD ?: 999f) < 30f

    val hasB12Deficiency: Boolean get() = (vitaminB12 ?: 999f) < 200f

    val hasElevatedInflammation: Boolean get() =
        (crpUltraSensitive ?: 0f) > 3f || (homocysteine ?: 0f) > 15f

    val activeRestrictions: List<String> =
        buildList {
            if (hasDyslipidemia) add("Dislipidemia: limitar grasas saturadas y trans, colesterol dietético")
            if (hasHyperglycemia) {
                add(
                    "Hiperglucemia/prediabetes: priorizar bajo índice glucémico, limitar azúcares simples",
                )
            }
            if (hasInsulinResistance) add("Resistencia insulínica: espaciar comidas, evitar picos de glucosa")
            if (hasHypertension) add("Hipertensión: dieta DASH, limitar sodio <2300mg/día, aumentar potasio")
            if (hasHyperuricemia) {
                add(
                    "Hiperuricemia/gota: evitar purinas (vísceras, mariscos, embutidos), limitar fructosa",
                )
            }
            if (hasRenalImpairment) add("Deterioro renal: controlar proteínas, fósforo y potasio")
            if (hasHepaticAlteration) add("Alteración hepática: evitar alcohol, limitar grasas saturadas")
            if (hasHypothyroidism) add("Hipotiroidismo: asegurar yodo y selenio adecuados")
            if (hasAnemia) add("Anemia: priorizar hierro hemo, vitamina C para absorción")
            if (hasVitaminDDeficiency) add("Déficit vitamina D: aumentar exposición solar y fuentes dietéticas")
            if (hasB12Deficiency) add("Déficit B12: priorizar proteína animal o suplementación")
            if (hasElevatedInflammation) add("Inflamación elevada: dieta antiinflamatoria, omega-3, antioxidantes")
        }
}
