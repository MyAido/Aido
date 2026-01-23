package com.rr.aido.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

interface SuggestionListener {
    fun onPickSuggestion(text: String)
    fun onMenuClick()
    fun onVoiceClick()
    fun onUndoClick()
    fun onRedoClick()
    fun onClipboardClick()
    fun onTriggerClick()
}

@Composable
fun SuggestionStrip(
    suggestions: List<String> = emptyList(),
    listener: SuggestionListener,
    isDark: Boolean
) {
    val iconColor = if (isDark) Color.White else Color.Black
    val dividerColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFB0B0B0)

    // Suggestion Strip Height usually around 40-50dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu Button
        StripIconButton(Icons.Default.Menu, "Menu", listener::onMenuClick, iconColor)

        // Divider
        VerticalDivider(dividerColor)

        // Suggestions Area (Weight 1f to fill space)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (suggestions.isEmpty()) {
                // Determine if we show a placeholder or just Voice/Undo/Redo in the center?
                // The user's previous layout had buttons. Let's put the extra actions here when no suggestions.
                Spacer(Modifier.width(8.dp))
                StripIconButton(Icons.Default.Mic, "Voice", listener::onVoiceClick, iconColor)
                Spacer(Modifier.width(16.dp))
                StripIconButton(Icons.Default.ContentPaste, "Clipboard", listener::onClipboardClick, iconColor)
                Spacer(Modifier.width(16.dp))
                StripIconButton(Icons.Default.FlashOn, "Triggers", listener::onTriggerClick, iconColor)
                Spacer(Modifier.width(16.dp))
                StripIconButton(Icons.Default.Undo, "Undo", listener::onUndoClick, iconColor)
                Spacer(Modifier.width(16.dp))
                StripIconButton(Icons.Default.Redo, "Redo", listener::onRedoClick, iconColor)
            } else {
                suggestions.take(3).forEach { suggestion ->
                    Text(
                        text = suggestion,
                        color = iconColor,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clickable { listener.onPickSuggestion(suggestion) },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    if (suggestion != suggestions.last()) {
                        VerticalDivider(dividerColor)
                    }
                }
            }
        }
    }
}

@Composable
fun StripIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun VerticalDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(color)
    )
}
