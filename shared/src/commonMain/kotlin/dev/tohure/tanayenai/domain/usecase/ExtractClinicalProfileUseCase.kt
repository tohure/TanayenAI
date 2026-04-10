package dev.tohure.tanayenai.domain.usecase

import co.touchlab.kermit.Logger
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.tohure.tanayenai.data.remote.dto.ClinicalExtractionDto
import dev.tohure.tanayenai.data.remote.dto.toClinicalProfile
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val log = Logger.withTag("ExtractClinicalProfileUseCase")
private val json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

@OptIn(ExperimentalEncodingApi::class)
class ExtractClinicalProfileUseCase(
    private val generativeModel: GenerativeModel,
    private val repository: ClinicalProfileRepository,
    private val userId: String,
) {
    // ── Modo 1: PDF (renderizado como imágenes JPEG por página) ──────────────
    suspend fun extractFromPdf(pageImages: List<String>): ExtractionResult =
        withContext(Dispatchers.Default) {
            try {
                val raw =
                    generativeModel
                        .generateContent(
                            content {
                                pageImages.forEach { b64 ->
                                    val bytes = Base64.decode(b64.replace("\\s".toRegex(), ""))
                                    image(bytes)
                                }
                                text(extractionPrompt())
                            },
                        ).text ?: throw Exception("Sin respuesta")
                processRawResponse(raw)
            } catch (e: Exception) {
                log.e(e) { "PDF extraction failed" }
                ExtractionResult.Error(e.message ?: "Error desconocido")
            }
        }

    // ── Modo 2 y 3: foto y texto via chat ─────────────────────────────────────
    // Gemini ya extrajo el JSON en el tag [CLINICAL:...] — solo guardamos
    suspend fun saveFromJson(rawJson: String): ExtractionResult = processRawResponse(rawJson)

    // ── Modo 4: valor individual del formulario manual ────────────────────────
    suspend fun saveIndividualValue(
        field: ClinicalField,
        value: Float,
    ): ExtractionResult =
        try {
            val existing =
                repository.getClinicalProfile(userId)
                    ?: ClinicalProfile(userId = userId)
            val updated =
                field
                    .applyTo(existing, value)
                    .copy(recordedAt = currentIsoDateTime())
            repository.saveClinicalProfile(updated)
            ExtractionResult.Success(updated)
        } catch (e: Exception) {
            ExtractionResult.Error(e.message ?: "Error")
        }

    // ── Lógica compartida ─────────────────────────────────────────────────────
    private suspend fun processRawResponse(raw: String): ExtractionResult {
        val cleaned =
            raw
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()

        return try {
            val dto = json.decodeFromString<ClinicalExtractionDto>(cleaned)
            val profile = dto.toClinicalProfile(userId)
            val existing = repository.getClinicalProfile(userId)
            val merged = merge(existing, profile)
            repository.saveClinicalProfile(merged)
            log.d { "Saved clinical profile. Restrictions: ${merged.activeRestrictions.size}" }
            ExtractionResult.Success(merged)
        } catch (e: Exception) {
            log.e(e) { "Failed to parse: $cleaned" }
            ExtractionResult.ParseError(cleaned)
        }
    }

    private fun merge(
        existing: ClinicalProfile?,
        new: ClinicalProfile,
    ): ClinicalProfile {
        if (existing == null) return new
        return new.copy(
            cholesterolTotal = new.cholesterolTotal ?: existing.cholesterolTotal,
            hdl = new.hdl ?: existing.hdl,
            ldl = new.ldl ?: existing.ldl,
            triglycerides = new.triglycerides ?: existing.triglycerides,
            vldl = new.vldl ?: existing.vldl,
            fastingGlucose = new.fastingGlucose ?: existing.fastingGlucose,
            hba1c = new.hba1c ?: existing.hba1c,
            fastingInsulin = new.fastingInsulin ?: existing.fastingInsulin,
            homaIr = new.homaIr ?: existing.homaIr,
            creatinine = new.creatinine ?: existing.creatinine,
            urea = new.urea ?: existing.urea,
            gfr = new.gfr ?: existing.gfr,
            uricAcid = new.uricAcid ?: existing.uricAcid,
            alt = new.alt ?: existing.alt,
            ast = new.ast ?: existing.ast,
            ggt = new.ggt ?: existing.ggt,
            totalBilirubin = new.totalBilirubin ?: existing.totalBilirubin,
            tsh = new.tsh ?: existing.tsh,
            t3Free = new.t3Free ?: existing.t3Free,
            t4Free = new.t4Free ?: existing.t4Free,
            hemoglobin = new.hemoglobin ?: existing.hemoglobin,
            hematocrit = new.hematocrit ?: existing.hematocrit,
            ferritin = new.ferritin ?: existing.ferritin,
            serumIron = new.serumIron ?: existing.serumIron,
            transferrinSaturation = new.transferrinSaturation ?: existing.transferrinSaturation,
            vitaminD = new.vitaminD ?: existing.vitaminD,
            vitaminB12 = new.vitaminB12 ?: existing.vitaminB12,
            folate = new.folate ?: existing.folate,
            zinc = new.zinc ?: existing.zinc,
            magnesium = new.magnesium ?: existing.magnesium,
            crpUltraSensitive = new.crpUltraSensitive ?: existing.crpUltraSensitive,
            homocysteine = new.homocysteine ?: existing.homocysteine,
            systolicPressure = new.systolicPressure ?: existing.systolicPressure,
            diastolicPressure = new.diastolicPressure ?: existing.diastolicPressure,
        )
    }

    private fun extractionPrompt() =
        """
        Este es un resultado de análisis de laboratorio clínico (puede ser una imagen de documento o varias páginas).
        Busca en TODAS las filas, columnas y tablas de cada página. Los valores suelen aparecer como: nombre del análisis, valor numérico y unidad.
        Extrae TODOS los valores que encuentres y conviértelos a las unidades indicadas.
        Devuelve SOLO el siguiente JSON, sin texto adicional, sin markdown:
        {
          "cholesterol_total": número en mg/dL o null,
          "hdl": número en mg/dL o null,
          "ldl": número en mg/dL o null,
          "triglycerides": número en mg/dL o null,
          "vldl": número en mg/dL o null,
          "fasting_glucose": número en mg/dL o null,
          "hba1c": número en % o null,
          "fasting_insulin": número en µU/mL o null,
          "homa_ir": número sin unidad o null,
          "creatinine": número en mg/dL o null,
          "urea": número en mg/dL o null,
          "gfr": número en mL/min/1.73m² o null,
          "uric_acid": número en mg/dL o null,
          "alt": número en U/L o null,
          "ast": número en U/L o null,
          "ggt": número en U/L o null,
          "total_bilirubin": número en mg/dL o null,
          "tsh": número en µU/mL o null,
          "t3_free": número en pg/mL o null,
          "t4_free": número en ng/dL o null,
          "hemoglobin": número en g/dL o null,
          "hematocrit": número en % o null,
          "ferritin": número en ng/mL o null,
          "serum_iron": número en µg/dL o null,
          "transferrin_saturation": número en % o null,
          "vitamin_d": número en ng/mL o null,
          "vitamin_b12": número en pg/mL o null,
          "folate": número en ng/mL o null,
          "zinc": número en µg/dL o null,
          "magnesium": número en mg/dL o null,
          "crp_ultra_sensitive": número en mg/L o null,
          "homocysteine": número en µmol/L o null,
          "systolic_pressure": número entero en mmHg o null,
          "diastolic_pressure": número entero en mmHg o null
        }
        Reglas: no inventes valores, usa null si no está presente, si hay varios resultados del mismo análisis usa el más reciente.
        """.trimIndent()
}

