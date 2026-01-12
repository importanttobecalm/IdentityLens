package com.identitylens.app.prompt

/**
 * Intent Parser - Extracts structured information from natural language user intent
 * 
 * Supports Turkish language input and parses:
 * - Era/time period
 * - Location
 * - Weather conditions
 * - Time of day
 * - Outfit/clothing
 * - Mood/atmosphere
 * - Actions
 */
class IntentParser {
    
    companion object {
        // Era patterns
        private val ERA_PATTERNS = mapOf(
            Regex("""(\d{4})'?ler""") to { match: MatchResult -> "${match.groupValues[1]}s" },
            Regex("""victorian|viktorya""", RegexOption.IGNORE_CASE) to { _: MatchResult -> "Victorian era" },
            Regex("""medieval|ortaçağ""", RegexOption.IGNORE_CASE) to { _: MatchResult -> "Medieval" },
            Regex("""modern|çağdaş""", RegexOption.IGNORE_CASE) to { _: MatchResult -> "Modern" },
            Regex("""retro|vintage|antika""", RegexOption.IGNORE_CASE) to { _: MatchResult -> "Vintage" }
        )
        
        // Location patterns
        private val LOCATION_PATTERNS = mapOf(
            "paris" to "Paris street",
            "new york|newyork" to "New York city",
            "tokyo|tokyo" to "Tokyo",
            "istanbul" to "Istanbul",
            "londra|london" to "London",
            "sokak|street" to "street",
            "park" to "park",
            "sahil|beach|plaj" to "beach",
            "orman|forest" to "forest",
            "dağ|mountain" to "mountain"
        )
        
        // Weather patterns
        private val WEATHER_PATTERNS = mapOf(
            "yağmur|rainy|rain" to "rainy",
            "kar|snow|snowy" to "snowy",
            "güneşli|sunny" to "sunny",
            "bulutlu|cloudy" to "cloudy",
            "sisli|foggy|fog" to "foggy",
            "fırtına|storm" to "stormy"
        )
        
        // Time of day patterns
        private val TIME_PATTERNS = mapOf(
            "gece|night" to "night",
            "gün batımı|sunset|akşam" to "sunset",
            "gün doğumu|sunrise|sabah" to "sunrise",
            "öğlen|noon" to "noon",
            "gündüz|day" to "day"
        )
        
        // Outfit patterns
        private val OUTFIT_PATTERNS = mapOf(
            "takım elbise|suit" to "suit",
            "elbise|dress" to "dress",
            "tişört|t-shirt|tshirt" to "t-shirt",
            "jean|jeans" to "jeans",
            "smokin|tuxedo" to "tuxedo",
            "gelinlik|wedding dress" to "wedding dress",
            "kostüm|costume" to "costume",
            "vintage" to "vintage clothing",
            "spor kıyafet|sportswear" to "sportswear"
        )
        
        // Mood patterns
        private val MOOD_PATTERNS = mapOf(
            "romantik|romantic" to "romantic",
            "gizemli|mysterious" to "mysterious",
            "neşeli|cheerful|happy" to "cheerful",
            "melankolik|melancholic" to "melancholic",
            "dramatik|dramatic" to "dramatic",
            "sakin|peaceful|calm" to "peaceful",
            "enerji|energetic" to "energetic"
        )
        
        // Action patterns
        private val ACTION_PATTERNS = mapOf(
            "yürü|walking" to "walking",
            "otur|sitting" to "sitting",
            "koş|running" to "running",
            "dans|dancing" to "dancing",
            "poz|posing" to "posing",
            "dik dur|standing" to "standing"
        )
    }
    
    /**
     * Parse natural language intent into structured format
     */
    fun parse(userIntent: String): ParsedIntent {
        val intent = userIntent.lowercase()
        
        return ParsedIntent(
            era = extractEra(intent),
            location = extractLocation(intent),
            weather = extractWeather(intent),
            timeOfDay = extractTimeOfDay(intent),
            outfit = extractOutfit(intent),
            mood = extractMood(intent),
            action = extractAction(intent),
            rawIntent = userIntent
        )
    }
    
    private fun extractEra(text: String): String? {
        ERA_PATTERNS.forEach { (pattern, extractor) ->
            pattern.find(text)?.let { match ->
                return extractor(match)
            }
        }
        return null
    }
    
    private fun extractLocation(text: String): String {
        LOCATION_PATTERNS.forEach { (pattern, location) ->
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return location
            }
        }
        return "outdoor scene"
    }
    
    private fun extractWeather(text: String): String? {
        WEATHER_PATTERNS.forEach { (pattern, weather) ->
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return weather
            }
        }
        return null
    }
    
    private fun extractTimeOfDay(text: String): String? {
        TIME_PATTERNS.forEach { (pattern, time) ->
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return time
            }
        }
        return null
    }
    
    private fun extractOutfit(text: String): String? {
        OUTFIT_PATTERNS.forEach { (pattern, outfit) ->
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return outfit
            }
        }
        return null
    }
    
    private fun extractMood(text: String): String? {
        MOOD_PATTERNS.forEach { (pattern, mood) ->
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return mood
            }
        }
        return null
    }
    
    private fun extractAction(text: String): String? {
        ACTION_PATTERNS.forEach { (pattern, action) ->
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return action
            }
        }
        return null
    }
}

/**
 * Parsed intent structure
 */
data class ParsedIntent(
    val era: String?,
    val location: String,
    val weather: String?,
    val timeOfDay: String?,
    val outfit: String?,
    val mood: String?,
    val action: String?,
    val rawIntent: String
) {
    /**
     * Get scene type for context-aware processing
     */
    fun getSceneType(): SceneType {
        return when {
            location.contains("street") -> SceneType.OUTDOOR
            location.contains("beach") -> SceneType.OUTDOOR
            location.contains("forest") -> SceneType.OUTDOOR
            action == "posing" -> SceneType.PORTRAIT
            else -> SceneType.GENERAL
        }
    }
}

enum class SceneType {
    PORTRAIT,
    OUTDOOR,
    INDOOR,
    GENERAL
}
