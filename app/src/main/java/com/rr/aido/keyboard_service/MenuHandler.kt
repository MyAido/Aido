package com.rr.aido.keyboard_service

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import com.rr.aido.R

class KeyboardMenuHandler(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val onEmojiClick: () -> Unit,
    private val onClipboardClick: () -> Unit,
    private val onTriggersClick: () -> Unit,
    private val onSettingsClick: () -> Unit,
    private val onUndoClick: () -> Unit
) {

    companion object {
        private const val TAG = "MenuHandler"
    }

    fun showMenuPopup(anchorView: View) {
        try {
            val menuView = layoutInflater.inflate(R.layout.keyboard_menu_popup, null)
            val popupWindow = PopupWindow(
                menuView,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                300.dpToPx(context),
                true
            )

            // Back button
            menuView.findViewById<View>(R.id.btn_back)?.setOnClickListener {
                popupWindow.dismiss()
            }

            // Setup menu items
            menuView.findViewById<View>(R.id.menu_emoji)?.setOnClickListener {
                popupWindow.dismiss()
                onEmojiClick()
            }

            menuView.findViewById<View>(R.id.menu_clipboard)?.setOnClickListener {
                popupWindow.dismiss()
                onClipboardClick()
            }

            menuView.findViewById<View>(R.id.menu_triggers)?.setOnClickListener {
                popupWindow.dismiss()
                onTriggersClick()
            }

            menuView.findViewById<View>(R.id.menu_settings)?.setOnClickListener {
                popupWindow.dismiss()
                // Open Keyboard Settings screen directly
                try {
                    val intent = Intent(context, Class.forName("com.rr.aido.MainActivity"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("navigate_to", "keyboard_settings")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening keyboard settings", e)
                    showToast("Unable to open settings")
                }
            }

            // Set background and elevation
            popupWindow.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
            )
            popupWindow.elevation = 12f
            popupWindow.animationStyle = android.R.style.Animation_Dialog

            // Show popup above the keyboard
            popupWindow.showAtLocation(anchorView, android.view.Gravity.BOTTOM, 0, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing menu popup", e)
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

