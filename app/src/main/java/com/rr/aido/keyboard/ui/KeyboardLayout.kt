package com.rr.aido.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.Alignment 
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import com.rr.aido.R
import com.rr.aido.keyboard_service.KeyboardClipboardManager
import com.rr.aido.data.models.Preprompt

interface KeyboardActionListener {
    fun onKey(code: Int)
    fun onText(text: String)
    fun onDelete()
    fun onEnter()
    fun onEmoji()
    fun onMedia(url: String, mimeType: String)
    fun onMoveCursor(offset: Int)
}

enum class CapsState { NONE, SHIFT, CAPS }
enum class LayoutState { ALPHA, SYMBOLS }

enum class KeyboardView {
    ALPHA, SYMBOLS, MENU, CLIPBOARD, TRIGGERS, EMOJI, NUMBER_PAD
}

@Composable
fun AidoKeyboard(
    actionListener: KeyboardActionListener,
    suggestionListener: SuggestionListener,
    suggestions: List<String> = emptyList(),
    themeMode: com.rr.aido.data.models.ThemeMode = com.rr.aido.data.models.ThemeMode.SYSTEM,
    // View State
    currentView: KeyboardView = KeyboardView.ALPHA,
    onViewChange: (KeyboardView) -> Unit = {},
    
    // Menu Actions
    onSettingsClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    
    // Clipboard Data
    clipboardHistory: List<KeyboardClipboardManager.ClipboardItem> = emptyList(),
    onPasteClick: (String) -> Unit = {},
    onDeleteClipClick: (Int) -> Unit = {},
    onClearClipboardClick: () -> Unit = {},
    
    // Trigger Data
    triggers: List<Preprompt> = emptyList(),
    onTriggerClick: (String) -> Unit = {}
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        com.rr.aido.data.models.ThemeMode.DARK -> true
        com.rr.aido.data.models.ThemeMode.LIGHT -> false
        else -> isSystemDark
    }
    
    val backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFD4D8DF) // Standard Keyboard Grey
    var capsState by remember { androidx.compose.runtime.mutableStateOf(CapsState.NONE) }
    var layoutState by remember { androidx.compose.runtime.mutableStateOf(LayoutState.ALPHA) }
    
    // Hoisted Media State (Persist across views)
    var isMediaSearchActive by remember { mutableStateOf(false) }
    var mediaSearchQuery by remember { mutableStateOf("") }
    var currentMediaTab by remember { mutableStateOf(MediaTab.EMOJI) }

    // Glide Typing State
    val glideManager = remember { GlideTypingManager() }
    val glidePath by glideManager.currentPath.collectAsState()
    val rootLayoutCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(backgroundColor)
            .safeDrawingPadding()
            .navigationBarsPadding()
    ) {
    CompositionLocalProvider(LocalKeyboardIsDark provides isDark) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = 4.dp, top = 4.dp)
    ) {
        // Suggestion Strip - Only show if in Alpha/Symbol mode to avoid clutter in panels
        if (currentView == KeyboardView.ALPHA || currentView == KeyboardView.SYMBOLS) {
            AdvancedSuggestionStrip(
                suggestions = suggestions, 
                listener = suggestionListener,
                isDark = isDark
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        when (currentView) {
            KeyboardView.MENU -> {
                KeyboardMenu(
                    onSettingsClick = onSettingsClick,
                    onThemeClick = onThemeClick,
                    onStickerClick = { 
                        currentMediaTab = MediaTab.STICKER
                        onViewChange(KeyboardView.EMOJI) 
                    }, 
                    onGifClick = { 
                        currentMediaTab = MediaTab.GIF
                        onViewChange(KeyboardView.EMOJI) 
                    },
                    onCloseClick = { onViewChange(KeyboardView.ALPHA) },
                    isDark = isDark
                )
            }
            KeyboardView.CLIPBOARD -> {
                KeyboardClipboard(
                    clipboardHistory = clipboardHistory,
                    onPasteClick = { text -> 
                        onPasteClick(text)
                        onViewChange(KeyboardView.ALPHA)
                    },
                    onDeleteClick = onDeleteClipClick,
                    onClearAllClick = onClearClipboardClick,
                    onBackClick = { onViewChange(KeyboardView.ALPHA) },
                    isDark = isDark
                )
            }
            KeyboardView.TRIGGERS -> {
                KeyboardTriggers(
                    triggers = triggers,
                    onTriggerClick = { text ->
                         onTriggerClick(text)
                         onViewChange(KeyboardView.ALPHA)
                    },
                    onBackClick = { onViewChange(KeyboardView.ALPHA) },
                    isDark = isDark
                )
            }
            KeyboardView.EMOJI -> {
                if (isMediaSearchActive) {
                     Column(modifier = Modifier.fillMaxWidth()) {
                         // Search Box Header
                         Row(
                             modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(if (isDark) Color(0xFF1F1F1F) else Color(0xFFF2F2F2))
                                .padding(8.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Icon(Icons.Default.Search, "Search", tint = if (isDark) Color.White else Color.Black)
                             Spacer(modifier = Modifier.width(8.dp))
                             Text(
                                 text = if(mediaSearchQuery.isEmpty()) "Search Tenor..." else mediaSearchQuery,
                                 color = if (isDark) Color.White else Color.Black,
                                 modifier = Modifier.weight(1f)
                             )
                             Icon(
                                 Icons.Default.Close, 
                                 "Close", 
                                 tint = if (isDark) Color.White else Color.Black,
                                 modifier = Modifier.clickable { 
                                     isMediaSearchActive = false 
                                     // Don't clear query so results show
                                 }
                             )
                         }
                         
                         // Alpha Keyboard for typing
                         val searchListener = object : KeyboardActionListener {
                             override fun onKey(code: Int) {}
                             override fun onText(text: String) {
                                  mediaSearchQuery += text
                             }
                             override fun onDelete() {
                                  if (mediaSearchQuery.isNotEmpty()) mediaSearchQuery = mediaSearchQuery.dropLast(1)
                             }
                             override fun onEnter() {
                                  isMediaSearchActive = false
                             }
                             override fun onEmoji() {}
                             override fun onMedia(url: String, mimeType: String) {}
                             override fun onMoveCursor(offset: Int) {}
                         }
                         
                         AlphaKeyboard(
                             actionListener = searchListener,
                             capsState = CapsState.NONE,
                             onCapsToggle = {},
                             onCapsReset = {},
                             onSymbolToggle = {}
                         )
                     }
                } else {
                    MediaKeyboard(
                        currentTab = currentMediaTab,
                        onTabChange = { currentMediaTab = it },
                        searchText = mediaSearchQuery,
                        onClearSearch = { mediaSearchQuery = "" },
                        onEmojiClick = { emoji -> actionListener.onText(emoji) },
                        onMediaClick = { url, mime -> 
                             actionListener.onMedia(url, mime)
                        },
                        onSearchClick = { isMediaSearchActive = true },
                        onBackClick = { onViewChange(KeyboardView.ALPHA) },
                        onBackspaceClick = { actionListener.onDelete() },
                        isDark = isDark
                    )
                }
            }
            KeyboardView.NUMBER_PAD -> {
                NumberPad(
                    actionListener = actionListener,
                    onReturnToAlpha = { 
                        onViewChange(KeyboardView.ALPHA) 
                        layoutState = LayoutState.ALPHA
                    },
                    onReturnToSymbols = { 
                        onViewChange(KeyboardView.ALPHA)
                        layoutState = LayoutState.SYMBOLS
                    },
                    isDark = isDark
                )
            }
            else -> {
                // Default Input Views (Alpha/Symbols)
                // Optimized Stable Callbacks
                val onCapsToggle = remember {
                    {
                        capsState = when (capsState) {
                            CapsState.NONE -> CapsState.SHIFT
                            CapsState.SHIFT -> CapsState.CAPS
                            CapsState.CAPS -> CapsState.NONE
                        }
                    }
                }
                
                val onCapsReset = remember {
                    {
                        if (capsState == CapsState.SHIFT) {
                            capsState = CapsState.NONE
                        }
                    }
                }
                
                val onSymbolToggle = remember {
                    { layoutState = LayoutState.SYMBOLS }
                }

                // Check for updates less frequently or memoize layouts
                if (layoutState == LayoutState.ALPHA) {
                    AlphaKeyboard(
                        actionListener = actionListener,
                        capsState = capsState,
                        onCapsToggle = onCapsToggle,
                        onCapsReset = onCapsReset,
                        onSymbolToggle = onSymbolToggle,
                        onKeyLayoutCoordinates = { char, coords ->
                             val root = rootLayoutCoordinates.value
                             if (root != null && root.isAttached) {
                                 try {
                                     val rect = root.localBoundingBoxOf(coords, false)
                                     glideManager.updateKeyPosition(char, rect)
                                 } catch (e: Exception) {
                                     // Ignore
                                 }
                             }
                        },
                        glideManager = glideManager,
                        onUpdateRootCoordinates = { rootLayoutCoordinates.value = it }
                    )
                } else {
                    SymbolKeyboard(
                        actionListener = actionListener,
                        onReturnToAlpha = { layoutState = LayoutState.ALPHA },
                        onNumberPadClick = { onViewChange(KeyboardView.NUMBER_PAD) }
                    )
                }
            }
        }
    }
        
        // Glide Path visualization - constrain to keyboard area only
    }
    }
}

