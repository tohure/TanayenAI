package dev.tohure.tanayenai.data.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

private const val MAX_PAGES = 5
private const val PAGE_WIDTH_PX = 1080

actual class PdfPicker(
    private val activity: ComponentActivity,
) {
    private var pendingCallback: ((PdfPickerResult) -> Unit)? = null

    private val launcher =
        activity.registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri == null) {
                pendingCallback?.invoke(PdfPickerResult.Cancelled)
            } else {
                try {
                    val bytes =
                        activity.contentResolver
                            .openInputStream(uri)
                            ?.use { it.readBytes() }
                            ?: throw Exception("No se pudo leer el archivo")

                    val pageImages = renderPdfToJpegPages(bytes)
                    if (pageImages.isEmpty()) throw Exception("No se pudo renderizar el PDF")
                    pendingCallback?.invoke(PdfPickerResult.Success(pageImages))
                } catch (e: Exception) {
                    pendingCallback?.invoke(PdfPickerResult.Error(e.message ?: "Error"))
                }
            }
            pendingCallback = null
        }

    actual suspend fun pickPdf(): PdfPickerResult =
        suspendCancellableCoroutine { cont ->
            pendingCallback = { cont.resume(it) }
            cont.invokeOnCancellation { pendingCallback = null }
            launcher.launch("application/pdf")
        }

    private fun renderPdfToJpegPages(pdfBytes: ByteArray): List<String> {
        val tempFile = File.createTempFile("pdf_tmp", ".pdf", activity.cacheDir)
        tempFile.writeBytes(pdfBytes)

        return try {
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pages = mutableListOf<String>()

            val pageCount = minOf(renderer.pageCount, MAX_PAGES)
            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val scale = PAGE_WIDTH_PX.toFloat() / page.width
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(PAGE_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                bitmap.recycle()
                pages.add(Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP))
            }

            renderer.close()
            pfd.close()
            pages
        } finally {
            tempFile.delete()
        }
    }
}
