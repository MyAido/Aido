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
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.Preprompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TriggerPanelHandler(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val dataStoreManager: DataStoreManager,
    private val onTriggerClick: (String) -> Unit
) {

    companion object {
        private const val TAG = "TriggerPanelHandler"
    }

    private var popupWindow: PopupWindow? = null

    fun createTriggerPanel(onBackClick: () -> Unit): View {
        try {
            val triggerView = layoutInflater.inflate(R.layout.trigger_panel, null)

            Log.d(TAG, "Creating trigger panel")

            // Setup back button
            triggerView.findViewById<android.widget.ImageButton>(R.id.btn_back_from_triggers)?.setOnClickListener {
                onBackClick()
            }

            // Load and display triggers
            loadTriggers(triggerView)

            return triggerView
        } catch (e: Exception) {
            Log.e(TAG, "Error creating trigger panel", e)
            throw e
        }
    }

    private fun loadTriggers(triggerView: View) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val preprompts = dataStoreManager.prepromptsFlow.first()

                Log.d(TAG, "Loaded ${preprompts.size} triggers")

                updateTriggerList(triggerView, preprompts)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading triggers", e)
            }
        }
    }

    private fun updateTriggerList(triggerView: View, preprompts: List<Preprompt>) {
        val container = triggerView.findViewById<LinearLayout>(R.id.trigger_list_container)
        container?.removeAllViews()

        // Update count
        triggerView.findViewById<TextView>(R.id.trigger_count)?.text = "${preprompts.size} triggers"

        if (preprompts.isEmpty()) {
            // Show empty message
            val emptyView = TextView(context).apply {
                text = "No triggers available\n\nAdd triggers in app settings"
                textSize = 14f
                setTextColor(context.getColor(android.R.color.darker_gray))
                setPadding(32, 64, 32, 32)
                gravity = android.view.Gravity.CENTER
            }
            container?.addView(emptyView)
        } else {
            // Show trigger items
            preprompts.forEach { preprompt ->
                val itemView = createTriggerItemView(preprompt, triggerView)
                container?.addView(itemView)
            }
        }
    }

    private fun createTriggerItemView(preprompt: Preprompt, rootView: View): View {
        val itemView = layoutInflater.inflate(R.layout.trigger_item, null)

        val titleView = itemView.findViewById<TextView>(R.id.trigger_title)
        val descriptionView = itemView.findViewById<TextView>(R.id.trigger_description)
        val triggerTextView = itemView.findViewById<TextView>(R.id.trigger_text)

        titleView?.text = preprompt.trigger
        descriptionView?.text = preprompt.instruction.take(100) + if (preprompt.instruction.length > 100) "..." else ""
        triggerTextView?.text = preprompt.trigger

        // Click to insert trigger
        itemView.setOnClickListener {
            onTriggerClick("${preprompt.trigger} ")
        }

        return itemView
    }


    fun showTriggerBottomSheet(anchorView: View) {
        try {
            val triggerView = createTriggerPanel {
                // Dismiss popup and close
                popupWindow?.dismiss()
            }

            // Calculate height (60% of screen)
            val displayMetrics = context.resources.displayMetrics
            val popupHeight = (displayMetrics.heightPixels * 0.6f).toInt()

            // Create popup window
            popupWindow = PopupWindow(
                triggerView,
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
            Log.e(TAG, "Error showing trigger bottom sheet", e)
        }
    }
}
