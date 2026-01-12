package com.identitylens.app.prompt

/**
 * UI Feedback Generator
 * 
 * Creates engaging, contextual feedback messages for users
 */
class UIFeedbackGenerator {
    
    companion object {
        // Location-based templates
        private val LOCATION_TEMPLATES = mapOf(
            "Paris" to "Paris %s hazırlanıyor... ✨",
            "New York" to "New York %s canlandırılıyor... 🗽",
            "Tokyo" to "Tokyo %s oluşturuluyor... 🗼",
            "beach" to "Sahil sahnesi hazırlanıyor... 🏖️",
            "forest" to "Orman atmosferi yaratılıyor... 🌲",
            "street" to "Sokak sahnesi tasarlanıyor... 🏙️"
        )
        
        // Weather-based emojis
        private val WEATHER_EMOJIS = mapOf(
            "rainy" to "🌧️",
            "sunny" to "☀️",
            "snowy" to "❄️",
            "cloudy" to "☁️",
            "foggy" to "🌫️"
        )
        
        // Time-based descriptions
        private val TIME_DESCRIPTIONS = mapOf(
            "night" to "gece ışıkları",
            "sunset" to "gün batımı",
            "sunrise" to "gün doğumu",
            "day" to "gündüz ışığı"
        )
        
        // Era-based descriptions
        private val ERA_DESCRIPTIONS = mapOf(
            "1920s" to "1920'ler dönemi",
            "1950s" to "1950'ler vintage havası",
            "Victorian era" to "Viktorya dönemi",
            "Medieval" to "Ortaçağ atmosferi"
        )
    }
    
    /**
     * Generate contextual UI feedback
     */
    fun generate(intent: ParsedIntent, sceneAnalysis: SceneAnalysis): String {
        // Build context-aware message
        val components = mutableListOf<String>()
        
        // Location
        val location = getLocationPhrase(intent.location)
        components.add(location)
        
        // Era
        intent.era?.let { era ->
            ERA_DESCRIPTIONS[era]?.let { desc ->
                components.add(desc)
            }
        }
        
        // Weather emoji
        val weatherEmoji = intent.weather?.let { WEATHER_EMOJIS[it] } ?: "✨"
        
        // Time description
        intent.timeOfDay?.let { time ->
            TIME_DESCRIPTIONS[time]?.let { desc ->
                components.add(desc)
            }
        }
        
        // Build message
        return when {
            components.size >= 2 -> {
                "${components[0]} ${components.drop(1).joinToString(", ")} hazırlanıyor... $weatherEmoji"
            }
            components.isNotEmpty() -> {
                "${components[0]} hazırlanıyor... $weatherEmoji"
            }
            else -> {
                "Sahneniz oluşturuluyor... ✨"
            }
        }
    }
    
    /**
     * Generate progress messages for different stages
     */
    fun generateProgressMessage(stage: ProcessingStage): String {
        return when (stage) {
            ProcessingStage.PARSING_INTENT -> "İsteğiniz analiz ediliyor... 🔍"
            ProcessingStage.ANALYZING_SCENE -> "Sahne detayları belirleniyor... 🎬"
            ProcessingStage.OPTIMIZING_PROMPT -> "Komutlar optimize ediliyor... ⚡"
            ProcessingStage.GENERATING -> "Görseliniz oluşturuluyor... 🎨"
            ProcessingStage.COMPLETE -> "Hazır! ✅"
        }
    }
    
    private fun getLocationPhrase(location: String): String {
        // Check if location matches a template
        LOCATION_TEMPLATES.forEach { (key, template) ->
            if (location.contains(key, ignoreCase = true)) {
                return template.format("")
            }
        }
        
        // Default
        return location.replaceFirstChar { it.uppercase() }
    }
}

enum class ProcessingStage {
    PARSING_INTENT,
    ANALYZING_SCENE,
    OPTIMIZING_PROMPT,
    GENERATING,
    COMPLETE
}
