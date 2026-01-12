package com.identitylens.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.identitylens.app.ui.state.StyleCategory

/**
 * Dynamic Style Carousel
 * Displays style categories in horizontal scrollable list
 */
@Composable
fun DynamicStyleCarousel(
    styles: List<StyleCategory>,
    selectedStyle: StyleCategory?,
    onStyleSelected: (StyleCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(styles) { style ->
            StyleCard(
                style = style,
                isSelected = style == selectedStyle,
                onClick = { onStyleSelected(style) }
            )
        }
    }
}

/**
 * Individual style card
 */
@Composable
private fun StyleCard(
    style: StyleCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        elevation = elevation,
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colors.primary) 
            else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Thumbnail or icon
            if (style.thumbnailUrl != null) {
                AsyncImage(
                    model = style.thumbnailUrl,
                    contentDescription = style.displayName,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = getStyleIcon(style.icon ?: "auto_awesome"),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (isSelected) 
                        MaterialTheme.colors.primary 
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.subtitle2,
                textAlign = TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colors.primary 
                    else MaterialTheme.colors.onSurface
            )
            
            Text(
                text = style.description,
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                maxLines = 2
            )
        }
    }
}

/**
 * Get Material Icon for style
 */
private fun getStyleIcon(iconName: String): ImageVector {
    return when (iconName) {
        "flash_on" -> Icons.Default.FlashOn
        "palette" -> Icons.Default.Palette
        "business_center" -> Icons.Default.BusinessCenter
        "camera_alt" -> Icons.Default.CameraAlt
        "movie" -> Icons.Default.Movie
        "auto_awesome" -> Icons.Default.AutoAwesome
        "auto_fix_high" -> Icons.Default.AutoFixHigh
        else -> Icons.Default.AutoAwesome
    }
}
