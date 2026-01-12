package com.identitylens.app.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Prompt Engineering screen
 */
class PromptEngineViewModel : ViewModel() {
    
    private val promptEngine = PromptEngine()
    
    private val _state = MutableStateFlow<PromptEngineState>(PromptEngineState.Idle)
    val state: StateFlow<PromptEngineState> = _state.asStateFlow()
    
    private val _output = MutableStateFlow<PromptEngineOutput?>(null)
    val output: StateFlow<PromptEngineOutput?> = _output.asStateFlow()
    
    /**
     * Generate prompts from user intent
     */
    fun generatePrompts(userIntent: String, imageMetadata: ImageMetadata) {
        viewModelScope.launch {
            try {
                _state.value = PromptEngineState.Processing(
                    stage = ProcessingStage.PARSING_INTENT
                )
                
                val result = promptEngine.generatePrompts(userIntent, imageMetadata)
                
                _output.value = result
                _state.value = PromptEngineState.Success(result)
                
            } catch (e: Exception) {
                _state.value = PromptEngineState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset state
     */
    fun reset() {
        _state.value = PromptEngineState.Idle
        _output.value = null
    }
}

/**
 * UI state for Prompt Engine
 */
sealed class PromptEngineState {
    object Idle : PromptEngineState()
    
    data class Processing(
        val stage: ProcessingStage
    ) : PromptEngineState()
    
    data class Success(
        val output: PromptEngineOutput
    ) : PromptEngineState()
    
    data class Error(
        val message: String
    ) : PromptEngineState()
}
