package com.rr.aido.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.rr.aido.R
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.AiProvider
import com.rr.aido.data.models.SelectionMenuStyle
import com.rr.aido.data.repository.GeminiRepositoryImpl
import com.rr.aido.data.repository.Result
import com.rr.aido.utils.PromptParser
import android.view.MotionEvent
import android.view.View.OnTouchListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * TextSelectionProcessor
 * Handles text selection events and shows context menu with available triggers
 * Applies AI transformations only to selected text, preserving surrounding content
 */
class TextSelectionProcessor(
    private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val geminiRepository: GeminiRepositoryImpl,
    private val scope: CoroutineScope,
    private val windowManager: WindowManager,
    private val onShowAnimation: () -> Unit,
    private val onHideAnimation: () -> Unit,
    private val onShowToast: (String, Boolean) -> Unit
) {
    
    private val TAG = "TextSelectionProcessor"
    
    private var currentMenuView: View? = null
    private var currentFloatingButton: View? = null
    private var currentNode: AccessibilityNodeInfo? = null
    private var selectedText: String = ""
    private var selectionStart: Int = 0
    private var selectionEnd: Int = 0
    private var fullText: String = ""
    
    /**
     * Handle text selection changed event
     */
    fun onTextSelectionChanged(event: AccessibilityEvent) {
        Log.d(TAG, "Text selection changed event received")
        
        // Get the source node and indices synchronously
        val source = event.source
        val itemCount = event.itemCount
        val fromIndex = event.fromIndex
        val toIndex = event.toIndex
        
        if (source == null) {
            Log.d(TAG, "Source node is null")
            removeMenu()
            return
        }
        scope.launch {
            val settings = dataStoreManager.settingsFlow.first()
            
            // Check if feature is enabled
            if (!settings.isTextSelectionMenuEnabled) {
                Log.d(TAG, "Text selection menu is disabled")
                return@launch
            }
            
            // Check overlay permission
            if (!android.provider.Settings.canDrawOverlays(context)) {
                Log.d(TAG, "Overlay permission not granted")
                return@launch
            }
            

            Log.d(TAG, "Selection: from=$fromIndex, to=$toIndex, itemCount=$itemCount")
            
            // Extract selected text
            fullText = source.text?.toString() ?: ""
            
            if (fromIndex >= 0 && toIndex >= 0 && fromIndex < fullText.length && toIndex <= fullText.length && fromIndex < toIndex) {
                selectedText = fullText.substring(fromIndex, toIndex)
                selectionStart = fromIndex
                selectionEnd = toIndex
                currentNode = source
                
                Log.d(TAG, "Selected text: '$selectedText'")
                
                if (selectedText.isNotEmpty()) {
                    // Show floating button first
                    showFloatingButton()
                } else {
                    removeFloatingButton()
                    removeMenu()
                }
            } else {
                // No valid selection, hide menu
                Log.d(TAG, "No valid selection")
                removeFloatingButton()
                removeMenu()
                // We don't recycle source here as we didn't obtain it inside the coroutine, 
                // but it's good practice to let the system handle it or recycle if we obtained a copy.
                // However, event.source returns a copy, so we should recycle it when done.
                // source.recycle() // Deprecated
            }
        }
    }
    
    /**
     * Show floating action button
     */
    private fun showFloatingButton() {
        scope.launch(Dispatchers.Main) {
            // Remove existing button
            removeFloatingButton()
            
            val settings = dataStoreManager.settingsFlow.first()
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            
            // Set position based on saved settings or default
            if (settings.floatingButtonX != -1 && settings.floatingButtonY != -1) {
                params.gravity = Gravity.TOP or Gravity.START
                params.x = settings.floatingButtonX
                params.y = settings.floatingButtonY
            } else {
                params.gravity = Gravity.BOTTOM or Gravity.END
                params.x = 50
                params.y = 200
            }
            
            // Dimensions in DP
            val density = context.resources.displayMetrics.density
            val closeButtonSize = (24 * density).toInt()
            val overlapOffset = (10 * density).toInt() // Hang half-way (approx)
            
            // Root Container (FrameLayout) to hold Pill + Close Button
            val rootContainer = android.widget.FrameLayout(context).apply {
                // Add padding to allow close button to "hang" on the corner
                // Top and Right padding = overlap offset
                setPadding(0, overlapOffset, overlapOffset, 0)
                clipChildren = false // Allow drawing outside bounds if needed
                clipToPadding = false
            }

            // The "Aido" Pill Container
            val pillContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding((24 * density).toInt(), (12 * density).toInt(), (24 * density).toInt(), (12 * density).toInt())
                
                // Glassmorphism Background
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 100f
                    orientation = android.graphics.drawable.GradientDrawable.Orientation.BL_TR
                    // Dark Grey to match Undo/Redo
                    setColor(0xEE252525.toInt()) 
                    setStroke(2, 0xFF444444.toInt())
                }
                background = drawable
                elevation = 0f
                
                // Layout params for pill
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                }
            }

            // Aido Label
            val label = TextView(context).apply {
                text = "Aido"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
            }
            pillContainer.addView(label)
            
            rootContainer.addView(pillContainer)

            // Close Button (Red Circle)
            val closeBtn = TextView(context).apply {
                text = "✕"
                textSize = 10f // Slightly smaller X
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                includeFontPadding = false
                
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFFFF5555.toInt()) // Red color
                    setStroke((1.5 * density).toInt(), 0xFFFFFFFF.toInt()) // White border
                }
                background = drawable
                elevation = 6f
                
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    closeButtonSize,
                    closeButtonSize
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                }
                
                setOnClickListener {
                    removeFloatingButton()
                    removeMenu()
                }
            }
            rootContainer.addView(closeBtn)
            
            // Drag handling on the root container
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false
            val touchSlop = 10
            
            rootContainer.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                            isDragging = true
                        }
                        params.gravity = Gravity.TOP or Gravity.START
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(v, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating view layout", e)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Check if touch is on close button
                            val rect = android.graphics.Rect()
                            closeBtn.getHitRect(rect)
                            
                            if (rect.contains(event.x.toInt(), event.y.toInt())) {
                                removeFloatingButton()
                                removeMenu()
                            } else {
                                scope.launch {
                                    val currentSettings = dataStoreManager.settingsFlow.first()
                                    showTriggerContextMenu(currentSettings.textSelectionMenuStyle)
                                }
                            }
                        } else {
                            scope.launch {
                                dataStoreManager.saveFloatingButtonPosition(params.x, params.y)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            
            try {
                windowManager.addView(rootContainer, params)
                currentFloatingButton = rootContainer
                Log.d(TAG, "Floating button shown at x=${params.x}, y=${params.y}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show floating button", e)
            }
        }
    }
    
    /**
     * Show context menu with available triggers
     */
    private fun showTriggerContextMenu(menuStyle: SelectionMenuStyle) {
        scope.launch(Dispatchers.Main) {
            // Remove existing menu and button
            removeFloatingButton()
            removeMenu()
            
            // Get preprompts and built-in triggers
            val preprompts = dataStoreManager.prepromptsFlow.first()
            val settings = dataStoreManager.settingsFlow.first()
            
            // Build list of available triggers
            val triggers = mutableListOf<Pair<String, String>>() // Pair of (trigger, displayName)
            
            // Add built-in special triggers if enabled
            if (settings.isSmartReplyEnabled) {
                triggers.add(Pair(settings.smartReplyTrigger, "Smart Reply"))
            }
            if (settings.isToneRewriteEnabled) {
                triggers.add(Pair(settings.toneRewriteTrigger, "Tone Rewrite"))
            }
            
            // Add custom preprompts
            preprompts.forEach { preprompt ->
                triggers.add(Pair(preprompt.trigger, preprompt.trigger))
            }
            
            if (triggers.isEmpty()) {
                onShowToast("Aido: No triggers available. Please configure triggers in settings.", true)
                return@launch
            }
            
            // Create layout params
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER
            
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF1E1E1E.toInt())
                setPadding(24, 16, 24, 16)
                elevation = 8f
            }
            
            // Header
            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, 12)
            }
            
            val titleView = TextView(context).apply {
                text = "Apply Trigger"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerLayout.addView(titleView)
            
            // Close button
            val closeButton = TextView(context).apply {
                text = "✕"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 18f
                gravity = Gravity.CENTER
                
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(0xFFFF5555.toInt())
                }
                background = drawable
                
                setOnClickListener {
                    removeMenu()
                }
            }
            
            val closeParams = LinearLayout.LayoutParams(
                (32 * context.resources.displayMetrics.density).toInt(),
                (32 * context.resources.displayMetrics.density).toInt()
            )
            headerLayout.addView(closeButton, closeParams)
            
            layout.addView(headerLayout)
            
            // Selected text preview
            val previewView = TextView(context).apply {
                text = "\"${selectedText.take(50)}${if (selectedText.length > 50) "..." else ""}\""
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 12f
                setPadding(0, 0, 0, 12)
                maxLines = 2
            }
            layout.addView(previewView)
            
            // ScrollView for triggers
            val scrollView = ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Max height 40% of screen
                    val screenHeight = context.resources.displayMetrics.heightPixels
                    height = (screenHeight * 0.4).toInt()
                }
            }
            
            // Create trigger buttons based on menu style
            when (menuStyle) {
                SelectionMenuStyle.GRID -> {
                    val gridContainer = GridLayout(context).apply {
                        columnCount = 2
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    
                    triggers.forEach { (trigger, displayName) ->
                        val button = Button(context).apply {
                            text = displayName
                            textSize = 12f
                            setOnClickListener {
                                applyTriggerToSelection(trigger)
                                removeMenu()
                            }
                            
                            val btnParams = GridLayout.LayoutParams().apply {
                                width = (140 * context.resources.displayMetrics.density).toInt()
                                height = GridLayout.LayoutParams.WRAP_CONTENT
                                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                                setMargins(4, 4, 4, 4)
                            }
                            layoutParams = btnParams
                        }
                        gridContainer.addView(button)
                    }
                    
                    scrollView.addView(gridContainer)
                }
                
                SelectionMenuStyle.LIST -> {
                    val listContainer = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    
                    triggers.forEach { (trigger, displayName) ->
                        val button = Button(context).apply {
                            text = displayName
                            textSize = 12f
                            setOnClickListener {
                                applyTriggerToSelection(trigger)
                                removeMenu()
                            }
                            
                            layoutParams = LinearLayout.LayoutParams(
                                (200 * context.resources.displayMetrics.density).toInt(),
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, 8)
                            }
                        }
                        listContainer.addView(button)
                    }
                    
                    scrollView.addView(listContainer)
                }
            }
            
            layout.addView(scrollView)
            
            try {
                windowManager.addView(layout, params)
                currentMenuView = layout
                Log.d(TAG, "Context menu shown with ${triggers.size} triggers")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show context menu", e)
                onShowToast("Aido: Failed to show menu. Check permissions.", true)
            }
        }
    }
    
    /**
     * Apply selected trigger to the selected text only
     */
    private fun applyTriggerToSelection(trigger: String) {
        scope.launch {
            try {
                Log.d(TAG, "Applying trigger '$trigger' to selected text: '$selectedText'")
                
                val settings = dataStoreManager.settingsFlow.first()
                val preprompts = dataStoreManager.prepromptsFlow.first()
                
                // Show animation
                onShowAnimation()
                
                // Check if it's a special trigger
                val processedText = when (trigger) {
                    settings.smartReplyTrigger -> {
                        // For smart reply on selection, treat selected text as context
                        onShowToast("Aido: Smart Reply works best on full conversations, not selections.", true)
                        onHideAnimation()
                        return@launch
                    }
                    
                    settings.toneRewriteTrigger -> {
                        // Apply tone rewrite to selected text
                        getToneRewrite(selectedText, settings)
                    }
                    
                    else -> {
                        // Find matching preprompt
                        val preprompt = preprompts.find { it.trigger == trigger }
                        if (preprompt != null) {
                            // Apply preprompt to selected text
                            processWithPreprompt(selectedText, preprompt.instruction, settings)
                        } else {
                            Log.e(TAG, "No matching preprompt found for trigger: $trigger")
                            onShowToast("Aido: Trigger not found.", true)
                            onHideAnimation()
                            return@launch
                        }
                    }
                }
                
                // Hide animation
                onHideAnimation()
                
                if (processedText != null) {
                    // Replace only the selected portion
                    replaceSelectedText(processedText)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error applying trigger to selection", e)
                onShowToast("Aido: Error - ${e.message}", true)
                onHideAnimation()
            }
        }
    }
    
    /**
     * Get tone rewrite for selected text
     */
    private suspend fun getToneRewrite(text: String, settings: com.rr.aido.data.models.Settings): String? {
        val provider = settings.provider
        val apiKey = when (provider) {

            AiProvider.GEMINI -> settings.apiKey
            AiProvider.CUSTOM -> settings.customApiKey
        }
        
        if (provider == AiProvider.GEMINI && apiKey.isEmpty()) {
            onShowToast("Aido: Please set your Gemini API key", true)
            return null
        }
        
        if (provider == AiProvider.CUSTOM && apiKey.isEmpty()) {
            onShowToast("Aido: Please set your custom API key", true)
            return null
        }
        
        val instructions = settings.toneRewritePrompt.ifEmpty { PromptParser.DEFAULT_TONE_REWRITE_INSTRUCTIONS }
        val prompt = """
            Original text: "$text"
            
            $instructions
            
            Return ONLY the rewritten text, nothing else.
        """.trimIndent()
        
        val model = if (provider == AiProvider.CUSTOM) settings.customModelName else settings.selectedModel
        
        val result = geminiRepository.sendPrompt(
            provider = provider,
            apiKey = apiKey,
            model = model,
            prompt = prompt,
            customApiUrl = settings.customApiUrl
        )
        
        return when (result) {
            is Result.Success -> result.data.trim()
            is Result.Error -> {
                onShowToast("Aido: Error - ${result.message}", true)
                null
            }
            else -> null
        }
    }
    
    /**
     * Process selected text with preprompt
     */
    private suspend fun processWithPreprompt(text: String, prepromptTemplate: String, settings: com.rr.aido.data.models.Settings): String? {
        val provider = settings.provider
        val apiKey = when (provider) {

            AiProvider.GEMINI -> settings.apiKey
            AiProvider.CUSTOM -> settings.customApiKey
        }
        
        if (provider == AiProvider.GEMINI && apiKey.isEmpty()) {
            onShowToast("Aido: Please set your Gemini API key", true)
            return null
        }
        
        if (provider == AiProvider.CUSTOM && apiKey.isEmpty()) {
            onShowToast("Aido: Please set your custom API key", true)
            return null
        }
        
        // Build final prompt using PromptParser logic
        // Concatenate instruction and text, as Preprompt instruction doesn't contain placeholders
        val finalPrompt = "$prepromptTemplate\n\n$text"
        
        val model = if (provider == AiProvider.CUSTOM) settings.customModelName else settings.selectedModel
        
        val result = geminiRepository.sendPrompt(
            provider = provider,
            apiKey = apiKey,
            model = model,
            prompt = finalPrompt,
            customApiUrl = settings.customApiUrl
        )
        
        return when (result) {
            is Result.Success -> result.data.trim()
            is Result.Error -> {
                onShowToast("Aido: Error - ${result.message}", true)
                null
            }
            else -> null
        }
    }
    
    /**
     * Replace only the selected text in the node
     */
    private fun replaceSelectedText(newText: String) {
        val node = currentNode
        if (node == null) {
            Log.e(TAG, "No current node for text replacement")
            onShowToast("Aido: Cannot replace text in this field", true)
            return
        }
        
        try {
            // Build new full text with only selection replaced
            val beforeSelection = fullText.substring(0, selectionStart)
            val afterSelection = fullText.substring(selectionEnd)
            val newFullText = beforeSelection + newText + afterSelection
            
            Log.d(TAG, "Replacing selection with: '$newText'")
            Log.d(TAG, "Full text before: '$fullText'")
            Log.d(TAG, "Full text after: '$newFullText'")
            
            // Set the new text
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newFullText
            )
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            if (success) {
                Log.d(TAG, "Text replaced successfully")
                onShowToast("Aido: ✓ Text updated", false)
            } else {
                Log.e(TAG, "Failed to replace text using ACTION_SET_TEXT")
                onShowToast("Aido: Could not update text in this field", true)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing selected text", e)
            onShowToast("Aido: Error replacing text - ${e.message}", true)
        }
    }
    
    /**
     * Remove floating button
     */
    private fun removeFloatingButton() {
        if (currentFloatingButton != null) {
            try {
                windowManager.removeView(currentFloatingButton)
                Log.d(TAG, "Floating button removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove floating button", e)
            }
            currentFloatingButton = null
        }
    }
    
    /**
     * Remove context menu
     */
    fun removeMenu() {
        if (currentMenuView != null) {
            try {
                windowManager.removeView(currentMenuView)
                Log.d(TAG, "Context menu removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove context menu", e)
            }
            currentMenuView = null
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        removeFloatingButton()
        removeMenu()
        // currentNode?.recycle() // Deprecated
        currentNode = null
    }
}
