package com.identitylens.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identitylens.app.models.IdentityPacket
import com.identitylens.app.ui.components.*
import com.identitylens.app.ui.state.StyleSelectorState
import com.identitylens.app.ui.viewmodels.StyleSelectorViewModel

/**
 * Style Selector Screen
 * Main interactive UI for customizing AI generation
 */
@Composable
fun StyleSelectorScreen(
    identityPacket: IdentityPacket,
    imageUri: String,
    viewModel: StyleSelectorViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onComplete: (String) -> Unit = {}
) {
    // Initialize ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize(identityPacket, imageUri)
    }
    
    // Collect states
    val state by viewModel.state.collectAsState()
    val styles by viewModel.availableStyles.collectAsState()
    val showInpaintingDialog by viewModel.showInpaintingDialog
    
    // Handle completion
    LaunchedEffect(state) {
        if (state is StyleSelectorState.Complete) {
            val finalUrl = (state as StyleSelectorState.Complete).finalImageUrl
            onComplete(finalUrl)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stil Seçimi") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Preview Engine (top section)
            PreviewEngine(
                state = state,
                onRegenerateClick = { viewModel.regeneratePreview() },
                onConfirmClick = { viewModel.generateFinal() },
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Style Selection Section
            Text(
                text = "Stil",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            DynamicStyleCarousel(
                styles = styles,
                selectedStyle = (state as? StyleSelectorState.Previewing)?.selectedStyle,
                onStyleSelected = { viewModel.selectStyle(it) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Clothing Modification Section
            ClothingModifier(
                selectedClothing = viewModel.selectedClothing.toList(),
                onClothingSelected = { viewModel.toggleClothing(it) },
                onInpaintingMaskClick = { viewModel.showInpaintingDialog() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Button
            Button(
                onClick = { viewModel.startGeneration() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                enabled = state is StyleSelectorState.Idle || 
                         state is StyleSelectorState.Previewing
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (state) {
                        is StyleSelectorState.Idle -> "Önizleme Oluştur"
                        is StyleSelectorState.Previewing -> "Tam Çözünürlük Oluştur"
                        is StyleSelectorState.Loading -> "İşleniyor..."
                        is StyleSelectorState.Finalizing -> "Tamamlanıyor..."
                        is StyleSelectorState.Complete -> "Tamamlandı"
                        is StyleSelectorState.Error -> "Hata"
                    },
                    style = MaterialTheme.typography.button
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text
            Text(
                text = when (state) {
                    is StyleSelectorState.Idle -> "Bir stil seçin ve önizleme oluşturun"
                    is StyleSelectorState.Loading -> "Önizleme hazırlanıyor..."
                    is StyleSelectorState.Previewing -> "Önizlemeyi beğendiyseniz tam çözünürlüğü oluşturun"
                    is StyleSelectorState.Finalizing -> "Yüksek kalite görsel üretiliyor..."
                    is StyleSelectorState.Complete -> "Görseliniz hazır!"
                    is StyleSelectorState.Error -> (state as StyleSelectorState.Error).message
                },
                style = MaterialTheme.typography.caption,
                color = if (state is StyleSelectorState.Error)
                    MaterialTheme.colors.error
                    else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Inpainting Dialog
    if (showInpaintingDialog) {
        InpaintingMaskDialog(
            imageUri = imageUri,
            onMaskConfirmed = { mask ->
                viewModel.applyInpaintingMask(mask)
            },
            onDismiss = { viewModel.hideInpaintingDialog() }
        )
    }
}
