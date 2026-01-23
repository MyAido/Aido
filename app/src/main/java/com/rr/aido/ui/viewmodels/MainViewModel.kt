package com.rr.aido.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.AiProvider
import com.rr.aido.data.models.Preprompt
import com.rr.aido.data.repository.GeminiRepositoryImpl
import com.rr.aido.data.repository.Result
import com.rr.aido.utils.PromptParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)
    private val geminiRepository = GeminiRepositoryImpl()

    // UI State
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Settings flow
    val settings = dataStoreManager.settingsFlow

    // Preprompts flow
    val preprompts = dataStoreManager.prepromptsFlow

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load settings and preprompts
            val currentSettings = settings.first()
            val currentPreprompts = preprompts.first()

            _uiState.value = _uiState.value.copy(
                isLoading = false
            )
        }
    }

    fun processDemoInput(input: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                demoInput = input,
                isProcessing = true,
                demoOutput = "",
                errorMessage = null
            )

            try {
                val currentPreprompts = preprompts.first()
                val currentSettings = settings.first()

                // Parse input
                val parseResult = PromptParser.parseInput(input, currentPreprompts)

                // Check provider
                val provider = currentSettings.provider
                if (provider == AiProvider.GEMINI && currentSettings.apiKey.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = "Please set your Gemini API key in Settings"
                    )
                    return@launch
                }

                // Send to Gemini
                val result = geminiRepository.sendPrompt(
                    provider = provider,
                    apiKey = currentSettings.apiKey,
                    model = currentSettings.selectedModel,
                    prompt = parseResult.finalPrompt
                )

                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            demoOutput = result.data,
                            parsedTrigger = parseResult.trigger
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = result.message
                        )
                    }
                    else -> {}
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }


    fun clearDemo() {
        _uiState.value = _uiState.value.copy(
            demoInput = "",
            demoOutput = "",
            parsedTrigger = null,
            errorMessage = null
        )
    }


    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.toggleService(enabled)
        }
    }


    fun updateServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveServiceEnabled(enabled)
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val demoInput: String = "",
    val demoOutput: String = "",
    val parsedTrigger: String? = null,
    val errorMessage: String? = null
)
