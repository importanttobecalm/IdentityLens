package com.identitylens.app.prompt

/**
 * Prompt Optimizer
 * 
 * Optimizes prompt length and token usage for mobile performance
 */
class PromptOptimizer {
    
    companion object {
        // Synonym groups (keep only the most impactful term)
        private val SYNONYM_GROUPS = listOf(
            setOf("beautiful", "gorgeous", "stunning", "attractive") to "stunning",
            setOf("high quality", "best quality", "highest quality") to "high quality",
            setOf("detailed", "intricate", "complex") to "detailed",
            setOf("realistic", "photorealistic", "lifelike") to "photorealistic",
            setOf("sharp", "crisp", "clear") to "sharp"
        )
        
        // Redundant pairs (if both present, keep first)
        private val REDUNDANT_PAIRS = mapOf(
            "photorealistic" to setOf("realistic", "lifelike", "photo-like"),
            "8k" to setOf("high resolution", "4k", "hd", "uhd"),
            "professional photography" to setOf("professional photo", "pro photo")
        )
        
        // Token estimation (rough: 1 token ≈ 4 characters)
        private const val CHARS_PER_TOKEN = 4
    }
    
    /**
     * Optimize prompt for token efficiency
     */
    fun optimize(prompt: String): String {
        var optimized = prompt
        
        // 1. Remove redundant synonyms
        optimized = removeRedundantSynonyms(optimized)
        
        // 2. Consolidate compound terms
        optimized = consolidateCompounds(optimized)
        
        // 3. Remove excessive adjectives
        optimized = removeExcessiveAdjectives(optimized)
        
        // 4. Clean up spacing
        optimized = cleanSpacing(optimized)
        
        return optimized
    }
    
    /**
     * Optimize negative prompts
     */
    fun optimizeNegatives(negatives: List<String>): String {
        // Remove duplicates and join
        val unique = negatives.distinct()
        
        // Group similar concepts
        val grouped = groupSimilarConcepts(unique)
        
        return grouped.joinToString(", ")
    }
    
    /**
     * Estimate token count
     */
    fun estimateTokens(text: String): Int {
        return text.length / CHARS_PER_TOKEN
    }
    
    private fun removeRedundantSynonyms(text: String): String {
        var result = text
        
        SYNONYM_GROUPS.forEach { (synonyms, keep) ->
            val found = synonyms.filter { result.contains(it, ignoreCase = true) }
            if (found.size > 1) {
                // Multiple synonyms found, keep only the preferred one
                found.filter { it != keep }.forEach { synonym ->
                    result = result.replace(Regex("""\b$synonym\b""", RegexOption.IGNORE_CASE), "")
                }
            }
        }
        
        return result
    }
    
    private fun consolidateCompounds(text: String): String {
        var result = text
        
        // Consolidate lighting terms
        result = result.replace(Regex("""dim lighting,?\s*moody atmosphere""", RegexOption.IGNORE_CASE), "dim moody lighting")
        result = result.replace(Regex("""warm light,?\s*golden hour""", RegexOption.IGNORE_CASE), "warm golden hour")
        
        // Consolidate quality terms
        result = result.replace(Regex("""8k,?\s*uhd,?\s*sharp""", RegexOption.IGNORE_CASE), "8k uhd sharp")
        
        return result
    }
    
    private fun removeExcessiveAdjectives(text: String): String {
        // If more than 3 adjectives in a row, keep first 2
        val adjectivePattern = Regex("""(\w+),\s*(\w+),\s*(\w+),\s*(\w+)""")
        return text.replace(adjectivePattern) { match ->
            "${match.groupValues[1]}, ${match.groupValues[2]}"
        }
    }
    
    private fun cleanSpacing(text: String): String {
        return text
            .replace(Regex(""",\s*,"""), ",")           // Remove double commas
            .replace(Regex("""\s+"""), " ")             // Multiple spaces to single
            .replace(Regex(""",\s*$"""), "")            // Trailing comma
            .replace(Regex("""^\s*,"""), "")            // Leading comma
            .trim()
    }
    
    private fun groupSimilarConcepts(negatives: List<String>): List<String> {
        // Group anatomy-related terms
        val anatomyTerms = negatives.filter { it.contains("anatomy") || it.contains("proportions") }
        val facialTerms = negatives.filter { it.contains("face") || it.contains("eyes") }
        val handTerms = negatives.filter { it.contains("hand") || it.contains("finger") }
        val qualityTerms = negatives.filter { it.contains("quality") || it.contains("artifact") }
        
        val grouped = mutableListOf<String>()
        
        // Add grouped terms
        if (anatomyTerms.isNotEmpty()) grouped.add("bad anatomy")
        if (facialTerms.isNotEmpty()) grouped.add("poorly drawn face")
        if (handTerms.isNotEmpty()) grouped.add("poorly drawn hands")
        if (qualityTerms.isNotEmpty()) grouped.add("low quality")
        
        // Add remaining unique terms
        val remaining = negatives.filter { term ->
            !anatomyTerms.contains(term) &&
            !facialTerms.contains(term) &&
            !handTerms.contains(term) &&
            !qualityTerms.contains(term)
        }
        
        grouped.addAll(remaining)
        
        return grouped.distinct()
    }
}
