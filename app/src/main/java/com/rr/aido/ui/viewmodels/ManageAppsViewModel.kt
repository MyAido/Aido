package com.rr.aido.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.AppInfo
import com.rr.aido.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ManageAppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val disabledApps: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val searchQuery: String = ""
)

class ManageAppsViewModel(
    private val appRepository: AppRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<ManageAppsUiState> = combine(
        _allApps,
        dataStoreManager.disabledAppsFlow,
        _searchQuery,
        _isLoading
    ) { allApps, disabledApps, query, isLoading ->
        val filteredApps = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }

        ManageAppsUiState(
            apps = filteredApps,
            disabledApps = disabledApps,
            isLoading = isLoading,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManageAppsUiState()
    )

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = appRepository.getInstalledApps()
                _allApps.value = apps
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppDisabled(packageName: String, isDisabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.toggleAppDisabled(packageName, isDisabled)
        }
    }
}
