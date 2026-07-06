package dev.tohure.tanayenai.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.tohure.tanayenai.data.pdf.PdfPicker
import dev.tohure.tanayenai.data.pdf.PdfPickerResult
import dev.tohure.tanayenai.domain.model.ClinicalProfile
import dev.tohure.tanayenai.domain.repository.ClinicalProfileRepository
import dev.tohure.tanayenai.domain.usecase.ClinicalField
import dev.tohure.tanayenai.domain.usecase.ExtractClinicalProfileUseCase
import dev.tohure.tanayenai.domain.usecase.ExtractionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val log = Logger.withTag("ClinicalProfileViewModel")

@Immutable
data class ClinicalProfileUiState(
    val profile: ClinicalProfile? = null,
    val isLoading: Boolean = true,
    val isExtracting: Boolean = false,
    val extractionSuccess: Boolean = false,
    val extractedValuesCount: Int = 0,
    val error: String? = null,
)

class ClinicalProfileViewModel(
    private val pdfPicker: PdfPicker,
    private val extractUseCase: ExtractClinicalProfileUseCase,
    private val repository: ClinicalProfileRepository,
    private val userId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ClinicalProfileUiState())

    @NativeCoroutinesState
    val uiState: StateFlow<ClinicalProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = repository.getClinicalProfile(userId)
            _uiState.value = _uiState.value.copy(profile = profile, isLoading = false)
        }
    }

    fun pickAndExtractPdf() {
        viewModelScope.launch {
            val result = pdfPicker.pickPdf()
            if (result !is PdfPickerResult.Success) return@launch

            _uiState.value = _uiState.value.copy(isExtracting = true, error = null)

            when (val extraction = extractUseCase.extractFromPdf(result.pageImages)) {
                is ExtractionResult.Success -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isExtracting = false,
                            profile = extraction.profile,
                            extractionSuccess = true,
                            extractedValuesCount = countNonNull(extraction.profile),
                        )
                }

                is ExtractionResult.ParseError -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isExtracting = false,
                            error = "No pude leer todos los valores del PDF.",
                        )
                }

                is ExtractionResult.Error -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isExtracting = false,
                            error = extraction.message,
                        )
                }
            }
        }
    }

    fun saveIndividualValue(
        field: ClinicalField,
        value: Float,
    ) {
        viewModelScope.launch {
            when (val r = extractUseCase.saveIndividualValue(field, value)) {
                is ExtractionResult.Success -> {
                    _uiState.value = _uiState.value.copy(profile = r.profile)
                }

                else -> {
                    _uiState.value = _uiState.value.copy(error = "Error al guardar")
                }
            }
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(extractionSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun countNonNull(p: ClinicalProfile) =
        listOfNotNull(
            p.cholesterolTotal,
            p.hdl,
            p.ldl,
            p.triglycerides,
            p.fastingGlucose,
            p.hba1c,
            p.fastingInsulin,
            p.homaIr,
            p.creatinine,
            p.urea,
            p.gfr,
            p.uricAcid,
            p.alt,
            p.ast,
            p.ggt,
            p.totalBilirubin,
            p.tsh,
            p.t3Free,
            p.t4Free,
            p.hemoglobin,
            p.hematocrit,
            p.ferritin,
            p.serumIron,
            p.transferrinSaturation,
            p.vitaminD,
            p.vitaminB12,
            p.folate,
            p.zinc,
            p.magnesium,
            p.crpUltraSensitive,
            p.homocysteine,
            p.systolicPressure,
            p.diastolicPressure,
        ).size
}
