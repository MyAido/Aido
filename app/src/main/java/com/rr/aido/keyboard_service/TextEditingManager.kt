package com.rr.aido.keyboard_service

import android.content.Context
import android.util.Log
import android.view.inputmethod.InputConnection

class TextEditingManager(private val context: Context) {

    private val undoStack = mutableListOf<TextState>()
    private val redoStack = mutableListOf<TextState>()
    private val maxStackSize = 50

    data class TextState(
        val text: String,
        val cursorPosition: Int
    )

    companion object {
        private const val TAG = "TextEditingManager"
    }


    fun saveState(text: String, cursorPosition: Int) {
        val state = TextState(text, cursorPosition)

        // Don't save duplicate states
        if (undoStack.isNotEmpty() && undoStack.last() == state) {
            return
        }

        undoStack.add(state)

        // Clear redo stack when new action is performed
        redoStack.clear()

        // Keep stack size limited
        if (undoStack.size > maxStackSize) {
            undoStack.removeAt(0)
        }
    }


    fun undo(inputConnection: InputConnection?): Boolean {
        if (inputConnection == null || undoStack.isEmpty()) {
            return false
        }

        try {
            // Get current text before undo
            val currentText = getCurrentText(inputConnection)
            val currentCursor = getCurrentCursorPosition(inputConnection)

            if (currentText != null) {
                redoStack.add(TextState(currentText, currentCursor))
            }

            // Get last state and remove it
            val lastState = undoStack.removeAt(undoStack.size - 1)

            // Delete current text and insert previous state
            deleteAllText(inputConnection)
            inputConnection.commitText(lastState.text, 1)
            inputConnection.setSelection(lastState.cursorPosition, lastState.cursorPosition)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing undo", e)
            return false
        }
    }


    fun redo(inputConnection: InputConnection?): Boolean {
        if (inputConnection == null || redoStack.isEmpty()) {
            return false
        }

        try {
            // Get current text before redo
            val currentText = getCurrentText(inputConnection)
            val currentCursor = getCurrentCursorPosition(inputConnection)

            if (currentText != null) {
                undoStack.add(TextState(currentText, currentCursor))
            }

            // Get last redo state
            val redoState = redoStack.removeAt(redoStack.size - 1)

            // Delete current text and insert redo state
            deleteAllText(inputConnection)
            inputConnection.commitText(redoState.text, 1)
            inputConnection.setSelection(redoState.cursorPosition, redoState.cursorPosition)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing redo", e)
            return false
        }
    }


    fun deleteLastWord(inputConnection: InputConnection?): Boolean {
        if (inputConnection == null) return false

        try {
            val textBeforeCursor = inputConnection.getTextBeforeCursor(100, 0)?.toString() ?: return false

            // Find last word
            val words = textBeforeCursor.trim().split(Regex("\\s+"))
            if (words.isEmpty()) return false

            val lastWord = words.last()
            val deleteLength = lastWord.length

            // Also delete trailing space if exists
            val totalDelete = if (textBeforeCursor.endsWith(" ")) {
                deleteLength + 1
            } else {
                deleteLength
            }

            inputConnection.deleteSurroundingText(totalDelete, 0)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting last word", e)
            return false
        }
    }

    private fun getCurrentText(inputConnection: InputConnection): String? {
        return try {
            val textBefore = inputConnection.getTextBeforeCursor(10000, 0)?.toString() ?: ""
            val textAfter = inputConnection.getTextAfterCursor(10000, 0)?.toString() ?: ""
            textBefore + textAfter
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentCursorPosition(inputConnection: InputConnection): Int {
        return try {
            val textBefore = inputConnection.getTextBeforeCursor(10000, 0)?.toString() ?: ""
            textBefore.length
        } catch (e: Exception) {
            0
        }
    }

    private fun deleteAllText(inputConnection: InputConnection) {
        val textBefore = inputConnection.getTextBeforeCursor(10000, 0)?.toString() ?: ""
        val textAfter = inputConnection.getTextAfterCursor(10000, 0)?.toString() ?: ""
        inputConnection.deleteSurroundingText(textBefore.length, textAfter.length)
    }


    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }
}
