package com.rr.aido.service

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppToggleProcessor(
    private val dataStoreManager: DataStoreManager,
    private val scope: CoroutineScope,
    private val onShowToast: (String, Boolean) -> Unit
) {
    private val TAG = "AppToggleProcessor"


    fun containsToggleCommand(text: String): Boolean {
        return text.contains("@on", ignoreCase = true) ||
               text.contains("@off", ignoreCase = true)
    }


    suspend fun processToggle(text: String, settings: Settings, currentNode: AccessibilityNodeInfo?): Boolean {
        // Check if toggle feature is enabled
        if (!settings.isAppToggleEnabled) {
            return false
        }

        val containsOn = text.contains("@on", ignoreCase = true)
        val containsOff = text.contains("@off", ignoreCase = true)

        return when {
            containsOff -> {
                // Turn app OFF
                dataStoreManager.setAppToggleState(false)

                // Remove @off from text
                val newText = text.replace("@off", "", ignoreCase = true).trim()
                currentNode?.let { node ->
                    val arguments = android.os.Bundle()
                    arguments.putCharSequence(
                        android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        newText
                    )
                    node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                }

                onShowToast("✅ Aido: Turned OFF (Use @on to enable)", true)
                Log.d(TAG, "App toggled OFF")
                true
            }
            containsOn -> {
                // Turn app ON
                dataStoreManager.setAppToggleState(true)

                // Remove @on from text
                val newText = text.replace("@on", "", ignoreCase = true).trim()
                currentNode?.let { node ->
                    val arguments = android.os.Bundle()
                    arguments.putCharSequence(
                        android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        newText
                    )
                    node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                }

                onShowToast("✅ Aido: Turned ON (Ready to process)", true)
                Log.d(TAG, "App toggled ON")
                true
            }
            else -> false
        }
    }
}
