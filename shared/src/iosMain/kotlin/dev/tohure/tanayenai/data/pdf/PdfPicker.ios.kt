package dev.tohure.tanayenai.data.pdf

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class PdfPicker {
    private var pendingCallback: ((PdfPickerResult) -> Unit)? = null

    actual suspend fun pickPdf(): PdfPickerResult =
        suspendCancellableCoroutine { cont ->
            pendingCallback = { cont.resume(it) }
            PdfPickerBridge.requestPicker()
            cont.invokeOnCancellation { pendingCallback = null }
        }

    /** Called from Swift with JPEG pages rendered from the PDF (one per page). */
    fun deliverPageImages(images: List<String>) {
        pendingCallback?.invoke(PdfPickerResult.Success(images))
        pendingCallback = null
    }

    fun deliverCancellation() {
        pendingCallback?.invoke(PdfPickerResult.Cancelled)
        pendingCallback = null
    }

    fun deliverError(message: String) {
        pendingCallback?.invoke(PdfPickerResult.Error(message))
        pendingCallback = null
    }
}

object PdfPickerBridge {
    private var onRequest: (() -> Unit)? = null

    fun onPickerRequested(callback: () -> Unit) {
        onRequest = callback
    }

    internal fun requestPicker() {
        onRequest?.invoke()
    }
}
