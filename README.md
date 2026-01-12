# IdentityLens - Smart Image Capture Module

Android uygulaması için AI destekli görüntü yakalama ve ön işleme modülü.

## 📋 Özellikler

### ✨ Akıllı Görüntü Yakalama
- **CameraX Entegrasyonu**: Modern Android kamera API'si ile yüksek kaliteli görüntü yakalama
- **ML Kit Yüz Algılama**: Gerçek zamanlı yüz algılama ve pozisyon kontrolü
- **Blur Algılama**: Laplacian varyans yöntemi ile netlik kontrolü
- **Açı Doğrulama**: Yüz açısının optimal olduğundan emin olma (pitch, yaw, roll)
- **Işık Analizi**: Lux sensörü ile çevre ışık ölçümü
- **Yüz Boyutu Kontrolü**: Yüzün çerçevenin %30-60'ını kaplamasını sağlama

### 🎯 Kalite Kontrol Sistemi
- **Blur Detector**: Görüntü netligi analizi (variance threshold: 100.0)
- **Face Angle Validator**: Euler açıları ile yüz yönelimi kontrolü
- **Image Quality Analyzer**: Kapsamlı kalite puanlama sistemi
- **Real-time Feedback**: Kullanıcıya anlık geri bildirim

### 📦 Cloud API Ready
- **Identity Packet Format**: JSON formatında standart veri paketi
- **468-Point Face Mesh**: ML Kit ile detaylı yüz haritası (opsiyonel)
- **Background Segmentation**: Arka plan ayrıştırma önizlemesi
- **Rich Metadata**: Işık, kamera, cihaz bilgileri

## 🏗️ Proje Yapısı

```
IdentityLens/
├── app/
│   ├── build.gradle                    # Dependency tanımları
│   └── src/main/
│       ├── AndroidManifest.xml         # Permissions & Activities
│       ├── java/com/identitylens/app/
│       │   ├── MainActivity.kt          # Ana ekran
│       │   ├── quality/                 # Kalite kontrol sınıfları
│       │   │   ├── BlurDetector.kt
│       │   │   ├── FaceAngleValidator.kt
│       │   │   └── ImageQualityAnalyzer.kt
│       │   ├── models/                  # Veri modelleri
│       │   │   └── IdentityPacket.kt
│       │   ├── camera/                  # Kamera modülü
│       │   │   └── CaptureActivity.kt
│       │   └── metadata/                # Metadata toplama
│       │       └── LightSensorManager.kt
│       └── res/
│           └── layout/
│               ├── activity_main.xml
│               └── activity_capture.xml
├── build.gradle                        # Root build config
└── settings.gradle
```

## 🚀 Kurulum

### 1. Gereksinimler
- Android Studio Arctic Fox veya üzeri
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Kotlin 1.9.10+

### 2. Projeyi Android Studio'da Açma
```bash
cd IdentityLens
# Android Studio ile açın veya:
code .
```

### 3. Dependencies
Tüm bağımlılıklar `app/build.gradle` dosyasında tanımlıdır:
- CameraX 1.3.1
- ML Kit (Face Detection, Face Mesh, Segmentation)
- Kotlin Coroutines
- Gson
- Retrofit/OkHttp (Cloud API için)

### 4. Build & Run
```bash
# Gradle sync
./gradlew clean build

# Android cihaza yükleme
./gradlew installDebug
```

## 📖 Kullanım

### Temel Akış

1. **Uygulama Başlatma**
   - `MainActivity` açılır
   - "Start Capture" butonuna basın
   - Kamera izni verilir

2. **Görüntü Yakalama**
   - `CaptureActivity` açılır
   - Kamera önizlemesi başlar
   - Gerçek zamanlı kalite geri bildirimi gösterilir
   - Yüzünüzü çerçeveye hizalayın
   - 📷 butonuna basarak fotoğraf çekin

3. **Kalite Kontrolü**
   - Blur algılama çalışır
   - Yüz açısı kontrol edilir
   - Işık seviyesi analiz edilir
   - Kalite puanı hesaplanır

4. **Identity Packet Oluşturma**
   - Geçerli fotoğraflar JSON formatında paketlenir
   - Cloud API'ye gönderilmeye hazır hale gelir

