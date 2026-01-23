package com.rr.aido.keyboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.rr.aido.keyboard.api.RetrofitClient
import com.rr.aido.utils.EmojiData

enum class MediaTab {
    EMOJI, GIF, STICKER, KAOMOJI
}

@Composable
fun MediaKeyboard(
    currentTab: MediaTab,
    onTabChange: (MediaTab) -> Unit,
    searchText: String = "",
    onClearSearch: () -> Unit,
    onEmojiClick: (String) -> Unit,
    onMediaClick: (String, String) -> Unit, // url, mimeType
    onSearchClick: () -> Unit,
    onBackClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    isDark: Boolean
) {
    val backgroundColor = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF2F2F2)
    val textColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.White else Color.Black
    val searchBgColor = if (isDark) Color(0xFF2C2C2C) else Color.White

    // Internal state removed, using passed currentTab
    // searchText is now passed in

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp) // Consistent keyboard height with other panels
            .background(backgroundColor)
    ) {
        // Search Bar (Only for Sticker/GIF)
        if (currentTab != MediaTab.EMOJI && currentTab != MediaTab.KAOMOJI) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(searchBgColor)
                    .clickable { onSearchClick() }, // Trigger search mode
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (searchText.isEmpty()) "Search..." else searchText,
                        color = if (searchText.isEmpty()) Color.Gray else textColor,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (searchText.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.Gray,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { onClearSearch() }
                        )
                    }
                }
            }
        }

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                MediaTab.EMOJI -> EmojiTab(onEmojiClick, textColor, isDark)
                MediaTab.GIF -> GifTab(searchText, onMediaClick)
                MediaTab.STICKER -> StickerTab(searchText, onMediaClick)
                MediaTab.KAOMOJI -> KaomojiTab(onEmojiClick, textColor)
            }
        }

        // Bottom Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(backgroundColor), // Or slightly darker?
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // ABC (Back)
            Text(
                text = "ABC",
                fontWeight = FontWeight.Bold,
                color = iconColor,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(8.dp)
            )

            // Tabs
            IconButton(
                isActive = currentTab == MediaTab.EMOJI,
                inactiveIcon = Icons.Outlined.EmojiEmotions,
                activeIcon = Icons.Filled.EmojiEmotions,
                onClick = { onTabChange(MediaTab.EMOJI) },
                tint = iconColor
            )

            IconButton(
                isActive = currentTab == MediaTab.GIF,
                inactiveIcon = Icons.Outlined.Gif,
                activeIcon = Icons.Filled.Gif,
                onClick = { onTabChange(MediaTab.GIF) },
                tint = iconColor
            )

            IconButton(
                isActive = currentTab == MediaTab.STICKER,
                inactiveIcon = Icons.Outlined.StickyNote2, // Placeholder for Sticker square
                activeIcon = Icons.Filled.StickyNote2,
                onClick = { onTabChange(MediaTab.STICKER) },
                tint = iconColor
            )

            // Kaomoji (Face)
            Text(
                text = ":-)",
                fontWeight = FontWeight.Bold,
                color = if (currentTab == MediaTab.KAOMOJI) Color.Blue else iconColor,
                modifier = Modifier
                    .clickable { onTabChange(MediaTab.KAOMOJI) }
                    .padding(8.dp)
            )

            // Backspace
            Icon(
                imageVector = com.rr.aido.keyboard.ui.KeyboardIcons.Backspace,
                contentDescription = "Backspace",
                tint = iconColor,
                modifier = Modifier
                    .clickable { onBackspaceClick() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun IconButton(
    isActive: Boolean,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: Color
) {
    Icon(
        imageVector = if (isActive) activeIcon else inactiveIcon,
        contentDescription = null,
        tint = if (isActive) Color.Blue else tint,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .size(24.dp)
    )
}

@Composable
fun EmojiTab(onEmojiClick: (String) -> Unit, textColor: Color, isDark: Boolean) {
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

    Column {
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

@Composable
fun GifTab(searchText: String, onMediaClick: (String, String) -> Unit) {
    var gifUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchText) {
        try {
            val response = if (searchText.isBlank()) {
                com.rr.aido.keyboard.api.RetrofitClient.tenorApi.getTrending(limit = 18)
            } else {
                com.rr.aido.keyboard.api.RetrofitClient.tenorApi.search(query = searchText, limit = 18)
            }
            gifUrls = response.results.mapNotNull { it.media.firstOrNull()?.tinygif?.url }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (gifUrls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading GIFs...", color = Color.Gray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(gifUrls) { url ->
                GifItem(url) { onMediaClick(url, "image/gif") }
            }
        }
    }
}

@Composable
fun GifItem(url: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // 2% of available disk
                    .build()
            }
            .build()
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .memoryCacheKey(url)
            .diskCacheKey(url)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "GIF",
        contentScale = ContentScale.Crop,
        placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
        error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color.DarkGray)
    )
}

@Composable
fun StickerTab(searchText: String, onMediaClick: (String, String) -> Unit) {
    // Stickers on Tenor often use the search term "sticker" appended to query or specific endpoint?
    // Tenor V1 has no explicit 'sticker' endpoint, but searching "tag sticker" works.

    var stickerUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(searchText) {
        try {
            val query = if (searchText.isBlank()) "sticker" else "$searchText sticker"
            val response = com.rr.aido.keyboard.api.RetrofitClient.tenorApi.search(query = query, limit = 18)
            // For stickers, usually transparent GIFs. 'tinygif' is fine.
            stickerUrls = response.results.mapNotNull { it.media.firstOrNull()?.tinygif?.url }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (stickerUrls.isEmpty()) {
         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading Stickers...", color = Color.Gray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stickerUrls) { url ->
                 AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .memoryCacheKey(url)
                        .diskCacheKey(url)
                        .build(),
                    contentDescription = "Sticker",
                    placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                    error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onMediaClick(url, "image/gif") }, // Stickers are often GIFs on Tenor
                     contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun KaomojiTab(onTextClick: (String) -> Unit, textColor: Color) {
    val kaomojis = listOf(
        ":-)", ":-(", ";-)", ":-P", "(^_^)", "(T_T)", "¯\\_(ツ)_/¯", "(╯°□°）╯︵ ┻━┻"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(kaomojis) { kaomoji ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onTextClick(kaomoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = kaomoji, color = textColor, fontSize = 16.sp)
            }
        }
    }
}
