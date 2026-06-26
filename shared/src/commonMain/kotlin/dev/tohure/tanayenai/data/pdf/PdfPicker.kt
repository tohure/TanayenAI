package dev.tohure.tanayenai.data.pdf

sealed class PdfPickerResult {
    /** Each string is a JPEG page rendered from the PDF, encoded as base64. */
    data class Success(
        val pageImages: List<String>,
    ) : PdfPickerResult()

    data object Cancelled : PdfPickerResult()

    data class Error(
        val message: String,
    ) : PdfPickerResult()
}

expect class PdfPicker {
    suspend fun pickPdf(): PdfPickerResult
}
