package com.rr.aido.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.AiProvider
import com.rr.aido.data.repository.GeminiRepositoryImpl
import com.rr.aido.data.repository.Result
import com.rr.aido.utils.PromptParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.view.WindowManager
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Button
import android.widget.ScrollView
import android.view.ViewGroup
import android.widget.GridLayout
import com.rr.aido.R
import com.rr.aido.ui.components.ProcessingAnimationView
import com.rr.aido.data.models.ProcessingAnimationType
import com.rr.aido.ui.overlays.SearchOverlayManager

class AidoAccessibilityService : AccessibilityService() {

    private val TAG = "AidoAccessibility"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var dataStoreManager: DataStoreManager
    private val geminiRepository = GeminiRepositoryImpl()

    private var lastProcessedText: String? = null
    private var isProcessing = false
    private var currentEditableNode: AccessibilityNodeInfo? = null
    private var currentPopupView: View? = null

    private var currentAnimationView: View? = null

    private lateinit var searchOverlayManager: SearchOverlayManager

    // Undo/Redo Manager
    private lateinit var undoRedoManager: UndoRedoManager

    // Text Selection Processor
    private lateinit var textSelectionProcessor: TextSelectionProcessor

    // Temporary state for Undo/Redo capture
    private var lastOriginalText: String? = null
    private var lastGeneratedText: String? = null
    private var undoRedoNode: AccessibilityNodeInfo? = null

    companion object {
        // Shared flag to temporarily pause processing (e.g., when editing preprompts)
        @Volatile
        var isPaused = false
    }

    private var disabledApps: Set<String> = emptySet()
    private var textShortcuts: List<com.rr.aido.data.models.TextShortcut> = emptyList()

    // App Toggle Processor
    private lateinit var appToggleProcessor: AppToggleProcessor