@Composable
fun AlphaKeyboard(
    actionListener: KeyboardActionListener,
    capsState: CapsState,
    onCapsToggle: () -> Unit,
    onCapsReset: () -> Unit,
    onSymbolToggle: () -> Unit,
    onKeyLayoutCoordinates: ((Char, LayoutCoordinates) -> Unit)? = null,
    glideManager: GlideTypingManager? = null,
    onUpdateRootCoordinates: ((LayoutCoordinates) -> Unit)? = null
) {
    val glidePath = glideManager?.currentPath?.collectAsState()?.value ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .then(
                if (glideManager != null) {
                    Modifier
                        .onGloballyPositioned { onUpdateRootCoordinates?.invoke(it) }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> glideManager.startGesture(offset) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    glideManager.updateGesture(change.position)
                                },
                                onDragEnd = {
                                    val word = glideManager.endGesture()
                                    if (word != null) actionListener.onText(word + " ")
                                },
                                onDragCancel = { glideManager.endGesture() }
                            )
                        }
                } else Modifier
            )
    ) {
    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
    // Row 1: Numbers
    KeyboardRow {
        val numbers = "1234567890"
        numbers.forEach { char ->
            KeyboardKey(
                modifier = Modifier.weight(1f).height(54.dp),
                text = char.toString(),
                onClick = { actionListener.onText(char.toString()) },
                onKeyLayout = { coords -> onKeyLayoutCoordinates?.invoke(char, coords) }
            )
        }
    }

    // Row 2: QWERTY
    KeyboardRow {
        val qwerty = "QWERTYUIOP"
        qwerty.forEach { char ->
            val label = if (capsState == CapsState.NONE) char.toString().lowercase() else char.toString().uppercase()
            val mapChar = char.lowercaseChar()
            KeyboardKey(
                modifier = Modifier.weight(1f).height(54.dp),
                text = label,
                onClick = { 
                    actionListener.onText(label)
                    onCapsReset()
                },
                onKeyLayout = { coords -> onKeyLayoutCoordinates?.invoke(mapChar, coords) }
            )
        }
    }

    // Row 3: ASDF
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp) 
    ) {
        val asdf = "ASDFGHJKL"
        asdf.forEach { char ->
            val label = if (capsState == CapsState.NONE) char.toString().lowercase() else char.toString().uppercase()
            val mapChar = char.lowercaseChar()
            KeyboardKey(
                modifier = Modifier.weight(1f).height(54.dp),
                text = label,
                onClick = { 
                    actionListener.onText(label)
                    onCapsReset()
                },
                onKeyLayout = { coords -> onKeyLayoutCoordinates?.invoke(mapChar, coords) }
            )
        }
    }

    // Row 4: Shift - ZXCV - Backspace
    KeyboardRow {
        // Shift
        KeyboardKey(
            modifier = Modifier.weight(1.5f).height(54.dp),
            icon = when (capsState) {
                CapsState.NONE -> KeyboardIcons.Shift
                CapsState.SHIFT -> KeyboardIcons.ShiftFilled
                CapsState.CAPS -> KeyboardIcons.ShiftCaps
            },
            isSpecial = true,
            onClick = onCapsToggle,
            onLongClick = { 
                // Optional: Force CAPS lock on long press if not handled by toggle
                // onCapsToggle() 
            }
        )
        
        val zxcv = "ZXCVBNM"
        zxcv.forEach { char ->
            val label = if (capsState == CapsState.NONE) char.toString().lowercase() else char.toString().uppercase()
            val mapChar = char.lowercaseChar()
            KeyboardKey(
                modifier = Modifier.weight(1f).height(54.dp),
                text = label,
                onClick = { 
                    actionListener.onText(label)
                    onCapsReset()
                },
                onKeyLayout = { coords -> onKeyLayoutCoordinates?.invoke(mapChar, coords) }
            )
        }

        // Backspace
        KeyboardKey(
            modifier = Modifier.weight(1.5f).height(54.dp),
            icon = KeyboardIcons.Backspace,
            isSpecial = true, 
            onClick = { actionListener.onDelete() },
            onRepeat = { actionListener.onDelete() }
        )
    }

    // Row 5: ?123 - @ - Emoji - Space - . - Enter
    KeyboardRow {
        // ?123 Toggle
        KeyboardKey(
            modifier = Modifier.weight(1.5f).height(54.dp),
            text = "?123",
            isSpecial = true,
            onClick = onSymbolToggle
        )
        
        // @
         KeyboardKey(
            modifier = Modifier.weight(1f).height(54.dp),
            text = "@",
            isSpecial = true,
            onClick = { actionListener.onText("@") }
        )

        // Emoji
        KeyboardKey(
            modifier = Modifier.weight(1f).height(54.dp),
            icon = KeyboardIcons.Emoji,
            isSpecial = true,
            onClick = { actionListener.onEmoji() }
        )

        // Space
        KeyboardKey(
            modifier = Modifier.weight(4f).height(54.dp)
                .pointerInput(Unit) {
                    var accumulator = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulator = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            accumulator += dragAmount
                            val step = 30f // Sensitivity: 30px per char
                            if (accumulator > step) {
                                while (accumulator > step) {
                                    actionListener.onMoveCursor(1)
                                    accumulator -= step
                                }
                            } else if (accumulator < -step) {
                                while (accumulator < -step) {
                                    actionListener.onMoveCursor(-1)
                                    accumulator += step
                                }
                            }
                        }
                    )
                },
            text = "space",
            onClick = { actionListener.onText(" ") }
        )

        // .
        KeyboardKey(
            modifier = Modifier.weight(1f).height(54.dp),
            text = ".",
            isSpecial = true,
            onClick = { actionListener.onText(".") }
        )

        // Enter
        KeyboardKey(
            modifier = Modifier.weight(1.5f).height(54.dp),
            icon = KeyboardIcons.Enter,
            isSpecial = true, // Or specific Go/Blue color
            onClick = { actionListener.onEnter() }
        )
    }
    }
    
    // Draw glide path
    if (glidePath.isNotEmpty()) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val path = Path()
            path.moveTo(glidePath.first().x, glidePath.first().y)
            for (i in 1 until glidePath.size) {
                path.lineTo(glidePath[i].x, glidePath[i].y)
            }
            
            drawPath(
                path = path, 
                color = Color.Cyan.copy(alpha = 0.6f),
                style = Stroke(width = 15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
    }
}

