package com.identitylens.app.prompt

import com.google.gson.annotations.SerializedName

/**
 * Data models for Prompt Engine
 */

/**
 * Main output from Prompt Engine
 */
data class PromptEngineOutput(
    @SerializedName("gemini_edit_instruction")
    val geminiEditInstruction: String,
    
    @SerializedName("flux_master_prompt")
    val fluxMasterPrompt: String,
    
    @SerializedName("negative_prompt")
    val negativePrompt: String,
    
    @SerializedName("lighting_params")
    val lightingParams: LightingParams,
    
    @SerializedName("ui_suggestion")
    val uiSuggestion: String,
    
    @SerializedName("metadata")
    val metadata: PromptMetadata
)

/**
 * Lighting parameters from scene analysis
 */
data class LightingParams(
    @SerializedName("direction")
    val direction: String,      // "side", "front", "back", "top"
    
    @SerializedName("ambient")
    val ambient: String,         // "blue_night", "warm_sunset", "neutral_day"
    
    @SerializedName("intensity")
    val intensity: String,       // "low", "medium", "high"
    
    @SerializedName("temperature")
    val temperature: Int         // Kelvin (e.g., 3200 for warm, 5600 for daylight)
)

/**
 * Prompt generation metadata
 */
data class PromptMetadata(
    @SerializedName("token_count")
    val tokenCount: Int,
    
    @SerializedName("scene_complexity")
    val sceneComplexity: String,  // "simple", "moderate", "complex"
    
    @SerializedName("optimization_applied")
    val optimizationApplied: Boolean = true,
    
    @SerializedName("processing_time_ms")
    val processingTimeMs: Long = 0
)

/**
 * Scene analysis from Gemini
 */
data class SceneAnalysis(
    val lighting: LightingSetup,
    val atmosphere: AtmosphericConditions,
    val perspective: CameraSetup,
    val colorGrading: ColorProfile,
    val complexity: String = "moderate"
)

data class LightingSetup(
    val primary: LightSource,
    val ambient: String,
    val reflections: List<String> = emptyList()
) {
    fun toParams(): LightingParams {
        return LightingParams(
            direction = primary.direction,
            ambient = ambient,
            intensity = primary.intensity,
            temperature = primary.temperature
        )
    }
}

data class LightSource(
    val type: String,            // "street lamp", "sun", "window", etc.
    val direction: String,       // "side", "front", "back", "top"
    val color: String,           // "warm", "cool", "neutral"
    val temperature: Int,        // Kelvin
    val intensity: String        // "low", "medium", "high"
)

data class AtmosphericConditions(
    val weather: String?,        // "rainy", "foggy", "clear"
    val effects: List<String>,   // ["haze", "rain", "mist"]
    val visibility: String       // "clear", "reduced", "low"
)

data class CameraSetup(
    val perspective: String,     // "eye-level", "low-angle", "high-angle"
    val depth: String,           // "shallow", "deep"
    val composition: String      // "centered", "rule-of-thirds", "dynamic"
)

data class ColorProfile(
    val palette: String,         // "warm", "cool", "neutral", "monochrome"
    val saturation: String,      // "vivid", "muted", "desaturated"
    val contrast: String         // "high", "medium", "low"
)

/**
 * Image metadata from Step 1
 */
data class ImageMetadata(
    val gender: String?,
    val skinTone: String?,
    val currentPose: String?,
    val facialFeatures: Map<String, Any> = emptyMap(),
    val lighting: String? = null
)