### Kod Örneği

```kotlin
// Kalite analizi
val qualityAnalyzer = ImageQualityAnalyzer()
val result = qualityAnalyzer.analyze(imageProxy, face, luxValue)

if (result.passed) {
    // Identity Packet oluştur
    val packet = IdentityPacket(
        timestamp = getCurrentTimestamp(),
        image = ImageData(...),
        facialData = FacialData(...),
        metadata = CaptureMetadata(...),
        qualityMetrics = QualityMetrics(...)
    )
    
    // JSON'a çevir
    val json = packet.toJson()
    
    // Cloud'a gönder
    uploadToCloud(json)
}
```

## ⚙️ Kalite Parametreleri

### Blur Detection
- **Threshold**: 100.0 (Laplacian variance)
- **Analysis Resolution**: 640x480 (performance için)

### Face Angle
- **Max Pitch**: ±15° (yukarı/aşağı)
- **Max Yaw**: ±15° (sağ/sol)
- **Max Roll**: ±10° (eğim)

### Lighting
- **Min Lux**: 200
- **Max Lux**: 1000
- **Ideal Lux**: 400

### Face Size
- **Min Size**: 30% of frame
- **Max Size**: 60% of frame

## 🌉 Prompt Engine (Semantic Bridge)

### Özellikler
- ✅ Türkçe doğal dil işleme
- ✅ Flux.1 PuLID için optimize edilmiş promptlar
- ✅ Gemini 2.5 Flash entegrasyonu (sahne analizi)
- ✅ Dinamik negatif prompt oluşturma
- ✅ Token optimizasyonu (< 160 token)
- ✅ Zero-shot kimlik koruma

### Kullanım

```kotlin
val promptEngine = PromptEngine()

val result = promptEngine.generatePrompts(
    userIntent = "Beni 1920'ler Paris'inde, yağmurlu bir sokakta göster.",
    imageMetadata = metadata
)

// Flux Prompt: "A person with exact facial features from reference..."
// Gemini Instruction: "Transform the person into 1920s Paris..."
// UI Feedback: "Paris sokakları 1920'ler dönemi hazırlanıyor... 🌧️"
```

Detaylı kullanım için: [PROMPT_ENGINE_GUIDE.md](PROMPT_ENGINE_GUIDE.md)

## ☁️ Cloud Inference Pipeline

### Mimari

```
Android App → FastAPI Server → Fal.ai → Flux.1 + PuLID → Harmonization → Output
```

### Backend Setup

```bash
cd backend
pip install -r requirements.txt

# Configure
cp .env.example .env
# Edit .env: FAL_API_KEY=your_key_here

# Run server
python api_server.py
```

### Android Entegrasyonu

```kotlin
val client = CloudInferenceClient(
    baseUrl = "https://your-server.com",
    apiKey = "your_api_key"
)

val result = client.generateWithRetry(
    identityPacket = identityPacket,
    masterPrompt = promptEngineOutput.fluxMasterPrompt,
    negativePrompt = promptEngineOutput.negativePrompt,
    mode = GenerationMode.SPEED
)

if (result is GenerationResult.Success) {
    // Load image from result.imageUrl
}
```

### Model Konfigürasyonu

**Speed Mode (Flux schnell):**
- Inference: 4-6 saniye
- Kalite: Çok İyi
- Maliyet: ~$0.025/görsel

**Quality Mode (Flux dev):**
- Inference: 8-10 saniye
- Kalite: Mükemmel
- Maliyet: ~$0.055/görsel

### PuLID Ayarları

- **Fidelity Weight**: 0.85 (kimlik benzerliği)
- **Harmonization**: 0.40 denoising (cilt dokusunu korur)
- **Face Detection**: RetinaFace
- **Embedding**: ArcFace R100

Detaylı dokümantasyon: [backend/README.md](backend/README.md)

## 🎨 Interactive UI (Jetpack Compose)

### Mimari

```
CaptureActivity → StyleSelectorScreen → ResultScreen
```

### Özellikler

