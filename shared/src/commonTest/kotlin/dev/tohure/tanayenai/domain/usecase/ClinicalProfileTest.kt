package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.data.remote.dto.ClinicalExtractionDto
import dev.tohure.tanayenai.data.remote.dto.toClinicalProfile
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClinicalProfileTest {
    @Test
    fun `hasDyslipidemia is true when LDL is high`() {
        val profile = ClinicalProfile(userId = "u1", ldl = 150f)
        assertTrue(profile.hasDyslipidemia)
    }

    @Test
    fun `hasDyslipidemia is false when all lipids are normal`() {
        val profile =
            ClinicalProfile(
                userId = "u1",
                cholesterolTotal = 180f,
                ldl = 100f,
                hdl = 55f,
                triglycerides = 120f,
            )
        assertFalse(profile.hasDyslipidemia)
    }

    @Test
    fun `hasHyperglycemia is true when fasting glucose is prediabetic`() {
        val profile = ClinicalProfile(userId = "u1", fastingGlucose = 110f)
        assertTrue(profile.hasHyperglycemia)
    }

    @Test
    fun `hasHypertension is true when systolic is elevated`() {
        val profile = ClinicalProfile(userId = "u1", systolicPressure = 135, diastolicPressure = 78)
        assertTrue(profile.hasHypertension)
    }

    @Test
    fun `activeRestrictions returns all matching conditions`() {
        val profile =
            ClinicalProfile(
                userId = "u1",
                ldl = 150f,
                fastingGlucose = 110f,
                systolicPressure = 135,
            )
        assertEquals(3, profile.activeRestrictions.size)
        assertTrue(profile.activeRestrictions.any { it.contains("Dislipidemia") })
        assertTrue(profile.activeRestrictions.any { it.contains("Hiperglucemia") })
        assertTrue(profile.activeRestrictions.any { it.contains("Hipertensión") })
    }

    @Test
    fun `activeRestrictions is empty when all values are normal`() {
        val profile =
            ClinicalProfile(
                userId = "u1",
                cholesterolTotal = 180f,
                ldl = 100f,
                hdl = 55f,
                triglycerides = 120f,
                fastingGlucose = 90f,
                systolicPressure = 118,
                diastolicPressure = 75,
            )
        assertTrue(profile.activeRestrictions.isEmpty())
    }

    @Test
    fun `toClinicalProfile maps DTO fields correctly`() {
        val dto =
            ClinicalExtractionDto(
                cholesterol_total = 210f,
                hdl = 38f,
                ldl = 140f,
                fasting_glucose = 105f,
                tsh = 5.2f,
            )
        val profile = dto.toClinicalProfile("u1")
        assertEquals(210f, profile.cholesterolTotal)
        assertEquals(38f, profile.hdl)
        assertEquals(140f, profile.ldl)
        assertEquals(105f, profile.fastingGlucose)
        assertEquals(5.2f, profile.tsh)
    }
}
