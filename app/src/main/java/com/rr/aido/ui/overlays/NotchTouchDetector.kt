package com.rr.aido.ui.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.rr.aido.utils.GoogleCircleToSearchTrigger
import kotlinx.coroutines.CoroutineScope

class NotchTouchDetector(
    private val context: Context,
    private val windowManager: WindowManager,
    private val scope: CoroutineScope
) {
    private val TAG = "NotchTouchDetector"
    private var overlayView: View? = null
    private var isEnabled = false

    fun setEnabled(enabled: Boolean) {
        if (this.isEnabled == enabled) return
        this.isEnabled = enabled
        if (enabled) {
            setupOverlay()
        } else {
            removeOverlay()
        }
    }

    private fun setupOverlay() {
        if (overlayView != null) return


        try {
            // Adjust dimensions based on density
            val density = context.resources.displayMetrics.density
            val width = (130 * density).toInt()  // Width to cover notch (approx)
            val height = (40 * density).toInt()  // Height to cover status bar

            val params = WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

            val view = View(context)
            // Debug color: view.setBackgroundColor(0x44FF0000)
            view.setBackgroundColor(0x00000000) // Transparent

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Log.d(TAG, "Double tap detected on notch!")
                    android.widget.Toast.makeText(context, "Double Tap detected!", android.widget.Toast.LENGTH_SHORT).show()
                    val success = GoogleCircleToSearchTrigger.trigger(context)
                    if (!success) {
                         android.widget.Toast.makeText(context, "Failed to trigger Circle to Search", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                
                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }
            })

            view.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true // Consume events to detect gestures
            }

            windowManager.addView(view, params)
            overlayView = view
            Log.d(TAG, "Notch overlay added")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add notch overlay", e)
        }
    }
    
    private fun removeOverlay() {
        if (overlayView == null) return
        try {
            windowManager.removeView(overlayView)
            Log.d(TAG, "Notch overlay removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove notch overlay", e)
        }
        overlayView = null
    }
}
