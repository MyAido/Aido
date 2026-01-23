package com.rr.aido.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.TextShortcut
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TextShortcutsViewModel(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    val shortcuts: StateFlow<List<TextShortcut>> = dataStoreManager.textShortcutsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addShortcut(trigger: String, replacement: String) {
        viewModelScope.launch {
            val shortcut = TextShortcut(trigger = trigger, replacement = replacement)
            dataStoreManager.addTextShortcut(shortcut)
        }
    }

    fun updateShortcut(shortcut: TextShortcut) {
        viewModelScope.launch {
            dataStoreManager.updateTextShortcut(shortcut)
        }
    }

    fun removeShortcut(id: String) {
        viewModelScope.launch {
            dataStoreManager.removeTextShortcut(id)
        }
    }
}
