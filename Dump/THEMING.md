# Aido Keyboard Theming

This document explains how the keyboard theming system works and how to customize it.

## Overview

The keyboard supports **Light**, **Dark**, and **System Default** themes. The theme is applied dynamically using a `ContextThemeWrapper` in `AidoInputMethodService`.

## Theme Resources

### 1. Attributes (`res/values/attrs.xml`)
We define custom attributes that the layout files use. These attributes are mapped to specific colors in the themes.
- `keyboardBackground`: Main background color of the keyboard.
- `keyboardKeyBackground`: Background color of the keys.
- `keyboardKeyShadow`: Shadow color for the 3D key effect.
- `keyboardKeyText`: Color of the key labels.
- `keyboardKeyIcon`: Tint color for icons (menu, mic, undo/redo).
- `keyboardPopupBackground`: Background color for the key popup preview.

### 2. Themes (`res/values/themes.xml`)
We define two themes that implement these attributes:
- `Theme.Aido.Keyboard.Light`: Maps attributes to `_light` colors.
- `Theme.Aido.Keyboard.Dark`: Maps attributes to `_dark` colors.

### 3. Colors (`res/values/colors.xml` & `res/values-night/colors.xml`)
The actual color values are defined here.
- `keyboard_bg_light` / `_dark`
- `key_bg_light` / `_dark`
- `key_shadow_light` / `_dark`
- ...and so on.

## How to Change Colors

To change the look of the keyboard, you only need to modify **`res/values/colors.xml`**.
The `values-night/colors.xml` file maps the `_light` names to `_dark` colors for backward compatibility if needed, but the new system primarily uses the explicit `_light` and `_dark` color resources defined in the main `colors.xml`.

**Example:**
To change the key background color in Dark Mode:
1. Open `res/values/colors.xml`.
2. Find `key_bg_dark`.
3. Change the hex code.

## Layouts

The layouts (`keyboard_with_suggestions.xml`, `key_preview.xml`) and drawables (`key_background.xml`) use `?attr/attributeName` instead of hardcoded colors. This allows them to automatically adapt to the applied theme.

## Logic

`AidoInputMethodService.kt` listens for changes in the `theme_mode` preference.
- If **System** is selected, it checks the system's night mode configuration.
- If **Light** or **Dark** is selected, it forces the respective theme.
- When the theme changes, the keyboard view is re-created with the new theme context.
