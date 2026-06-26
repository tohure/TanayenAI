package dev.tohure.tanayenai.ui.chat

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun createImageUri(context: Context): Uri? {
        val contentValues =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "tanayen_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun resizeAndEncodeImage(
        context: Context,
        uri: Uri,
        maxSize: Int = 800,
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            val width = originalBitmap.width
            val height = originalBitmap.height

            val scale =
                if (width > maxSize || height > maxSize) {
                    if (width > height) {
                        maxSize.toFloat() / width.toFloat()
                    } else {
                        maxSize.toFloat() / height.toFloat()
                    }
                } else {
                    1f
                }

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizedBitmap =
                Bitmap.createBitmap(
                    originalBitmap,
                    0,
                    0,
                    width,
                    height,
                    matrix,
                    false,
                )

            val outputStream = ByteArrayOutputStream()
            // 70% compression quality as requested
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()

            originalBitmap.recycle()
            if (resizedBitmap != originalBitmap) resizedBitmap.recycle()

            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
