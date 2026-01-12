package com.identitylens.app.api

import com.google.gson.annotations.SerializedName
import com.identitylens.app.models.IdentityPacket
import com.identitylens.app.prompt.LightingParams
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service for Cloud Inference Pipeline
 */
interface CloudInferenceApi {
    
    @POST("api/generate")
    suspend fun generateImage(
        @Body request: GenerationRequest
    ): Response<GenerationResponse>
    
    @GET("api/status/{requestId}")
    suspend fun getGenerationStatus(
        @Path("requestId") requestId: String
    ): Response<GenerationStatus>
}

/**
 * Request for cloud image generation
 */
data class GenerationRequest(
    @SerializedName("identity_packet")
    val identityPacket: IdentityPacket,
    
    @SerializedName("master_prompt")
    val masterPrompt: String,
    
    @SerializedName("negative_prompt")
    val negativePrompt: String,
    
    @SerializedName("mode")
    val mode: String = "speed",  // speed | quality
    
    @SerializedName("lighting_params")
    val lightingParams: LightingParams? = null,
    
    @SerializedName("enable_harmonization")
    val enableHarmonization: Boolean = true,
    
    @SerializedName("denoising_strength")
    val denoisingStrength: Float = 0.40f  // 0.35-0.45 range
)

/**
 * Response from cloud generation
 */
data class GenerationResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("image_url")
    val imageUrl: String?,
    
    @SerializedName("image_base64")
    val imageBase64: String?,
    
    @SerializedName("inference_time")
    val inferenceTime: Float,
    
    @SerializedName("model_version")
    val modelVersion: String,
    
    @SerializedName("seed")
    val seed: Int,
    
    @SerializedName("error")
    val error: ErrorResponse?
)

/**
 * Generation status for async requests
 */
data class GenerationStatus(
    @SerializedName("status")
    val status: String,  // pending | processing | completed | failed
    
    @SerializedName("progress")
    val progress: Float,  // 0.0 - 1.0
    
    @SerializedName("result")
    val result: GenerationResponse?
)

/**
 * Error response
 */
data class ErrorResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("action")
    val action: String,
    
    @SerializedName("retry")
    val retry: Boolean
)

/**
 * Error codes (must match backend)
 */
object ErrorCodes {
    const val NO_FACE_DETECTED = 1001
    const val LOW_QUALITY_IMAGE = 1002
    const val INFERENCE_TIMEOUT = 2001
    const val API_RATE_LIMIT = 2002
    const val NSFW_CONTENT = 3001
    const val API_ERROR = 4001
    const val UNKNOWN_ERROR = 9999
}
