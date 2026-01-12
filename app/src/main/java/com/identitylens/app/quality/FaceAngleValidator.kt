package com.identitylens.app.quality

import com.google.mlkit.vision.face.Face
import kotlin.math.abs

/**
 * Face Angle Validator using ML Kit Face Detection Euler Angles
 * 
 * Validates that the user's face is properly oriented for optimal capture.
 * Uses Euler angles (X=pitch, Y=yaw, Z=roll) from ML Kit face detection.
 */
class FaceAngleValidator {
    
    companion object {
        // Angle thresholds in degrees
        private const val MAX_PITCH = 15.0f  // Max up/down tilt
        private const val MAX_YAW = 15.0f    // Max left/right rotation
        private const val MAX_ROLL = 10.0f   // Max sideways tilt
        
        // Ideal angle (face looking straight at camera)
        private const val IDEAL_ANGLE = 0.0f
    }
    
    /**
     * Validates face angle from ML Kit Face object
     */
    fun validate(face: Face): ValidationResult {
        // Get Euler angles from ML Kit
        val pitch = face.headEulerAngleX  // Up/down tilt
        val yaw = face.headEulerAngleY    // Left/right rotation
        val roll = face.headEulerAngleZ   // Sideways tilt
        
        // Validate each angle
        val pitchValid = abs(pitch) <= MAX_PITCH
        val yawValid = abs(yaw) <= MAX_YAW
        val rollValid = abs(roll) <= MAX_ROLL
        
        // Overall validation
        val isValid = pitchValid && yawValid && rollValid
        
        // Calculate quality scores
        val pitchScore = calculateAngleScore(pitch, MAX_PITCH)
        val yawScore = calculateAngleScore(yaw, MAX_YAW)
        val rollScore = calculateAngleScore(roll, MAX_ROLL)
        val overallScore = (pitchScore + yawScore + rollScore) / 3.0
        
        return ValidationResult(
            isValid = isValid,
            pitch = pitch,
            yaw = yaw,
            roll = roll,
            pitchValid = pitchValid,
            yawValid = yawValid,
            rollValid = rollValid,
            pitchScore = pitchScore,
            yawScore = yawScore,
            rollScore = rollScore,
            overallScore = overallScore
        )
    }
    
    /**
     * Calculate quality score for a single angle (0.0 to 1.0)
     */
    private fun calculateAngleScore(angle: Float, maxAngle: Float): Double {
        val deviation = abs(angle)
        if (deviation >= maxAngle) return 0.0
        
        return (1.0 - (deviation / maxAngle)).toDouble()
    }
    
    /**
     * Get actionable feedback for user
     */
    fun getFeedback(result: ValidationResult): String {
        if (result.isValid) {
            return "Perfect! Face aligned correctly."
        }
        
        val feedback = mutableListOf<String>()
        
        if (!result.pitchValid) {
            feedback.add(
                if (result.pitch > 0) "Look down slightly"
                else "Look up slightly"
            )
        }
        
        if (!result.yawValid) {
            feedback.add(
                if (result.yaw > 0) "Turn your face left"
                else "Turn your face right"
            )
        }
        
        if (!result.rollValid) {
            feedback.add(
                if (result.roll > 0) "Tilt your head left"
                else "Tilt your head right"
            )
        }
        
        return feedback.joinToString(", ")
    }
}

/**
 * Result of face angle validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val pitch: Float,
    val yaw: Float,
    val roll: Float,
    val pitchValid: Boolean,
    val yawValid: Boolean,
    val rollValid: Boolean,
    val pitchScore: Double,
    val yawScore: Double,
    val rollScore: Double,
    val overallScore: Double
) {
    fun getQualityLevel(): AngleQuality {
        return when {
            overallScore >= 0.9 -> AngleQuality.EXCELLENT
            overallScore >= 0.7 -> AngleQuality.GOOD
            overallScore >= 0.5 -> AngleQuality.ACCEPTABLE
            else -> AngleQuality.POOR
        }
    }
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "pitch" to pitch,
            "yaw" to yaw,
            "roll" to roll,
            "pitchValid" to pitchValid,
            "yawValid" to yawValid,
            "rollValid" to rollValid,
            "overallScore" to overallScore
        )
    }
}

enum class AngleQuality {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    POOR
}
