package com.identitylens.app.api

import android.util.Log
import com.identitylens.app.models.IdentityPacket
import com.identitylens.app.prompt.LightingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Client for Cloud Inference Pipeline
 * 
 * Manages communication with backend server for Flux.1 + PuLID generation
 */
class CloudInferenceClient(
    private val baseUrl: String,
    private val apiKey: String? = null
) {
    
    companion object {
        private const val TAG = "CloudInferenceClient"
        private const val DEFAULT_TIMEOUT = 30L  // seconds
        private const val MAX_RETRIES = 3
    }
    
    private val api: CloudInferenceApi
    
    init {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .apply {
                // Add API key interceptor if provided
                apiKey?.let { key ->
                    addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $key")
                            .build()
                        chain.proceed(request)
                    }
                }
                
                // Add logging in debug mode
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(CloudInferenceApi::class.java)
    }
    
    /**
     * Generate image with identity preservation
     */
    suspend fun generateImage(
        identityPacket: IdentityPacket,
        masterPrompt: String,
        negativePrompt: String,
        mode: GenerationMode = GenerationMode.SPEED,
        lightingParams: LightingParams? = null
    ): GenerationResult = withContext(Dispatchers.IO) {
        
        val request = GenerationRequest(
            identityPacket = identityPacket,
            masterPrompt = masterPrompt,
            negativePrompt = negativePrompt,
            mode = mode.value,
            lightingParams = lightingParams
        )
        
        try {
            val response = api.generateImage(request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    GenerationResult.Success(
                        imageUrl = body.imageUrl ?: "",
                        imageBase64 = body.imageBase64,
                        inferenceTime = body.inferenceTime,
                        modelVersion = body.modelVersion,
                        seed = body.seed
                    )
                } else {
                    GenerationResult.Error(
                        code = body?.error?.code ?: ErrorCodes.UNKNOWN_ERROR,
                        message = body?.error?.message ?: "Unknown error",
                        action = body?.error?.action ?: "Please try again",
                        canRetry = body?.error?.retry ?: false
                    )
                }
            } else {
                GenerationResult.Error(
                    code = ErrorCodes.API_ERROR,
                    message = "HTTP ${response.code()}: ${response.message()}",
                    action = "Please try again",
                    canRetry = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            GenerationResult.Error(
                code = ErrorCodes.API_ERROR,
                message = e.message ?: "Network error",
                action = "Please check your connection and try again",
                canRetry = true
            )
        }
    }
    
    /**
     * Generate with automatic retry
     */
    suspend fun generateWithRetry(
        identityPacket: IdentityPacket,
        masterPrompt: String,
        negativePrompt: String,
        mode: GenerationMode = GenerationMode.SPEED,
        lightingParams: LightingParams? = null,
        maxRetries: Int = MAX_RETRIES,
        onProgress: ((String) -> Unit)? = null
    ): GenerationResult = withContext(Dispatchers.IO) {
        
        var lastResult: GenerationResult? = null
        
        repeat(maxRetries) { attempt ->
            onProgress?.invoke("Attempt ${attempt + 1}/$maxRetries...")
            
            val result = generateImage(
                identityPacket,
                masterPrompt,
                negativePrompt,
                mode,
                lightingParams
            )
            
            when (result) {
                is GenerationResult.Success -> return@withContext result
                is GenerationResult.Error -> {
                    lastResult = result
                    
                    // Don't retry if error is not retryable
                    if (!result.canRetry) {
                        return@withContext result
                    }
                    
                    // Exponential backoff
                    if (attempt < maxRetries - 1) {
                        val delayMs = (1000L * (1 shl attempt))  // 1s, 2s, 4s
                        delay(delayMs)
                    }
                }
            }
        }
        
        lastResult ?: GenerationResult.Error(
            code = ErrorCodes.UNKNOWN_ERROR,
            message = "Max retries exceeded",
            action = "Please try again later",
            canRetry = false
        )
    }
    
    /**
     * Check generation status (for async mode)
     */
    suspend fun getStatus(requestId: String): GenerationStatus? = withContext(Dispatchers.IO) {
        try {
            val response = api.getGenerationStatus(requestId)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get status", e)
            null
        }
    }
}

/**
 * Generation mode
 */
enum class GenerationMode(val value: String) {
    SPEED("speed"),      // Flux schnell - 4-6s
    QUALITY("quality")   // Flux dev - 8-10s
}

/**
 * Result from generation
 */
sealed class GenerationResult {
    data class Success(
        val imageUrl: String,
        val imageBase64: String?,
        val inferenceTime: Float,
        val modelVersion: String,
        val seed: Int
    ) : GenerationResult()
    
    data class Error(
        val code: Int,
        val message: String,
        val action: String,
        val canRetry: Boolean
    ) : GenerationResult()
}

/**
 * BuildConfig stub (replace with actual BuildConfig)
 */
private object BuildConfig {
    const val DEBUG = true
}
