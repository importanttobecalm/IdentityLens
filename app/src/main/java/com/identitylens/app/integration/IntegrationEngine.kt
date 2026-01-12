package com.identitylens.app.integration

import com.identitylens.app.models.IdentityPacket
import kotlinx.coroutines.flow.Flow

/**
 * Integration Engine Interface
 * Defines contract for neural processing integrations
 */
interface IntegrationEngine {
    
    /**
     * Process identity packet with neural tools
     */
    suspend fun processNeuralIntegration(
        identityPacket: IdentityPacket,
        options: NeuralProcessingOptions = NeuralProcessingOptions()
    ): Flow<NeuralProcessingResult>
    
    /**
     * Check if neural processing is available
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Get supported capabilities
     */
    fun getSupportedCapabilities(): List<NeuralCapability>
}

/**
 * Neural processing options
 */
data class NeuralProcessingOptions(
    val enableSegmentation: Boolean = true,
    val enableAlignment: Boolean = true,
    val enableBlending: Boolean = true,
    val precision: SegmentationPrecision = SegmentationPrecision.HIGH,
    val alignmentMode: AlignmentMode = AlignmentMode.LANDMARKS_468,
    val blendMode: BlendMode = BlendMode.MEDIUM
)

/**
 * Segmentation precision levels
 */
enum class SegmentationPrecision {
    LOW, MEDIUM, HIGH
}

/**
 * Face alignment modes
 */
enum class AlignmentMode {
    LANDMARKS_68,
    LANDMARKS_468,
    DENSE_MESH
}

/**
 * Blending modes
 */
enum class BlendMode {
    SOFT, MEDIUM, HARD
}

/**
 * Neural capabilities
 */
enum class NeuralCapability {
    IMAGE_SEGMENTATION,
    FACE_ALIGNMENT,
    NEURAL_BLENDING,
    TEXTURE_SYNTHESIS,
    LIGHTING_HARMONIZATION
}

/**
 * Processing result
 */
sealed class NeuralProcessingResult {
    data class Progress(
        val stage: ProcessingStage,
        val progress: Float,
        val message: String
    ) : NeuralProcessingResult()
    
    data class Success(
        val segmentationMask: String? = null,  // Base64
        val alignedFace: String? = null,  // Base64
        val blendedImage: String? = null,  // Base64
        val metadata: Map<String, Any> = emptyMap()
    ) : NeuralProcessingResult()
    
    data class Error(
        val code: String,
        val message: String,
        val canRetry: Boolean = true
    ) : NeuralProcessingResult()
}

/**
 * Processing stages
 */
enum class ProcessingStage {
    IDLE,
    SEGMENTATION,
    ALIGNMENT,
    BLENDING,
    FINALIZATION,
    COMPLETE
}
