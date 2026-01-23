package com.rr.aido.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.AiProvider
import com.rr.aido.data.models.SearchEngine
import com.rr.aido.data.repository.GeminiRepositoryImpl
import com.rr.aido.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * SettingsViewModel - Settings screen ke liye
 * API key, model selection handle karta hai
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    val dataStoreManager = DataStoreManager(application)
    private val geminiRepository = GeminiRepositoryImpl()
    
    // UI State
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Settings flow
    val settings = dataStoreManager.settingsFlow
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settings.collect { currentSettings ->
                _uiState.value = _uiState.value.copy(
                    provider = currentSettings.provider,
                    apiKey = currentSettings.apiKey,
                    selectedModel = currentSettings.selectedModel,
                    customApiUrl = currentSettings.customApiUrl,
                    customApiKey = currentSettings.customApiKey,
                    customModelName = currentSettings.customModelName,
                    isServiceEnabled = currentSettings.isServiceEnabled,
                    isOfflineMode = currentSettings.isOfflineMode,
                    themeMode = currentSettings.themeMode,
                    isSmartReplyEnabled = currentSettings.isSmartReplyEnabled,
                    isToneRewriteEnabled = currentSettings.isToneRewriteEnabled,
                    isAllTriggerEnabled = currentSettings.isAllTriggerEnabled,
                    isSearchTriggerEnabled = currentSettings.isSearchTriggerEnabled,
                    smartReplyPrompt = currentSettings.smartReplyPrompt,
                    toneRewritePrompt = currentSettings.toneRewritePrompt,
                    smartReplyTrigger = currentSettings.smartReplyTrigger,
                    toneRewriteTrigger = currentSettings.toneRewriteTrigger,
                    searchTrigger = currentSettings.searchTrigger,
                    searchEngine = currentSettings.searchEngine,
                    customSearchUrl = currentSettings.customSearchUrl,
                    isProcessingAnimationEnabled = currentSettings.isProcessingAnimationEnabled,
                    processingAnimationType = currentSettings.processingAnimationType,
                    isUndoRedoEnabled = currentSettings.isUndoRedoEnabled,
                    isTextSelectionMenuEnabled = currentSettings.isTextSelectionMenuEnabled,
                    textSelectionMenuStyle = currentSettings.textSelectionMenuStyle,
                    allMenuOrder = currentSettings.allMenuOrder,
                    isAppToggleEnabled = currentSettings.isAppToggleEnabled,
                    isStreamingModeEnabled = currentSettings.isStreamingModeEnabled,
                    streamingDelayMs = currentSettings.streamingDelayMs
                )
            }
        }
    }
    
    /**
     * Update API key
     */
    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(
            apiKey = apiKey,
            testResult = null,
            errorMessage = null
        )
    }
    
    /**
     * Update selected model
     */
    fun updateSelectedModel(model: String) {
        _uiState.value = _uiState.value.copy(selectedModel = model, testResult = null)
        viewModelScope.launch {
            dataStoreManager.saveSelectedModel(model)
        }
    }

    /**
     * Select provider
     */
    fun updateProvider(provider: AiProvider) {
        _uiState.value = _uiState.value.copy(
            provider = provider,
            isTesting = false,
            testResult = null,
            errorMessage = null
        )
        viewModelScope.launch {
            dataStoreManager.saveProvider(provider)
        }
    }
    
    /**
     * Test and save API key
     */
    fun testAndSaveApiKey() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTesting = true,
                testResult = null,
                errorMessage = null
            )
            
            try {
                val currentProvider = _uiState.value.provider



                val apiKeyToTest = if (currentProvider == AiProvider.CUSTOM) {
                    _uiState.value.customApiKey
                } else {
                    _uiState.value.apiKey
                }
                
                val result = geminiRepository.testApiKey(
                    provider = currentProvider,
                    apiKey = apiKeyToTest,
                    model = _uiState.value.selectedModel
                )
                
                when (result) {
                    is Result.Success -> {
                        if (result.data) {
                            // Save provider + API key
                            dataStoreManager.saveProvider(currentProvider)
                            dataStoreManager.saveApiKey(_uiState.value.apiKey)
                            dataStoreManager.saveSelectedModel(_uiState.value.selectedModel)
                            
                            _uiState.value = _uiState.value.copy(
                                isTesting = false,
                                testResult = "API Key is valid and saved successfully! ✅"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isTesting = false,
                                errorMessage = "API Key validation failed. Please check your key."
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isTesting = false,
                            errorMessage = result.message
                        )
                    }
                    else -> {}
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Toggle service on/off
     */
    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.toggleService(enabled)
            _uiState.value = _uiState.value.copy(isServiceEnabled = enabled)
        }
    }
    
    /**
     * Toggle offline mode
     */
    fun toggleOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveOfflineMode(enabled)
            _uiState.value = _uiState.value.copy(isOfflineMode = enabled)
        }
    }

    /**
     * Toggle Smart Reply
     */
    fun toggleSmartReply(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveSmartReplyEnabled(enabled)
            _uiState.value = _uiState.value.copy(isSmartReplyEnabled = enabled)
        }
    }

    /**
     * Toggle Tone Rewrite
     */
    fun toggleToneRewrite(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveToneRewriteEnabled(enabled)
            _uiState.value = _uiState.value.copy(isToneRewriteEnabled = enabled)
        }
    }

    /**
     * Toggle @all Trigger
     */
    fun toggleAllTrigger(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAllTriggerEnabled(enabled)
            _uiState.value = _uiState.value.copy(isAllTriggerEnabled = enabled)
        }
    }

    /**
     * Toggle @search Trigger
     */
    fun toggleSearchTrigger(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveSearchTriggerEnabled(enabled)
            _uiState.value = _uiState.value.copy(isSearchTriggerEnabled = enabled)
        }
    }

    /**
     * Update @search Trigger
     */
    fun updateSearchTrigger(trigger: String) {
        viewModelScope.launch {
            dataStoreManager.saveSearchTrigger(trigger)
            _uiState.value = _uiState.value.copy(searchTrigger = trigger)
        }
    }

    fun updateSearchEngine(engine: com.rr.aido.data.models.SearchEngine) {
        viewModelScope.launch {
            dataStoreManager.saveSearchEngine(engine)
            _uiState.value = _uiState.value.copy(searchEngine = engine)
        }
    }

    fun updateCustomSearchUrl(url: String) {
        viewModelScope.launch {
            dataStoreManager.saveCustomSearchUrl(url)
            _uiState.value = _uiState.value.copy(customSearchUrl = url)
        }
    }

    /**
     * Update Smart Reply Prompt
     */
    fun updateSmartReplyPrompt(prompt: String) {
        viewModelScope.launch {
            dataStoreManager.saveSmartReplyPrompt(prompt)
            _uiState.value = _uiState.value.copy(smartReplyPrompt = prompt)
        }
    }

    /**
     * Update Tone Rewrite Prompt
     */
    fun updateToneRewritePrompt(prompt: String) {
        viewModelScope.launch {
            dataStoreManager.saveToneRewritePrompt(prompt)
            _uiState.value = _uiState.value.copy(toneRewritePrompt = prompt)
        }
    }

    /**
     * Update Smart Reply Trigger
     */
    fun updateSmartReplyTrigger(trigger: String) {
        viewModelScope.launch {
            dataStoreManager.saveSmartReplyTrigger(trigger)
            _uiState.value = _uiState.value.copy(smartReplyTrigger = trigger)
        }
    }

    /**
     * Update Tone Rewrite Trigger
     */
    fun updateToneRewriteTrigger(trigger: String) {
        viewModelScope.launch {
            dataStoreManager.saveToneRewriteTrigger(trigger)
            _uiState.value = _uiState.value.copy(toneRewriteTrigger = trigger)
        }
    }

    /**
     * Export Backup
     */
    fun exportBackup(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Get current settings and preprompts
                val currentSettings = settings.first() // This might block if not careful, but settings is a flow
                // Actually, we can just use the current state from UI or fetch fresh
                // Let's fetch fresh to be safe
                val freshSettings = settings.first()
                val preprompts = dataStoreManager.prepromptsFlow.first()
                
                val json = dataStoreManager.exportBackup(freshSettings, preprompts)
                onComplete(json)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Export failed: ${e.message}")
            }
        }
    }

    /**
     * Import Backup
     */
    fun importBackup(json: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                dataStoreManager.importBackup(json)
                onComplete(null) // Success
            } catch (e: Exception) {
                onComplete("Import failed: ${e.message}")
            }
        }
    }
    
    /**
     * Update trigger method
     */
    fun updateTriggerMethod(method: com.rr.aido.data.models.TriggerMethod) {
        viewModelScope.launch {
            dataStoreManager.saveTriggerMethod(method)
        }
    }

    /**
     * Update theme mode
     */
    fun updateThemeMode(mode: com.rr.aido.data.models.ThemeMode) {
        viewModelScope.launch {
            dataStoreManager.saveThemeMode(mode)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }
    
    /**
     * Update haptic feedback preference
     */
    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveHapticFeedback(enabled)
        }
    }
    
    /**
     * Update app shortcuts display preference
     */
    fun updateShowAppShortcuts(show: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveShowAppShortcuts(show)
        }
    }

    /**
     * Toggle Processing Animation
     */
    fun toggleProcessingAnimation(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveProcessingAnimationEnabled(enabled)
            _uiState.value = _uiState.value.copy(isProcessingAnimationEnabled = enabled)
        }
    }

    /**
     * Update Processing Animation Type
     */
    fun updateProcessingAnimationType(type: com.rr.aido.data.models.ProcessingAnimationType) {
        viewModelScope.launch {
            dataStoreManager.saveProcessingAnimationType(type)
            _uiState.value = _uiState.value.copy(processingAnimationType = type)
        }
    }

    /**
     * Toggle Undo/Redo
     */
    fun toggleUndoRedo(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveUndoRedoEnabled(enabled)
            _uiState.value = _uiState.value.copy(isUndoRedoEnabled = enabled)
        }
    }

    /**
     * Toggle Text Selection Menu
     */
    fun toggleTextSelectionMenu(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveTextSelectionMenuEnabled(enabled)
            _uiState.value = _uiState.value.copy(isTextSelectionMenuEnabled = enabled)
        }
    }

    /**
     * Update Text Selection Menu Style
     */
    fun updateTextSelectionMenuStyle(style: com.rr.aido.data.models.SelectionMenuStyle) {
        viewModelScope.launch {
            dataStoreManager.saveTextSelectionMenuStyle(style)
            _uiState.value = _uiState.value.copy(textSelectionMenuStyle = style)
        }
    }

    /**
     * Update @all Menu Order
     */
    fun updateAllMenuOrder(order: List<String>) {
        viewModelScope.launch {
            dataStoreManager.saveAllMenuOrder(order)
            _uiState.value = _uiState.value.copy(allMenuOrder = order)
        }
    }
    
    /**
     * Toggle App Toggle Feature
     */
    fun toggleAppToggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAppToggleEnabled(enabled)
            _uiState.value = _uiState.value.copy(isAppToggleEnabled = enabled)
        }
    }
    
    /**
     * Toggle Streaming Mode
     */
    fun toggleStreamingMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveStreamingModeEnabled(enabled)
            _uiState.value = _uiState.value.copy(isStreamingModeEnabled = enabled)
        }
    }
    
    /**
     * Update Streaming Delay
     */
    fun updateStreamingDelay(delayMs: Int) {
        viewModelScope.launch {
            dataStoreManager.saveStreamingDelayMs(delayMs)
            _uiState.value = _uiState.value.copy(streamingDelayMs = delayMs)
        }
    }
    
    /**
     * Update custom API URL
     */
    fun updateCustomApiUrl(url: String) {
        _uiState.value = _uiState.value.copy(customApiUrl = url)
        viewModelScope.launch {
            dataStoreManager.saveCustomApiUrl(url)
        }
    }

    /**
     * Update custom API key
     */
    fun updateCustomApiKey(key: String) {
        _uiState.value = _uiState.value.copy(customApiKey = key)
        viewModelScope.launch {
            dataStoreManager.saveCustomApiKey(key)
        }
    }

    /**
     * Update custom model name
     */
    fun updateCustomModelName(model: String) {
        _uiState.value = _uiState.value.copy(customModelName = model)
        viewModelScope.launch {
            dataStoreManager.saveCustomModelName(model)
        }
    }
    
    /**
     * Clear test result
     */
    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(
            testResult = null,
            errorMessage = null
        )
    }

    /**
     * Toggle section expansion
     */
    fun toggleSection(sectionTitle: String, isExpanded: Boolean) {
        val currentExpanded = _uiState.value.expandedSections.toMutableMap()
        currentExpanded[sectionTitle] = isExpanded
        _uiState.value = _uiState.value.copy(expandedSections = currentExpanded)
    }
}