    override fun onCreate() {
        super.onCreate()
        dataStoreManager = DataStoreManager(applicationContext)
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        undoRedoManager = UndoRedoManager(applicationContext, windowManager, dataStoreManager, serviceScope)
        undoRedoManager = UndoRedoManager(applicationContext, windowManager, dataStoreManager, serviceScope)

        searchOverlayManager = SearchOverlayManager(
            context = applicationContext,
            windowManager = windowManager,
            scope = serviceScope,
            onInsertClick = { url, textToReplace ->
                replaceTextInNode(url) // Or logic to replace textToReplace with url
            }
        )

        textSelectionProcessor = TextSelectionProcessor(
            context = applicationContext,
            dataStoreManager = dataStoreManager,
            geminiRepository = geminiRepository,
            scope = serviceScope,
            windowManager = windowManager,
            onShowAnimation = { serviceScope.launch { showProcessingAnimation() } },
            onHideAnimation = { hideProcessingAnimation() },
            onShowToast = { message, force -> showToast(message, force) }
        )

        appToggleProcessor = AppToggleProcessor(
            dataStoreManager = dataStoreManager,
            scope = serviceScope,
            onShowToast = { message, force -> showToast(message, force) }
        )

        Log.d(TAG, "Aido Accessibility Service created")

        // Monitor disabled apps
        serviceScope.launch {
            dataStoreManager.disabledAppsFlow.collect {
                disabledApps = it
                Log.d(TAG, "Disabled apps updated: $it")
            }
        }

        // Monitor text shortcuts
        serviceScope.launch {
            dataStoreManager.textShortcutsFlow.collect {
                textShortcuts = it
                Log.d(TAG, "Text shortcuts updated: ${it.size}")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Check if service is temporarily paused (e.g., editing preprompts)
        if (isPaused) {
            Log.d(TAG, "Service is paused, skipping event")
            return
        }

        // Check if app is blacklisted
        val packageName = event.packageName?.toString()
        if (packageName != null && disabledApps.contains(packageName)) {
            Log.d(TAG, "App is blacklisted: $packageName, skipping event")
            return
        }

        Log.d(TAG, "Event received: ${event.eventType}, Package: ${event.packageName}")

        // Only process text change events
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                Log.d(TAG, "Text changed event")
                handleTextEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                Log.d(TAG, "View focused event")
                handleTextEvent(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                Log.d(TAG, "Text selection changed event")
                textSelectionProcessor.onTextSelectionChanged(event)
            }
        }
    }


    private fun handleTextEvent(event: AccessibilityEvent) {
        // Try to get text from event first
        val eventText = if (event.text.isNotEmpty()) {
            event.text.joinToString(" ")
        } else {
            ""
        }

        Log.d(TAG, "Event text: $eventText")

        // Get the source node
        val source = event.source
        if (source == null) {
            Log.d(TAG, "Source is null, using event text")
            if (eventText.isNotEmpty()) {
                checkAndProcessText(eventText)
            }
            return
        }

        // Check if it's an editable text field
        if (!source.isEditable) {
            Log.d(TAG, "Source is not editable")
            source.recycle()
            return
        }

        // Get current text from node
        val nodeText = source.text?.toString() ?: ""
        Log.d(TAG, "Node text: $nodeText")

        // Use whichever text is available
        val currentText = if (nodeText.isNotEmpty()) nodeText else eventText

        if (currentText.isNotEmpty()) {
            // Store the node for later text replacement
            currentEditableNode?.recycle()
            currentEditableNode = source
            checkAndProcessText(currentText)
            checkAndProcessShortcuts(currentText)
        } else {
            source.recycle()
        }
    }


    private fun checkAndProcessShortcuts(text: String) {
        if (textShortcuts.isEmpty()) return

        // Find a matching shortcut that the text ends with
        // We look for shortcuts where the text ends with the trigger
        // Example: text "Hello !email", trigger "!email" -> match
        val matchingShortcut = textShortcuts.find { text.endsWith(it.trigger) }

        if (matchingShortcut != null) {
            Log.d(TAG, "Found shortcut match: ${matchingShortcut.trigger} -> ${matchingShortcut.replacement}")

            // Calculate the new text
            // Remove the trigger from the end and append the replacement
            val newText = text.substring(0, text.length - matchingShortcut.trigger.length) + matchingShortcut.replacement

            // Replace the text
            replaceTextInNode(newText)
        }
    }


    private fun checkAndProcessText(text: String) {
        serviceScope.launch {
            val settings = dataStoreManager.settingsFlow.first()

            // ONLY check toggle if feature is enabled
            if (settings.isAppToggleEnabled) {
                // Check for @on/@off toggle commands FIRST (even if app is toggled off)
                if (appToggleProcessor.containsToggleCommand(text)) {
                    try {
                        val wasProcessed = appToggleProcessor.processToggle(text, settings, currentEditableNode)
                        if (wasProcessed) {
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing toggle command", e)
                    }
                }

                // If app is toggled OFF, skip all processing
                if (!settings.isAppToggledOn) {
                    Log.d(TAG, "App is toggled OFF, skipping processing")
                    return@launch
                }
            }

            val smartReplyTrigger = settings.smartReplyTrigger
            val toneRewriteTrigger = settings.toneRewriteTrigger

            // Check for Smart Reply trigger
            if (text.trim().endsWith(smartReplyTrigger)) {
                Log.d(TAG, "Smart Reply trigger detected")
                processSmartReply(text, settings)
                return@launch
            }

            // Check for Tone Rewrite trigger
            if (text.trim().endsWith(toneRewriteTrigger)) {
                Log.d(TAG, "Tone Rewrite trigger detected")
                processToneRewrite(text, settings)
                return@launch
            }

            // Check for @all trigger
            if (text.trim().endsWith("@all")) {
                Log.d(TAG, "@all trigger detected")
                processAllTrigger(text, settings)
                return@launch
            }

            // Check for Search trigger
            val searchTrigger = settings.searchTrigger
            if (text.trim().endsWith(searchTrigger)) {
                Log.d(TAG, "Search trigger detected: $searchTrigger")
                // Fix: Prevent re-triggering if text hasn't changed (e.g. after closing popup)
                if (text != lastProcessedText) {
                    processSearchTrigger(text, settings)
                } else {
                    Log.d(TAG, "Skipping search - already processed")
                }
                return@launch
            }

            // Check if text contains a trigger
            val trigger = PromptParser.extractTrigger(text)

            Log.d(TAG, "Checking text: $text")
            Log.d(TAG, "Trigger found: $trigger")

            if (trigger != null && text != lastProcessedText && !isProcessing) {
                Log.d(TAG, "Processing trigger: $trigger")
                processTextWithTrigger(text)
            }
        }
    }


    private fun processSmartReply(text: String, settings: com.rr.aido.data.models.Settings) {
        if (!settings.isSmartReplyEnabled) {
            Log.d(TAG, "Smart Reply is disabled in settings")
            return
        }

        if (!android.provider.Settings.canDrawOverlays(this@AidoAccessibilityService)) {
            Log.d(TAG, "Overlay permission not granted")
            showToast("Aido: Please grant 'Display over other apps' permission for Smart Reply", force = true)
            return
        }

        Log.d(TAG, "Processing Smart Reply...")

        serviceScope.launch {
            // Show processing animation
            showProcessingAnimation()

            try {
                // 1. Read context (screen content)
                val contextText = readScreenContext()
                Log.d(TAG, "Context read: ${contextText.take(100)}...")

                // 2. Get suggestions from AI
                val suggestions = getSmartReplySuggestions(contextText, settings)

                // 3. Show popup
                if (suggestions.isNotEmpty()) {
                    showReplyPopup(suggestions, settings.smartReplyTrigger)
                } else {
                    showToast("Aido: No suggestions found")
                }
            } finally {
                // Hide animation
                hideProcessingAnimation()
            }
        }
    }


    private fun processToneRewrite(text: String, settings: com.rr.aido.data.models.Settings) {
        if (!settings.isToneRewriteEnabled) {
            return
        }

        if (!android.provider.Settings.canDrawOverlays(this@AidoAccessibilityService)) {
            showToast("Aido: Please grant 'Display over other apps' permission", force = true)
            return
        }

        // Extract text to rewrite (remove trigger)
        val trigger = settings.toneRewriteTrigger
        val originalText = text.substringBeforeLast(trigger).trim()
        if (originalText.isEmpty()) {
            showToast("Aido: Type something before $trigger", force = true)
            return
        }

        Log.d(TAG, "Rewriting text: $originalText")

        serviceScope.launch {
            // Show processing animation
            showProcessingAnimation()

            try {
                // Get rewrites from AI
                val suggestions = getToneRewrites(originalText, settings)

                if (suggestions.isNotEmpty()) {
                    // We want to replace the whole "originalText @tone" sequence
                    // But since 'text' passed here IS that sequence (mostly), we can pass 'text' as target to replace.
                    showReplyPopup(suggestions, text)
                } else {
                    showToast("Aido: Could not generate rewrites")
                }
            } finally {
                // Hide animation
                hideProcessingAnimation()
            }
        }
    }


    private fun processAllTrigger(text: String, settings: com.rr.aido.data.models.Settings) {
        if (!settings.isAllTriggerEnabled) {
            return
        }

        serviceScope.launch {
            // Get preprompts
            val preprompts = dataStoreManager.prepromptsFlow.first()

            // Build set of all available (enabled) triggers
           val availableTriggers = mutableSetOf<String>()

            if (settings.isSmartReplyEnabled) {
                availableTriggers.add(settings.smartReplyTrigger)
            }

            if (settings.isToneRewriteEnabled) {
                availableTriggers.add(settings.toneRewriteTrigger)
            }

            if (settings.isSearchTriggerEnabled) {
                availableTriggers.add(settings.searchTrigger)
            }

            // Add all preprompts
            preprompts.forEach { availableTriggers.add(it.trigger) }

            // Use custom order if available, otherwise use default ordering
            val allTriggers = if (settings.allMenuOrder.isNotEmpty()) {
                // Filter custom order to only show available/enabled triggers
                settings.allMenuOrder.filter { it in availableTriggers }
            } else {
                // Default order: special commands first, then preprompts
                val specialCommands = mutableListOf<String>()
                if (settings.isSmartReplyEnabled) specialCommands.add(settings.smartReplyTrigger)
                if (settings.isToneRewriteEnabled) specialCommands.add(settings.toneRewriteTrigger)
                if (settings.isSearchTriggerEnabled) specialCommands.add(settings.searchTrigger)

                val customTriggers = preprompts.map { it.trigger }
                specialCommands + customTriggers
            }

            if (allTriggers.isNotEmpty()) {
                showReplyPopup(allTriggers, "@all")
            } else {
                showToast("Aido: No commands available")
            }
        }
    }


    private fun processSearchTrigger(text: String, settings: com.rr.aido.data.models.Settings) {
        if (!settings.isSearchTriggerEnabled) {
            return
        }

        if (!android.provider.Settings.canDrawOverlays(this@AidoAccessibilityService)) {
            showToast("Aido: Please grant 'Display over other apps' permission", force = true)
            return
        }

        val trigger = settings.searchTrigger

        // Extract query: "hello world @search" -> "hello world"
        val query = text.substringBeforeLast(trigger).trim()

        if (query.isEmpty()) {
            showToast("Aido: Type something before $trigger", force = true)
            return
        }

        searchOverlayManager.showSearchPopup(query, text, settings)

        // Mark as processed to prevent re-trigger loop
        lastProcessedText = text
    }

    private fun readScreenContext(): String {
        val root = rootInActiveWindow ?: return ""
        val textBuilder = StringBuilder()

        // Simple DFS to collect text
        fun traverse(node: AccessibilityNodeInfo) {
            if (node.text != null && node.text.isNotEmpty()) {
                // Ignore the input field itself (which contains @reply)
                if (node != currentEditableNode) {
                    textBuilder.append(node.text).append("\n")
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    traverse(child)
                    child.recycle()
                }
            }
        }

        traverse(root)
        return textBuilder.toString()
    }

    private suspend fun getSmartReplySuggestions(context: String, settings: com.rr.aido.data.models.Settings): List<String> {
        val provider = settings.provider
        val apiKeyToUse = when (provider) {

            AiProvider.GEMINI -> settings.apiKey
            AiProvider.CUSTOM -> settings.customApiKey
        }

        if (provider == AiProvider.GEMINI && apiKeyToUse.isEmpty()) {
            showToast("Aido: Please set your Gemini API key", force = true)
            return emptyList()
        }

        if (provider == AiProvider.CUSTOM && apiKeyToUse.isEmpty()) {
            showToast("Aido: Please set your custom API key", force = true)
            return emptyList()
        }

        // Construct prompt
        val instructions = settings.smartReplyPrompt.ifEmpty { PromptParser.DEFAULT_SMART_REPLY_INSTRUCTIONS }
        val prompt = """
            Context from screen:
            $context

            $instructions
        """.trimIndent()

        // Get model based on provider
        val modelToUse = if (provider == AiProvider.CUSTOM) settings.customModelName else settings.selectedModel

        val result = geminiRepository.sendPrompt(
            provider = provider,
            apiKey = apiKeyToUse,
            model = modelToUse,
            prompt = prompt,
            customApiUrl = settings.customApiUrl
        )

        return when (result) {
            is Result.Success -> {
                result.data.lines()
                    .filter { it.isNotBlank() }
                    .map { it.trim().removePrefix("- ").removePrefix("\"").removeSuffix("\"") }
                    .take(6)
            }
            is Result.Error -> {
                Log.e(TAG, "Smart Reply Error: ${result.message}")
                showToast("Aido: Error - ${result.message}", force = true)
                emptyList()
            }
            else -> {
                emptyList()
            }
        }
    }

    private suspend fun getToneRewrites(originalText: String, settings: com.rr.aido.data.models.Settings): List<String> {
        val provider = settings.provider
        val apiKeyToUse = when (provider) {

            AiProvider.GEMINI -> settings.apiKey
            AiProvider.CUSTOM -> settings.customApiKey
        }

        if (provider == AiProvider.GEMINI && apiKeyToUse.isEmpty()) {
            showToast("Aido: Please set your Gemini API key", force = true)
            return emptyList()
        }

        if (provider == AiProvider.CUSTOM && apiKeyToUse.isEmpty()) {
            showToast("Aido: Please set your custom API key", force = true)
            return emptyList()
        }

        val instructions = settings.toneRewritePrompt.ifEmpty { PromptParser.DEFAULT_TONE_REWRITE_INSTRUCTIONS }
        val prompt = """
            Original text: "$originalText"

            $instructions
        """.trimIndent()

        val modelToUse = if (provider == AiProvider.CUSTOM) settings.customModelName else settings.selectedModel

        val result = geminiRepository.sendPrompt(
            provider = provider,
            apiKey = apiKeyToUse,
            model = modelToUse,
            prompt = prompt,
            customApiUrl = settings.customApiUrl
        )

        return when (result) {
            is Result.Success -> {
                result.data.lines()
                    .filter { it.isNotBlank() }
                    .map { it.trim().removePrefix("- ").removePrefix("\"").removeSuffix("\"") }
                    .take(6)
            }
            is Result.Error -> {
                Log.e(TAG, "Tone Rewrite Error: ${result.message}")
                showToast("Aido: Error - ${result.message}", force = true)
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun showReplyPopup(suggestions: List<String>, textToReplace: String, forceGrid: Boolean = false) {
        serviceScope.launch(Dispatchers.Main) {
            // Remove existing popup if any
            removePopup()

            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.BOTTOM

            val layout = LinearLayout(this@AidoAccessibilityService).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF1E1E1E.toInt()) // Dark background
                setPadding(32, 24, 32, 32)
            }

            // Header Row (Title + @all button + Close Icon)
            val headerLayout = LinearLayout(this@AidoAccessibilityService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, 16)
            }

            // Title
            val titleView = TextView(this@AidoAccessibilityService).apply {
                text = "Aido Suggestions"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                // Make it clickable to launch the app
                setOnClickListener {
                    val launchIntent = Intent(this@AidoAccessibilityService, com.rr.aido.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(launchIntent)
                }
            }
            headerLayout.addView(titleView)

            // @all Button (Only show if not already in @all mode)
            if (textToReplace != "@all") {
                val allButton = TextView(this@AidoAccessibilityService).apply {
                    text = "@all"
                    setTextColor(0xFF000000.toInt()) // Black text
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(24, 8, 24, 8)

                    // White rounded background
                    val drawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 16f * resources.displayMetrics.density
                        setColor(0xFFFFFFFF.toInt()) // White background
                    }
                    background = drawable

                    setOnClickListener {
                        // Switch to @all view
                        serviceScope.launch {
                            val preprompts = dataStoreManager.prepromptsFlow.first()
                            val builtInTriggers = listOf("@reply", "@tone")
                            val customTriggers = preprompts.map { it.trigger }
                            val allTriggers = builtInTriggers + customTriggers

                            // Fix: If coming from @tone, textToReplace is the entire text.
                            // We want to preserve the original text and only replace the "@tone" suffix.
                            val nextTextToReplace = if (textToReplace.trim().endsWith("@tone")) {
                                "@tone"
                            } else {
                                textToReplace
                            }

                            showReplyPopup(allTriggers, nextTextToReplace, forceGrid = true)
                        }
                    }
                }

                val allButtonParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 16, 0)
                }
                headerLayout.addView(allButton, allButtonParams)
            }

            // Close Icon (X)
            val closeIcon = TextView(this@AidoAccessibilityService).apply {
                text = "✕" // Unicode multiplication X
                setTextColor(0xFFFFFFFF.toInt()) // White text
                textSize = 20f
                gravity = android.view.Gravity.CENTER

                // Create circular background
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFFFF5555.toInt()) // Red background
                }
                background = drawable

                setOnClickListener {
                    removePopup()
                }
            }