sealed class ExtractionResult {
    data class Success(
        val profile: ClinicalProfile,
    ) : ExtractionResult()

    data class ParseError(
        val rawResponse: String,
    ) : ExtractionResult()

    data class Error(
        val message: String,
    ) : ExtractionResult()
}

// Enum para entrada manual — campos más comunes del formulario
enum class ClinicalField(
    val displayName: String,
    val unit: String,
) {
    FASTING_GLUCOSE("Glucosa en ayunas", "mg/dL"),
    SYSTOLIC_PRESSURE("Presión sistólica", "mmHg"),
    DIASTOLIC_PRESSURE("Presión diastólica", "mmHg"),
    CHOLESTEROL_TOTAL("Colesterol total", "mg/dL"),
    HDL("HDL", "mg/dL"),
    LDL("LDL", "mg/dL"),
    TRIGLYCERIDES("Triglicéridos", "mg/dL"),
    HBA1C("HbA1c", "%"),
    TSH("TSH", "µU/mL"),
    VITAMIN_D("Vitamina D", "ng/mL"),
    URIC_ACID("Ácido úrico", "mg/dL"),
    FERRITIN("Ferritina", "ng/mL"),
    HEMOGLOBIN("Hemoglobina", "g/dL"),
    CRP("PCR ultrasensible", "mg/L"),
    CREATININE("Creatinina", "mg/dL"),
    ;

    fun applyTo(
        profile: ClinicalProfile,
        value: Float,
    ): ClinicalProfile =
        when (this) {
            FASTING_GLUCOSE -> profile.copy(fastingGlucose = value)
            SYSTOLIC_PRESSURE -> profile.copy(systolicPressure = value.toInt())
            DIASTOLIC_PRESSURE -> profile.copy(diastolicPressure = value.toInt())
            CHOLESTEROL_TOTAL -> profile.copy(cholesterolTotal = value)
            HDL -> profile.copy(hdl = value)
            LDL -> profile.copy(ldl = value)
            TRIGLYCERIDES -> profile.copy(triglycerides = value)
            HBA1C -> profile.copy(hba1c = value)
            TSH -> profile.copy(tsh = value)
            VITAMIN_D -> profile.copy(vitaminD = value)
            URIC_ACID -> profile.copy(uricAcid = value)
            FERRITIN -> profile.copy(ferritin = value)
            HEMOGLOBIN -> profile.copy(hemoglobin = value)
            CRP -> profile.copy(crpUltraSensitive = value)
            CREATININE -> profile.copy(creatinine = value)
        }
}
