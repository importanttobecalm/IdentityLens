package com.identitylens.app.integration

import android.content.Context
import android.util.Base64
import com.identitylens.app.models.IdentityPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Neural Integration Module
 * Integrates MCP Context7 tools for advanced face processing
 */
class NeuralIntegrationModule(
    private val context: Context,
    private val apiKey: String
) : IntegrationEngine {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val baseUrl = "https://mcp.context7.com/mcp"
    
    companion object {
        private const val TAG = "NeuralIntegrationModule"
    }
    
    override suspend fun processNeuralIntegration(
        identityPacket: IdentityPacket,
        options: NeuralProcessingOptions
    ): Flow<NeuralProcessingResult> = flow {
        
        emit(NeuralProcessingResult.Progress(
            stage = ProcessingStage.IDLE,
            progress = 0f,
            message = "Neural işleme başlatılıyor..."
        ))
        
        try {
            var segmentationMask: String? = null
            var alignedFace: String? = null
            var blendedImage: String? = null
            
            // Step 1: Image Segmentation
            if (options.enableSegmentation) {
                emit(NeuralProcessingResult.Progress(
                    stage = ProcessingStage.SEGMENTATION,
                    progress = 0.2f,
                    message = "Yüz segmentasyonu yapılıyor..."
                ))
                
                segmentationMask = performSegmentation(
                    identityPacket,
                    options.precision
                )
            }
            
            // Step 2: Face Alignment
            if (options.enableAlignment) {
                emit(NeuralProcessingResult.Progress(
                    stage = ProcessingStage.ALIGNMENT,
                    progress = 0.5f,
                    message = "Yüz hizalaması yapılıyor..."
                ))
                
                alignedFace = performAlignment(
                    identityPacket,
                    options.alignmentMode
                )
            }
            
            // Step 3: Neural Blending (if applicable)
            if (options.enableBlending && segmentationMask != null) {
                emit(NeuralProcessingResult.Progress(
                    stage = ProcessingStage.BLENDING,
                    progress = 0.8f,
                    message = "Neural harmanlamayapılıyor..."
                ))
                
                // Note: Blending requires both subject and background
                // This will be called later in the pipeline
            }
            
            emit(NeuralProcessingResult.Progress(
                stage = ProcessingStage.COMPLETE,
                progress = 1.0f,
                message = "Neural işlem tamamlandı!"
            ))
            
            emit(NeuralProcessingResult.Success(
                segmentationMask = segmentationMask,
                alignedFace = alignedFace,
                blendedImage = blendedImage,
                metadata = mapOf(
                    "processing_time" to System.currentTimeMillis(),
                    "tools_used" to listOf("segmentation", "alignment")
                )
            ))
            
        } catch (e: Exception) {
            emit(NeuralProcessingResult.Error(
                code = "NEURAL_PROCESSING_ERROR",
                message = "Neural işlem hatası: ${e.message}",
                canRetry = true
            ))
        }
    }
    
    /**
     * Perform image segmentation via MCP
     */
    private suspend fun performSegmentation(
        identityPacket: IdentityPacket,
        precision: SegmentationPrecision
    ): String = withContext(Dispatchers.IO) {
        
        val requestBody = """
            {
                "tool": "image_segmentation",
                "parameters": {
                    "image_base64": "${identityPacket.image.cleanFace}",
                    "segmentation_type": "face",
                    "precision": "${precision.name.lowercase()}"
                }
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/tools/segmentation")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("Segmentation failed: ${response.code}")
        }
        
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response")
        
        val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
        val maskBase64 = jsonResponse["mask_base64"]?.jsonPrimitive?.content
            ?: throw IOException("No mask in response")
        
        maskBase64
    }
    
    /**
     * Perform face alignment via MCP
     */
    private suspend fun performAlignment(
        identityPacket: IdentityPacket,
        mode: AlignmentMode
    ): String = withContext(Dispatchers.IO) {
        
        val requestBody = """
            {
                "tool": "face_alignment",
                "parameters": {
                    "image_base64": "${identityPacket.image.cleanFace}",
                    "alignment_mode": "${mode.name.lowercase()}",
                    "normalize": true
                }
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/tools/alignment")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("Alignment failed: ${response.code}")
        }
        
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response")
        
        val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
        val alignedFaceBase64 = jsonResponse["aligned_face_base64"]?.jsonPrimitive?.content
            ?: throw IOException("No aligned face in response")
        
        alignedFaceBase64
    }
    
    /**
     * Perform neural blending
     */
    suspend fun performBlending(
        subjectBase64: String,
        backgroundBase64: String,
        maskBase64: String,
        blendMode: BlendMode
    ): String = withContext(Dispatchers.IO) {
        
        val requestBody = """
            {
                "tool": "neural_blending",
                "parameters": {
                    "subject_image_base64": "$subjectBase64",
                    "background_image_base64": "$backgroundBase64",
                    "mask_base64": "$maskBase64",
                    "blend_mode": "${blendMode.name.lowercase()}",
                    "preserve_detail": true
                }
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$baseUrl/tools/blending")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("Blending failed: ${response.code}")
        }
        
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response")
        
        val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
        val blendedImageBase64 = jsonResponse["blended_image_base64"]?.jsonPrimitive?.content
            ?: throw IOException("No blended image in response")
        
        blendedImageBase64
    }
    
    override suspend fun isAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getSupportedCapabilities(): List<NeuralCapability> {
        return listOf(
            NeuralCapability.IMAGE_SEGMENTATION,
            NeuralCapability.FACE_ALIGNMENT,
            NeuralCapability.NEURAL_BLENDING
        )
    }
}
