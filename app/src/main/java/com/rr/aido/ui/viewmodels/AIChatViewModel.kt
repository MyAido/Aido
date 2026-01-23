package com.rr.aido.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.AiProvider
import com.rr.aido.data.models.ChatMessage
import com.rr.aido.data.repository.GeminiRepository
import com.rr.aido.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AIChatViewModel(
    private val dataStoreManager: DataStoreManager,
    private val geminiRepository: GeminiRepository
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    val settings = dataStoreManager.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.rr.aido.data.models.Settings()
    )
    
    init {
        loadChatHistory()
    }
    
    private fun loadChatHistory() {
        viewModelScope.launch {
            dataStoreManager.chatHistoryFlow.collect { history ->
                _messages.value = history
            }
        }
    }
    
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        
        // Add user message
        _messages.value = _messages.value + userMessage
        saveChatHistory()
        
        // Get AI response
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val currentSettings = settings.value
                val provider = currentSettings.provider
                val apiKey = when (provider) {

                    AiProvider.GEMINI -> currentSettings.apiKey
                    AiProvider.CUSTOM -> currentSettings.customApiKey
                }
                
                if (provider == AiProvider.GEMINI && apiKey.isEmpty()) {
                    _errorMessage.value = "Please set your API key in Settings"
                    _isLoading.value = false
                    return@launch
                }
                
                if (provider == AiProvider.CUSTOM && apiKey.isEmpty()) {
                    _errorMessage.value = "Please set your Custom API key in Settings"
                    _isLoading.value = false
                    return@launch
                }
                
                val model = if (provider == AiProvider.CUSTOM) currentSettings.customModelName else currentSettings.selectedModel
                
                val result = geminiRepository.sendPrompt(
                    provider = provider,
                    apiKey = apiKey,
                    model = model,
                    prompt = text,
                    customApiUrl = currentSettings.customApiUrl
                )
                
                when (result) {
                    is Result.Success -> {
                        val aiMessage = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            text = result.data,
                            isUser = false,
                            timestamp = System.currentTimeMillis()
                        )
                        _messages.value = _messages.value + aiMessage
                        saveChatHistory()
                    }
                    is Result.Error -> {
                        _errorMessage.value = result.message
                    }
                    else -> {
                        _errorMessage.value = "Unknown error occurred"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            _messages.value = emptyList()
            dataStoreManager.clearChatHistory()
        }
    }
    
    private fun saveChatHistory() {
        viewModelScope.launch {
            dataStoreManager.saveChatHistory(_messages.value)
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
