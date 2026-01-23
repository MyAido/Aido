package com.rr.aido.keyboard_service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class KeyboardClipboardManager(private val context: Context) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val clipboardHistory = mutableListOf<ClipboardItem>()
    private val _historyFlow = kotlinx.coroutines.flow.MutableStateFlow<List<ClipboardItem>>(emptyList())
    val historyFlow: kotlinx.coroutines.flow.StateFlow<List<ClipboardItem>> = _historyFlow

    // Persistent storage
    private val prefs = context.getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val maxHistorySize = 100 // Increased from 20 to 100
    private var lastClipText: String? = null
    private var lastClipTime: Long = 0
    private val debounceDelayMs = 500L // Ignore duplicates within 500ms

    data class ClipboardItem(
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "ClipboardManager"
    }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        Log.d(TAG, "Clipboard changed detected")
        saveCurrentClipToHistory()
    }

    init {
        // Load saved clipboard history from persistent storage
        loadHistoryFromPreferences()
        Log.d(TAG, "Loaded ${clipboardHistory.size} items from persistent storage")

        // Add current clipboard item if exists and not already in history
        getCurrentClip()?.let { text ->
            if (text.isNotBlank()) {
                addToHistory(text)
            }
        }

        // Listen for clipboard changes
        try {
            clipboardManager.addPrimaryClipChangedListener(clipboardListener)
            Log.d(TAG, "Clipboard listener registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up clipboard listener", e)
        }
    }


    fun getCurrentClip(): String? {
        return try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                item.text?.toString() ?: item.coerceToText(context)?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting clipboard", e)
            null
        }
    }


    fun copyToClipboard(text: String) {
        try {
            val clip = ClipData.newPlainText("Aido Keyboard", text)
            clipboardManager.setPrimaryClip(clip)
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to clipboard", e)
        }
    }


    private fun saveCurrentClipToHistory() {
        val text = getCurrentClip()
        val currentTime = System.currentTimeMillis()

        Log.d(TAG, "saveCurrentClipToHistory called, text: ${text?.take(30)}")

        if (!text.isNullOrBlank() && text.length <= 500) {
            // Debounce: ignore if same text within 500ms
            if (text == lastClipText && (currentTime - lastClipTime) < debounceDelayMs) {
                Log.d(TAG, "Debounced: same clip within ${currentTime - lastClipTime}ms")
                return
            }

            lastClipText = text
            lastClipTime = currentTime
            addToHistory(text)
        }
    }


    fun addToHistory(text: String) {
        if (text.isBlank()) return

        // Check if already exists at position 0 (most recent)
        if (clipboardHistory.isNotEmpty() && clipboardHistory[0].text == text) {
            Log.d(TAG, "Already at top of history, skipping")
            return
        }

        // Remove old occurrence if exists elsewhere in history
        val removed = clipboardHistory.removeAll { it.text == text }
        if (removed) {
            Log.d(TAG, "Removed old occurrence from history")
        }

        // Add to top
        clipboardHistory.add(0, ClipboardItem(text))
        lastClipText = text

        // Keep only max items
        if (clipboardHistory.size > maxHistorySize) {
            clipboardHistory.removeAt(clipboardHistory.size - 1)
        }

        Log.d(TAG, "Added to clipboard history (${clipboardHistory.size} total items): ${text.take(50)}")
        _historyFlow.value = clipboardHistory.toList()

        // Save to persistent storage
        saveHistoryToPreferences()
    }


    fun refreshFromSystemClipboard() {
        val currentText = getCurrentClip()
        Log.d(TAG, "Manual refresh, current clipboard: ${currentText?.take(30)}")
        if (!currentText.isNullOrBlank()) {
            addToHistory(currentText)
        }
    }


    fun getHistory(): List<ClipboardItem> {
        return clipboardHistory.toList()
    }


    fun clearHistory() {
        clipboardHistory.clear()
        lastClipText = null
        _historyFlow.value = emptyList()
        saveHistoryToPreferences() // Persist the clear operation
        Log.d(TAG, "Clipboard history cleared")
    }


    fun deleteFromHistory(index: Int) {
        if (index in clipboardHistory.indices) {
            clipboardHistory.removeAt(index)
            _historyFlow.value = clipboardHistory.toList()
            saveHistoryToPreferences() // Persist the deletion
            Log.d(TAG, "Deleted item at index $index")
        }
    }


    private fun saveHistoryToPreferences() {
        try {
            val json = gson.toJson(clipboardHistory)
            prefs.edit().putString("history", json).apply()
            Log.d(TAG, "Saved ${clipboardHistory.size} items to persistent storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving clipboard history", e)
        }
    }


    private fun loadHistoryFromPreferences() {
        try {
            val json = prefs.getString("history", null)
            if (json != null) {
                val type = object : TypeToken<MutableList<ClipboardItem>>() {}.type
                val loaded = gson.fromJson<MutableList<ClipboardItem>>(json, type)
                if (loaded != null) {
                    clipboardHistory.clear()
                    clipboardHistory.addAll(loaded)
                    _historyFlow.value = clipboardHistory.toList()
                    Log.d(TAG, "Loaded ${clipboardHistory.size} items from persistent storage")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading clipboard history", e)
            // If loading fails, start with empty history
            clipboardHistory.clear()
        }
    }


    fun cleanup() {
        try {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
            Log.d(TAG, "Clipboard listener removed")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing clipboard listener", e)
        }
    }
}
