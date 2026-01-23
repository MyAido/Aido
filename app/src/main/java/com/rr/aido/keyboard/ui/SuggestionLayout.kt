package com.rr.aido.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdvancedSuggestionStrip(
    suggestions: List<String>,
    listener: SuggestionListener,
    isDark: Boolean
) {
    // Logic:
    // 1. If suggestions exist -> Show Suggestions (with Menu button to force show tools)
    // 2. If suggestions empty -> Show Tools (Mic, Clipboard, etc.)
    // 3. Manual override: If user clicks Menu while suggestions exist, show Tools.

    var manualToolMode by remember { mutableStateOf(false) }

    // Reset manual mode if suggestions disappear (e.g. user deleted text),
    // though "suggestions.isEmpty()" covers the display logic,
    // we might want to reset the manual flag so next time they type it flows correctly.
    // However, for now, let's keep it simple.

    val showTools = suggestions.isEmpty() || manualToolMode

    val backgroundColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF2F2F2)
    val iconColor = if (isDark) Color.White else Color.Black
    val dividerColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFD0D0D0)
    val suggestionTextColor = if (isDark) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTools) {
            // TOOLBAR VIEW

            // Left Button Logic:
            // If we are in manual mode (suggestions exist but we hid them), show Back arrow to return.
            // If we are in default mode (suggestions empty), show 'Menu' icon (or Settings?) or nothing?
            // User asked for "Hamburger" logic. Let's keep the Hamburger/Menu icon as the anchor.

            if (suggestions.isNotEmpty()) {
                 // Back button to return to suggestions
                StripIconButton(
                    icon = Icons.Default.ArrowBack,
                    description = "Back",
                    onClick = { manualToolMode = false },
                    tint = iconColor
                )
                VerticalDivider(dividerColor)
            } else {
                // If suggestions are empty, we are naturally in Tool mode.
                // We can show a Menu icon that maybe opens a bigger menu or settings?
                // Or just keep it static.
                 StripIconButton(
                    icon = KeyboardIcons.GridMenu,
                    description = "Menu",
                    onClick = { listener.onMenuClick() }, // Open Grid/Settings
                    tint = iconColor
                )
                VerticalDivider(dividerColor)
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic
                StripIconButton(Icons.Default.Mic, "Voice", listener::onVoiceClick, iconColor)

                // Clipboard
                StripIconButton(Icons.Default.ContentPaste, "Clipboard", listener::onClipboardClick, iconColor)

                // Triggers
                StripIconButton(Icons.Default.FlashOn, "Triggers", listener::onTriggerClick, iconColor)

                // Undo
                StripIconButton(Icons.Default.Undo, "Undo", listener::onUndoClick, iconColor)

                // Redo
                StripIconButton(Icons.Default.Redo, "Redo", listener::onRedoClick, iconColor)

                // We can add more or spacing
            }

        } else {
            // SUGGESTION VIEW
            // Helper button to switch to Tools
            // SUGGESTION VIEW - Expand Button
            StripIconButton(
                // Use a caret or arrow here typically, but user asked for "Hamburger" style behavior.
                // But typically Gboard uses a Chevron Right.
                // I will use ArrowForward as a safe bet for "Expand".
                // If they want the Grid icon ALWAYS visible, I can put it here too.
                // Let's use ArrowForward for now as "Expand to Tools".
                icon = androidx.compose.material.icons.Icons.Default.ArrowForward,
                description = "Expand Tools",
                onClick = { manualToolMode = true },
                tint = iconColor
            )

            VerticalDivider(dividerColor)

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
               val count = suggestions.size.coerceAtMost(3)
               for (i in 0 until count) {
                   val suggestion = suggestions[i]
                   val isCenter = (i == 1 && count == 3)

                   Box(
                       modifier = Modifier
                           .weight(1f)
                           .fillMaxHeight()
                           .padding(4.dp)
                           .clip(RoundedCornerShape(8.dp))
                           .background(if (isCenter) Color(0x1F888888) else Color.Transparent)
                           .clickable { listener.onPickSuggestion(suggestion) },
                       contentAlignment = Alignment.Center
                   ) {
                       Text(
                           text = suggestion,
                           color = suggestionTextColor,
                           fontSize = 16.sp,
                           fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                           maxLines = 1,
                           textAlign = TextAlign.Center
                       )
                   }

                   if (i < count - 1) {
                       VerticalDivider(dividerColor.copy(alpha = 0.5f))
                   }
               }
            }
        }
    }
}
