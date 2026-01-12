package com.identitylens.app.examples

import com.identitylens.app.prompt.*
import kotlinx.coroutines.runBlocking

/**
 * Example usage of Prompt Engine
 */
fun main() = runBlocking {
    
    // Sample user intent (Turkish)
    val userIntent = "Beni 1920'ler Paris'inde, yağmurlu bir sokakta, şık bir takım elbiseyle göster."
    
    // Sample metadata from Step 1
    val imageMetadata = ImageMetadata(
        gender = "male",
        skinTone = "wheatish",
        currentPose = "standing_straight",
        facialFeatures = mapOf(
            "eyeColor" to "brown",
            "hairColor" to "black"
        )
    )
    
    // Create Prompt Engine
    val promptEngine = PromptEngine()
    
    // Generate prompts
    println("Processing: \"$userIntent\"")
    println("-".repeat(80))
    
    val result = promptEngine.generatePrompts(userIntent, imageMetadata)
    
    // Display results
    println("\n✅ PROMPT ENGINE OUTPUT:\n")
    
    println("📝 GEMINI EDIT INSTRUCTION:")
    println(result.geminiEditInstruction)
    println()
    
    println("🎨 FLUX MASTER PROMPT:")
    println(result.fluxMasterPrompt)
    println()
    
    println("🚫 NEGATIVE PROMPT:")
    println(result.negativePrompt)
    println()
    
    println("💡 LIGHTING PARAMETERS:")
    println("  Direction: ${result.lightingParams.direction}")
    println("  Ambient: ${result.lightingParams.ambient}")
    println("  Intensity: ${result.lightingParams.intensity}")
    println("  Temperature: ${result.lightingParams.temperature}K")
    println()
    
    println("📱 UI SUGGESTION:")
    println(result.uiSuggestion)
    println()
    
    println("📊 METADATA:")
    println("  Token Count: ${result.metadata.tokenCount}")
    println("  Scene Complexity: ${result.metadata.sceneComplexity}")
    println("  Processing Time: ${result.metadata.processingTimeMs}ms")
    println()
    
    println("-".repeat(80))
    
    // JSON output
    val json = com.google.gson.Gson().toJson(result)
    println("\n📦 JSON OUTPUT:")
    println(json)
}

/**
 * Example with different scenarios
 */
fun exampleScenarios() = runBlocking {
    val promptEngine = PromptEngine()
    
    val scenarios = listOf(
        "Beni 1950'lerde New York'ta, gün batımında, vintage bir elbiseyle göster." to
                ImageMetadata("female", "fair", "standing"),
        
        "Tokyo sokaklarında, gece, neon ışıklar altında, rahat kıyafetlerle göster." to
                ImageMetadata("male", "medium", "walking"),
        
        "Sahilde, güneşli bir günde, plaj kıyafetiyle göster." to
                ImageMetadata("female", "tan", "sitting"),
        
        "Karlı bir ormanda, dramatik ışıkta,  kış kıyafetleriyle göster." to
                ImageMetadata("male", "fair", "standing")
    )
    
    scenarios.forEach { (intent, metadata) ->
        println("\n" + "=".repeat(80))
        println("SCENARIO: $intent")
        println("=".repeat(80))
        
        val result = promptEngine.generatePrompts(intent, metadata)
        
        println("\nFlux Prompt: ${result.fluxMasterPrompt}")
        println("\nUI: ${result.uiSuggestion}")
        println("\nTokens: ${result.metadata.tokenCount}, Complexity: ${result.metadata.sceneComplexity}")
    }
}
