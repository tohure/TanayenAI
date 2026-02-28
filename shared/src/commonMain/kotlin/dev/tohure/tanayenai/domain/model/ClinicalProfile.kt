package dev.tohure.tanayenai.domain.model

data class ClinicalProfile(
    val userId: String,
    val cholesterolTotal: Float? = null,
    val hdl: Float? = null,
    val ldl: Float? = null,
    val triglycerides: Float? = null,
    val fastingGlucose: Float? = null,
    val hba1c: Float? = null,
    val systolicPressure: Int? = null,
    val diastolicPressure: Int? = null,
    val uricAcid: Float? = null,
) {
    val hasDyslipidemia: Boolean get() =
        (cholesterolTotal != null && cholesterolTotal > 200) ||
            (ldl != null && ldl > 130) ||
            (hdl != null && hdl < 40)

    val hasHyperglycemia: Boolean get() =
        (fastingGlucose != null && fastingGlucose > 100) ||
            (hba1c != null && hba1c > 5.7f)

    val hasHypertension: Boolean get() =
        (systolicPressure != null && systolicPressure > 130) ||
            (diastolicPressure != null && diastolicPressure > 80)
}
