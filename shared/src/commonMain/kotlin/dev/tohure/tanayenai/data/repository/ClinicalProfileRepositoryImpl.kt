package dev.tohure.tanayenai.data.repository

import dev.tohure.tanayenai.db.TanayenDatabase
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.model.currentIsoDateTime
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import db.ClinicalProfile as DbClinicalProfile

class ClinicalProfileRepositoryImpl(
    private val database: TanayenDatabase,
) : ClinicalProfileRepository {
    private val queries = database.userProfileQueries

    override suspend fun getClinicalProfile(userId: String): ClinicalProfile? =
        withContext(Dispatchers.Default) {
            queries.getClinicalProfile(userId).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun saveClinicalProfile(profile: ClinicalProfile): Unit =
        withContext(Dispatchers.Default) {
            queries.upsertClinicalProfile(
                userId = profile.userId,
                cholesterolTotal = profile.cholesterolTotal?.toDouble(),
                hdl = profile.hdl?.toDouble(),
                ldl = profile.ldl?.toDouble(),
                triglycerides = profile.triglycerides?.toDouble(),
                vldl = profile.vldl?.toDouble(),
                fastingGlucose = profile.fastingGlucose?.toDouble(),
                hba1c = profile.hba1c?.toDouble(),
                fastingInsulin = profile.fastingInsulin?.toDouble(),
                homaIr = profile.homaIr?.toDouble(),
                creatinine = profile.creatinine?.toDouble(),
                urea = profile.urea?.toDouble(),
                gfr = profile.gfr?.toDouble(),
                uricAcid = profile.uricAcid?.toDouble(),
                alt = profile.alt?.toDouble(),
                ast = profile.ast?.toDouble(),
                ggt = profile.ggt?.toDouble(),
                totalBilirubin = profile.totalBilirubin?.toDouble(),
                tsh = profile.tsh?.toDouble(),
                t3Free = profile.t3Free?.toDouble(),
                t4Free = profile.t4Free?.toDouble(),
                hemoglobin = profile.hemoglobin?.toDouble(),
                hematocrit = profile.hematocrit?.toDouble(),
                ferritin = profile.ferritin?.toDouble(),
                serumIron = profile.serumIron?.toDouble(),
                transferrinSaturation = profile.transferrinSaturation?.toDouble(),
                vitaminD = profile.vitaminD?.toDouble(),
                vitaminB12 = profile.vitaminB12?.toDouble(),
                folate = profile.folate?.toDouble(),
                zinc = profile.zinc?.toDouble(),
                magnesium = profile.magnesium?.toDouble(),
                crpUltraSensitive = profile.crpUltraSensitive?.toDouble(),
                homocysteine = profile.homocysteine?.toDouble(),
                systolicPressure = profile.systolicPressure?.toLong(),
                diastolicPressure = profile.diastolicPressure?.toLong(),
                recordedAt = profile.recordedAt ?: currentIsoDateTime(),
            )
        }

    private fun DbClinicalProfile.toDomain() =
        ClinicalProfile(
            userId = user_id,
            cholesterolTotal = cholesterol_total?.toFloat(),
            hdl = hdl?.toFloat(),
            ldl = ldl?.toFloat(),
            triglycerides = triglycerides?.toFloat(),
            vldl = vldl?.toFloat(),
            fastingGlucose = fasting_glucose?.toFloat(),
            hba1c = hba1c?.toFloat(),
            fastingInsulin = fasting_insulin?.toFloat(),
            homaIr = homa_ir?.toFloat(),
            creatinine = creatinine?.toFloat(),
            urea = urea?.toFloat(),
            gfr = gfr?.toFloat(),
            uricAcid = uric_acid?.toFloat(),
            alt = alt?.toFloat(),
            ast = ast?.toFloat(),
            ggt = ggt?.toFloat(),
            totalBilirubin = total_bilirubin?.toFloat(),
            tsh = tsh?.toFloat(),
            t3Free = t3_free?.toFloat(),
            t4Free = t4_free?.toFloat(),
            hemoglobin = hemoglobin?.toFloat(),
            hematocrit = hematocrit?.toFloat(),
            ferritin = ferritin?.toFloat(),
            serumIron = serum_iron?.toFloat(),
            transferrinSaturation = transferrin_saturation?.toFloat(),
            vitaminD = vitamin_d?.toFloat(),
            vitaminB12 = vitamin_b12?.toFloat(),
            folate = folate?.toFloat(),
            zinc = zinc?.toFloat(),
            magnesium = magnesium?.toFloat(),
            crpUltraSensitive = crp_ultra_sensitive?.toFloat(),
            homocysteine = homocysteine?.toFloat(),
            systolicPressure = systolic_pressure?.toInt(),
            diastolicPressure = diastolic_pressure?.toInt(),
            recordedAt = recorded_at,
        )
}
