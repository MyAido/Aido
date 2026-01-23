package com.rr.aido.keyboard.ui

import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned

// iOS Colors
private val LightKeyBackground = Color(0xFFFFFFFF)
private val LightKeyShadow = Color(0xFF888888)
private val DarkKeyBackground = Color(0xFF6D6D6D).copy(alpha = 0.3f)
private val DarkKeyBackgroundPressed = Color(0xFFFFFFFF).copy(alpha = 0.5f)

private val LightSpecialKeyBackground = Color(0xFFACB4BC)
private val DarkSpecialKeyBackground = Color(0xFF424242)

private val LightTextColor = Color.Black
private val DarkTextColor = Color.White




// Theme CompositionLocal
val LocalKeyboardIsDark = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun KeyboardKey(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    isSpecial: Boolean = false,
    isPressed: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onRepeat: (() -> Unit)? = null, // Backend for backspace
    weight: Float = 1f,
    backgroundColor: Color? = null,
    textColor: Color? = null,
    onKeyLayout: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null
) {
    val isDark = LocalKeyboardIsDark.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState by interactionSource.collectIsPressedAsState()
    
    // Scale animation
    val scale by animateFloatAsState(if (isPressedState) 0.95f else 1f, label = "scale")

    // Repeating logic
    if (onRepeat != null) {
        val currentOnRepeat by remember { androidx.compose.runtime.mutableStateOf(onRepeat) }
        LaunchedEffect(isPressedState) {
            if (isPressedState) {
                delay(500) // Initial delay
                while (isPressedState) {
                    currentOnRepeat!!() // Call callback
                    delay(50) // Repeat interval
                }
            }
        }
    }
    
    // UI Constants
    val cornerRadius = 5.dp
    val shadowElevation = if (isDark) 0.dp else 1.dp
    
    // Colors
    val finalBackgroundColor = when {
        isPressedState -> if (isDark) DarkKeyBackgroundPressed else Color(0xFFE5E5E5) // Pressed state
        backgroundColor != null -> backgroundColor // Use custom color if provided (and not pressed)
        isSpecial -> if (isDark) DarkSpecialKeyBackground else LightSpecialKeyBackground
        else -> if (isDark) DarkKeyBackground else LightKeyBackground
    }
    
    val contentColor = textColor ?: if (isDark) DarkTextColor else LightTextColor

    Box(
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 6.dp)
            .zIndex(if (isPressedState) 10f else 0f) // Bring to front when pressed
            .onGloballyPositioned { layoutCoordinates ->
                onKeyLayout?.invoke(layoutCoordinates)
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. The Actual Key visual
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if(isPressed) 0.dp else shadowElevation, 
                    shape = RoundedCornerShape(cornerRadius),
                    clip = false
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(finalBackgroundColor)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (text != null) {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = if (text.length > 1) 16.sp else 22.sp,
                    fontWeight = if (text.length > 1) FontWeight.Normal else FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            } else if (icon != null) {
                Image(
                    imageVector = icon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.fillMaxSize(0.5f) // Icon size scaling
                )
            }
        }

        // 2. The Popup (Key Preview)
        // Only show for single-character text keys (not "space", "enter", etc)
        if (isPressedState && text != null && !isSpecial && text.length == 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-60).dp) // Float above the key
                    .size(width = 62.dp, height = 70.dp) // Fixed large size for preview
                    .shadow(4.dp, RoundedCornerShape(5.dp))
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) Color(0xFF3A3A3C) else Color.White),
                contentAlignment = Alignment.Center
            ) {
                 Text(
                    text = text,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                 )
            }
        }
    }
}

@Composable
fun KeyboardKeyContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
        content = content
    )
}
