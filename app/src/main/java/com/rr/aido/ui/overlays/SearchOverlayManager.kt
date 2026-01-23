package com.rr.aido.ui.overlays

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.graphics.PixelFormat
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val scope: CoroutineScope,
    private val onInsertClick: (String, String) -> Unit // (url, textToReplace)
) {
    private val TAG = "SearchOverlayManager"
    private var currentPopupView: View? = null
    private var currentWebView: WebView? = null
    private var currentTextToReplace: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    fun showSearchPopup(query: String, textToReplace: String, aidoSettings: com.rr.aido.data.models.Settings) {
        scope.launch(Dispatchers.Main) {
            // Remove existing popup if any
            removePopup()

            currentTextToReplace = textToReplace

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.heightPixels * 0.7).toInt(), // 70% height
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // Allow outside touch to pass through? No, we want focus.
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.BOTTOM
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE // Handle keyboard

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFFFFFFFF.toInt()) // White background
                elevation = 16f
            }

            // Header Row (Top Bar)
            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundColor(0xFFF0F0F0.toInt()) // Light gray header
            }

            // Back Button (<)
            val backButton = Button(context).apply {
                text = "<"
                layoutParams = LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    if (currentWebView?.canGoBack() == true) {
                        currentWebView?.goBack()
                    }
                }
            }
            headerLayout.addView(backButton)

            // Forward Button (>)
            val forwardButton = Button(context).apply {
                text = ">"
                layoutParams = LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    if (currentWebView?.canGoForward() == true) {
                        currentWebView?.goForward()
                    }
                }
            }
            headerLayout.addView(forwardButton)

            // Title / URL (Flexible space)
            val titleView = TextView(context).apply {
                text = "Web Search"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(16, 0, 16, 0)
                }
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
            }
            headerLayout.addView(titleView)

            // Insert Link Button
            val insertButton = Button(context).apply {
                text = "Insert"
                setOnClickListener {
                    val url = currentWebView?.url ?: ""
                    if (url.isNotEmpty()) {
                        onInsertClick(url, currentTextToReplace)
                        removePopup()
                    }
                }
            }
            headerLayout.addView(insertButton)

            // Close Button (X)
            val closeButton = Button(context).apply {
                text = "X"
                setOnClickListener {
                    removePopup()
                }
            }
            headerLayout.addView(closeButton)

            layout.addView(headerLayout)

            // Progress Bar
            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                max = 100
            }
            layout.addView(progressBar)

            // WebView
            val webView = WebView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                // Set User-Agent to avoid CAPTCHA (mimic Pixel 7)
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

                isLongClickable = true
                setOnLongClickListener { v ->
                    val hitTestResult = (v as WebView).hitTestResult

                    // Try to get selected text first via JS
                    v.evaluateJavascript("(function(){ return window.getSelection().toString() })()") { selection ->
                        val selectedText = selection?.trim()?.replace("^\"|\"$".toRegex(), "") ?: "" // Remove quotes added by evaluateJavascript

                        val options = mutableListOf<String>()
                        val actions = mutableListOf<() -> Unit>()

                        // Link Options
                        if (hitTestResult.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                            hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                            val url = hitTestResult.extra
                            if (!url.isNullOrEmpty()) {
                                options.add("Copy Link Address")
                                actions.add {
                                    copyToClipboard("Copied Link", url)
                                }
                            }
                        }

                        // Image Options
                        if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                            hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                            val imageUrl = hitTestResult.extra
                            if (!imageUrl.isNullOrEmpty()) {
                                options.add("Copy Image")
                                actions.add {
                                    copyImageToClipboard(imageUrl)
                                }
                                options.add("Copy Image Link")
                                actions.add {
                                    copyToClipboard("Copied Image URL", imageUrl)
                                }
                            }
                        }

                        // Text Option
                        if (selectedText.isNotEmpty()) {
                            options.add("Copy Selected Text")
                            actions.add {
                                copyToClipboard("Copied Text", selectedText)
                            }
                        }

                        if (options.isNotEmpty()) {
                            showContextMenuDialog(options, actions)
                        } else {
                            // If no custom options and no text selected, let system handle it (e.g. show selection handles)
                            // But since we are in async callback, we can't return false from here effectively to affect the original event consumption if we already returned true.
                            // However, we returned true below.
                            // If we want system behavior for text, we should have returned false initially.
                            // But we need to check selection asynchronously.
                            // Compromise: If hit type is text and we have no selection, we might have blocked the initial long press.
                            // But usually long press on text triggers selection.
                            // If we consume it, selection might not start.
                            // Let's try to NOT consume if it's text type and let system handle it.
                        }
                    }

                    // If it's text type, return false to let system show selection handles
                    if (hitTestResult.type == WebView.HitTestResult.EDIT_TEXT_TYPE ||
                        hitTestResult.type == WebView.HitTestResult.UNKNOWN_TYPE) { // UNKNOWN is often text
                         return@setOnLongClickListener false
                    }

                    true // Consume for images/links to show our menu
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false // Load in WebView
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        titleView.text = view?.title ?: url
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        progressBar.progress = newProgress
                        if (newProgress == 100) {
                            progressBar.visibility = View.GONE
                        } else {
                            progressBar.visibility = View.VISIBLE
                        }
                    }
                }

                // Determine URL to load
                val urlToLoad = if (Patterns.WEB_URL.matcher(query).matches()) {
                    if (query.startsWith("http://") || query.startsWith("https://")) {
                        query
                    } else {
                        "https://$query"
                    }
                } else {
                    // Use selected search engine
                    val engine = aidoSettings.searchEngine
                    val template = if (engine == com.rr.aido.data.models.SearchEngine.CUSTOM) {
                        aidoSettings.customSearchUrl.ifEmpty { com.rr.aido.data.models.SearchEngine.GOOGLE.urlTemplate }
                    } else {
                        engine.urlTemplate
                    }
                    try {
                        template.replace("%s", java.net.URLEncoder.encode(query, "UTF-8"))
                    } catch (e: Exception) {
                        "https://www.google.com/search?q=$query"
                    }
                }

                loadUrl(urlToLoad)
            }
            currentWebView = webView
            layout.addView(webView)

            try {
                windowManager.addView(layout, params)
                currentPopupView = layout
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show search popup", e)
            }
        }
    }

    fun removePopup() {
        if (currentPopupView != null) {
            try {
                windowManager.removeView(currentPopupView)
                currentWebView?.destroy()
                currentWebView = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove popup", e)
            }
            currentPopupView = null
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, "$label to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun copyImageToClipboard(imageUrl: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder().url(imageUrl).build()
                val response = okhttp3.OkHttpClient().newCall(request).execute()
                val inputStream = response.body?.byteStream()

                if (inputStream != null) {
                    val filename = "aido_image_${System.currentTimeMillis()}.jpg"
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }

                        // Now copy URI to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = ClipData.newUri(resolver, "Copied Image", uri)
                        clipboard.setPrimaryClip(clip)

                        launch(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Image copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy image", e)
                launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Failed to copy image", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showContextMenuDialog(options: List<String>, actions: List<() -> Unit>) {
        val dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        dialogParams.gravity = Gravity.CENTER
        dialogParams.dimAmount = 0.5f

        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(32, 32, 32, 32)
            elevation = 24f
        }

        // Title
        val title = TextView(context).apply {
            text = "Options"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
            setTextColor(0xFF000000.toInt())
        }
        dialogLayout.addView(title)

        options.forEachIndexed { index, option ->
            val button = Button(context).apply {
                text = option
                setOnClickListener {
                    actions[index]()
                    try {
                        windowManager.removeView(dialogLayout)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to remove dialog", e)
                    }
                }
            }
            dialogLayout.addView(button)
        }

        // Cancel Button
        val cancelButton = Button(context).apply {
            text = "Cancel"
            setOnClickListener {
                try {
                    windowManager.removeView(dialogLayout)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to remove dialog", e)
                }
            }
        }
        dialogLayout.addView(cancelButton)

        try {
            windowManager.addView(dialogLayout, dialogParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show context menu", e)
        }
    }
}
