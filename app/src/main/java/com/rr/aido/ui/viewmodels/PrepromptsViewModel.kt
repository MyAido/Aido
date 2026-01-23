package com.rr.aido.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.Preprompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * PrepromptsViewModel - Preprompts management ke liye
 * Add, edit, delete preprompts
 */
class PrepromptsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataStoreManager = DataStoreManager(application)
    private val gson = Gson()
    
    // UI State
    private val _uiState = MutableStateFlow(PrepromptsUiState())
    val uiState: StateFlow<PrepromptsUiState> = _uiState.asStateFlow()
    
    // Preprompts flow
    val preprompts = dataStoreManager.prepromptsFlow
    
    /**
     * Show add dialog
     */
    fun showAddDialog() {
        // Set flag to disable trigger detection in trigger input field
        val prefs = getApplication<Application>().getSharedPreferences("aido_prefs", Application.MODE_PRIVATE)
        prefs.edit().putBoolean("is_trigger_dialog_open", true).apply()
        
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingPreprompt = null,
            dialogTrigger = "",
            dialogInstruction = "",
            dialogExample = "",
            errorMessage = null
        )
    }
    
    /**
     * Show edit dialog
     */
    fun showEditDialog(preprompt: Preprompt) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingPreprompt = preprompt,
            dialogTrigger = preprompt.trigger,
            dialogInstruction = preprompt.instruction,
            dialogExample = preprompt.example,
            errorMessage = null
        )
    }
    
    /**
     * Hide dialog
     */
    fun hideDialog() {
        // Clear flag to re-enable trigger detection
        val prefs = getApplication<Application>().getSharedPreferences("aido_prefs", Application.MODE_PRIVATE)
        prefs.edit().putBoolean("is_trigger_dialog_open", false).apply()
        
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            editingPreprompt = null,
            dialogTrigger = "",
            dialogInstruction = "",
            dialogExample = "",
            errorMessage = null
        )
    }
    
    /**
     * Update dialog fields
     */
    fun updateDialogTrigger(trigger: String) {
        _uiState.value = _uiState.value.copy(dialogTrigger = trigger)
    }
    
    fun updateDialogInstruction(instruction: String) {
        _uiState.value = _uiState.value.copy(dialogInstruction = instruction)
    }
    
    fun updateDialogExample(example: String) {
        _uiState.value = _uiState.value.copy(dialogExample = example)
    }
    
    /**
     * Save preprompt (add or edit)
     */
    fun savePreprompt() {
        viewModelScope.launch {
            val trigger = _uiState.value.dialogTrigger.trim()
            val instruction = _uiState.value.dialogInstruction.trim()
            val example = _uiState.value.dialogExample.trim()
            
            // Validation
            if (trigger.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Trigger cannot be empty"
                )
                return@launch
            }
            
            // Check if trigger starts with a valid symbol
            val validSymbols = "`~!@#$%^&*()-_=+[]{}\\|;:'\",<.>/?"
            if (trigger.length < 2 || !validSymbols.contains(trigger.first())) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Trigger must start with a symbol (e.g., @, ~, !, /, #)"
                )
                return@launch
            }
            
            // Check if after symbol there are only word characters
            val afterSymbol = trigger.substring(1)
            if (!afterSymbol.matches(Regex("\\w+"))) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "After symbol, only letters/numbers allowed"
                )
                return@launch
            }
            
            if (instruction.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Instruction cannot be empty"
                )
                return@launch
            }
            
            val currentPreprompts = preprompts.first()
            val editingPreprompt = _uiState.value.editingPreprompt
            
            // Check for duplicate trigger (only when adding new or changing trigger)
            if (editingPreprompt == null || editingPreprompt.trigger != trigger) {
                val exists = currentPreprompts.any { it.trigger == trigger }
                if (exists) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Trigger $trigger already exists"
                    )
                    return@launch
                }
            }
            
            val newPreprompt = Preprompt(
                trigger = trigger,
                instruction = instruction,
                example = example,
                isDefault = editingPreprompt?.isDefault ?: false
            )
            
            if (editingPreprompt != null) {
                // Update existing
                dataStoreManager.updatePreprompt(
                    oldTrigger = editingPreprompt.trigger,
                    newPreprompt = newPreprompt,
                    currentList = currentPreprompts
                )
            } else {
                // Add new
                dataStoreManager.addPreprompt(
                    preprompt = newPreprompt,
                    currentList = currentPreprompts
                )
            }
            
            hideDialog()
        }
    }
    
    /**
     * Delete preprompt
     */
    fun deletePreprompt(preprompt: Preprompt) {
        viewModelScope.launch {
            val currentPreprompts = preprompts.first()
            dataStoreManager.deletePreprompt(
                trigger = preprompt.trigger,
                currentList = currentPreprompts
            )
        }
    }

    /**
     * Reorder preprompts
     */
    fun reorderPreprompts(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentPreprompts = preprompts.first()
            dataStoreManager.reorderPreprompts(
                fromIndex = fromIndex,
                toIndex = toIndex,
                currentList = currentPreprompts
            )
        }
    }
    
    /**
     * Reset to default preprompts
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            dataStoreManager.resetToDefaultPreprompts()
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Generate JSON string of current preprompts for export.
     */
    suspend fun generatePrepromptsJson(): String {
        val currentPreprompts = preprompts.first()
        return gson.toJson(currentPreprompts)
    }

    /**
     * Import preprompts from JSON. Returns error message if validation fails, otherwise null.
     */
    suspend fun importPrepromptsFromJson(json: String): String? {
        return try {
            val type = object : TypeToken<List<Preprompt>>() {}.type
            val imported: List<Preprompt> = gson.fromJson(json, type) ?: return "File format not supported"

            if (imported.isEmpty()) {
                return "No prompts found in file"
            }

            val invalid = imported.firstOrNull { preprompt ->
                preprompt.trigger.isBlank() || !preprompt.trigger.startsWith("@") || preprompt.instruction.isBlank()
            }
            if (invalid != null) {
                val triggerLabel = invalid.trigger.ifBlank { "(blank trigger)" }
                return "Invalid prompt data for $triggerLabel"
            }

            val duplicates = imported.groupBy { it.trigger }.filter { it.value.size > 1 }.keys
            if (duplicates.isNotEmpty()) {
                return "Duplicate triggers found: ${duplicates.joinToString()}"
            }

            dataStoreManager.savePreprompts(imported)
            null
        } catch (e: Exception) {
            "Failed to import prompts: ${e.message ?: "Unknown error"}"
        }
    }

    fun exportPreprompts(onExport: (String) -> Unit) {
        viewModelScope.launch {
            val json = generatePrepromptsJson()
            onExport(json)
        }
    }

    fun importPreprompts(json: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val error = importPrepromptsFromJson(json)
            onResult(error)
        }
    }

    fun toggleReorderMode() {
        _uiState.value = _uiState.value.copy(isReorderMode = !_uiState.value.isReorderMode)
    }
}

/**
 * UI State for Preprompts screen
 */
data class PrepromptsUiState(
    val showAddDialog: Boolean = false,
    val editingPreprompt: Preprompt? = null,
    val dialogTrigger: String = "",
    val dialogInstruction: String = "",
    val dialogExample: String = "",
    val errorMessage: String? = null,
    val isReorderMode: Boolean = false
)
