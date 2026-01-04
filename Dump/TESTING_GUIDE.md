# 🧪 Aido Testing Guide

## 🔍 Step-by-Step Debugging

### Step 1: Rebuild App
```bash
Build > Clean Project
Build > Rebuild Project
```

### Step 2: Uninstall Old Version
- Long press Aido app icon
- Uninstall
- Or use: `adb uninstall com.rr.aido`

### Step 3: Fresh Install
- Run app from Android Studio
- Wait for installation

### Step 4: Open Logcat
Android Studio mein:
1. Click "Logcat" tab (bottom)
2. Filter lagao: `AidoAccessibility`
3. Clear logs (trash icon)

### Step 5: Enable Accessibility
1. App kholo
2. "Grant Permission" button dabao
3. Aido service ON karo
4. Back button se app mein wapas aao
5. Check: Green card "✅ Accessibility service is enabled!" dikhna chahiye

### Step 6: Set API Key
1. Settings > API Key paste karo
2. Model select karo
3. "Test and Save API Key" dabao
4. Wait for success message

### Step 7: Test in WhatsApp

#### Test 1: Basic Trigger Detection
1. WhatsApp kholo
2. Kisi chat mein jao
3. Type karo: `hello@aido`
4. **Expected Logs:**
   ```
   Event received: TYPE_VIEW_TEXT_CHANGED
   Event text: hello@aido
   Checking text: hello@aido
   Trigger found: @aido
   Processing trigger: @aido
   Starting to process text: hello@aido
   ```

#### Test 2: Grammar Fix
1. Type: `This sentence are wrong@fixg`
2. **Expected:**
   - Toast: "Aido: Processing @fixg..."
   - Toast: "Aido: ✅ Response copied to clipboard!"
   - Clipboard mein corrected text

#### Test 3: Question
1. Type: `What is the capital of France@aido`
2. **Expected:**
   - Processing toast
   - Response in clipboard
   - Paste karo to check

## 🐛 Common Issues & Solutions

### Issue 1: No Events in Logcat
**Problem:** Koi bhi log nahi aa raha

**Solution:**
1. Check accessibility service ON hai ya nahi
2. Service restart karo:
   - Settings > Accessibility > Aido > OFF
   - Wait 2 seconds
   - ON karo
3. App force stop karo aur reopen karo

### Issue 2: Events Aa Rahe Hain But Text Nahi Mil Raha
**Logs:**
```
Event received: TYPE_VIEW_TEXT_CHANGED
Event text: 
Node text: 
```

**Solution:**
- Yeh WhatsApp ka security feature ho sakta hai
- Try in different apps:
  - Google Keep (Notes)
  - Gmail
  - Messages (SMS app)
  - Chrome browser search

### Issue 3: Trigger Detect Nahi Ho Raha
**Logs:**
```
Checking text: hello@aido
Trigger found: null
```

**Solution:**
- Check PromptParser regex
- Trigger ke pehle space hona chahiye ya nahi?
- Try: `hello @aido` (with space)

### Issue 4: API Key Error
**Toast:** "Aido: Please set API key in settings"

**Solution:**
1. Settings mein jao
2. API key check karo
3. "Test and Save" dobara dabao
4. App restart karo

### Issue 5: No Response from Gemini
**Logs:**
```
Processing with preprompt: @aido
Final prompt: Give only the most relevant...
Error: Failed to send prompt
```

**Solution:**
- Internet connection check karo
- API key valid hai ya nahi verify karo
- Gemini API quota check karo

## 📊 Expected Log Flow (Success Case)

```
[AccessibilityUtils] Package name: com.rr.aido
[AccessibilityUtils] Enabled services: com.rr.aido/com.rr.aido.service.AidoAccessibilityService
[AccessibilityUtils] Accessibility service enabled: true

[AidoAccessibility] Aido Accessibility Service created
[AidoAccessibility] Event received: 8, Package: com.whatsapp
[AidoAccessibility] Text changed event
[AidoAccessibility] Event text: What is the capital of France@aido
[AidoAccessibility] Node text: What is the capital of France@aido
[AidoAccessibility] Checking text: What is the capital of France@aido
[AidoAccessibility] Trigger found: @aido
[AidoAccessibility] Processing trigger: @aido
[AidoAccessibility] Starting to process text: What is the capital of France@aido
[AidoAccessibility] Settings loaded - API Key: SET
[AidoAccessibility] Preprompts loaded: 12
[AidoAccessibility] Parse result - Trigger: @aido, Matched: true
[AidoAccessibility] Processing with preprompt: @aido
[AidoAccessibility] Final prompt: Give only the most relevant and complete answer to the query. Do not explain, do not add introductions, disclaimers, or extra text. Output only the answer. What is the capital of France
[AidoAccessibility] Got response from Gemini: Paris
```

## 🎯 Test Cases

### ✅ Must Pass Tests

1. **Accessibility Detection**
   - [ ] Green card shows when service enabled
   - [ ] Red card shows when service disabled
   - [ ] Updates within 2 seconds

2. **API Key Management**
   - [ ] Can save API key
   - [ ] Test button works
   - [ ] Shows success/error message

3. **Trigger Detection**
   - [ ] @aido detected
   - [ ] @fixg detected
   - [ ] @summ detected
   - [ ] Custom triggers detected

4. **Text Processing**
   - [ ] Toast shows "Processing..."
   - [ ] Response copied to clipboard
   - [ ] Success toast shows

5. **Multiple Apps**
   - [ ] Works in WhatsApp
   - [ ] Works in Gmail
   - [ ] Works in Notes
   - [ ] Works in Chrome

## 🔧 Debug Commands

### Check Accessibility Services
```bash
adb shell settings get secure enabled_accessibility_services
```

### Check App Logs
```bash
adb logcat | grep -E "Aido|Accessibility"
```

### Clear App Data
```bash
adb shell pm clear com.rr.aido
```

### Force Stop App
```bash
adb shell am force-stop com.rr.aido
```

## 📝 Report Format

Agar koi issue ho, toh yeh information do:

```
**Device:** [Phone model, Android version]
**App Version:** 1.0
**Issue:** [Brief description]

**Steps to Reproduce:**
1. 
2. 
3. 

**Expected:** 
**Actual:** 

**Logcat Output:**
```
[Paste relevant logs here]
```

**Screenshots:** [If applicable]
```

## 🚀 Next Steps After Testing

1. If working: Test in 5+ different apps
2. If not working: Share logcat output
3. Test all 12 default commands
4. Test custom preprompts
5. Test with real Gemini API (uncomment code)

---

**Happy Testing! 🎉**
