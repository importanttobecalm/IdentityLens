package com.identitylens.app.prompt

/**
 * Negative Prompt Generator
 * 
 * Dynamically generates context-aware negative prompts to prevent common artifacts
 */
class NegativePromptGenerator {
    
    companion object {
        // Universal negatives (always included)
        private val UNIVERSAL_NEGATIVES = listOf(
            "deformed",
            "disfigured",
            "bad anatomy",
            "bad proportions",
            "extra limbs",
            "duplicate",
            "mutation",
            "ugly",
            "poorly drawn hands",
            "poorly drawn face",
            "low quality",
            "worst quality",
            "jpeg artifacts",
            "watermark",
            "signature",
            "text",
            "blurry"
        )
        
        // Portrait-specific negatives
        private val PORTRAIT_NEGATIVES = listOf(
            "asymmetric eyes",
            "lazy eye",
            "cross-eyed",
            "misaligned pupils",
            "distorted face",
            "wrong number of fingers",
            "extra fingers",
            "missing fingers"
        )
        
        // Outdoor scene negatives
        private val OUTDOOR_NEGATIVES = listOf(
            "indoor",
            "studio",
            "artificial background"
        )
        
        // PuLID identity preservation negatives
        private val IDENTITY_NEGATIVES = listOf(
            "different person",
            "wrong face",
            "face swap",
            "cartoonish face",
            "anime style",
            "illustration",
            "painting",
            "sketch"
        )
        
        // Era-specific anachronisms
        private val ANACHRONISM_PATTERNS = mapOf(
            "1920s" to listOf(
                "modern clothing",
                "contemporary architecture",
                "smartphones",
                "computers",
                "modern cars",
                "LED lights",
                "plastic"
            ),
            "1950s" to listOf(
                "digital devices",
                "modern technology",
                "contemporary fashion"
            ),
            "Medieval" to listOf(
                "modern buildings",
                "electricity",
                "modern weapons",
                "contemporary clothing"
            ),
            "Victorian era" to listOf(
                "automobiles",
                "modern technology",
                "contemporary fashion"
            )
        )
        
        // Weather-specific negatives
        private val WEATHER_NEGATIVES = mapOf(
            "rainy" to listOf("dry surfaces", "no reflections", "harsh shadows", "desert"),
            "sunny" to listOf("darkness", "night", "shadows everywhere"),
            "snowy" to listOf("green grass", "summer", "hot weather"),
            "foggy" to listOf("crystal clear", "sharp distant objects")
        )
    }
    
    /**
     * Generate context-aware negative prompts
     */
    fun generate(
        intent: ParsedIntent,
        sceneAnalysis: SceneAnalysis
    ): List<String> {
        val negatives = mutableListOf<String>()
        
        // 1. Universal negatives (always include)
        negatives.addAll(UNIVERSAL_NEGATIVES)
        
        // 2. Identity preservation negatives (critical for PuLID)
        negatives.addAll(IDENTITY_NEGATIVES)
        
        // 3. Scene type specific
        when (intent.getSceneType()) {
            SceneType.PORTRAIT -> negatives.addAll(PORTRAIT_NEGATIVES)
            SceneType.OUTDOOR -> negatives.addAll(OUTDOOR_NEGATIVES)
            else -> {}
        }
        
        // 4. Era-specific anachronisms
        intent.era?.let { era ->
            ANACHRONISM_PATTERNS[era]?.let { anachronisms ->
                negatives.addAll(anachronisms)
            }
        }
        
        // 5. Weather-specific negatives
        intent.weather?.let { weather ->
            WEATHER_NEGATIVES[weather]?.let { weatherNegs ->
                negatives.addAll(weatherNegs)
            }
        }
        
        // 6. Time-specific negatives
        intent.timeOfDay?.let { time ->
            when (time) {
                "night" -> negatives.addAll(listOf("daylight", "bright sun", "noon"))
                "day", "noon" -> negatives.addAll(listOf("darkness", "night sky", "stars"))
                "sunset", "sunrise" -> negatives.addAll(listOf("mid-day sun", "complete darkness"))
            }
        }
        
        // Remove duplicates and return
        return negatives.distinct()
    }
    
    /**
     * Generate negative prompt as comma-separated string
     */
    fun generateAsString(intent: ParsedIntent, sceneAnalysis: SceneAnalysis): String {
        return generate(intent, sceneAnalysis).joinToString(", ")
    }
}
