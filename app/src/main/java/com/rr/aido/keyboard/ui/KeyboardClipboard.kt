package com.rr.aido.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.aido.keyboard_service.KeyboardClipboardManager

@Composable
fun KeyboardClipboard(
    clipboardHistory: List<KeyboardClipboardManager.ClipboardItem>,
    onPasteClick: (String) -> Unit,
    onDeleteClick: (Int) -> Unit,
    onClearAllClick: () -> Unit,
    onBackClick: () -> Unit,
    isDark: Boolean
) {
    val backgroundColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF2F2F2)
    val itemBackgroundColor = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.Gray else Color.DarkGray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp) // increased height to match alpha keyboard + suggestions strip
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onBackClick() }
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Clipboard",
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            
            // Clear All Button
            if (clipboardHistory.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    color = Color.Red, // Or theme accent
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onClearAllClick() }
                        .padding(8.dp)
                )
            }
        }

        if (clipboardHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Clipboard is empty\nCopy text to see it here",
                    color = secondaryTextColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(clipboardHistory) { index, item ->
                    ClipboardItemRow(
                        text = item.text,
                        timestamp = item.timestamp,
                        onClick = { onPasteClick(item.text) },
                        onDelete = { onDeleteClick(index) },
                        backgroundColor = itemBackgroundColor,
                        textColor = textColor,
                        secondaryColor = secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun ClipboardItemRow(
    text: String,
    timestamp: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    secondaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = secondaryColor,
            modifier = Modifier
                .size(24.dp)
                .clickable { onDelete() }
        )
    }
}
