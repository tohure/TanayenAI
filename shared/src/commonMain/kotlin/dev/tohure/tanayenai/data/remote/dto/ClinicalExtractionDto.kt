package dev.tohure.tanayenai.data.remote.dto

import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
data class ClinicalExtractionDto(
    val cholesterol_total: Float? = null,
    val hdl: Float? = null,
    val ldl: Float? = null,
    val triglycerides: Float? = null,
    val vldl: Float? = null,
    val fasting_glucose: Float? = null,
    val hba1c: Float? = null,
    val fasting_insulin: Float? = null,
    val homa_ir: Float? = null,
    val creatinine: Float? = null,
    val urea: Float? = null,
    val gfr: Float? = null,
    val uric_acid: Float? = null,
    val alt: Float? = null,
    val ast: Float? = null,
    val ggt: Float? = null,
    val total_bilirubin: Float? = null,
    val tsh: Float? = null,
    val t3_free: Float? = null,
    val t4_free: Float? = null,
    val hemoglobin: Float? = null,
    val hematocrit: Float? = null,
    val ferritin: Float? = null,
    val serum_iron: Float? = null,
    val transferrin_saturation: Float? = null,
    val vitamin_d: Float? = null,
    val vitamin_b12: Float? = null,
    val folate: Float? = null,
    val zinc: Float? = null,
    val magnesium: Float? = null,
    val crp_ultra_sensitive: Float? = null,
    val homocysteine: Float? = null,
    val systolic_pressure: Int? = null,
    val diastolic_pressure: Int? = null,
)

fun ClinicalExtractionDto.toClinicalProfile(userId: String) =
    ClinicalProfile(
        userId = userId,
        cholesterolTotal = cholesterol_total,
        hdl = hdl,
        ldl = ldl,
        triglycerides = triglycerides,
        vldl = vldl,
        fastingGlucose = fasting_glucose,
        hba1c = hba1c,
        fastingInsulin = fasting_insulin,
        homaIr = homa_ir,
        creatinine = creatinine,
        urea = urea,
        gfr = gfr,
        uricAcid = uric_acid,
        alt = alt,
        ast = ast,
        ggt = ggt,
        totalBilirubin = total_bilirubin,
        tsh = tsh,
        t3Free = t3_free,
        t4Free = t4_free,
        hemoglobin = hemoglobin,
        hematocrit = hematocrit,
        ferritin = ferritin,
        serumIron = serum_iron,
        transferrinSaturation = transferrin_saturation,
        vitaminD = vitamin_d,
        vitaminB12 = vitamin_b12,
        folate = folate,
        zinc = zinc,
        magnesium = magnesium,
        crpUltraSensitive = crp_ultra_sensitive,
        homocysteine = homocysteine,
        systolicPressure = systolic_pressure,
        diastolicPressure = diastolic_pressure,
        recordedAt = currentIsoDateTime(),
    )

fun buildClinicalSummaryFromJson(rawJson: String): String {
    val fieldNames =
        mapOf(
            "fasting_glucose" to "Glucosa",
            "systolic_pressure" to "Presión sistólica",
            "diastolic_pressure" to "Presión diastólica",
            "cholesterol_total" to "Colesterol total",
            "hdl" to "HDL",
            "ldl" to "LDL",
            "triglycerides" to "Triglicéridos",
            "hba1c" to "HbA1c",
            "vitamin_d" to "Vitamina D",
            "uric_acid" to "Ácido úrico",
            "tsh" to "TSH",
            "ferritin" to "Ferritina",
            "hemoglobin" to "Hemoglobina",
            "crp_ultra_sensitive" to "PCR",
            "creatinine" to "Creatinina",
            "vitamin_b12" to "Vitamina B12",
            "homa_ir" to "HOMA-IR",
        )
    return try {
        val parsed = Json.parseToJsonElement(rawJson).jsonObject
        parsed.entries
            .mapNotNull { (key, value) -> fieldNames[key]?.let { "$it: $value" } }
            .joinToString(", ")
    } catch (e: Exception) {
        ""
    }
}
