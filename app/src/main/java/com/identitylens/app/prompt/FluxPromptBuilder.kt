package com.identitylens.app.prompt

/**
 * Flux.1 PuLID Prompt Builder
 * 
 * Constructs optimized prompts for Flux.1 with PuLID identity preservation
 */
class FluxPromptBuilder {
    
    companion object {
        // Identity anchor templates
        private const val IDENTITY_ANCHOR = "A person with the exact facial features and identity from the reference image"
        
        // Quality modifiers
        private val QUALITY_TERMS = listOf(
            "photorealistic",
            "8k uhd",
            "sharp focus",
            "professional photography",
            "cinematic lighting"
        )
        
        // Era-specific style modifiers
        private val ERA_STYLES = mapOf(
            "1920s" to "film noir aesthetic, golden age cinema, Art Deco",
            "1950s" to "vintage Americana, mid-century modern, Kodachrome",
            "1980s" to "vibrant colors, neon lights, retro synth aesthetic",
            "Victorian era" to "period drama, classical portraiture, oil painting style",
            "Medieval" to "historical accuracy, renaissance painting, dramatic chiaroscuro"
        )
    }
    
    /**
     * Build optimized Flux prompt
     */
    fun build(
        intent: ParsedIntent,
        sceneAnalysis: SceneAnalysis,
        imageMetadata: ImageMetadata
    ): String {
        val components = mutableListOf<String>()
        
        // 1. Identity anchor (always first for PuLID)
        components.add(IDENTITY_ANCHOR)
        
        // 2. Action and pose
        val action = buildActionPhrase(intent, imageMetadata)
        components.add(action)
        
        // 3. Location and environment
        val location = buildLocationPhrase(intent, sceneAnalysis)
        components.add(location)
        
        // 4. Outfit/clothing (if specified)
        intent.outfit?.let { outfit ->
            val outfitPhrase = buildOutfitPhrase(outfit, intent.era)
            components.add(outfitPhrase)
        }
        
        // 5. Lighting setup
        val lighting = buildLightingPhrase(sceneAnalysis.lighting)
        components.add(lighting)
        
        // 6. Atmospheric effects
        val atmosphere = buildAtmospherePhrase(intent, sceneAnalysis.atmosphere)
        if (atmosphere.isNotEmpty()) {
            components.add(atmosphere)
        }
        
        // 7. Era-specific style
        intent.era?.let { era ->
            ERA_STYLES[era]?.let { style ->
                components.add(style)
            }
        }
        
        // 8. Technical quality
        components.add(QUALITY_TERMS.joinToString(", "))
        
        // Join all components
        return components.joinToString(", ")
    }
    
    private fun buildActionPhrase(intent: ParsedIntent, metadata: ImageMetadata): String {
        val action = intent.action ?: metadata.currentPose ?: "standing"
        return action
    }
    
    private fun buildLocationPhrase(intent: ParsedIntent, scene: SceneAnalysis): String {
        val components = mutableListOf<String>()
        
        // Era + location
        if (intent.era != null) {
            components.add("${intent.era} ${intent.location}")
        } else {
            components.add(intent.location)
        }
        
        // Time of day
        intent.timeOfDay?.let {
            components.add("during $it")
        }
        
        // Weather
        intent.weather?.let {
            components.add("$it weather")
        }
        
        return components.joinToString(" ")
    }
    
    private fun buildOutfitPhrase(outfit: String, era: String?): String {
        return if (era != null) {
            "wearing $era $outfit"
        } else {
            "wearing $outfit"
        }
    }
    
    private fun buildLightingPhrase(lighting: LightingSetup): String {
        val components = mutableListOf<String>()
        
        // Primary light
        with(lighting.primary) {
            components.add("illuminated by $type from $direction")
            if (color != "neutral") {
                components.add("$color light")
            }
        }
        
        // Ambient
        if (lighting.ambient.isNotEmpty()) {
            components.add(lighting.ambient)
        }
        
        // Reflections
        if (lighting.reflections.isNotEmpty()) {
            components.add("with reflections on ${lighting.reflections.joinToString(", ")}")
        }
        
        return components.joinToString(", ")
    }
    
    private fun buildAtmospherePhrase(intent: ParsedIntent, atmosphere: AtmosphericConditions): String {
        val components = mutableListOf<String>()
        
        // Atmospheric effects
        if (atmosphere.effects.isNotEmpty()) {
            components.addAll(atmosphere.effects)
        }
        
        // Mood
        intent.mood?.let {
            components.add("$it atmosphere")
        }
        
        return components.joinToString(", ")
    }
}
