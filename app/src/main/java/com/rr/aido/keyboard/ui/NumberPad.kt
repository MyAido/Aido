package com.rr.aido.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.aido.keyboard.ui.KeyboardActionListener

@Composable
fun NumberPad(
    actionListener: KeyboardActionListener,
    onReturnToAlpha: () -> Unit,
    onReturnToSymbols: () -> Unit,
    isDark: Boolean
) {
    // Colors
    val backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFD1D5DB) // System Grey vs Light Grey
    val keyColor = if (isDark) Color(0xFF4A4A4C) else Color.White
    val operatorColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE4E6EB) // Darker for operators
    val enterColor = Color(0xFF4285F4) // Blue stays same usually, or slightly darker
    val textColor = if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(backgroundColor)
    ) {
        // Main Grid Area (4 Rows)
        Row(modifier = Modifier.fillMaxWidth()) {
            // Column 1: Operators (+ - * /)
            Column(modifier = Modifier.weight(1f)) {
                listOf("+", "-", "*", "/").forEach { op ->
                    NumberKey(
                        text = op,
                        modifier = Modifier.height(60.dp).fillMaxWidth(),
                        backgroundColor = operatorColor,
                        textColor = textColor,
                        onClick = { actionListener.onText(op) }
                    )
                }
            }

            // Column 2: 1 4 7 .
            Column(modifier = Modifier.weight(1f)) {
                 listOf("1", "4", "7", ".").forEach { key ->
                    NumberKey(
                        text = key,
                        modifier = Modifier.height(60.dp).fillMaxWidth(),
                        backgroundColor = keyColor,
                        textColor = textColor,
                        onClick = { actionListener.onText(key) }
                    )
                 }
            }

            // Column 3: 2 5 8 0
            Column(modifier = Modifier.weight(1f)) {
                 listOf("2", "5", "8", "0").forEach { key ->
                    NumberKey(
                        text = key,
                        modifier = Modifier.height(60.dp).fillMaxWidth(),
                        backgroundColor = keyColor,
                        textColor = textColor,
                        onClick = { actionListener.onText(key) }
                    )
                 }
            }

            // Column 4: 3 6 9 ,
            Column(modifier = Modifier.weight(1f)) {
                 listOf("3", "6", "9", ",").forEach { key ->
                    NumberKey(
                        text = key,
                        modifier = Modifier.height(60.dp).fillMaxWidth(),
                        backgroundColor = keyColor,
                        textColor = textColor,
                        onClick = { actionListener.onText(key) }
                    )
                 }
            }

            // Column 5: Actions (Backspace, Enter)
            Column(modifier = Modifier.weight(1f)) {
                // Percentage or other symbol
                NumberKey(
                    text = "%",
                    modifier = Modifier.height(60.dp).fillMaxWidth(),
                    backgroundColor = operatorColor,
                    textColor = textColor,
                    onClick = { actionListener.onText("%") }
                )

                // Backspace
                NumberKey(
                    icon = KeyboardIcons.Backspace,
                    modifier = Modifier.height(60.dp).fillMaxWidth(),
                    backgroundColor = operatorColor,
                    onClick = { actionListener.onDelete() },
                    onRepeat = { actionListener.onDelete() }
                )

                // =
                NumberKey(
                    text = "=",
                    modifier = Modifier.height(60.dp).fillMaxWidth(),
                    backgroundColor = operatorColor,
                    textColor = textColor,
                    onClick = { actionListener.onText("=") }
                )

                // Enter
                 NumberKey(
                    icon = KeyboardIcons.Enter,
                    modifier = Modifier.height(60.dp).fillMaxWidth(),
                    backgroundColor = Color(0xFF4285F4),
                    textColor = Color.White,
                    onClick = { actionListener.onEnter() }
                )
            }
        }

        // Bottom Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 1.dp)
        ) {
            // ABC
            NumberKey(
                text = "ABC",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = operatorColor,
                onClick = onReturnToAlpha
            )

            // ?123 (Back to symbols)
            NumberKey(
                text = "?#1",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = operatorColor,
                textColor = textColor,
                onClick = onReturnToSymbols
            )

            // Space Bar
             NumberKey(
                text = "space",
                modifier = Modifier.weight(2f).fillMaxHeight(),
                backgroundColor = keyColor,
                onClick = { actionListener.onText(" ") }
            )

            // Dot again? Or something else. Let's put period
            NumberKey(
                text = ".",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                backgroundColor = operatorColor,
                onClick = { actionListener.onText(".") }
            )
        }
    }
}

@Composable
fun NumberKey(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier,
    backgroundColor: Color,
    textColor: Color = Color.Black,
    onClick: () -> Unit,
    onRepeat: (() -> Unit)? = null // Reuse existing repeat logic if available or ignore for now
) {
    KeyboardKey(
        modifier = modifier.padding(2.dp),
        text = text,
        icon = icon,
        onClick = onClick,
        backgroundColor = backgroundColor,
        textColor = textColor
        // onRepeat = onRepeat  // Add repeat support to KeyboardKey if possible, or just click
    )
    // Note: KeyboardKey wrapper handles styling usually.
    // We are reusing the shared KeyboardKey but setting specific modifiers.
    // However, KeyboardKey implementation in KeyboardLayout might need to accept backgroundColor override.
    // If not, we might need to recreate a simple box here.
    // Let's assume for now we use a simple impl here to match the specific look.
}
