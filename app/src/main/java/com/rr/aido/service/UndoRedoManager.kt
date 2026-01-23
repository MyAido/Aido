package com.rr.aido.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.util.Log
import com.rr.aido.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class UndoRedoManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val dataStoreManager: DataStoreManager,
    private val serviceScope: CoroutineScope
) {
    private val TAG = "UndoRedoManager"
    
    // State
    private var popupView: View? = null
    private var lastOriginalText: String? = null
    private var lastGeneratedText: String? = null
    private var targetNode: AccessibilityNodeInfo? = null
    private var autoDismissJob: Job? = null
    
    // Position
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var currentX = 0
    private var currentY = 0
    
    fun showPopup(newText: String, originalText: String, node: AccessibilityNodeInfo) {
        lastGeneratedText = newText
        lastOriginalText = originalText
        targetNode = node
        
        serviceScope.launch(Dispatchers.Main) {
            // Remove existing popup
            removePopup(false)
            
            // Load saved position
            val settings = dataStoreManager.settingsFlow.first()
            currentX = settings.undoRedoPopupX
            currentY = settings.undoRedoPopupY
            
            // If position is 0,0 (default), set a reasonable default (Top Center)
            if (currentX == 0 && currentY == 0) {
                currentY = 150
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = currentX
            params.y = currentY
            
            val layout = createPopupLayout()
            
            // Add drag listener
            layout.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        // Cancel auto-dismiss when touched
                        autoDismissJob?.cancel()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(view, params)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        // Save new position
                        currentX = params.x
                        currentY = params.y
                        serviceScope.launch {
                            dataStoreManager.saveUndoRedoPosition(currentX, currentY)
                        }
                        // Restart auto-dismiss timer
                        startAutoDismissTimer()
                        true
                    }
                    else -> false
                }
            }
            
            try {
                windowManager.addView(layout, params)
                popupView = layout
                startAutoDismissTimer()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show popup", e)
            }
        }
    }
    
    private fun createPopupLayout(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            // Darker, more opaque background for better visibility
            // Pill shape
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 100f // Fully rounded pill
                setColor(0xEE252525.toInt()) // Dark grey
                setStroke(2, 0xFF444444.toInt()) // Subtle border
            }
            background = drawable
            setPadding(32, 16, 32, 16)
            elevation = 10f
        }
        
        // Undo Button
        val undoButton = TextView(context).apply {
            text = "↶ Undo"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                performUndo()
                // Do NOT dismiss, just restart timer
                startAutoDismissTimer()
            }
        }
        
        // Divider
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(2, 40).apply {
                gravity = Gravity.CENTER_VERTICAL
                setMargins(8, 0, 8, 0)
            }
            setBackgroundColor(0xFF555555.toInt())
        }
        
        // Redo Button
        val redoButton = TextView(context).apply {
            text = "↷ Redo"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            setOnClickListener {
                performRedo()
                // Do NOT dismiss, just restart timer
                startAutoDismissTimer()
            }
        }
        
        // Close Button (small x)
        val closeButton = TextView(context).apply {
            text = "✕"
            textSize = 14f
            setTextColor(0xFFAAAAAA.toInt())
            gravity = Gravity.CENTER
            setPadding(24, 8, 8, 8)
            setOnClickListener {
                removePopup(true)
            }
        }
        
        layout.addView(undoButton)
        layout.addView(divider)
        layout.addView(redoButton)
        layout.addView(closeButton)
        
        return layout
    }
    
    private fun startAutoDismissTimer() {
        autoDismissJob?.cancel()
        autoDismissJob = serviceScope.launch {
            delay(10000) // 10 seconds
            removePopup(true)
        }
    }
    
    fun removePopup(force: Boolean) {
        autoDismissJob?.cancel()
        if (popupView != null) {
            try {
                windowManager.removeView(popupView)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove popup", e)
            }
            popupView = null
        }
    }
    
    private fun performUndo() {
        val textToRestore = lastOriginalText ?: return
        val node = targetNode ?: return
        
        // Try to refresh node if needed
        if (!node.refresh()) {
             // Node might be stale, but we can try anyway if we have a reference
        }
        
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            textToRestore
        )
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
    
    private fun performRedo() {
        val textToRestore = lastGeneratedText ?: return
        val node = targetNode ?: return
        
        if (!node.refresh()) {
             // Node might be stale
        }
        
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            textToRestore
        )
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
}
