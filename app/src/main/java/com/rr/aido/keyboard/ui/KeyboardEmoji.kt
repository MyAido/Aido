package com.rr.aido.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.aido.utils.EmojiData

@Composable
fun KeyboardEmoji(
    onEmojiClick: (String) -> Unit,
    onBackClick: () -> Unit,
    isDark: Boolean
) {
    val backgroundColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF2F2F2)
    val textColor = if (isDark) Color.White else Color.Black
    val tabSelectedColor = if (isDark) Color(0xFF3C3C3E) else Color.LightGray

    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf(
        "Smileys", "Gestures", "Hearts", "Animals", "Food",
        "Sports", "Travel", "Objects", "Symbols"
    )

    val currentEmojis = remember(selectedCategoryIndex) {
        when(categories[selectedCategoryIndex]) {
            "Smileys" -> EmojiData.SMILEYS_PEOPLE
            "Gestures" -> EmojiData.GESTURES_HANDS
            "Hearts" -> EmojiData.HEARTS_LOVE
            "Animals" -> EmojiData.ANIMALS_NATURE
            "Food" -> EmojiData.FOOD_DRINK
            "Sports" -> EmojiData.ACTIVITIES_SPORTS
            "Travel" -> EmojiData.TRAVEL_PLACES
            "Objects" -> EmojiData.OBJECTS
            "Symbols" -> EmojiData.SYMBOLS
            else -> EmojiData.SMILEYS_PEOPLE
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp) // Consistent height
            .background(backgroundColor)
    ) {
        // Header with Tabs
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                     imageVector = Icons.Default.ArrowBack,
                     contentDescription = "Back",
                     tint = textColor
                )
            }

            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = textColor,
                divider = {},
                indicator = {}
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = title,
                                color = if(selectedCategoryIndex == index) textColor else textColor.copy(alpha = 0.6f),
                                fontWeight = if(selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 40.dp),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(currentEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onEmojiClick(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }
    }
}
