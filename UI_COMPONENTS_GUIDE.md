# IdentityLens UI Components - Jetpack Compose

Minimalist interactive UI for Style & Scene selection with AI-powered customization.

## 🎨 Components

### 1. StyleSelectorScreen
Main screen coordinating all UI elements.

**Features:**
- Dynamic style carousel
- Clothing modifier chips
- Inpainting mask tool
- Preview engine with state machine
- Progressive feedback

### 2. DynamicStyleCarousel
Horizontal scrolling style categories.

**Styles:**
- Cyberpunk
- Renaissance
- Professional
- Vintage
- Cinematic
- Anime
- Fantasy

### 3. ClothingModifier
Quick clothing selection with chips.

**Presets:**
- Takım Elbise
- Deri Ceket
- Vintage Gömlek
- Spor Kıyafet
- And more...

### 4. InpaintingMaskDialog
Touch-based mask drawing for selective editing.

**Features:**
- Touch drawing
- Brush size control
- Undo/Clear
- Real-time preview

### 5. PreviewEngine
Multi-state preview system.

**States:**
- Idle (empty)
- Loading (animated progress)
- Previewing (low-res with actions)
- Finalizing (high-res generation)
- Complete (final image)
- Error (retry option)

## 🚀 Usage

### Basic Implementation

```kotlin
@Composable
fun MyApp() {
    val identityPacket = // From Step 1
    val imageUri = // Captured image URI
    
    StyleSelectorScreen(
        identityPacket = identityPacket,
        imageUri = imageUri,
        onBackClick = { /* Navigate back */ },
        onComplete = { finalImageUrl ->
            // Show final result
        }
    )
}
```

### With Navigation

```kotlin
NavHost(navController, startDestination = "capture") {
    composable("capture") {
        CaptureActivity()
    }
    composable("styleSelector") {
        StyleSelectorScreen(
            identityPacket = savedPacket,
            imageUri = savedUri,
            onComplete = { url ->
                navController.navigate("result/$url")
            }
        )
    }
}
```

## 📊 State Flow

```
User Action → ViewModel → State Update → UI Recomposition
```

**Example:**
1. User selects "Cyberpunk" style
2. ViewModel calls `selectStyle(cyberpunk)`
3. State → `Loading(progress=0.1, message="...")`
4. API call to Cloud Inference
5. State → `Previewing(imageUrl, style, ...)`
6. UI shows preview with actions

## 🎯 State Machine

```
Idle → Loading → Previewing ← Regenerate
                      ↓
                 Finalizing
                      ↓
                  Complete
```

## 🔧 Customization

### Add New Style

```kotlin
val newStyle = StyleCategory(
    id = "steampunk",
    displayName = "Steampunk",
    description = "Victorian + Technology",
    promptModifier = "steampunk style, Victorian era, brass gears, steam-powered",
    icon = "build"
)

DefaultStyles.CATEGORIES += newStyle
```

### Custom Feedback Messages

```kotlin
object CustomMessages {
    val CUSTOM_MESSAGES = listOf(
        "Magic happening... ✨",
        "AI working hard... 🤖",
        "Pixels aligning... 🎨"
    )
}
```

### Modify Theme

```kotlin
MaterialTheme(
    colors = lightColors(
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC6)
    )
) {
    StyleSelectorScreen(...)
}
```

## 📱 Performance

### Optimizations

1. **Lazy Loading**: Styles loaded only when visible
2. **Image Caching**: Coil handles preview caching
3. **State Hoisting**: Minimal recompositions
4. **Remember**: Expensive calculations cached

### Metrics

- Initial render: ~100ms
- Style selection: ~50ms
- State update: ~16ms (60 FPS)

## ⚙️ Advanced Features

### Inpainting Integration

```kotlin
viewModel.applyInpaintingMask(drawnMask)
// Mask sent with API request
```

### Progress Tracking

```kotlin
cloudClient.generateWithRetry(
    ...,
    onProgress = { message ->
        // Update UI with custom progress
    }
)
```

### Error Handling

All errors show user-friendly messages:
- "Yüz algılanamadı" 
- "İşlem zaman aşımına uğradı"
- "İstek limiti aşıldı"

## 🧪 Testing

### Preview Testing

```kotlin
@Preview
@Composable
fun PreviewStyleCard() {
    StyleCard(
        style = DefaultStyles.CATEGORIES[0],
        isSelected = true,
        onClick = {}
    )
}
```

### State Testing

```kotlin
@Test
fun `test state transition from idle to loading`() {
    val viewModel = StyleSelectorViewModel(...)
    viewModel.selectStyle(testStyle)
    
    assertEquals(
        StyleSelectorState.Loading::class,
        viewModel.state.value::class
    )
}
```

## 📚 Dependencies

```gradle
// Jetpack Compose
implementation "androidx.compose.ui:ui:1.5.4"
implementation "androidx.compose.material:material:1.5.4"
implementation "androidx.activity:activity-compose:1.8.1"

// ViewModel
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2"

// Image Loading
implementation "io.coil-kt:coil-compose:2.5.0"

// Utilities
implementation "com.google.accompanist:accompanist-flowlayout:0.32.0"
```

## 🎨 Design Principles

### Minimalism
- No technical jargon exposed to users
- Clean, uncluttered interface
- Focus on visual choices

### Intelligence
- AI-powered style suggestions
- Automatic parameter optimization
- Smart defaults

### Feedback
- Real-time progress updates
- Clear error messages
- Visual confirmations

---

**IdentityLens Interactive UI** - Powered by Jetpack Compose 🎨
