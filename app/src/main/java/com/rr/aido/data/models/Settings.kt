package com.rr.aido.data.models

enum class TriggerMethod(val id: String, val displayName: String, val description: String) {
    ACCESSIBILITY(
        id = "accessibility",
        displayName = "Accessibility",
        description = "Automatic trigger detection while typing in any app. Requires accessibility permission."
    ),
    KEYBOARD(
        id = "keyboard",
        displayName = "Aido Keyboard",
        description = "Custom keyboard with built-in AI processing. No accessibility permission needed."
    );

    companion object {
        fun fromId(id: String?): TriggerMethod {
            return values().firstOrNull { it.id == id } ?: ACCESSIBILITY
        }
    }
}

data class Settings(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val selectedModel: String = "gemini-2.5-flash-lite",
    val customApiUrl: String = "https://api.openai.com/v1/",
    val customApiKey: String = "",
    val customModelName: String = "gpt-4o-mini",
    val isServiceEnabled: Boolean = false,
    val isOfflineMode: Boolean = false,  // Privacy ke liye - remote calls disable kar sakte hain
    val triggerMethod: TriggerMethod = TriggerMethod.ACCESSIBILITY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hapticFeedback: Boolean = true,  // Keyboard haptic feedback
    val showAppShortcuts: Boolean = true,  // Show app shortcuts in keyboard menu
    val isSmartReplyEnabled: Boolean = false, // Smart Reply feature
    val isToneRewriteEnabled: Boolean = false, // Tone Rewrite feature
    val isAllTriggerEnabled: Boolean = false, // @all Trigger feature
    val isSearchTriggerEnabled: Boolean = false, // @search Trigger feature (Default OFF)
    val smartReplyPrompt: String = "", // Custom prompt for Smart Reply
    val toneRewritePrompt: String = "", // Custom prompt for Tone Rewrite
    val smartReplyTrigger: String = "@reply", // Custom trigger for Smart Reply
    val toneRewriteTrigger: String = "@tone", // Custom trigger for Tone Rewrite
    val searchTrigger: String = "@search", // Custom trigger for Search
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO, // Selected search engine
    val customSearchUrl: String = "", // Custom search URL template
    val isProcessingAnimationEnabled: Boolean = false, // Processing animation feature (disabled by default)
    val processingAnimationType: ProcessingAnimationType = ProcessingAnimationType.GENTLE_GLOW, // Animation style
    val isUndoRedoEnabled: Boolean = false, // Undo/Redo popup feature
    val undoRedoPopupX: Int = 0,
    val undoRedoPopupY: Int = 0,
    val isTextSelectionMenuEnabled: Boolean = false, // Text Selection Context Menu (default OFF)
    val textSelectionMenuStyle: SelectionMenuStyle = SelectionMenuStyle.GRID, // Grid or List layout
    val floatingButtonX: Int = -1,
    val floatingButtonY: Int = -1,
    // @all Menu customization - ordered list of triggers with visibility
    val allMenuOrder: List<String> = emptyList(), // Custom order of all commands in @all menu (empty = use default)
    // App Toggle (@on/@off) feature
    val isAppToggleEnabled: Boolean = false, // Toggle feature disabled by default
    val isAppToggledOn: Boolean = true, // App is ON by default when feature is enabled
    // Streaming Text Animation
    val isStreamingModeEnabled: Boolean = false, // Streaming mode disabled by default
    val streamingDelayMs: Int = 50, // Delay between words in milliseconds (50ms = fast, smooth animation)
    val responseLanguage: String = "English", // Language for AI responses
    val isCircleToSearchEnabled: Boolean = false // Circle to Search trigger (Double tap notch)
)

enum class ThemeMode(val id: String, val displayName: String) {
    SYSTEM("system", "System Default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromId(id: String?): ThemeMode {
            return values().firstOrNull { it.id == id } ?: SYSTEM
        }
    }
}

enum class AiProvider(val id: String, val displayName: String, val description: String) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        description = "Use your own Gemini API key for responses."
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom API",
        description = "Use your own OpenAI-compatible API endpoint."
    );

    companion object {
        fun fromId(id: String?): AiProvider {
            return values().firstOrNull { it.id == id } ?: GEMINI
        }
    }
}

object GeminiModels {
    val models = listOf(
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemma-3n-e2b-it",
        "gemma-3n-e4b-it"
    )
}

enum class ProcessingAnimationType(val id: String, val displayName: String, val description: String) {
    PULSE_WAVE(
        id = "pulse_wave",
        displayName = "Pulse Wave",
        description = "Expanding circular waves with gradient"
    ),
    TYPING_DOTS(
        id = "typing_dots",
        displayName = "Typing Dots",
        description = "Animated dots simulating typing"
    ),
    BRAIN_THINKING(
        id = "brain_thinking",
        displayName = "Brain Thinking",
        description = "Rotating brain icon with particles"
    ),
    COLOR_WAVE(
        id = "color_wave",
        displayName = "Color Wave",
        description = "Flowing gradient wave animation"
    ),
    GENTLE_GLOW(
        id = "gentle_glow",
        displayName = "Gentle Glow",
        description = "Peaceful pulsing glow effect"
    ),
    BREATHING_CIRCLE(
        id = "breathing_circle",
        displayName = "Breathing Circle",
        description = "Calming breathing animation"
    ),
    SHIMMER(
        id = "shimmer",
        displayName = "Shimmer",
        description = "Subtle sparkling effect"
    ),
    PARTICLE_FLOW(
        id = "particle_flow",
        displayName = "Particle Flow",
        description = "Floating particles animation"
    ),
    MATRIX_RAIN(
        id = "matrix_rain",
        displayName = "Matrix Rain",
        description = "Tech-style falling characters"
    );

    companion object {
        fun fromId(id: String?): ProcessingAnimationType {
            return values().firstOrNull { it.id == id } ?: GENTLE_GLOW
        }
    }
}

enum class SearchEngine(val id: String, val displayName: String, val urlTemplate: String) {
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s"),
    BING("bing", "Bing", "https://www.bing.com/search?q=%s"),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    BRAVE("brave", "Brave", "https://search.brave.com/search?q=%s"),
    YANDEX("yandex", "Yandex", "https://yandex.com/search/?text=%s"),
    CUSTOM("custom", "Custom", "%s");

    companion object {
        fun fromId(id: String): SearchEngine = values().find { it.id == id } ?: DUCKDUCKGO
    }
}

enum class SelectionMenuStyle(val id: String, val displayName: String) {
    GRID("grid", "Grid Layout"),
    LIST("list", "List Layout");

    companion object {
        fun fromId(id: String?): SelectionMenuStyle {
            return values().firstOrNull { it.id == id } ?: GRID
        }
    }
}

