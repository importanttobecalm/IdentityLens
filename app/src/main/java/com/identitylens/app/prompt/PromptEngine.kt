package com.identitylens.app.prompt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * Prompt Engine - Main orchestrator for semantic bridge
 * 
 * Transforms user intent and metadata into optimized AI prompts
 */
class PromptEngine(
    private val intentParser: IntentParser = IntentParser(),
    private val sceneReasoner: SceneReasoner = SceneReasoner(),
    private val fluxBuilder: FluxPromptBuilder = FluxPromptBuilder(),
    private val negativeGenerator: NegativePromptGenerator = NegativePromptGenerator(),
    private val optimizer: PromptOptimizer = PromptOptimizer(),
    private val uiFeedbackGenerator: UIFeedbackGenerator = UIFeedbackGenerator()
) {
    
    /**
     * Generate optimized prompts from user intent and image metadata
     * 
     * @param userIntent Natural language description in Turkish
     * @param imageMetadata Visual metadata from Step 1 (face capture)
     * @return Complete prompt package ready for AI models
     */
    suspend fun generatePrompts(
        userIntent: String,
        imageMetadata: ImageMetadata
    ): PromptEngineOutput {
        val processingTime = measureTimeMillis {
            return@measureTimeMillis processInternal(userIntent, imageMetadata)
        }
        
        val result = processInternal(userIntent, imageMetadata)
        
        // Add processing time to metadata
        return result.copy(
            metadata = result.metadata.copy(processingTimeMs = processingTime)
        )
    }
    
    private suspend fun processInternal(
        userIntent: String,
        imageMetadata: ImageMetadata
    ): PromptEngineOutput = withContext(Dispatchers.Default) {
        
        // Step 1: Parse natural language intent
        val parsedIntent = intentParser.parse(userIntent)
        
        // Step 2: Analyze scene with reasoning engine
        val sceneAnalysis = sceneReasoner.analyzeScene(parsedIntent, imageMetadata)
        
        // Step 3: Build Flux.1 PuLID prompt
        val fluxPrompt = fluxBuilder.build(parsedIntent, sceneAnalysis, imageMetadata)
        
        // Step 4: Generate negative prompts
        val negatives = negativeGenerator.generate(parsedIntent, sceneAnalysis)
        
        // Step 5: Optimize both prompts for token efficiency
        val optimizedFlux = optimizer.optimize(fluxPrompt)
        val optimizedNegatives = optimizer.optimizeNegatives(negatives)
        
        // Step 6: Create Gemini edit instruction
        val geminiInstruction = buildGeminiInstruction(parsedIntent, sceneAnalysis, imageMetadata)
        
        // Step 7: Generate UI feedback
        val uiFeedback = uiFeedbackGenerator.generate(parsedIntent, sceneAnalysis)
        
        // Step 8: Assemble final output
        PromptEngineOutput(
            geminiEditInstruction = geminiInstruction,
            fluxMasterPrompt = optimizedFlux,
            negativePrompt = optimizedNegatives,
            lightingParams = sceneAnalysis.lighting.toParams(),
            uiSuggestion = uiFeedback,
            metadata = PromptMetadata(
                tokenCount = optimizer.estimateTokens(optimizedFlux),
                sceneComplexity = sceneAnalysis.complexity,
                optimizationApplied = true
            )
        )
    }
    
    /**
     * Build Gemini edit instruction (natural language description for Gemini)
     */
    private fun buildGeminiInstruction(
        intent: ParsedIntent,
        scene: SceneAnalysis,
        metadata: ImageMetadata
    ): String {
        val components = mutableListOf<String>()
        
        // Core instruction
        components.add("Transform the person into this scene while preserving their facial identity exactly:")
        
        // Scene description
        val sceneDesc = buildSceneDescription(intent, scene)
        components.add(sceneDesc)
        
        // Clothing transformation
        intent.outfit?.let { outfit ->
            val era = intent.era?.let { "$it " } ?: ""
            components.add("Replace clothing with period-accurate ${era}${outfit}.")
        }
        
        // Lighting instructions
        val lightingDesc = buildLightingDescription(scene.lighting)
        components.add(lightingDesc)
        
        // Atmosphere
        if (scene.atmosphere.effects.isNotEmpty()) {
            components.add("Add atmospheric effects: ${scene.atmosphere.effects.joinToString(", ")}.")
        }
        
        // Quality reminder
        components.add("Maintain photorealistic quality with sharp details and accurate period elements.")
        
        return components.joinToString(" ")
    }
    
    private fun buildSceneDescription(intent: ParsedIntent, scene: SceneAnalysis): String {
        val parts = mutableListOf<String>()
        
        if (intent.era != null) {
            parts.add("${intent.era} era")
        }
        
        parts.add(intent.location)
        
        intent.timeOfDay?.let { parts.add("during $it") }
        intent.weather?.let { parts.add("with $it weather") }
        
        return parts.joinToString(" ")
    }
    
    private fun buildLightingDescription(lighting: LightingSetup): String {
        val parts = mutableListOf<String>()
        
        with(lighting.primary) {
            parts.add("Lighting: primary $type from $direction ($temperature K $color light)")
        }
        
        if (lighting.ambient.isNotEmpty()) {
            parts.add("ambient $lighting.ambient)")
        }
        
        if (lighting.reflections.isNotEmpty()) {
            parts.add("with reflections on ${lighting.reflections.joinToString(", ")}")
        }
        
        return parts.joinToString(", ")
    }
}
