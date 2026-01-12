package com.identitylens.app.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

/**
 * Inpainting Mask Dialog
 * Allows user to draw mask for selective editing
 */
@Composable
fun InpaintingMaskDialog(
    imageUri: String,
    onMaskConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var brushSize by remember { mutableStateOf(25f) }
    var maskPaths by remember { mutableStateOf<List<MaskPath>>(emptyList()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            elevation = 8.dp
        ) {
            Column {
                // Header
                TopAppBar(
                    title = { Text("Alan Seçimi") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Kapat")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (maskPaths.isNotEmpty()) {
                                    maskPaths = maskPaths.dropLast(1)
                                }
                            },
                            enabled = maskPaths.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Undo, "Geri Al")
                        }
                        IconButton(
                            onClick = { maskPaths = emptyList() },
                            enabled = maskPaths.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DeleteOutline, "Temizle")
                        }
                    }
                )
                
                // Canvas for drawing mask
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Base image
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Base image",
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Drawing canvas overlay
                    InpaintingCanvas(
                        paths = maskPaths,
                        brushSize = brushSize,
                        onPathAdded = { path ->
                            maskPaths = maskPaths + path
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Brush size slider
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Fırça Boyutu: ${brushSize.toInt()}",
                        style = MaterialTheme.typography.body2
                    )
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 10f..50f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Confirm button
                Button(
                    onClick = {
                        // Convert paths to bitmap mask
                        val mask = createMaskBitmap(maskPaths, 512, 512)
                        onMaskConfirmed(mask)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = maskPaths.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Uygula")
                }
            }
        }
    }
}

/**
 * Canvas for drawing inpainting mask
 */
@Composable
private fun InpaintingCanvas(
    paths: List<MaskPath>,
    brushSize: Float,
    onPathAdded: (MaskPath) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    
    Canvas(
        modifier = modifier
            .pointerInput(brushSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPath = currentPath + change.position
                    },
                    onDragEnd = {
                        if (currentPath.isNotEmpty()) {
                            onPathAdded(MaskPath(currentPath, brushSize))
                            currentPath = emptyList()
                        }
                    }
                )
            }
    ) {
        // Draw all completed paths
        paths.forEach { maskPath ->
            for (i in 0 until maskPath.points.size - 1) {
                drawLine(
                    color = Color.Green.copy(alpha = 0.5f),
                    start = maskPath.points[i],
                    end = maskPath.points[i + 1],
                    strokeWidth = maskPath.size,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        
        // Draw current path being drawn
        for (i in 0 until currentPath.size - 1) {
            drawLine(
                color = Color.Green.copy(alpha = 0.5f),
                start = currentPath[i],
                end = currentPath[i + 1],
                strokeWidth = brushSize,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * Mask path data
 */
data class MaskPath(
    val points: List<Offset>,
    val size: Float
)

/**
 * Create bitmap from mask paths
 */
private fun createMaskBitmap(
    paths: List<MaskPath>,
    width: Int,
    height: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Fill with black (no mask)
    canvas.drawColor(android.graphics.Color.BLACK)
    
    // Draw white strokes (masked areas)
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    
    paths.forEach { maskPath ->
        paint.strokeWidth = maskPath.size
        
        for (i in 0 until maskPath.points.size - 1) {
            canvas.drawLine(
                maskPath.points[i].x,
                maskPath.points[i].y,
                maskPath.points[i + 1].x,
                maskPath.points[i + 1].y,
                paint
            )
        }
    }
    
    return bitmap
}
