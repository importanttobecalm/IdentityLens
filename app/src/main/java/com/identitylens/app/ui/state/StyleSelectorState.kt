package com.identitylens.app.ui.state

import android.graphics.Bitmap
import com.identitylens.app.prompt.LightingParams

/**
 * UI State for Style Selector Screen
 */
sealed class StyleSelectorState {
    object Idle : StyleSelectorState()
    
    data class Loading(
        val progress: Float,
        val message: String
    ) : StyleSelectorState()
    
    data class Previewing(
        val previewImageUrl: String,
        val selectedStyle: StyleCategory,
        val modifications: List<Modification>
    ) : StyleSelectorState()
    
    data class Finalizing(
        val progress: Float,
        val message: String,
        val lowResPreview: String? = null
    ) : StyleSelectorState()
    
    data class Complete(
        val finalImageUrl: String,
        val metadata: GenerationMetadata
    ) : StyleSelectorState()
    
    data class Error(
        val message: String,
        val canRetry: Boolean
    ) : StyleSelectorState()
}

/**
 * Style category for dynamic carousel
 */
data class StyleCategory(
    val id: String,
    val displayName: String,
    val thumbnailUrl: String? = null,
    val description: String,
    val promptModifier: String,
    val icon: String? = null  // Material icon name
)

/**
 * User modification
 */
data class Modification(
    val type: ModificationType,
    val value: Any
)

enum class ModificationType {
    CLOTHING,
    POSE,
    BACKGROUND,
    LIGHTING,
    INPAINTING_MASK
}

/**
 * Generation metadata
 */
data class GenerationMetadata(
    val inferenceTime: Float,
    val modelVersion: String,
    val seed: Int
)

/**
 * Feedback messages for progressive loading
 */
object FeedbackMessages {
    val PROCESSING_MESSAGES = listOf(
        "Yüz özellikleri analiz ediliyor... 🔍",
        "Sahne oluşturuluyor... 🎨",
        "Işıklar ayarlanıyor... 💡",
        "Detaylar ekleniyor... ✨",
        "Son rötuşlar yapılıyor... 🖌️"
    )
    
    val FINALIZING_MESSAGES = listOf(
        "Yüksek çözünürlük işleniyor... 📸",
        "Doku detayları iyileştiriliyor... 🎯",
        "Renk harmonizasyonu yapılıyor... 🌈",
        "Netlik artırılıyor... 🔬",
        "Final render tamamlanıyor... ⏳"
    )
    
    fun getProgressMessage(stage: ProcessingStage, progress: Float): String {
        val messages = when (stage) {
            ProcessingStage.PREVIEW -> PROCESSING_MESSAGES
            ProcessingStage.FINAL -> FINALIZING_MESSAGES
        }
        
        val index = (progress * messages.size).toInt().coerceIn(0, messages.size - 1)
        return messages[index]
    }
}

enum class ProcessingStage {
    PREVIEW,
    FINAL
}

/**
 * Default style categories
 */
object DefaultStyles {
    val CATEGORIES = listOf(
        StyleCategory(
            id = "cyberpunk",
            displayName = "Cyberpunk",
            description = "Neon ışıklar, gelecek",
            promptModifier = "cyberpunk style, neon lights, futuristic city, digital art",
            icon = "flash_on"
        ),
        StyleCategory(
            id = "renaissance",
            displayName = "Rönesans",
            description = "Klasik sanat",
            promptModifier = "renaissance style, classical art, oil painting, museum quality",
            icon = "palette"
        ),
        StyleCategory(
            id = "professional",
            displayName = "Profesyonel",
            description = "İş dünyası",
            promptModifier = "professional business portrait, corporate style, clean background",
            icon = "business_center"
        ),
        StyleCategory(
            id = "vintage",
            displayName = "Vintage",
            description = "Nostaljik",
            promptModifier = "vintage style, retro aesthetic, film photography, nostalgic",
            icon = "camera_alt"
        ),
        StyleCategory(
            id = "cinematic",
            displayName = "Sinematik",
            description = "Film sahnesi",
            promptModifier = "cinematic lighting, movie scene, dramatic atmosphere, Hollywood style",
            icon = "movie"
        ),
        StyleCategory(
            id = "anime",
            displayName = "Anime",
            description = "Japon animasyonu",
            promptModifier = "anime style, manga art, Japanese animation, vibrant colors",
            icon = "auto_awesome"
        ),
        StyleCategory(
            id = "fantasy",
            displayName = "Fantezi",
            description = "Büyülü dünya",
            promptModifier = "fantasy art, magical atmosphere, ethereal lighting, mystical",
            icon = "auto_fix_high"
        )
    )
}

/**
 * Clothing presets
 */
object ClothingPresets {
    val ITEMS = listOf(
        "Takım Elbise",
        "Deri Ceket",
        "Vintage Gömlek",
        "Spor Kıyafet",
        "Elbise",
        "Smokin",
        "Astronot Kıyafeti",
        "Ortaçağ Zırhı",
        "Casual Tişört",
        "Hoodie",
        "Palto"
    )
}
