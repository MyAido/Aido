# Keyboard Theme Verification Plan

## Manual Verification Checklist

### 1. System Default (Auto)
- [ ] **Setup**: Go to Aido Settings -> Keyboard Theme -> Select **System Default**.
- [ ] **Action**: Change Android System Theme to **Dark**.
- [ ] **Expected**: Keyboard background changes to Dark Grey (`#263238`), keys become dark.
- [ ] **Action**: Change Android System Theme to **Light**.
- [ ] **Expected**: Keyboard background changes to Light Grey (`#ECEFF1`), keys become white.

### 2. Force Light Theme
- [ ] **Setup**: Go to Aido Settings -> Keyboard Theme -> Select **Light**.
- [ ] **Action**: Change Android System Theme to **Dark**.
- [ ] **Expected**: Keyboard remains in **Light** mode (White keys) despite system being Dark.

### 3. Force Dark Theme
- [ ] **Setup**: Go to Aido Settings -> Keyboard Theme -> Select **Dark**.
- [ ] **Action**: Change Android System Theme to **Light**.
- [ ] **Expected**: Keyboard remains in **Dark** mode (Dark keys) despite system being Light.

### 4. Persistence
- [ ] **Action**: Select **Dark** theme. Restart the device (or force stop the app).
- [ ] **Expected**: Keyboard remembers the **Dark** setting upon next launch.

### 5. Visual Polish
- [ ] **Check**: Key shadows are visible but subtle (3D effect).
- [ ] **Check**: Popup preview matches the current theme (Light popup on Light theme, Dark on Dark).
- [ ] **Check**: Icons (Undo/Redo, Mic, Menu) are correctly tinted (Dark Grey in Light mode, Light Grey in Dark mode).

## Automated UI Test Plan (Concept)

If implementing Espresso tests, follow this structure:

```kotlin
@RunWith(AndroidJUnit4::class)
class KeyboardThemeTest {

    @Rule
    @JvmField
    val rule = ServiceTestRule()

    @Test
    fun testThemeSwitching() {
        // 1. Set Preference to SYSTEM
        DataStoreManager(context).saveThemeMode(ThemeMode.SYSTEM)

        // 2. Simulate Night Mode
        setSystemNightMode(Configuration.UI_MODE_NIGHT_YES)
        
        // 3. Verify Background Color
        onView(withId(R.id.keyboard_view))
            .check(matches(hasBackgroundColor(R.color.keyboard_bg_dark)))

        // 4. Simulate Day Mode
        setSystemNightMode(Configuration.UI_MODE_NIGHT_NO)

        // 5. Verify Background Color
        onView(withId(R.id.keyboard_view))
            .check(matches(hasBackgroundColor(R.color.keyboard_bg_light)))
    }

    @Test
    fun testForceDarkTheme() {
        // 1. Set Preference to DARK
        DataStoreManager(context).saveThemeMode(ThemeMode.DARK)

        // 2. Simulate Day Mode
        setSystemNightMode(Configuration.UI_MODE_NIGHT_NO)

        // 3. Verify Background is still Dark
        onView(withId(R.id.keyboard_view))
            .check(matches(hasBackgroundColor(R.color.keyboard_bg_dark)))
    }
}
```
