package com.rr.aido.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Utility to trigger Google's native Circle to Search
 * Based on MiCTS implementation
 */
@SuppressLint("PrivateApi")
object GoogleCircleToSearchTrigger {
    
    private const val TAG = "GoogleCTSTrigger"
    
    /**
     * Triggers Google's native Circle to Search feature
     * Requires Google app to be set as default assistant
     * 
     * @param context Application context
     * @param vibrate Whether to provide haptic feedback
     * @return true if trigger was successful, false otherwise
     */
    fun trigger(context: Context, vibrate: Boolean = true): Boolean {
        val result = runCatching {
            // Prepare bundle with entry point and timing info
            val bundle = Bundle().apply {
                putLong("invocation_time_ms", SystemClock.elapsedRealtime())
                putInt("omni.entry_point", 1) // Entry point 1 for app launch
                putBoolean("aido_trigger", true)
            }
            
            // Use reflection to access IVoiceInteractionManagerService
            val iVimsClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
            val vis = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "voiceinteraction")
            val vims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, vis)
            
            // Invoke showSessionFromSession to trigger Circle to Search
            if (Build.VERSION.SDK_INT >= 34) { // Android 14 (Upside Down Cake)
                 HiddenApiBypass.invoke(
                    iVimsClass, vims, "showSessionFromSession",
                    null, bundle, 7, "aido_assistant"
                ) as Boolean
            } else {
                HiddenApiBypass.invoke(
                    iVimsClass, vims, "showSessionFromSession",
                    null, bundle, 7
                ) as Boolean
            }
        }.onFailure { e ->
            Log.e(TAG, "Failed to trigger Google Circle to Search: ${e.message}", e)
        }.getOrDefault(false)
        
        // Provide haptic feedback if successful
        if (result && vibrate) {
            provideHapticFeedback(context)
        }
        
        return result
    }
    
    private fun provideHapticFeedback(context: Context) {
        runCatching {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setFlags(128)
                .build()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), attr)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1, 75, 76), -1, attr)
            }
        }.onFailure { e ->
            Log.e(TAG, "Haptic feedback failed: ${e.message}")
        }
    }
}