@Composable
fun SymbolKeyboard(
    actionListener: KeyboardActionListener,
    onReturnToAlpha: () -> Unit,
    onNumberPadClick: () -> Unit
) {
    var symbolPage by remember { androidx.compose.runtime.mutableStateOf(0) }

    if (symbolPage == 0) {
        Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            // PAGE 1 (Image 0)
        
        // Row 1: Numbers
        KeyboardRow {
            val numbers = "1234567890"
            numbers.forEach { char ->
                KeyboardKey(
                    modifier = Modifier.weight(1f).height(67.5.dp),
                    text = char.toString(),
                    onClick = { actionListener.onText(char.toString()) }
                )
            }
        }

        // Row 2: @ # $ _ & - + ( ) /
        KeyboardRow {
            val r2 = "@#\$_&-+()/"
            r2.forEach { char ->
                KeyboardKey(
                    modifier = Modifier.weight(1f).height(67.5.dp),
                    text = char.toString(),
                    onClick = { actionListener.onText(char.toString()) }
                )
            }
        }

        // Row 3: =\< * " ' : ; ! ? Backspace
        KeyboardRow {
            // Toggle to Page 2
            KeyboardKey(
                modifier = Modifier.weight(1.5f).height(67.5.dp),
                text = "=\\<", 
                isSpecial = true,
                onClick = { symbolPage = 1 }
            )

            val r3 = "*\"':;!?"
            r3.forEach { char ->
                KeyboardKey(
                    modifier = Modifier.weight(1f).height(67.5.dp),
                    text = char.toString(),
                    onClick = { actionListener.onText(char.toString()) }
                )
            }

            // Backspace
            KeyboardKey(
                modifier = Modifier.weight(1.5f).height(67.5.dp),
                icon = KeyboardIcons.Backspace,
                isSpecial = true, 
                onClick = { actionListener.onDelete() },
                onRepeat = { actionListener.onDelete() }
            )
        }

        // Row 4: ABC - , - 1234 - Space - . - Enter
        KeyboardRow {
            // Return to Alpha
            KeyboardKey(
                modifier = Modifier.weight(1.5f).height(67.5.dp),
                text = "ABC",
                isSpecial = true,
                onClick = onReturnToAlpha
            )
            
            // Comma
            KeyboardKey(
                modifier = Modifier.weight(1f).height(67.5.dp),
                text = ",",
                isSpecial = true,
                onClick = { actionListener.onText(",") }
            )

            // 1234 (Placeholder/Emoji?) - Visual match for "1234" in box
            // 1234 (Number Pad Trigger)
            KeyboardKey(
                modifier = Modifier.weight(1f).height(67.5.dp),
                text = "12\n34", 
                isSpecial = true,
                onClick = onNumberPadClick
            )
            
            // Space
            KeyboardKey(
                modifier = Modifier.weight(4f).height(67.5.dp),
                text = "space",
                onClick = { actionListener.onText(" ") }
            )

            // Dot
            KeyboardKey(
                modifier = Modifier.weight(1f).height(67.5.dp),
                text = ".",
                isSpecial = true,
                onClick = { actionListener.onText(".") }
            )
            
            // Enter
            KeyboardKey(
                modifier = Modifier.weight(1.5f).height(67.5.dp),
                icon = KeyboardIcons.Enter,
                isSpecial = true, 
                onClick = { actionListener.onEnter() }
            )
        }

        }

    } else {
        Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            // PAGE 2 (Image 2)
        
        // Row 1: ~ ` | • √ π ÷ × § Δ
        KeyboardRow {
            val r1 = "~`|•√π÷×§Δ"
            r1.forEach { char ->
                KeyboardKey(
                    modifier = Modifier.weight(1f).height(67.5.dp),
                    text = char.toString(),
                    onClick = { actionListener.onText(char.toString()) }
                )
            }
        }

        // Row 2: £ ¢ € ¥ ^ ° = { } \
        KeyboardRow {
            val r2 = "£¢€¥^°={}\\"
            r2.forEach { char ->
                KeyboardKey(
                    modifier = Modifier.weight(1f).height(67.5.dp),
                    text = char.toString(),
                    onClick = { actionListener.onText(char.toString()) }
                )
            }
        }

        // Row 3: ?123 % © ® ™ ✓ [ ] Backspace
        KeyboardRow {
            // Return to Page 1
            KeyboardKey(
                modifier = Modifier.weight(1.5f).height(67.5.dp),
                text = "?123", 
                isSpecial = true,
                onClick = { symbolPage = 0 }
            )

            val r3 = "%©®™✓[]"
            r3.forEach { char ->
                KeyboardKey(
                    modifier = Modifier.weight(1f).height(67.5.dp),
                    text = char.toString(),
                    onClick = { actionListener.onText(char.toString()) }
                )
            }

            // Backspace
            KeyboardKey(
                modifier = Modifier.weight(1.5f).height(67.5.dp),
                icon = KeyboardIcons.Backspace,
                isSpecial = true, 
                onClick = { actionListener.onDelete() },
                onRepeat = { actionListener.onDelete() }
            )
        }

        // Row 4: ABC - < - 1234 - Space - > - Enter
        KeyboardRow {
            // Return to Alpha
            KeyboardKey(
                modifier = Modifier.weight(1.5f).height(67.5.dp),
                text = "ABC",
                isSpecial = true,
                onClick = onReturnToAlpha
            )
            
            // <
            KeyboardKey(
                modifier = Modifier.weight(1f).height(67.5.dp),
                text = "<",
                isSpecial = true,
                onClick = { actionListener.onText("<") }
            )

            // 1234 (Placeholder)
            // 1234 (Number Pad Trigger)
            KeyboardKey(
                modifier = Modifier.weight(1f).height(67.5.dp),
                text = "12\n34", 
                isSpecial = true,
                onClick = onNumberPadClick
            )
            
            // Space
            KeyboardKey(
                modifier = Modifier.weight(4f).height(67.5.dp),
                text = "space",
                onClick = { actionListener.onText(" ") }
            )

            // >
            KeyboardKey(
                modifier = Modifier.weight(1f).height(67.5.dp),
                text = ">",
                isSpecial = true,
                onClick = { actionListener.onText(">") }
            )
            
            // Enter
            KeyboardKey(
                modifier = Modifier.weight(1.5f).height(67.5.dp),
                icon = KeyboardIcons.Enter,
                isSpecial = true, 
                onClick = { actionListener.onEnter() }
            )
        }
        }
    }
}


@Composable
fun KeyboardRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp)
    ) {
        content()
    }
}
