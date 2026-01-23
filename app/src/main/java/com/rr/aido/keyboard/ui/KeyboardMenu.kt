package com.rr.aido.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyboardMenu(
    onSettingsClick: () -> Unit,
    onThemeClick: () -> Unit,
    onStickerClick: () -> Unit,
    onGifClick: () -> Unit,
    onCloseClick: () -> Unit,
    isDark: Boolean
) {
    val backgroundColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF2F2F2)
    val textColor = if (isDark) Color.White else Color.Black
    val itemColor = if (isDark) Color(0xFF2C2C2C) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp) // Match approx keyboard height
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aido Menu",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
            
            // Close Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onCloseClick() }
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = textColor
                )
            }
        }

        // Grid Options
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Use spacedBy for consistent gaps
        ) {
            MenuItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = onSettingsClick,
                backgroundColor = itemColor,
                textColor = textColor
            )
            
            MenuItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ColorLens,
                label = "Theme",
                onClick = onThemeClick,
                backgroundColor = itemColor,
                textColor = textColor
            )
            
            MenuItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.StickyNote2,
                label = "Stickers",
                onClick = onStickerClick,
                backgroundColor = itemColor,
                textColor = textColor
            )
            
            MenuItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Gif,
                label = "GIF",
                onClick = onGifClick,
                backgroundColor = itemColor,
                textColor = textColor
            )
        }
    }
}

@Composable
fun MenuItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp) // Reduced internal padding slightly
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(28.dp) // Slightly smaller icon to prevent overflow
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp, // Slightly smaller text
            maxLines = 1
        )
    }
}
