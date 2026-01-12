# IdentityLens - Prompt Engine Kullanım Kılavuzu

## 🎯 Genel Bakış

Prompt Engine, kullanıcının doğal dil talebini (Türkçe) alıp Flux.1 PuLID ve Gemini 2.5 Flash için optimize edilmiş teknik komutlara dönüştürür.

## 🚀 Hızlı Başlangıç

### Temel Kullanım

```kotlin
// 1. Prompt Engine oluştur
val promptEngine = PromptEngine()

// 2. Kullanıcı isteği (Türkçe doğal dil)
val userIntent = "Beni 1920'ler Paris'inde, yağmurlu bir sokakta, şık bir takım elbiseyle göster."

// 3. Görüntü metadata'sı (Adım 1'den)
val imageMetadata = ImageMetadata(
    gender = "male",
    skinTone = "wheatish",
    currentPose = "standing_straight"
)

// 4. Prompt oluştur
val result = promptEngine.generatePrompts(userIntent, imageMetadata)

// 5. Sonuçları kullan
println("Flux Prompt: ${result.fluxMasterPrompt}")
println("Gemini Instruction: ${result.geminiEditInstruction}")
println("UI Feedback: ${result.uiSuggestion}")
```

## 📦 Çıktı Formatı

Prompt Engine şu JSON yapısını döner:

```json
{
  "gemini_edit_instruction": "...",
  "flux_master_prompt": "...",
  "negative_prompt": "...",
  "lighting_params": {
    "direction": "side",
    "ambient": "blue_night",
    "intensity": "medium",
    "temperature": 3200
  },
  "ui_suggestion": "Paris sokakları aydınlatılıyor... ✨",
  "metadata": {
    "token_count": 87,
    "scene_complexity": "moderate",
    "optimization_applied": true
  }
}
```

## 🎨 Desteklenen Özellikler

### Dönem/Era
- `"1920'ler"` → "1920s era"
- `"1950'lerde"` → "1950s"
- `"Victorian"` → "Victorian era"
- `"Medieval/Ortaçağ"` → "Medieval"

### Lokasyon
- `"Paris'te"` → "Paris street"
- `"New York'ta"` → "New York city"
- `"Tokyo'da"` → "Tokyo"
- `"sahilde"` → "beach"
- `"ormanda"` → "forest"

### Hava Durumu
- `"yağmurlu"` → "rainy"
- `"karlı"` → "snowy"
- `"güneşli"` → "sunny"
- `"sisli"` → "foggy"

### Zaman
- `"gece"` → "night"
- `"gün batımında"` → "sunset"
- `"sabah"` → "sunrise"
- `"öğlen"` → "noon"

### Kıyafet
- `"takım elbise"` → "suit"
- `"elbise"` → "dress"
- `"vintage kıyafet"` → "vintage clothing"

### Ruh Hali
- `"romantik"` → "romantic"
- `"dramatik"` → "dramatic"
- `"sakin"` → "peaceful"

## 💡 Örnek Senaryolar

### Senaryo 1: Vintage Paris
```kotlin
val intent = "Beni 1920'ler Paris'inde, yağmurlu bir sokakta, şık bir takım elbiseyle göster."
val result = promptEngine.generatePrompts(intent, metadata)

// Flux Prompt:
// "A person with exact facial features from reference, standing on rainy 1920s 
//  Paris street, wearing vintage suit, illuminated by street lamp from side, 
//  warm light, with reflections on wet pavement, puddles, windows, light rain, 
//  atmospheric haze, film noir aesthetic, golden age cinema, Art Deco, 
//  photorealistic 8k, sharp focus"
```

### Senaryo 2: Modern Tokyo
```kotlin
val intent = "Tokyo sokaklarında, gece, neon ışıklar altında göster."
val result = promptEngine.generatePrompts(intent, metadata)

// UI Feedback: "Tokyo gece ışıkları hazırlanıyor... ✨"
// Lighting: { direction: "front", ambient: "neon_glow", temperature: 6500 }
```

### Senaryo 3: Sahil Günü
```kotlin
val intent = "Sahilde, güneşli bir günde, plaj kıyafetiyle göster."
val result = promptEngine.generatePrompts(intent, metadata)

// Scene Complexity: "simple"
// Reflections: ["water", "sand"]
```

## ⚙️ Gelişmiş Kullanım

### ViewModel ile Entegrasyon

```kotlin
class MyActivity : AppCompatActivity() {
    
    private val viewModel: PromptEngineViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe state
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is PromptEngineState.Processing -> {
                        showFeedback(state.stage)
                    }
                    is PromptEngineState.Success -> {
                        val output = state.output
                        sendToAI(output)
                    }
                    is PromptEngineState.Error -> {
                        showError(state.message)
                    }
                }
            }
        }
        
        // Generate prompts
        viewModel.generatePrompts(userIntent, imageMetadata)
    }
}
```

### Özelleştirme

