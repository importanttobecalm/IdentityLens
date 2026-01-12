package com.identitylens.app.quality

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.face.Face

/**
 * Comprehensive Image Quality Analyzer
 * 
 * Orchestrates all quality checks for captured images
 */
class ImageQualityAnalyzer(
    private val blurDetector: BlurDetector = BlurDetector(),
    private val faceAngleValidator: FaceAngleValidator = FaceAngleValidator()
) {
    
    companion object {
        // Face size thresholds (percentage of frame)
        private const val MIN_FACE_SIZE = 0.30
        private const val MAX_FACE_SIZE = 0.60
        
        // Lighting thresholds (lux)
        private const val MIN_LUX = 200.0
        private const val MAX_LUX = 1000.0
        private const val IDEAL_LUX = 400.0
        
        // Overall quality threshold
        private const val MIN_QUALITY_SCORE = 0.7
    }
    
    /**
     * Analyze image quality with all checks
     */
    fun analyze(
        imageProxy: ImageProxy,
        detectedFace: Face,
        luxValue: Double
    ): QualityAnalysisResult {
        // 1. Blur Detection
        val blurResult = blurDetector.analyze(imageProxy)
        
        // 2. Face Angle Validation
        val angleResult = faceAngleValidator.validate(detectedFace)
        
        // 3. Lighting Analysis
        val lightingResult = analyzeLighting(luxValue)
        
        // 4. Face Size Validation
        val faceSizeResult = analyzeFaceSize(
            detectedFace,
            imageProxy.width,
            imageProxy.height
        )
        
        // 5. Calculate overall quality score
        val overallScore = calculateOverallScore(
            blurScore = blurResult.qualityScore,
            angleScore = angleResult.overallScore,
            lightingScore = lightingResult.score,
            faceSizeScore = faceSizeResult.score
        )
        
        // 6. Determine if image passes
        val passed = overallScore >= MIN_QUALITY_SCORE &&
                     blurResult.isSharp &&
                     angleResult.isValid &&
                     lightingResult.isAcceptable &&
                     faceSizeResult.isValid
        
        return QualityAnalysisResult(
            passed = passed,
            overallScore = overallScore,
            blurResult = blurResult,
            angleResult = angleResult,
            lightingResult = lightingResult,
            faceSizeResult = faceSizeResult
        )
    }
    
    private fun analyzeLighting(luxValue: Double): LightingResult {
        val isAcceptable = luxValue in MIN_LUX..MAX_LUX
        
        val score = when {
            luxValue < MIN_LUX -> (luxValue / MIN_LUX).coerceIn(0.0, 1.0)
            luxValue > MAX_LUX -> (MAX_LUX / luxValue).coerceIn(0.0, 1.0)
            else -> {
                val deviation = kotlin.math.abs(luxValue - IDEAL_LUX)
                val maxDeviation = IDEAL_LUX - MIN_LUX
                1.0 - (deviation / maxDeviation) * 0.3
            }
        }
        
        val environment = classifyLightingEnvironment(luxValue)
        
        return LightingResult(
            luxValue = luxValue,
            isAcceptable = isAcceptable,
            score = score,
            environment = environment
        )
    }
    
    private fun classifyLightingEnvironment(luxValue: Double): String {
        return when {
            luxValue < 10 -> "Very Dark"
            luxValue < 50 -> "Dark"
            luxValue < 200 -> "Dim"
            luxValue < 1000 -> "Optimal"
            luxValue < 10000 -> "Bright"
            else -> "Very Bright"
        }
    }
    
    private fun analyzeFaceSize(
        face: Face,
        frameWidth: Int,
        frameHeight: Int
    ): FaceSizeResult {
        val boundingBox = face.boundingBox
        val faceArea = boundingBox.width() * boundingBox.height()
        val frameArea = frameWidth * frameHeight
        val faceRatio = faceArea.toDouble() / frameArea.toDouble()
        
        val isValid = faceRatio in MIN_FACE_SIZE..MAX_FACE_SIZE
        
        val score = when {
            faceRatio < MIN_FACE_SIZE -> (faceRatio / MIN_FACE_SIZE).coerceIn(0.0, 1.0)
            faceRatio > MAX_FACE_SIZE -> (MAX_FACE_SIZE / faceRatio).coerceIn(0.0, 1.0)
            else -> 1.0
        }
        
        return FaceSizeResult(
            faceRatio = faceRatio,
            isValid = isValid,
            score = score
        )
    }
    
    private fun calculateOverallScore(
        blurScore: Double,
        angleScore: Double,
        lightingScore: Double,
        faceSizeScore: Double
    ): Double {
        return (blurScore * 0.35 +
                angleScore * 0.30 +
                lightingScore * 0.20 +
                faceSizeScore * 0.15)
    }
}

data class QualityAnalysisResult(
    val passed: Boolean,
    val overallScore: Double,
    val blurResult: BlurResult,
    val angleResult: ValidationResult,
    val lightingResult: LightingResult,
    val faceSizeResult: FaceSizeResult
) {
    fun getFeedback(): List<String> {
        val feedback = mutableListOf<String>()
        
        if (!passed) {
            if (!blurResult.isSharp) {
                feedback.add("Hold phone steady - image is blurry")
            }
            
            if (!angleResult.isValid) {
                feedback.add(FaceAngleValidator().getFeedback(angleResult))
            }
            
            if (!lightingResult.isAcceptable) {
                feedback.add(
                    when {
                        lightingResult.luxValue < 200 -> "Need more light"
                        else -> "Too much light - find shade"
                    }
                )
            }
            
            if (!faceSizeResult.isValid) {
                feedback.add(
                    when {
                        faceSizeResult.faceRatio < 0.30 -> "Move closer to camera"
                        else -> "Move back from camera"
                    }
                )
            }
        }
        
        return feedback
    }
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "overallScore" to overallScore,
            "blurScore" to blurResult.qualityScore,
            "angleScore" to angleResult.overallScore,
            "lightingScore" to lightingResult.score,
            "faceSizeScore" to faceSizeResult.score,
            "passed" to passed
        )
    }
}

data class LightingResult(
    val luxValue: Double,
    val isAcceptable: Boolean,
    val score: Double,
    val environment: String
)

data class FaceSizeResult(
    val faceRatio: Double,
    val isValid: Boolean,
    val score: Double
)
