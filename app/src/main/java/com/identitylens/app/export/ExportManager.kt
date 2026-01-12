package com.identitylens.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Android Export Manager
 * Save to gallery and share functionality
 */
class ExportManager(private val context: Context) {
    
    companion object {
        private const val DIRECTORY_NAME = "IdentityLens"
        private const val FILE_PROVIDER_AUTHORITY = "com.identitylens.app.fileprovider"
    }
    
    /**
     * Save image to Android Gallery
     */
    fun saveToGallery(
        bitmap: Bitmap,
        filename: String? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP,
        quality: Int = 95
    ): Result<Uri> {
        return try {
            val displayName = filename ?: generateFilename(format)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(bitmap, displayName, format, quality)
            } else {
                saveToExternalStorage(bitmap, displayName, format, quality)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save using MediaStore (Android 10+)
     */
    private fun saveToMediaStore(
        bitmap: Bitmap,
        displayName: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): Result<Uri> {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(format))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$DIRECTORY_NAME")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        
        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return Result.failure(IOException("Failed to create MediaStore entry"))
        
        resolver.openOutputStream(imageUri)?.use { outputStream ->
            bitmap.compress(format, quality, outputStream)
        } ?: return Result.failure(IOException("Failed to open output stream"))
        
        return Result.success(imageUri)
    }
    
    /**
     * Save to external storage (Android 9 and below)
     */
    private fun saveToExternalStorage(
        bitmap: Bitmap,
        displayName: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): Result<Uri> {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, DIRECTORY_NAME)
        
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        val file = File(appDir, displayName)
        
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(format, quality, outputStream)
        }
        
        // Notify media scanner
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(file)
        mediaScanIntent.data = contentUri
        context.sendBroadcast(mediaScanIntent)
        
        return Result.success(contentUri)
    }
    
    /**
     * Create share intent for image
     */
    fun createShareIntent(
        bitmap: Bitmap,
        message: String = "IdentityLens AI ile oluşturuldu 🎨"
    ): Intent {
        // Save to cache
        val file = saveToCacheDirectory(bitmap)
        
        // Create content URI
        val contentUri = FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            file
        )
        
        // Create share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        return Intent.createChooser(shareIntent, "Paylaş")
    }
    
    /**
     * Share to specific platform
     */
    fun shareToInstagram(bitmap: Bitmap): Intent {
        val file = saveToCacheDirectory(bitmap)
        val contentUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        return intent
    }
    
    fun shareToWhatsApp(bitmap: Bitmap): Intent {
        val file = saveToCacheDirectory(bitmap)
        val contentUri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        return intent
    }
    
    /**
     * Save bitmap to cache directory
     */
    private fun saveToCacheDirectory(bitmap: Bitmap): File {
        val cacheDir = File(context.cacheDir, "shared_images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        val filename = generateFilename(Bitmap.CompressFormat.JPEG)
        val file = File(cacheDir, filename)
        
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }
        
        return file
    }
    
    /**
     * Generate filename with timestamp
     */
    private fun generateFilename(format: Bitmap.CompressFormat): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        val extension = when (format) {
            Bitmap.CompressFormat.JPEG -> "jpg"
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "jpg"
        }
        return "identitylens_$timestamp.$extension"
    }
    
    /**
     * Get MIME type for format
     */
    private fun getMimeType(format: Bitmap.CompressFormat): String {
        return when (format) {
            Bitmap.CompressFormat.JPEG -> "image/jpeg"
            Bitmap.CompressFormat.PNG -> "image/png"
            Bitmap.CompressFormat.WEBP -> "image/webp"
            else -> "image/jpeg"
        }
    }
    
    /**
     * Clear cached images
     */
    fun clearCache() {
        val cacheDir = File(context.cacheDir, "shared_images")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }
}

/**
 * Social media optimization helper
 */
object SocialMediaOptimizer {
    
    fun optimizeForInstagram(bitmap: Bitmap): Bitmap {
        // Instagram max: 1080x1350 (portrait)
        return resizeBitmap(bitmap, maxWidth = 1080, maxHeight = 1350)
    }
    
    fun optimizeForWhatsApp(bitmap: Bitmap): Bitmap {
        // WhatsApp max: 1600x1600
        return resizeBitmap(bitmap, maxWidth = 1600, maxHeight = 1600)
    }
    
    fun optimizeForFacebook(bitmap: Bitmap): Bitmap {
        // Facebook max: 2048x2048
        return resizeBitmap(bitmap, maxWidth = 2048, maxHeight = 2048)
    }
    
    private fun resizeBitmap(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val ratio = Math.min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
