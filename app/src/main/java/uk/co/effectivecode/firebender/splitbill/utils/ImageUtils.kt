package uk.co.effectivecode.firebender.splitbill.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    
    fun encodeImage(context: Context, uri: Uri, quality: Int = 90): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) return null
            
            // Just compress without resizing to maintain quality for OCR
            val base64String = bitmapToBase64(originalBitmap, quality)
            
            originalBitmap.recycle()
            
            base64String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Keep the old method for backward compatibility, but redirect to new method
    @Deprecated("Use encodeImage instead for better OCR accuracy")
    fun resizeAndEncodeImage(context: Context, uri: Uri, maxWidth: Int = 800, maxHeight: Int = 600): String? {
        return encodeImage(context, uri, quality = 85)
    }
    
    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