            // Add close icon with proper layout params for vertical centering
            val closeIconParams = LinearLayout.LayoutParams(
                (40 * resources.displayMetrics.density).toInt(),
                (40 * resources.displayMetrics.density).toInt()
            ).apply {
                setMargins(0, 0, 0, 0) // Margin handled by @all button or title
                gravity = Gravity.CENTER_VERTICAL
            }
            headerLayout.addView(closeIcon, closeIconParams)

            layout.addView(headerLayout)

            // Decide layout based on trigger type or forceGrid
            val isGridLayout = textToReplace == "@all" || forceGrid

            // ScrollView for suggestions
            val scrollView = ScrollView(this@AidoAccessibilityService).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Set height to ~35% of screen height
                    val displayMetrics = resources.displayMetrics
                    val screenHeight = displayMetrics.heightPixels
                    height = (screenHeight * 0.35).toInt()
                }
            }

            // Container for buttons inside ScrollView
            if (isGridLayout) {
                // Grid layout for @all trigger
                val gridContainer = GridLayout(this@AidoAccessibilityService).apply {
                    columnCount = 3
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                suggestions.forEach { suggestion ->
                    val button = Button(this@AidoAccessibilityService).apply {
                        text = suggestion
                        setOnClickListener {
                            insertReply(suggestion, textToReplace)
                            removePopup()
                        }
                        val params = GridLayout.LayoutParams().apply {
                            width = 0
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            setMargins(8, 8, 8, 8)
                        }
                        layoutParams = params
                    }
                    gridContainer.addView(button)
                }

                scrollView.addView(gridContainer)
            } else {
                // Vertical layout for @reply and @tone
                val buttonsContainer = LinearLayout(this@AidoAccessibilityService).apply {
                    orientation = LinearLayout.VERTICAL
                }

                suggestions.forEach { suggestion ->
                    val button = Button(this@AidoAccessibilityService).apply {
                        text = suggestion
                        setOnClickListener {
                            insertReply(suggestion, textToReplace)
                            removePopup()
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 8)
                        }
                    }
                    buttonsContainer.addView(button)
                }

                scrollView.addView(buttonsContainer)
            }
            layout.addView(scrollView)

            try {
                windowManager.addView(layout, params)
                currentPopupView = layout
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show popup", e)
                showToast("Aido: Failed to show popup. Check permissions.")
            }
        }
    }

    private fun removePopup() {
        if (currentPopupView != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            try {
                windowManager.removeView(currentPopupView)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove popup", e)
            }
            currentPopupView = null
        }
    }

    private fun insertReply(reply: String, textToReplace: String) {
        val node = currentEditableNode ?: return
        val currentText = node.text?.toString() ?: ""

        // Replace the target text with the selected reply
        val newText = if (currentText.contains(textToReplace)) {
            currentText.replace(textToReplace, reply)
        } else {
            // Fallback: if exact match fails (e.g. spacing), try to replace trigger at end
            if (currentText.endsWith("@reply")) {
                currentText.replace("@reply", reply)
            } else if (currentText.endsWith("@tone")) {
                // This is harder, we need to replace the sentence before it too.
                // For now, just append if we can't find the match? No, that's bad.
                // Let's try to replace the whole node text if it matches roughly?
                reply
            } else {
                reply
            }
        }

        replaceTextInNode(newText)

        // Auto-trigger: After replacing text, check if the new text contains a trigger
        // This now works because currentEditableNode is preserved
        serviceScope.launch {
            // Small delay to ensure text is updated
            kotlinx.coroutines.delay(100)
            checkAndProcessText(newText)
        }
    }

    private fun processTextWithTrigger(text: String) {
        isProcessing = true
        lastProcessedText = text

        serviceScope.launch {
            try {
                Log.d(TAG, "Starting to process text: $text")

                // Load settings and preprompts
                val settings = dataStoreManager.settingsFlow.first()
                val preprompts = dataStoreManager.prepromptsFlow.first()

                Log.d(TAG, "Settings loaded - Provider: ${settings.provider}, API Key: ${if (settings.apiKey.isEmpty()) "NOT SET" else "SET"}")
                Log.d(TAG, "Service enabled: ${settings.isServiceEnabled}")
                Log.d(TAG, "Trigger method: ${settings.triggerMethod}")
                Log.d(TAG, "Preprompts loaded: ${preprompts.size}")

                // Check if service is enabled
                if (!settings.isServiceEnabled) {
                    Log.d(TAG, "Service is disabled by user")
                    isProcessing = false
                    return@launch
                }

                // Check if accessibility method is selected
                if (settings.triggerMethod != com.rr.aido.data.models.TriggerMethod.ACCESSIBILITY) {
                    Log.d(TAG, "Accessibility method not selected, skipping trigger")
                    isProcessing = false
                    return@launch
                }

                val provider = settings.provider
                val apiKeyToUse = when (provider) {

                    AiProvider.GEMINI -> settings.apiKey
                    AiProvider.CUSTOM -> settings.customApiKey
                }

                if (provider == AiProvider.GEMINI && apiKeyToUse.isEmpty()) {
                    Log.d(TAG, "Gemini provider selected but API key not set")
                    showToast("Aido: Please set your Gemini API key", force = true)
                    isProcessing = false
                    return@launch
                }

                if (provider == AiProvider.CUSTOM && apiKeyToUse.isEmpty()) {
                    Log.d(TAG, "Custom provider selected but API key not set")
                    showToast("Aido: Please set your custom API key", force = true)
                    isProcessing = false
                    return@launch
                }

                // Check if offline mode
                if (settings.isOfflineMode) {
                    Log.d(TAG, "Offline mode enabled")
                    showToast("Aido: Offline mode is enabled", force = true)
                    isProcessing = false
                    return@launch
                }

                // Parse input
                val parseResult = PromptParser.parseInput(text, preprompts)

                Log.d(TAG, "Parse result - Trigger: ${parseResult.trigger}, Matched: ${parseResult.matchedPreprompt != null}")

                if (parseResult.matchedPreprompt == null) {
                    Log.d(TAG, "No matching preprompt found for trigger: ${parseResult.trigger}")
                    isProcessing = false
                    return@launch
                }

                Log.d(TAG, "Processing with preprompt: ${parseResult.matchedPreprompt.trigger}")
                Log.d(TAG, "Final prompt: ${parseResult.finalPrompt}")

                // Show processing animation
                showProcessingAnimation()

                // Get model based on provider
                val modelToUse = if (provider == AiProvider.CUSTOM) settings.customModelName else settings.selectedModel

                // Send to API
                val result = geminiRepository.sendPrompt(
                    provider = provider,
                    apiKey = apiKeyToUse,
                    model = modelToUse,
                    prompt = parseResult.finalPrompt,
                    customApiUrl = settings.customApiUrl
                )

                // Hide animation
                hideProcessingAnimation()

                when (result) {
                    is Result.Success -> {
                        Log.d(TAG, "Got response from Gemini: ${result.data}")

                        // Replace text in the input field
                        val replaced = replaceTextInNode(result.data)

                        if (!replaced) {
                            // Fallback: Copy to clipboard
                            copyToClipboard(result.data)
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error: ${result.message}")
                        showToast("Aido: ❌ ${result.message}", force = true)
                    }
                    else -> {
                        // Handle Loading or other states if necessary
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception in processTextWithTrigger", e)
                showToast("Aido: Error - ${e.message}", force = true)
            } finally {
                isProcessing = false
            }
        }
    }


    private fun replaceTextInNode(newText: String): Boolean {
        val node = currentEditableNode
        if (node == null) {
            Log.d(TAG, "No editable node available for replacement")
            return false
        }

        // Capture original text for Undo/Redo
        val originalText = node.text?.toString() ?: ""
        lastOriginalText = originalText
        lastGeneratedText = newText
        undoRedoNode = node // Keep reference to node (might need refreshing though)

// Get settings to check if streaming mode is enabled
        serviceScope.launch {
            val settings = dataStoreManager.settingsFlow.first()

            if (settings.isStreamingModeEnabled) {
                // Streaming mode: Display word-by-word with animation
                streamTextToNode(node, newText, settings.streamingDelayMs, settings)
            } else {
                // Instant mode: Replace all at once
                replaceTextInstantly(node, newText, settings)
            }
        }

        return true
    }


    private suspend fun streamTextToNode(
        node: AccessibilityNodeInfo,
        fullText: String,
        delayMs: Int,
        settings: com.rr.aido.data.models.Settings
    ) {
        withContext(Dispatchers.Main) {
            try {
                // Split text into words
                val words = fullText.split(" ")
                val textBuilder = StringBuilder()

                // Display each word progressively
                for ((index, word) in words.withIndex()) {
                    // Add the current word
                    if (index > 0) {
                        textBuilder.append(" ")
                    }
                    textBuilder.append(word)

                    // Update the node with accumulated text
                    val arguments = Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        textBuilder.toString()
                    )
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                    // Delay before next word (skip delay on last word)
                    if (index < words.size - 1) {
                        delay(delayMs.toLong())
                    }
                }

                Log.d(TAG, "Text streamed successfully with ${words.size} words")

                // Show Undo/Redo popup after streaming is complete
                if (settings.isUndoRedoEnabled) {
                    undoRedoManager.showPopup(fullText, lastOriginalText ?: "", undoRedoNode ?: node)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to stream text, falling back to instant replace", e)
                // Fallback to instant replacement
                replaceTextInstantly(node, fullText, settings)
            }
        }
    }


    private suspend fun replaceTextInstantly(
        node: AccessibilityNodeInfo,
        newText: String,
        settings: com.rr.aido.data.models.Settings
    ) {
        withContext(Dispatchers.Main) {
            try {
                // Method 1: Using ACTION_SET_TEXT
                val arguments = Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
                val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                if (success) {
                    Log.d(TAG, "Text replaced successfully using ACTION_SET_TEXT")

                    // Check if Undo/Redo is enabled and show popup
                    if (settings.isUndoRedoEnabled) {
                        undoRedoManager.showPopup(newText, lastOriginalText ?: "", undoRedoNode ?: node)
                    }

                    return@withContext
                }

                // Method 2: Focus and paste (fallback)
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

                // Copy new text to clipboard
                copyToClipboard(newText)

                // Clear existing text by setting empty
                val clearArgs = Bundle()
                clearArgs.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    ""
                )
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clearArgs)

                // Paste new text
                node.performAction(AccessibilityNodeInfo.ACTION_PASTE)

                Log.d(TAG, "Text replaced using paste method")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to replace text", e)
            }
        }
        // Note: NOT clearing currentEditableNode here to allow subsequent operations
        // It will be cleared/updated on the next text change event
    }


    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Aido Response", text)
        clipboard.setPrimaryClip(clip)
    }


    private suspend fun showProcessingAnimation() {
        val settings = dataStoreManager.settingsFlow.first()

        // Check if animation is enabled
        if (!settings.isProcessingAnimationEnabled) {
            return
        }

        // Check if overlay permission is granted
        if (!android.provider.Settings.canDrawOverlays(this@AidoAccessibilityService)) {
            return
        }

        serviceScope.launch(Dispatchers.Main) {
            // Remove existing animation if any
            hideProcessingAnimation()

            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER

            val animationView = ProcessingAnimationView(this@AidoAccessibilityService).apply {
                setAnimationType(settings.processingAnimationType)
                // Use transparent background for peaceful animations
                val backgroundColor = when (settings.processingAnimationType) {
                    ProcessingAnimationType.GENTLE_GLOW,
                    ProcessingAnimationType.BREATHING_CIRCLE,
                    ProcessingAnimationType.SHIMMER,
                    ProcessingAnimationType.PARTICLE_FLOW -> 0x00000000.toInt() // Fully transparent
                    else -> 0x80000000.toInt() // Semi-transparent black for other animations
                }
                setBackgroundColor(backgroundColor)
            }

            try {
                windowManager.addView(animationView, params)
                currentAnimationView = animationView
                animationView.startAnimation()
                Log.d(TAG, "Processing animation started: ${settings.processingAnimationType}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show animation overlay", e)
            }
        }
    }


    private fun hideProcessingAnimation() {
        if (currentAnimationView != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            try {
                (currentAnimationView as? ProcessingAnimationView)?.stopAnimation()
                windowManager.removeView(currentAnimationView)
                Log.d(TAG, "Processing animation stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove animation overlay", e)
            }
            currentAnimationView = null
        }
    }


    private fun showToast(message: String, force: Boolean = false) {
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!force && !isDebuggable) {
            return
        }
        serviceScope.launch(Dispatchers.Main) {
            android.widget.Toast.makeText(
                applicationContext,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        textSelectionProcessor.cleanup()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}

