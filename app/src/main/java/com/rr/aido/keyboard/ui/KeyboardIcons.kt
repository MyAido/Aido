package com.rr.aido.keyboard.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object KeyboardIcons {
    // Custom Shift Arrow (iOS Style - Outline)
    val Shift: ImageVector
        get() = ImageVector.Builder(
            name = "Shift",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null, // No fill for outline
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Up arrow shape
                moveTo(12f, 4f)
                lineTo(5f, 11f)
                lineTo(9f, 11f)
                lineTo(9f, 20f)
                lineTo(15f, 20f)
                lineTo(15f, 11f)
                lineTo(19f, 11f)
                close()
            }
        }.build()

    // Custom Shift Filled (Active)
    val ShiftFilled: ImageVector
        get() = ImageVector.Builder(
            name = "ShiftFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
             path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // Up arrow shape filled
                moveTo(12f, 4f)
                lineTo(5f, 11f)
                lineTo(9f, 11f)
                lineTo(9f, 20f)
                lineTo(15f, 20f)
                lineTo(15f, 11f)
                lineTo(19f, 11f)
                close()
            }
        }.build()

    // Custom Shift Caps Lock (Filled with Line)
    val ShiftCaps: ImageVector
        get() = ImageVector.Builder(
            name = "ShiftCaps",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
             path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero
            ) {
                // Up arrow shape filled
                moveTo(12f, 2f) // Shifted up slightly
                lineTo(5f, 9f)
                lineTo(9f, 9f)
                lineTo(9f, 17f)
                lineTo(15f, 17f)
                lineTo(15f, 9f)
                lineTo(19f, 9f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero
            ) {
                // Underline
                moveTo(5f, 20f)
                lineTo(19f, 20f)
                lineTo(19f, 22f)
                lineTo(5f, 22f)
                close()
            }
        }.build()

    // Custom Backspace (Pentagon with X)
    val Backspace: ImageVector
        get() = ImageVector.Builder(
            name = "Backspace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Outline shape
                moveTo(22f, 6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                lineTo(9f, 4f)
                curveTo(8.4f, 4f, 7.8f, 4.3f, 7.4f, 4.7f)
                lineTo(1.4f, 11.3f) // Point
                curveTo(1f, 11.7f, 1f, 12.3f, 1.4f, 12.7f)
                lineTo(7.4f, 19.3f)
                curveTo(7.8f, 19.7f, 8.4f, 20f, 9f, 20f)
                lineTo(20f, 20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                lineTo(22f, 6f)
                close()
                // X inside
                moveTo(17.3f, 14.3f)
                lineTo(15.9f, 15.7f)
                lineTo(13.5f, 13.3f)
                lineTo(11.1f, 15.7f)
                lineTo(9.7f, 14.3f)
                lineTo(12.1f, 11.9f)
                lineTo(9.7f, 9.5f)
                lineTo(11.1f, 8.1f)
                lineTo(13.5f, 10.5f)
                lineTo(15.9f, 8.1f)
                lineTo(17.3f, 9.5f)
                lineTo(14.9f, 11.9f)
                lineTo(17.3f, 14.3f)
                close()
            }
        }.build()

    // Custom Enter (Return Arrow)
    val Enter: ImageVector
        get() = ImageVector.Builder(
            name = "Enter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 6f)
                lineTo(20f, 14f)
                lineTo(5f, 14f)
                moveTo(9f, 10f)
                lineTo(5f, 14f)
                lineTo(9f, 18f)
            }
        }.build()

    // Custom Emoji (Smiley Face)
    val Emoji: ImageVector
        get() = ImageVector.Builder(
            name = "Emoji",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                 // Face circle
                 moveTo(22f, 12f)
                 curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
                 curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
                 curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f)
                 curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
                 close()
            }
             path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                 // Eyes
                 moveTo(8f, 9f)
                 lineTo(8.01f, 9f)
                 moveTo(16f, 9f)
                 lineTo(16.01f, 9f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Smile
                moveTo(7f, 15f)
                 curveTo(7f, 15f, 9f, 17f, 12f, 17f)
                 curveTo(15f, 17f, 17f, 15f, 17f, 15f)
            }
        }.build()
    // Custom Grid Menu (4 Squares - Gboard Style)
    val GridMenu: ImageVector
        get() = ImageVector.Builder(
            name = "GridMenu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                 // Top Left
                 moveTo(4f, 4f)
                 lineTo(10f, 4f)
                 lineTo(10f, 10f)
                 lineTo(4f, 10f)
                 close()
                 
                 // Top Right
                 moveTo(14f, 4f)
                 lineTo(20f, 4f)
                 lineTo(20f, 10f)
                 lineTo(14f, 10f)
                 close()
                 
                 // Bottom Left
                 moveTo(4f, 14f)
                 lineTo(10f, 14f)
                 lineTo(10f, 20f)
                 lineTo(4f, 20f)
                 close()
                 
                 // Bottom Right
                 moveTo(14f, 14f)
                 lineTo(20f, 14f)
                 lineTo(20f, 20f)
                 lineTo(14f, 20f)
                 close()
            }
        }.build()
}
