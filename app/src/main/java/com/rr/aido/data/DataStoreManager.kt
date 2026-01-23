package com.rr.aido.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rr.aido.data.models.AiProvider
import com.rr.aido.data.models.DefaultPreprompts
import com.rr.aido.data.models.Preprompt
import com.rr.aido.data.models.SearchEngine
import com.rr.aido.data.models.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

// DataStore extension
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aido_preferences")

class DataStoreManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        private val PROVIDER = stringPreferencesKey("provider")
        private val API_KEY = stringPreferencesKey("api_key")
        private val SELECTED_MODEL = stringPreferencesKey("selected_model")
        private val CUSTOM_API_URL = stringPreferencesKey("custom_api_url")
        private val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        private val CUSTOM_MODEL_NAME = stringPreferencesKey("custom_model_name")
        private val IS_SERVICE_ENABLED = booleanPreferencesKey("is_service_enabled")
        private val IS_OFFLINE_MODE = booleanPreferencesKey("is_offline_mode")
        private val PREPROMPTS_JSON = stringPreferencesKey("preprompts_json")
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val ANONYMOUS_FAVORITES = stringPreferencesKey("anonymous_favorites")
        private val TRIGGER_METHOD = stringPreferencesKey("trigger_method")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        private val SHOW_APP_SHORTCUTS = booleanPreferencesKey("show_app_shortcuts")
        private val IS_SMART_REPLY_ENABLED = booleanPreferencesKey("is_smart_reply_enabled")
        private val IS_TONE_REWRITE_ENABLED = booleanPreferencesKey("is_tone_rewrite_enabled")
        private val IS_ALL_TRIGGER_ENABLED = booleanPreferencesKey("is_all_trigger_enabled")
        private val IS_SEARCH_TRIGGER_ENABLED = booleanPreferencesKey("is_search_trigger_enabled")
        private val SMART_REPLY_PROMPT = stringPreferencesKey("smart_reply_prompt")
        private val TONE_REWRITE_PROMPT = stringPreferencesKey("tone_rewrite_prompt")
        private val SMART_REPLY_TRIGGER = stringPreferencesKey("smart_reply_trigger")
        private val TONE_REWRITE_TRIGGER = stringPreferencesKey("tone_rewrite_trigger")
        private val SEARCH_TRIGGER = stringPreferencesKey("search_trigger")
        private val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        private val CUSTOM_SEARCH_URL = stringPreferencesKey("custom_search_url")
        private val IS_PROCESSING_ANIMATION_ENABLED = booleanPreferencesKey("is_processing_animation_enabled")
        private val PROCESSING_ANIMATION_TYPE = stringPreferencesKey("processing_animation_type")
        private val IS_UNDO_REDO_ENABLED = booleanPreferencesKey("is_undo_redo_enabled")
        private val UNDO_REDO_POPUP_X = intPreferencesKey("undo_redo_popup_x")
        private val UNDO_REDO_POPUP_Y = intPreferencesKey("undo_redo_popup_y")
        private val IS_TEXT_SELECTION_MENU_ENABLED = booleanPreferencesKey("is_text_selection_menu_enabled")
        private val TEXT_SELECTION_MENU_STYLE = stringPreferencesKey("text_selection_menu_style")
        private val FLOATING_BUTTON_X = intPreferencesKey("floating_button_x")
        private val FLOATING_BUTTON_Y = intPreferencesKey("floating_button_y")
        private val DISABLED_APPS = stringSetPreferencesKey("disabled_apps")
        private val TEXT_SHORTCUTS_JSON = stringPreferencesKey("text_shortcuts_json")
        private val CHAT_HISTORY_JSON = stringPreferencesKey("chat_history_json")

                // Menu Customization
        private val ALL_MENU_ORDER_JSON = stringPreferencesKey("all_menu_order_json")
        // App Toggle feature (Global On/Off)
        private val IS_APP_TOGGLE_ENABLED = booleanPreferencesKey("is_app_toggle_enabled")
        private val IS_APP_TOGGLED_ON = booleanPreferencesKey("is_app_toggled_on")
        // Streaming Text Animation
        private val IS_STREAMING_MODE_ENABLED = booleanPreferencesKey("is_streaming_mode_enabled")
        private val STREAMING_DELAY_MS = intPreferencesKey("streaming_delay_ms")

        // Legacy keys
        private val SHOW_REPLY_IN_ALL = booleanPreferencesKey("show_reply_in_all")
        private val SHOW_TONE_IN_ALL = booleanPreferencesKey("show_tone_in_all")
        private val SHOW_SEARCH_IN_ALL = booleanPreferencesKey("show_search_in_all")
        private val SPECIAL_COMMANDS_POSITION = stringPreferencesKey("special_commands_position")
    }

    // Settings flow
    val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            provider = AiProvider.fromId(preferences[PROVIDER]),
            apiKey = preferences[API_KEY] ?: "",
            selectedModel = preferences[SELECTED_MODEL] ?: "gemini-2.5-flash-lite",
            customApiUrl = preferences[CUSTOM_API_URL] ?: "https://api.openai.com/v1/",
            customApiKey = preferences[CUSTOM_API_KEY] ?: "",
            customModelName = preferences[CUSTOM_MODEL_NAME] ?: "gpt-4o-mini",
            isServiceEnabled = preferences[IS_SERVICE_ENABLED] ?: true, // Default ON
            isOfflineMode = preferences[IS_OFFLINE_MODE] ?: false,
            triggerMethod = com.rr.aido.data.models.TriggerMethod.fromId(preferences[TRIGGER_METHOD]),
            themeMode = com.rr.aido.data.models.ThemeMode.fromId(preferences[THEME_MODE]),
            hapticFeedback = preferences[HAPTIC_FEEDBACK] ?: true,
            showAppShortcuts = preferences[SHOW_APP_SHORTCUTS] ?: true,
            isSmartReplyEnabled = preferences[IS_SMART_REPLY_ENABLED] ?: false, // Default OFF
            isToneRewriteEnabled = preferences[IS_TONE_REWRITE_ENABLED] ?: false, // Default OFF
            isAllTriggerEnabled = preferences[IS_ALL_TRIGGER_ENABLED] ?: false, // Default OFF
            isSearchTriggerEnabled = preferences[IS_SEARCH_TRIGGER_ENABLED] ?: false, // Default OFF
            smartReplyPrompt = preferences[SMART_REPLY_PROMPT] ?: "",
            toneRewritePrompt = preferences[TONE_REWRITE_PROMPT] ?: "",
            smartReplyTrigger = preferences[SMART_REPLY_TRIGGER] ?: "@reply",
            toneRewriteTrigger = preferences[TONE_REWRITE_TRIGGER] ?: "@tone",
            searchTrigger = preferences[SEARCH_TRIGGER] ?: "@search",
            searchEngine = SearchEngine.fromId(preferences[SEARCH_ENGINE] ?: SearchEngine.DUCKDUCKGO.id),
            customSearchUrl = preferences[CUSTOM_SEARCH_URL] ?: "",
            isProcessingAnimationEnabled = preferences[IS_PROCESSING_ANIMATION_ENABLED] ?: false,
            processingAnimationType = com.rr.aido.data.models.ProcessingAnimationType.fromId(preferences[PROCESSING_ANIMATION_TYPE]),
            isUndoRedoEnabled = preferences[IS_UNDO_REDO_ENABLED] ?: false, // Default OFF
            undoRedoPopupX = preferences[UNDO_REDO_POPUP_X] ?: 0,
            undoRedoPopupY = preferences[UNDO_REDO_POPUP_Y] ?: 0,
            isTextSelectionMenuEnabled = preferences[IS_TEXT_SELECTION_MENU_ENABLED] ?: false, // Default OFF
            textSelectionMenuStyle = com.rr.aido.data.models.SelectionMenuStyle.fromId(preferences[TEXT_SELECTION_MENU_STYLE]),
            floatingButtonX = preferences[FLOATING_BUTTON_X] ?: -1, // -1 means default position
            floatingButtonY = preferences[FLOATING_BUTTON_Y] ?: -1,
            allMenuOrder = try {
                // Try to load custom order
                val json = preferences[ALL_MENU_ORDER_JSON]
                if (!json.isNullOrEmpty()) {
                    val type = object : TypeToken<List<String>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            },
            isAppToggleEnabled = preferences[IS_APP_TOGGLE_ENABLED] ?: false, // Default OFF
            isAppToggledOn = preferences[IS_APP_TOGGLED_ON] ?: true, // Default ON
            isStreamingModeEnabled = preferences[IS_STREAMING_MODE_ENABLED] ?: false, // Default OFF
            streamingDelayMs = preferences[STREAMING_DELAY_MS] ?: 50 // Default 50ms delay between words
        )
    }

    // Preprompts flow
    val prepromptsFlow: Flow<List<Preprompt>> = context.dataStore.data.map { preferences ->
        val json = preferences[PREPROMPTS_JSON]
        if (json.isNullOrEmpty()) {
            // First time - return default preprompts
            DefaultPreprompts.list
        } else {
            try {
                val type = object : TypeToken<List<Preprompt>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                DefaultPreprompts.list
            }
        }
    }

    // Check if first launch
    val isFirstLaunchFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_LAUNCH] ?: true
    }

    // Save API key
    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    // Save provider
    suspend fun saveProvider(provider: AiProvider) {
        context.dataStore.edit { preferences ->
            preferences[PROVIDER] = provider.id
        }
    }

    // Save selected model
    suspend fun saveSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MODEL] = model
        }
    }

    // Save service enabled status
    suspend fun saveServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SERVICE_ENABLED] = enabled
        }
    }

    // Save offline mode
    suspend fun saveOfflineMode(offline: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_OFFLINE_MODE] = offline
        }
    }

    suspend fun saveTriggerMethod(triggerMethod: com.rr.aido.data.models.TriggerMethod) {
        context.dataStore.edit { preferences ->
            preferences[TRIGGER_METHOD] = triggerMethod.id
        }
    }

    // Save theme mode
    suspend fun saveThemeMode(themeMode: com.rr.aido.data.models.ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.id
        }
    }

    // Save haptic feedback preference
    suspend fun saveHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK] = enabled
        }
    }

    // Save show app shortcuts preference
    suspend fun saveShowAppShortcuts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_APP_SHORTCUTS] = enabled
        }
    }

    // Save smart reply preference
    suspend fun saveSmartReplyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SMART_REPLY_ENABLED] = enabled
        }
    }

    // Save tone rewrite preference
    suspend fun saveToneRewriteEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_TONE_REWRITE_ENABLED] = enabled
        }
    }

    // Save @all trigger preference
    suspend fun saveAllTriggerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ALL_TRIGGER_ENABLED] = enabled
        }
    }

    // Save @search trigger preference
    suspend fun saveSearchTriggerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SEARCH_TRIGGER_ENABLED] = enabled
        }
    }

    // Save Search Trigger
    suspend fun saveSearchTrigger(trigger: String) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_TRIGGER] = trigger
        }
    }

    suspend fun saveSearchEngine(engine: com.rr.aido.data.models.SearchEngine) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_ENGINE] = engine.id
        }
    }

    suspend fun saveCustomSearchUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_SEARCH_URL] = url
        }
    }

    // Save Smart Reply Prompt
    suspend fun saveSmartReplyPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[SMART_REPLY_PROMPT] = prompt
        }
    }

    // Save Tone Rewrite Prompt
    suspend fun saveToneRewritePrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[TONE_REWRITE_PROMPT] = prompt
        }
    }

    // Save Smart Reply Trigger
    suspend fun saveSmartReplyTrigger(trigger: String) {
        context.dataStore.edit { preferences ->
            preferences[SMART_REPLY_TRIGGER] = trigger
        }
    }

    // Save Tone Rewrite Trigger
    suspend fun saveToneRewriteTrigger(trigger: String) {
        context.dataStore.edit { preferences ->
            preferences[TONE_REWRITE_TRIGGER] = trigger
        }
    }

    // Save processing animation enabled
    suspend fun saveProcessingAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PROCESSING_ANIMATION_ENABLED] = enabled
        }
    }

    // Save processing animation type
    suspend fun saveProcessingAnimationType(type: com.rr.aido.data.models.ProcessingAnimationType) {
        context.dataStore.edit { preferences ->
            preferences[PROCESSING_ANIMATION_TYPE] = type.id
        }
    }

    // Save Undo/Redo enabled
    suspend fun saveUndoRedoEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_UNDO_REDO_ENABLED] = enabled
        }
    }

    suspend fun saveUndoRedoPosition(x: Int, y: Int) {
        context.dataStore.edit { preferences ->
            preferences[UNDO_REDO_POPUP_X] = x
            preferences[UNDO_REDO_POPUP_Y] = y
        }
    }

    // Save text selection menu enabled
    suspend fun saveTextSelectionMenuEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_TEXT_SELECTION_MENU_ENABLED] = enabled
        }
    }

    // Save text selection menu style
    suspend fun saveTextSelectionMenuStyle(style: com.rr.aido.data.models.SelectionMenuStyle) {
        context.dataStore.edit { preferences ->
            preferences[TEXT_SELECTION_MENU_STYLE] = style.id
        }
    }

    // Save floating button position
    suspend fun saveFloatingButtonPosition(x: Int, y: Int) {
        context.dataStore.edit { preferences ->
            preferences[FLOATING_BUTTON_X] = x
            preferences[FLOATING_BUTTON_Y] = y
        }
    }

    // Save @all menu order
    suspend fun saveAllMenuOrder(order: List<String>) {
        context.dataStore.edit { preferences ->
            val json = gson.toJson(order)
            preferences[ALL_MENU_ORDER_JSON] = json
        }
    }

    // Save app toggle feature enabled
    suspend fun saveAppToggleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_APP_TOGGLE_ENABLED] = enabled
        }
    }

    // Set app toggle state (on/off)
    suspend fun setAppToggleState(isOn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_APP_TOGGLED_ON] = isOn
        }
    }

    // Save streaming mode enabled
    suspend fun saveStreamingModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_STREAMING_MODE_ENABLED] = enabled
        }
    }

    // Save streaming delay
    suspend fun saveStreamingDelayMs(delayMs: Int) {
        context.dataStore.edit { preferences ->
            preferences[STREAMING_DELAY_MS] = delayMs
        }
    }

    // Toggle app disabled state
    suspend fun toggleAppDisabled(packageName: String, isDisabled: Boolean) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[DISABLED_APPS] ?: emptySet()
            if (isDisabled) {
                preferences[DISABLED_APPS] = currentSet + packageName
            } else {
                preferences[DISABLED_APPS] = currentSet - packageName
            }
        }
    }

    // Save disabled apps set
    suspend fun saveDisabledApps(apps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[DISABLED_APPS] = apps
        }
    }

    // Save text shortcuts
    suspend fun saveTextShortcuts(shortcuts: List<com.rr.aido.data.models.TextShortcut>) {
        context.dataStore.edit { preferences ->
            val json = gson.toJson(shortcuts)
            preferences[TEXT_SHORTCUTS_JSON] = json
        }
    }

    // Add a text shortcut
    suspend fun addTextShortcut(shortcut: com.rr.aido.data.models.TextShortcut) {
        val currentList = textShortcutsFlow.first()
        val newList = currentList + shortcut
        saveTextShortcuts(newList)
    }

    // Update a text shortcut
    suspend fun updateTextShortcut(shortcut: com.rr.aido.data.models.TextShortcut) {
        val currentList = textShortcutsFlow.first()
        val newList = currentList.map { if (it.id == shortcut.id) shortcut else it }
        saveTextShortcuts(newList)
    }

    // Remove a text shortcut
    suspend fun removeTextShortcut(shortcutId: String) {
        val currentList = textShortcutsFlow.first()
        val newList = currentList.filter { it.id != shortcutId }
        saveTextShortcuts(newList)
    }

    // Save custom API URL
    suspend fun saveCustomApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_API_URL] = url
        }
    }

    // Save custom API key
    suspend fun saveCustomApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_API_KEY] = key
        }
    }

    // Save custom model name
    suspend fun saveCustomModelName(model: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_MODEL_NAME] = model
        }
    }

    // Toggle service on/off
    suspend fun toggleService(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SERVICE_ENABLED] = enabled
        }
    }

    // Save preprompts
    suspend fun savePreprompts(preprompts: List<Preprompt>) {
        context.dataStore.edit { preferences ->
            val json = gson.toJson(preprompts)
            preferences[PREPROMPTS_JSON] = json
        }
    }

    // Mark first launch complete
    suspend fun markFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = false
        }
    }

    // Get anonymous favorites
    suspend fun getAnonymousFavorites(): Set<String> {
        var result = emptySet<String>()
        context.dataStore.data.collect { preferences ->
            val json = preferences[ANONYMOUS_FAVORITES]
            result = if (!json.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<Set<String>>() {}.type
                    gson.fromJson(json, type) ?: emptySet()
                } catch (e: Exception) {
                    emptySet()
                }
            } else {
                emptySet()
            }
        }
        return result
    }

    // Save anonymous favorites
    suspend fun saveAnonymousFavorites(favorites: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[ANONYMOUS_FAVORITES] = gson.toJson(favorites)
        }
    }

    // Save complete settings
    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[PROVIDER] = settings.provider.id
            preferences[API_KEY] = settings.apiKey
            preferences[SELECTED_MODEL] = settings.selectedModel
            preferences[CUSTOM_API_URL] = settings.customApiUrl
            preferences[CUSTOM_API_KEY] = settings.customApiKey
            preferences[CUSTOM_MODEL_NAME] = settings.customModelName
            preferences[IS_SERVICE_ENABLED] = settings.isServiceEnabled
            preferences[IS_OFFLINE_MODE] = settings.isOfflineMode
            preferences[THEME_MODE] = settings.themeMode.id
            preferences[HAPTIC_FEEDBACK] = settings.hapticFeedback
            preferences[SHOW_APP_SHORTCUTS] = settings.showAppShortcuts
            preferences[IS_SMART_REPLY_ENABLED] = settings.isSmartReplyEnabled
            preferences[IS_TONE_REWRITE_ENABLED] = settings.isToneRewriteEnabled
            preferences[IS_ALL_TRIGGER_ENABLED] = settings.isAllTriggerEnabled
            preferences[IS_SEARCH_TRIGGER_ENABLED] = settings.isSearchTriggerEnabled
            preferences[SMART_REPLY_PROMPT] = settings.smartReplyPrompt
            preferences[TONE_REWRITE_PROMPT] = settings.toneRewritePrompt
            preferences[SMART_REPLY_TRIGGER] = settings.smartReplyTrigger
            preferences[TONE_REWRITE_TRIGGER] = settings.toneRewriteTrigger
            preferences[SEARCH_TRIGGER] = settings.searchTrigger
            preferences[SEARCH_ENGINE] = settings.searchEngine.id
            preferences[CUSTOM_SEARCH_URL] = settings.customSearchUrl
            preferences[IS_PROCESSING_ANIMATION_ENABLED] = settings.isProcessingAnimationEnabled
            preferences[PROCESSING_ANIMATION_TYPE] = settings.processingAnimationType.id
            preferences[IS_UNDO_REDO_ENABLED] = settings.isUndoRedoEnabled
            preferences[IS_TEXT_SELECTION_MENU_ENABLED] = settings.isTextSelectionMenuEnabled
            preferences[TEXT_SELECTION_MENU_STYLE] = settings.textSelectionMenuStyle.id
            preferences[ALL_MENU_ORDER_JSON] = gson.toJson(settings.allMenuOrder)
            preferences[IS_APP_TOGGLE_ENABLED] = settings.isAppToggleEnabled
            preferences[IS_APP_TOGGLED_ON] = settings.isAppToggledOn
            preferences[IS_STREAMING_MODE_ENABLED] = settings.isStreamingModeEnabled
            preferences[STREAMING_DELAY_MS] = settings.streamingDelayMs
        }
    }

    // Add new preprompt
    suspend fun addPreprompt(preprompt: Preprompt, currentList: List<Preprompt>) {
        val updatedList = currentList + preprompt
        savePreprompts(updatedList)

        // Auto-add to @all menu order if not already there
        val currentSettings = settingsFlow.first()
        val currentOrder = currentSettings.allMenuOrder.toMutableList()
        if (preprompt.trigger !in currentOrder) {
            currentOrder.add(preprompt.trigger)
            saveAllMenuOrder(currentOrder)
        }
    }

    // Update preprompt
    suspend fun updatePreprompt(oldTrigger: String, newPreprompt: Preprompt, currentList: List<Preprompt>) {
        val updatedList = currentList.map {
            if (it.trigger == oldTrigger) newPreprompt else it
        }
        savePreprompts(updatedList)

        // Auto-update trigger name in @all menu order
        if (oldTrigger != newPreprompt.trigger) {
            val currentSettings = settingsFlow.first()
            val currentOrder = currentSettings.allMenuOrder.map {
                if (it == oldTrigger) newPreprompt.trigger else it
            }
            saveAllMenuOrder(currentOrder)
        }
    }

    // Delete preprompt
    suspend fun deletePreprompt(trigger: String, currentList: List<Preprompt>) {
        val updatedList = currentList.filter { it.trigger != trigger }
        savePreprompts(updatedList)

        // Auto-remove from @all menu order
        val currentSettings = settingsFlow.first()
        val currentOrder = currentSettings.allMenuOrder.filter { it != trigger }
        saveAllMenuOrder(currentOrder)
    }

    // Reorder preprompts
    suspend fun reorderPreprompts(fromIndex: Int, toIndex: Int, currentList: List<Preprompt>) {
        val mutableList = currentList.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
        savePreprompts(mutableList)
    }

    // Reset to default preprompts
    suspend fun resetToDefaultPreprompts() {
        savePreprompts(DefaultPreprompts.list)
    }

    // Export Backup Data
    suspend fun exportBackup(settings: Settings, preprompts: List<Preprompt>): String {
        val textShortcuts = textShortcutsFlow.first()
        val disabledApps = disabledAppsFlow.first()
        val backupData = com.rr.aido.data.models.BackupData(settings, preprompts, textShortcuts, disabledApps)
        return gson.toJson(backupData)
    }

    // Import Backup Data
    suspend fun importBackup(json: String) {
        try {
            val backupData = gson.fromJson(json, com.rr.aido.data.models.BackupData::class.java)
            saveSettings(backupData.settings)
            savePreprompts(backupData.preprompts)
            saveTextShortcuts(backupData.textShortcuts ?: emptyList())
            saveDisabledApps(backupData.disabledApps ?: emptySet())
        } catch (e: Exception) {
            // Fallback for old backup format (List<Preprompt>)
            try {
                val type = object : TypeToken<List<Preprompt>>() {}.type
                val preprompts: List<Preprompt> = gson.fromJson(json, type)
                savePreprompts(preprompts)
            } catch (e2: Exception) {
                throw Exception("Invalid backup file format")
            }
        }
    }

    // Add disabled apps preference and methods
    val disabledAppsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DISABLED_APPS] ?: emptySet()
        }

    // Text Shortcuts Flow
    val textShortcutsFlow: Flow<List<com.rr.aido.data.models.TextShortcut>> = context.dataStore.data.map { preferences ->
        val json = preferences[TEXT_SHORTCUTS_JSON] ?: "[]"
        try {
            val type = object : TypeToken<List<com.rr.aido.data.models.TextShortcut>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Chat History Flow
    val chatHistoryFlow: Flow<List<com.rr.aido.data.models.ChatMessage>> = context.dataStore.data.map { preferences ->
        val json = preferences[CHAT_HISTORY_JSON]
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<com.rr.aido.data.models.ChatMessage>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveChatHistory(messages: List<com.rr.aido.data.models.ChatMessage>) {
        context.dataStore.edit { preferences ->
            val json = gson.toJson(messages)
            preferences[CHAT_HISTORY_JSON] = json
        }
    }

    suspend fun clearChatHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(CHAT_HISTORY_JSON)
        }
    }
}
