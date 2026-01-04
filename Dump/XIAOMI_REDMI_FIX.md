# 🔧 Fixing Aido on Xiaomi/Redmi Devices (MIUI/HyperOS)

## Problem
Accessibility service shows "Not working" on MIUI devices.

## Why This Happens
MIUI has aggressive battery optimization and background process management that kills accessibility services. This is a known MIUI limitation, not an app bug.

## Complete Solution

Follow **ALL** steps below for the app to work properly:

### Step 1: Disable Battery Optimization ⚡

1. Open **Settings**
2. Go to **Apps** → **Manage apps**
3. Find and tap **Aido**
4. Tap **Battery saver**
5. Select **No restrictions**

### Step 2: Enable Autostart 🚀

1. Open the **Security** app (or **Settings** → **Apps**)
2. Go to **Permissions** → **Autostart**
3. Find **Aido**
4. Toggle it **ON** ✅

### Step 3: Allow Background Permissions 🔓

1. **Settings** → **Apps** → **Manage apps**
2. Select **Aido**
3. Go to **Other permissions**
4. Enable:
   - **Display pop-up windows while running in the background**
   - **Start in background** (if available)

### Step 4: Lock App in Recent Apps 🔒

1. Press the **Recent apps** button
2. Find the **Aido** app card
3. **Swipe down** on the app card
4. A **lock icon** will appear
5. This prevents MIUI from killing the app

### Step 5: Disable MIUI Optimization ⚙️

1. Go to **Settings** → **About phone**
2. Tap **MIUI version** **7-10 times** rapidly
3. "Developer Options enabled" will appear
4. Go to **Settings** → **Additional settings** → **Developer options**
5. Find **MIUI optimization** and turn it **OFF**
6. **Restart** your phone

### Step 6: Allow Restricted Settings (MIUI 14+/HyperOS only) 🛡️

If you're on MIUI 14 or HyperOS:

1. **Settings** → **Apps** → **Manage apps**
2. Select **Aido**
3. Scroll to the bottom
4. Enable **Allow restricted settings**

### Step 7: Re-enable Accessibility Service ♿

1. Go to **Settings** → **Accessibility**
2. Tap **Downloaded apps** (or **Installed services**)
3. Find **Aido**
4. Toggle it **OFF**, then **ON** again
5. Grant all requested permissions

### Step 8: Verify Display Overlay Permission 👁️

1. **Settings** → **Apps** → **Manage apps**
2. Select **Aido**
3. Find **Display over other apps**
4. Ensure it's **Allowed**

## Quick Checklist ✅

- [ ] Battery optimization: **No restrictions**
- [ ] Autostart: **Enabled**
- [ ] Background permissions: **Allowed**
- [ ] App locked in recents 🔒
- [ ] MIUI optimization: **Disabled**
- [ ] Restricted settings: **Allowed** (MIUI 14+)
- [ ] Accessibility service: **Enabled**
- [ ] Display over apps: **Allowed**

## Important Tips 💡

1. **Avoid Super Battery Saver mode** - It forcefully kills accessibility services
2. **After phone restart** - Check accessibility service and re-enable if needed
3. **After app updates** - Verify settings haven't been reset
4. **Don't clear Aido from recents** - Keep it locked

## Still Not Working? 🆘

1. **Uninstall and reinstall** the app
2. Repeat all steps above
3. Check if your MIUI version is compatible
4. Consider updating MIUI to the latest version

## Tested On ✔️

- MIUI 12, 13, 14
- HyperOS
- Redmi Note series (8, 9, 10, 11, 12, 13)
- Poco series
- Xiaomi Mi series

## Why So Many Steps? 🤔

MIUI is designed to maximize battery life by aggressively managing background apps. While this is good for battery, it breaks apps that need to run in the background like accessibility services. These steps "whitelist" Aido so MIUI doesn't interfere with it.

---

**Note:** These settings are required for ANY accessibility service app on MIUI, not just Aido. Once configured, the app will work reliably!

For more help, visit: [Aido Support](#) or contact the developer.
