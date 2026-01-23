package com.rr.aido.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodManager

object AccessibilityUtils {

    private const val TAG = "AccessibilityUtils"


    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // Try multiple service name formats
        val serviceName1 = "${context.packageName}/.service.AidoAccessibilityService"
        val serviceName2 = "${context.packageName}/com.rr.aido.service.AidoAccessibilityService"
        val serviceName3 = "com.rr.aido/.service.AidoAccessibilityService"
        val serviceName4 = "com.rr.aido/com.rr.aido.service.AidoAccessibilityService"

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        Log.d(TAG, "Package name: ${context.packageName}")
        Log.d(TAG, "Enabled services: $enabledServices")
        Log.d(TAG, "Checking format 1: $serviceName1")
        Log.d(TAG, "Checking format 2: $serviceName2")
        Log.d(TAG, "Checking format 3: $serviceName3")
        Log.d(TAG, "Checking format 4: $serviceName4")

        if (enabledServices.isNullOrEmpty()) {
            Log.d(TAG, "No accessibility services enabled")
            return false
        }

        // Check if any format matches
        val isEnabled = enabledServices.contains(serviceName1) ||
               enabledServices.contains(serviceName2) ||
               enabledServices.contains(serviceName3) ||
               enabledServices.contains(serviceName4) ||
               enabledServices.contains("AidoAccessibilityService")

        Log.d(TAG, "Accessibility service enabled: $isEnabled")
        return isEnabled
    }


    fun isAidoKeyboardEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledInputMethods = imm.enabledInputMethodList

        val aidoKeyboardId = "${context.packageName}/com.rr.aido.service.AidoInputMethodService"

        val isEnabled = enabledInputMethods.any {
            it.id == aidoKeyboardId || it.serviceName.contains("AidoInputMethodService")
        }

        Log.d(TAG, "Aido keyboard enabled: $isEnabled")
        return isEnabled
    }


    fun isAidoKeyboardActive(context: Context): Boolean {
        val currentKeyboard = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )

        val aidoKeyboardId = "${context.packageName}/com.rr.aido.service.AidoInputMethodService"

        val isActive = currentKeyboard?.contains("AidoInputMethodService") == true ||
                      currentKeyboard == aidoKeyboardId

        Log.d(TAG, "Aido keyboard active: $isActive (current: $currentKeyboard)")
        return isActive
    }


    fun redirectToKeyboardSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Redirected to keyboard settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening keyboard settings", e)
        }
    }


    fun showKeyboardPicker(context: Context) {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
            Log.d(TAG, "Showing keyboard picker")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing keyboard picker", e)
        }
    }
}

