package com.rr.aido.keyboard_service

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.rr.aido.R

/**
 * Handles clipboard panel display with history
 * Updated to use PopupWindow instead of replacing input view
 */
class ClipboardPanelHandler(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val clipboardManager: KeyboardClipboardManager,
    private val onPasteClick: (String) -> Unit
) {
    
    companion object {
        private const val TAG = "ClipboardPanelHandler"
    }
    
    private var popupWindow: PopupWindow? = null
    
    fun createClipboardPanel(onBackClick: () -> Unit): View {
        try {
            val clipboardView = layoutInflater.inflate(R.layout.clipboard_panel, null)
            
            // Refresh clipboard from system
            clipboardManager.refreshFromSystemClipboard()
            
            Log.d(TAG, "Creating clipboard panel, history size: ${clipboardManager.getHistory().size}")
            
            // Setup back button
            clipboardView.findViewById<android.widget.ImageButton>(R.id.btn_back_from_clipboard)?.setOnClickListener {
                onBackClick()
            }
            
            // Setup clear button
            clipboardView.findViewById<android.widget.Button>(R.id.btn_clear_clipboard)?.setOnClickListener {
                clipboardManager.clearHistory()
                updateClipboardList(clipboardView)
            }
            
            // Setup refresh button
            clipboardView.findViewById<android.widget.ImageButton>(R.id.btn_refresh_clipboard)?.setOnClickListener {
                Log.d(TAG, "Refresh button clicked")
                clipboardManager.refreshFromSystemClipboard()
                updateClipboardList(clipboardView)
                val count = clipboardManager.getHistory().size
                android.widget.Toast.makeText(context, "Refreshed: $count items", android.widget.Toast.LENGTH_SHORT).show()
            }
            
            // Update list
            updateClipboardList(clipboardView)
            
            return clipboardView
        } catch (e: Exception) {
            Log.e(TAG, "Error creating clipboard panel", e)
            throw e
        }
    }
    
    private fun updateClipboardList(clipboardView: View) {
        val container = clipboardView.findViewById<LinearLayout>(R.id.clipboard_list_container)
        container?.removeAllViews()
        
        val history = clipboardManager.getHistory()
        
        Log.d(TAG, "Updating clipboard list, history size: ${history.size}")
        history.forEachIndexed { index, item ->
            Log.d(TAG, "  Item $index: ${item.text.take(30)}")
        }
        
        // Update count
        clipboardView.findViewById<TextView>(R.id.clipboard_count)?.text = "${history.size} items"
        
        if (history.isEmpty()) {
            // Show empty message
            val emptyView = TextView(context).apply {
                text = "No clipboard history\n\nCopy some text and it will appear here"
                textSize = 14f
                setTextColor(context.getColor(android.R.color.darker_gray))
                setPadding(32, 64, 32, 32)
                gravity = android.view.Gravity.CENTER
            }
            container?.addView(emptyView)
        } else {
            // Show clipboard items
            history.forEachIndexed { index, item ->
                val itemView = createClipboardItemView(item, index, clipboardView)
                container?.addView(itemView)
            }
        }
    }
    
    private fun createClipboardItemView(item: KeyboardClipboardManager.ClipboardItem, index: Int, rootView: View): View {
        val itemView = layoutInflater.inflate(R.layout.clipboard_item, null)
        
        val textView = itemView.findViewById<TextView>(R.id.clipboard_text)
        val deleteButton = itemView.findViewById<android.widget.ImageButton>(R.id.btn_delete_clip)
        
        // Set text (truncate if too long)
        val displayText = if (item.text.length > 100) {
            item.text.substring(0, 100) + "..."
        } else {
            item.text
        }
        textView?.text = displayText
        
        // Click to paste
        itemView.setOnClickListener {
            onPasteClick(item.text)
        }
        
        // Delete button
        deleteButton?.setOnClickListener {
            clipboardManager.deleteFromHistory(index)
            updateClipboardList(rootView)
        }
        
        return itemView
    }
    
    /**
     * Show clipboard as a bottom sheet popup window overlay
     * This doesn't replace the keyboard input view
     */
    fun showClipboardBottomSheet(anchorView: View) {
        try {
            val clipboardView = createClipboardPanel {
                // Dismiss popup
                popupWindow?.dismiss()
            }
            
            // Calculate height (60% of screen)
            val displayMetrics = context.resources.displayMetrics
            val popupHeight = (displayMetrics.heightPixels * 0.6f).toInt()
            
            // Create popup window
            popupWindow = PopupWindow(
                clipboardView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                popupHeight,
                true
            ).apply {
                isFocusable = true
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                elevation = 16f
                animationStyle = android.R.style.Animation_Dialog
            }
            
            // Show at bottom of screen
            popupWindow?.showAtLocation(anchorView, android.view.Gravity.BOTTOM, 0, 0)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing clipboard bottom sheet", e)
        }
    }
}