#### Intent Parser'a Yeni Pattern Eklemek

```kotlin
// IntentParser.kt dosyasında
private val LOCATION_PATTERNS = mapOf(
    // Var olanlar...
    "mağaza|market|shopping" to "shopping mall",  // YENİ
    "cafe|kahve" to "cafe"                         // YENİ
)
```

#### Negatif Prompt Eklemek

```kotlin
// NegativePromptGenerator.kt dosyasında
private val CUSTOM_NEGATIVES = listOf(
    "your custom negative",
    "another negative"
)
```

## 🎯 Zero-Shot Prensipleri

### Identity Preservation

Prompt Engine, PuLID için özel olarak "zero-shot" yaklaşımı kullanır:

```
✅ DOĞRU:
"A person with the exact facial features from the reference image"

❌ YANLIŞ:
"Generate an image of [name]"
"Create a photo similar to reference"
```

### Transformation Instructions

```
✅ DOĞRU:
"Change ONLY the clothing, preserve ALL facial characteristics"

❌ YANLIŞ:
"Make the person wear a suit" (belirsiz)
```

## 📊 Performans Optimizasyonu

### Token Limitleri

- **Flux Master Prompt**: 75-100 token (hedef)
- **Negative Prompt**: 40-60 token
- **Toplam**: < 160 token (hızlı inference)

### Optimization Stratejileri

1. **Synonym Reduction**: "beautiful, gorgeous, stunning" → "stunning"
2. **Compound Terms**: "dim lighting, moody atmosphere" → "dim moody lighting"
3. **Technical Consolidation**: "8k, uhd, high res" → "8k uhd sharp"

### İşlem Süreleri

- Intent Parsing: ~50ms
- Scene Reasoning: ~100ms
- Prompt Building: ~50ms
- Optimization: ~30ms
- **Toplam**: ~230ms

## 🔧 Sorun Giderme

### Problem: Intent tanınmıyor

```kotlin
// Debug için parsed intent'i kontrol edin
val parsed = intentParser.parse(userIntent)
println("Parsed: ${parsed}")

// Manuel override
val customParsed = ParsedIntent(
    era = "1920s",
    location = "Paris street",
    // ...
)
```

### Problem: Token sayısı çok yüksek

```kotlin
// Token sayısını kontrol edin
val tokenCount = optimizer.estimateTokens(prompt)
println("Tokens: $tokenCount")

// Manuel optimizasyon
val optimized = optimizer.optimize(prompt)
```

### Problem: Negatif promptlar yetersiz

```kotlin
// Ek negatifler ekleyin
val additionalNegatives = listOf("custom negative 1", "custom negative 2")
val allNegatives = negativeGenerator.generate(intent, scene) + additionalNegatives
```

## 🌍 Çoklu Dil Desteği

Şu anda Türkçe desteklenmektedir. İngilizce eklemek için:

```kotlin
// IntentParser.kt
when (detectedLanguage) {
    "tr" -> parseTurkish(text)
    "en" -> parseEnglish(text)
    else -> parseTurkish(text)  // default
}
```

## 📱 UI Entegrasyonu

### Progress Göstergesi

```kotlin
val stages = listOf(
    ProcessingStage.PARSING_INTENT,     // "İsteğiniz analiz ediliyor... 🔍"
    ProcessingStage.ANALYZING_SCENE,    // "Sahne detayları belirleniyor... 🎬"
    ProcessingStage.OPTIMIZING_PROMPT,  // "Komutlar optimize ediliyor... ⚡"
    ProcessingStage.GENERATING          // "Görseliniz oluşturuluyor... 🎨"
)

stages.forEach { stage ->
    val message = uiFeedbackGenerator.generateProgressMessage(stage)
    showProgressMessage(message)
}
```

### Dinamik Feedback

```kotlin
// Context-aware feedback
val feedback = uiFeedbackGenerator.generate(parsedIntent, sceneAnalysis)
// "Paris sokakları 1920'ler dönemi yağmurlu gece ışıkları hazırlanıyor... 🌧️"
```

## 🔗 API Entegrasyonu

### Gemini API (Gelecek Versiyon)

```kotlin
// SceneReasoner.kt'da yorum satırını kaldırın
suspend fun analyzeSceneWithGemini(...): SceneAnalysis {
    val geminiResponse = geminiClient.generateContent(
        prompt = buildGeminiPrompt(...),
        temperature = 0.3
    )
    return parseGeminiResponse(geminiResponse)
}
```

### Flux.1 PuLID API

```kotlin
// Generated prompts kullanarak
val fluxRequest = FluxRequest(
    prompt = result.fluxMasterPrompt,
    negativePrompt = result.negativePrompt,
    referenceImage = userPhoto,
    guidanceScale = 7.5
)

val generatedImage = fluxClient.generate(fluxRequest)
```

---

**Not**: Prompt Engine sürekli geliştirilmektedir. Yeni pattern'lar ve optimizasyonlar eklenebilir.
