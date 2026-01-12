package com.identitylens.app.ui.viewmodels

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identitylens.app.api.CloudInferenceClient
import com.identitylens.app.api.GenerationMode
import com.identitylens.app.api.GenerationResult
import com.identitylens.app.models.IdentityPacket
import com.identitylens.app.prompt.ImageMetadata
import com.identitylens.app.prompt.PromptEngine
import com.identitylens.app.ui.state.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Style Selector Screen
 */
class StyleSelectorViewModel(
    private val promptEngine: PromptEngine = PromptEngine(),
    private val cloudClient: CloudInferenceClient
) : ViewModel() {
    
    private val _state = MutableStateFlow<StyleSelectorState>(StyleSelectorState.Idle)
    val state: StateFlow<StyleSelectorState> = _state.asStateFlow()
    
    private val _availableStyles = MutableStateFlow(DefaultStyles.CATEGORIES)
    val availableStyles: StateFlow<List<StyleCategory>> = _availableStyles.asStateFlow()
    
    val selectedClothing = mutableStateListOf<String>()
    
    var showInpaintingDialog = mutableStateOf(false)
        private set
    
    var capturedImageUri = ""
        private set
    
    private var identityPacket: IdentityPacket? = null
    private var currentStyle: StyleCategory? = null
    private var inpaintingMask: Bitmap? = null
    private var currentPromptResult: com.identitylens.app.prompt.PromptEngineOutput? = null
    
    /**
     * Initialize with captured identity packet
     */
    fun initialize(packet: IdentityPacket, imageUri: String) {
        identityPacket = packet
        capturedImageUri = imageUri
    }
    
    /**
     * Select a style and generate preview
     */
    fun selectStyle(style: StyleCategory) {
        if (_state.value is StyleSelectorState.Loading || 
            _state.value is StyleSelectorState.Finalizing) {
            return  // Already processing
        }
        
        currentStyle = style
        generatePreview(style)
    }
    
    /**
     * Generate low-res preview
     */
    private fun generatePreview(style: StyleCategory) {
        viewModelScope.launch {
            try {
                _state.value = StyleSelectorState.Loading(
                    progress = 0.1f,
                    message = FeedbackMessages.PROCESSING_MESSAGES[0]
                )
                
                // Build prompt with style and clothing
                val intent = buildUserIntent(style)
                val packet = identityPacket ?: return@launch
                
                val promptResult = promptEngine.generatePrompts(
                    userIntent = intent,
                    imageMetadata = packet.toImageMetadata()
                )
                
                currentPromptResult = promptResult
                
                // Update progress
                updateProgress(0.3f, ProcessingStage.PREVIEW)
                
                // Generate low-res preview
                val result = cloudClient.generateImage(
                    identityPacket = packet,
                    masterPrompt = promptResult.fluxMasterPrompt,
                    negativePrompt = promptResult.negativePrompt,
                    mode = GenerationMode.SPEED,
                    lightingParams = promptResult.lightingParams
                )
                
                handleGenerationResult(result, isPreview = true)
                
            } catch (e: Exception) {
                _state.value = StyleSelectorState.Error(
                    message = "Önizleme oluşturulamadı: ${e.message}",
                    canRetry = true
                )
            }
        }
    }
    
    /**
     * Regenerate preview with current settings
     */
    fun regeneratePreview() {
        currentStyle?.let { style ->
            generatePreview(style)
        }
    }
    
    /**
     * Generate final high-res image
     */
    fun generateFinal() {
        viewModelScope.launch {
            try {
                val currentState = _state.value as? StyleSelectorState.Previewing
                    ?: return@launch
                
                _state.value = StyleSelectorState.Finalizing(
                    progress = 0.1f,
                    message = FeedbackMessages.FINALIZING_MESSAGES[0],
                    lowResPreview = currentState.previewImageUrl
                )
                
                val packet = identityPacket ?: return@launch
                val promptResult = currentPromptResult ?: return@launch
                
                // Update progress
                updateProgress(0.3f, ProcessingStage.FINAL)
                
                // Generate final with quality mode
                val result = cloudClient.generateWithRetry(
                    identityPacket = packet,
                    masterPrompt = promptResult.fluxMasterPrompt,
                    negativePrompt = promptResult.negativePrompt,
                    mode = GenerationMode.QUALITY,
                    lightingParams = promptResult.lightingParams,
                    onProgress = { message ->
                        // Update with progress messages
                    }
                )
                
                handleGenerationResult(result, isPreview = false)
                
            } catch (e: Exception) {
                _state.value = StyleSelectorState.Error(
                    message = "Final görüntü oluşturulamadı: ${e.message}",
                    canRetry = true
                )
            }
        }
    }
    
    /**
     * Start generation (preview or final based on current state)
     */
    fun startGeneration() {
        when (val state = _state.value) {
            is StyleSelectorState.Idle -> {
                currentStyle?.let { generatePreview(it) }
            }
            is StyleSelectorState.Previewing -> {
                generateFinal()
            }
            else -> {
                // Already processing
            }
        }
    }
    
    /**
     * Toggle clothing selection
     */
    fun toggleClothing(item: String) {
        if (selectedClothing.contains(item)) {
            selectedClothing.remove(item)
        } else {
            selectedClothing.add(item)
        }
    }
    
    /**
     * Show inpainting dialog
     */
    fun showInpaintingDialog() {
        showInpaintingDialog.value = true
    }
    
    /**
     * Hide inpainting dialog
     */
    fun hideInpaintingDialog() {
        showInpaintingDialog.value = false
    }
    
    /**
     * Apply inpainting mask
     */
    fun applyInpaintingMask(mask: Bitmap) {
        inpaintingMask = mask
        hideInpaintingDialog()
        
        // TODO: Send mask to API for selective editing
    }
    
    /**
     * Build user intent string from selections
     */
    private fun buildUserIntent(style: StyleCategory): String {
        val parts = mutableListOf<String>()
        
        // Add style
        parts.add("Show me in ${style.promptModifier} style")
        
        // Add clothing if selected
        if (selectedClothing.isNotEmpty()) {
            parts.add("wearing ${selectedClothing.joinToString(", ")}")
        }
        
        return parts.joinToString(", ")
    }
    
    /**
     * Handle generation result
     */
    private fun handleGenerationResult(
        result: GenerationResult,
        isPreview: Boolean
    ) {
        when (result) {
            is GenerationResult.Success -> {
                if (isPreview) {
                    _state.value = StyleSelectorState.Previewing(
                        previewImageUrl = result.imageUrl,
                        selectedStyle = currentStyle!!,
                        modifications = emptyList()
                    )
                } else {
                    _state.value = StyleSelectorState.Complete(
                        finalImageUrl = result.imageUrl,
                        metadata = GenerationMetadata(
                            inferenceTime = result.inferenceTime,
                            modelVersion = result.modelVersion,
                            seed = result.seed
                        )
                    )
                }
            }
            is GenerationResult.Error -> {
                _state.value = StyleSelectorState.Error(
                    message = result.message,
                    canRetry = result.canRetry
                )
            }
        }
    }
    
    /**
     * Update progress with appropriate message
     */
    private fun updateProgress(progress: Float, stage: ProcessingStage) {
        val message = FeedbackMessages.getProgressMessage(stage, progress)
        
        when (val currentState = _state.value) {
            is StyleSelectorState.Loading -> {
                _state.value = currentState.copy(
                    progress = progress,
                    message = message
                )
            }
            is StyleSelectorState.Finalizing -> {
                _state.value = currentState.copy(
                    progress = progress,
                    message = message
                )
            }
            else -> {}
        }
    }
}

/**
 * Extension to convert IdentityPacket to ImageMetadata
 */
private fun IdentityPacket.toImageMetadata(): ImageMetadata {
    return ImageMetadata(
        gender = null,  // Extract from facial data if available
        skinTone = null,
        currentPose = null,
        facialFeatures = emptyMap()
    )
}