- ✅ **Dynamic Style Carousel**: Gemini-önerili stil kategorileri
- ✅ **Clothing Modifier**: 11 hazır kıyafet seçeneği
- ✅ **Inpainting Mask Tool**: Dokunmatik maskeleme aracı
- ✅ **Preview Engine**: 3-aşamalı önizleme (Low-Res → Review → Final)
- ✅ **Progressive Feedback**: Türkçe durum mesajları
- ✅ **Minimalist Design**: Teknik detaylar gizli

### Ana Bileşenler

#### 1. DynamicStyleCarousel
7 stil kategorisi:
- Cyberpunk 🌃
- Rönesans🎨
- Profesyonel 💼
- Vintage 📷
- Sinematik 🎬
- Anime ✨
- Fantezi 🔮

#### 2. InpaintingMaskDialog
- Fırça boyutu kontrolü (10-50px)
- Geri al / Temizle
- Dokunmatik çizim

#### 3. PreviewEngine
**State Machine:**
```
Idle → Loading → Previewing → Finalizing → Complete
```

### Kullanım

```kotlin
StyleSelectorScreen(
    identityPacket = identityPacket,
    imageUri = imageUri,
    onComplete = { finalImageUrl ->
        // Sonuç göster
    }
)
```

### State Yönetimi

```kotlin
sealed class StyleSelectorState {
    object Idle
    data class Loading(progress: Float, message: String)
    data class Previewing(imageUrl: String, selectedStyle: StyleCategory)
    data class Finalizing(progress: Float, message: String)
    data class Complete(imageUrl: String, metadata: GenerationMetadata)
    data class Error(message: String, canRetry: Boolean)
}
```

Detaylı kullanım: [UI_COMPONENTS_GUIDE.md](UI_COMPONENTS_GUIDE.md)

## 🔧 Özelleştirme

### Threshold Değerlerini Ayarlama

```kotlin
// BlurDetector.kt içinde
private const val SHARP_THRESHOLD = 100.0  // Daha düşük = daha toleranslı

// FaceAngleValidator.kt içinde
private const val MAX_PITCH = 15.0f  // Degrees
private const val MAX_YAW = 15.0f
private const val MAX_ROLL = 10.0f

// ImageQualityAnalyzer.kt içinde
private const val MIN_LUX = 200.0
private const val MAX_LUX = 1000.0
```

### Cloud API Endpoint'i Değiştirme

```kotlin
val cloudClient = CloudInferenceClient(
    baseUrl = "https://your-custom-server.com",
    apiKey = "your_api_key"
)
```

## 📊 JSON Schema

Identity Packet formatı:
```json
{
  "version": "1.0",
  "captureId": "uuid",
  "timestamp": "2026-01-12T17:00:00Z",
  "image": { "cleanFace": "base64", "resolution": {...} },
  "facialData": { "faceMesh": {...}, "eulerAngles": {...} },
  "segmentation": { "backgroundMask": "base64" },
  "metadata": { "lighting": {...}, "camera": {...}, "device": {...} },
  "qualityMetrics": { "overallScore": 0.95, "passed": true }
}
```

## 🧪 Test Etme

### Android Emulator
```bash
# Emulator başlat
emulator -avd Pixel_5_API_34

# Uygulamayı yükle
./gradlew installDebug
```

### Fiziksel Cihaz
1. USB Debugging'i etkinleştirin
2. Cihazı bilgisayara bağlayın
3. Android Studio'dan "Run" butonuna basın

## 📝 Notlar

### Performans İpuçları
- Blur detection için görüntü 640x480'e düşürülür
- ML Kit FAST mode kullanılır
- Background segmentation 512x512 resolution'da çalışır
- Coroutines ile async processing yapılır

### Güvenlik
- Kamera izni runtime'da istenir
- Görüntüler geçici olarak işlenir
- JSON transmission HTTPS üzerinden yapılmalı

## 🤝 Katkıda Bulunma

Bu modül IdentityLens projesi için geliştirilmiştir. 

## 📄 Lisans

MIT License - Detaylar için LICENSE dosyasına bakın.

## 📞 Destek

Sorularınız için:
- GitHub Issues
- Email: support@identitylens.com

---

**IdentityLens** - AI-Powered Smart Image Capture 📸✨
