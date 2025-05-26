package uk.co.effectivecode.firebender.splitbill.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    
    fun resizeAndEncodeImage(context: Context, uri: Uri, maxWidth: Int = 800, maxHeight: Int = 600): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) return null
            
            val resizedBitmap = resizeBitmap(originalBitmap, maxWidth, maxHeight)
            val base64String = bitmapToBase64(resizedBitmap, 85) // 85% quality
            
            originalBitmap.recycle()
            resizedBitmap.recycle()
            
            base64String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val ratio = kotlin.math.min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        
        if (ratio >= 1) return bitmap
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}