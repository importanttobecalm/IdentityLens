package com.identitylens.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.identitylens.app.ui.state.ClothingPresets

/**
 * Clothing Modifier Component
 * Quick selection chips + advanced inpainting tool
 */
@Composable
fun ClothingModifier(
    selectedClothing: List<String>,
    onClothingSelected: (String) -> Unit,
    onInpaintingMaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Kıyafet",
            style = MaterialTheme.typography.h6
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Quick selection chips
        FlowRow(
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp
        ) {
            ClothingPresets.ITEMS.forEach { clothing ->
                ClothingChip(
                    text = clothing,
                    isSelected = selectedClothing.contains(clothing),
                    onClick = { onClothingSelected(clothing) }
                )
            }
            
            // Advanced inpainting button
            OutlinedButton(
                onClick = onInpaintingMaskClick,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Özel Maskeleme")
            }
        }
    }
}

/**
 * Individual clothing chip
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ClothingChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        colors = ChipDefaults.filterChipColors(
            selectedBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
            selectedContentColor = MaterialTheme.colors.primary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2
        )
    }
}
