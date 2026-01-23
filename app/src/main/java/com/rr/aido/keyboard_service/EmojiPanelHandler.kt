package com.rr.aido.keyboard_service

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.GridView
import android.widget.PopupWindow
import com.rr.aido.R
import com.rr.aido.utils.EmojiData

class EmojiPanelHandler(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val inputConnection: InputConnection?
) {

    companion object {
        private const val TAG = "EmojiPanelHandler"
    }

    private var popupWindow: PopupWindow? = null

    fun createEmojiPanel(onBackClick: () -> Unit): View {
        try {
            val mediaView = layoutInflater.inflate(R.layout.media_panel, null)
            var currentCategory = "Smileys"

            // Add navigation bar padding for Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaView.setOnApplyWindowInsetsListener { view, insets ->
                    val navBarInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        insets.getInsets(android.view.WindowInsets.Type.navigationBars())
                    } else {
                        @Suppress("DEPRECATION")
                        android.graphics.Insets.of(0, 0, 0, insets.systemWindowInsetBottom)
                    }
                    view.setPadding(
                        view.paddingLeft,
                        view.paddingTop,
                        view.paddingRight,
                        navBarInsets.bottom
                    )
                    insets
                }
            } else {
                mediaView.setPadding(0, 0, 0, 48)
            }

            // Setup emoji grid with initial category
            fun updateEmojiGrid(category: String) {
                currentCategory = category
                val emojis = when(category) {
                    "Smileys" -> EmojiData.SMILEYS_PEOPLE
                    "Gestures" -> EmojiData.GESTURES_HANDS
                    "Hearts" -> EmojiData.HEARTS_LOVE
                    "Animals" -> EmojiData.ANIMALS_NATURE
                    "Food" -> EmojiData.FOOD_DRINK
                    "Sports" -> EmojiData.ACTIVITIES_SPORTS
                    "Travel" -> EmojiData.TRAVEL_PLACES
                    "Objects" -> EmojiData.OBJECTS
                    "Symbols" -> EmojiData.SYMBOLS
                    else -> EmojiData.SMILEYS_PEOPLE
                }

                val emojiGrid = mediaView.findViewById<GridView>(R.id.emoji_grid)
                val adapter = android.widget.ArrayAdapter(
                    context,
                    R.layout.emoji_item,
                    emojis
                )
                emojiGrid?.adapter = adapter

                emojiGrid?.setOnItemClickListener { _, _, position, _ ->
                    val emoji = emojis[position]
                    inputConnection?.commitText(emoji, 1)
                }
            }

            // Initial load
            updateEmojiGrid("Smileys")

            // Category buttons
            setupCategoryButton(mediaView, R.id.btn_category_smileys) { updateEmojiGrid("Smileys") }
            setupCategoryButton(mediaView, R.id.btn_category_gestures) { updateEmojiGrid("Gestures") }
            setupCategoryButton(mediaView, R.id.btn_category_hearts) { updateEmojiGrid("Hearts") }
            setupCategoryButton(mediaView, R.id.btn_category_animals) { updateEmojiGrid("Animals") }
            setupCategoryButton(mediaView, R.id.btn_category_food) { updateEmojiGrid("Food") }
            setupCategoryButton(mediaView, R.id.btn_category_sports) { updateEmojiGrid("Sports") }
            setupCategoryButton(mediaView, R.id.btn_category_travel) { updateEmojiGrid("Travel") }
            setupCategoryButton(mediaView, R.id.btn_category_objects) { updateEmojiGrid("Objects") }
            setupCategoryButton(mediaView, R.id.btn_category_symbols) { updateEmojiGrid("Symbols") }

            // Back button
            mediaView.findViewById<android.widget.ImageButton>(R.id.btn_back_to_keyboard)?.setOnClickListener {
                onBackClick()
            }

            return mediaView
        } catch (e: Exception) {
            Log.e(TAG, "Error creating emoji panel", e)
            throw e
        }
    }

    private fun setupCategoryButton(view: View, buttonId: Int, onClick: () -> Unit) {
        view.findViewById<Button>(buttonId)?.setOnClickListener { onClick() }
    }


    fun showEmojiBottomSheet(anchorView: View) {
        try {
            val emojiView = createEmojiPanel {
                // Dismiss popup
                popupWindow?.dismiss()
            }

            // Calculate height (70% of screen for emoji grid)
            val displayMetrics = context.resources.displayMetrics
            val popupHeight = (displayMetrics.heightPixels * 0.7f).toInt()

            // Create popup window
            popupWindow = PopupWindow(
                emojiView,
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
            Log.e(TAG, "Error showing emoji bottom sheet", e)
        }
    }
}