/**
 * UI State for Settings screen
 */
data class SettingsUiState(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val selectedModel: String = "gemini-2.5-flash-lite",
    val customApiUrl: String = "https://api.openai.com/v1/",
    val customApiKey: String = "",
    val customModelName: String = "gpt-4o-mini",
    val isServiceEnabled: Boolean = true,
    val isOfflineMode: Boolean = false,
    val isSmartReplyEnabled: Boolean = false,
    val isToneRewriteEnabled: Boolean = false,
    val isAllTriggerEnabled: Boolean = false,
    val isSearchTriggerEnabled: Boolean = false,
    val smartReplyPrompt: String = "",
    val toneRewritePrompt: String = "",
    val smartReplyTrigger: String = "@reply",
    val toneRewriteTrigger: String = "@tone",
    val searchTrigger: String = "@search",
    val searchEngine: com.rr.aido.data.models.SearchEngine = com.rr.aido.data.models.SearchEngine.DUCKDUCKGO,
    val customSearchUrl: String = "",
    val themeMode: com.rr.aido.data.models.ThemeMode = com.rr.aido.data.models.ThemeMode.SYSTEM,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val errorMessage: String? = null,
    val isProcessingAnimationEnabled: Boolean = false,
    val processingAnimationType: com.rr.aido.data.models.ProcessingAnimationType = com.rr.aido.data.models.ProcessingAnimationType.GENTLE_GLOW,
    val isUndoRedoEnabled: Boolean = false,
    val isTextSelectionMenuEnabled: Boolean = true,
    val textSelectionMenuStyle: com.rr.aido.data.models.SelectionMenuStyle = com.rr.aido.data.models.SelectionMenuStyle.GRID,
    val allMenuOrder: List<String> = emptyList(),
    val isAppToggleEnabled: Boolean = false,
    val isStreamingModeEnabled: Boolean = false,
    val streamingDelayMs: Int = 50,
    val expandedSections: Map<String, Boolean> = mapOf(
        "Trigger & Setup" to true, // Default expanded
        "AI Configuration" to false,
        "Features" to false,
        "Keyboard" to false,
        "Privacy & Data" to false,
        "Backup & Restore" to false
    )
)
