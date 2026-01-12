package com.identitylens.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.identitylens.app.ui.state.StyleSelectorState
import kotlin.math.sin

/**
 * Preview Engine Component
 * Displays preview/final images with state-based rendering
 */
@Composable
fun PreviewEngine(
    state: StyleSelectorState,
    onRegenerateClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        elevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is StyleSelectorState.Idle -> {
                    EmptyPreview()
                }
                
                is StyleSelectorState.Loading -> {
                    LoadingPreview(
                        progress = state.progress,
                        message = state.message
                    )
                }
                
                is StyleSelectorState.Previewing -> {
                    ImagePreview(
                        imageUrl = state.previewImageUrl,
                        onRegenerateClick = onRegenerateClick,
                        onConfirmClick = onConfirmClick
                    )
                }
                
                is StyleSelectorState.Finalizing -> {
                    FinalizingPreview(
                        progress = state.progress,
                        message = state.message,
                        lowResPreview = state.lowResPreview
                    )
                }
                
                is StyleSelectorState.Complete -> {
                    FinalImagePreview(imageUrl = state.finalImageUrl)
                }
                
                is StyleSelectorState.Error -> {
                    ErrorPreview(
                        message = state.message,
                        canRetry = state.canRetry,
                        onRetryClick = onRegenerateClick
                    )
                }
            }
        }
    }
}

/**
 * Empty state preview
 */
@Composable
private fun EmptyPreview() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bir stil seçin ve önizleme oluşturun",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Loading preview with animated progress
 */
@Composable
private fun LoadingPreview(
    progress: Float,
    message: String
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing progress indicator
        Box(contentAlignment = Alignment.Center) {
            // Pulsing background
            val pulseScale by rememberInfiniteTransition().animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Canvas(modifier = Modifier.size(80.dp)) {
                drawCircle(
                    color = Color.Blue.copy(alpha = 0.2f),
                    radius = size.minDimension / 2 * pulseScale
                )
            }
            
            CircularProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.size(60.dp),
                strokeWidth = 4.dp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Animated message
        AnimatedContent(
            targetState = message,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) with
                        fadeOut(animationSpec = tween(300))
            }
        ) { targetMessage ->
            Text(
                text = targetMessage,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Image preview with actions
 */
@Composable
private fun ImagePreview(
    imageUrl: String,
    onRegenerateClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Action buttons overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRegenerateClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Yenile")
            }
            
            Button(
                onClick = onConfirmClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Onayla")
            }
        }
    }
}

/**
 * Finalizing preview (shows low-res while processing high-res)
 */
@Composable
private fun FinalizingPreview(
    progress: Float,
    message: String,
    lowResPreview: String?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Show low-res preview in background
        if (lowResPreview != null) {
            AsyncImage(
                model = lowResPreview,
                contentDescription = "Low res preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                alpha = 0.5f
            )
        }
        
        // Loading overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LoadingPreview(progress, message)
        }
    }
}

/**
 * Final completed image
 */
@Composable
private fun FinalImagePreview(imageUrl: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Final image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Success checkmark animation
        var showCheck by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
            showCheck = true
        }
        
        AnimatedVisibility(
            visible = showCheck,
            enter = scaleIn() + fadeIn()
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Complete",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp),
                tint = Color.Green
            )
        }
    }
}

/**
 * Error preview
 */
@Composable
private fun ErrorPreview(
    message: String,
    canRetry: Boolean,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colors.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        if (canRetry) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onRetryClick) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Tekrar Dene")
            }
        }
    }
}
