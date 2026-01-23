package com.rr.aido.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rr.aido.data.DataStoreManager
import com.rr.aido.data.models.*
import com.rr.aido.data.repository.MarketplaceRepository
import com.rr.aido.data.repository.MarketplaceRepositoryImpl
import com.rr.aido.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MarketplaceViewModel(
    private val marketplaceRepository: MarketplaceRepository = MarketplaceRepositoryImpl(),
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(MarketplaceTab.BROWSE)
    val selectedTab: StateFlow<MarketplaceTab> = _selectedTab.asStateFlow()

    private val _filter = MutableStateFlow(MarketplaceFilter())
    val filter: StateFlow<MarketplaceFilter> = _filter.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _showUploadDialog = MutableStateFlow(false)
    val showUploadDialog: StateFlow<Boolean> = _showUploadDialog.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Get current user ID from Firebase Auth
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    // Get current user name
    val currentUserName: String
        get() = _currentUser.value?.displayName ?: "Anonymous"

    init {
        // Check if already authenticated (from previous session)
        updateUserProfile()
        loadFeaturedPreprompts()
        loadFavorites()
        loadAnonymousFavorites()
        observeSyncStatus()
        trackInstalledPreprompts()
        observeAuthState()
    }

    private suspend fun ensureAuthenticated() {
        try {
            if (auth.currentUser == null) {
                // Sign in anonymously only when needed
                auth.signInAnonymously().await()
                Log.d("MarketplaceViewModel", "Signed in anonymously for upload: ${auth.currentUser?.uid}")
                updateUserProfile()
            }
        } catch (e: Exception) {
            Log.e("MarketplaceViewModel", "Anonymous sign-in failed", e)
            throw e
        }
    }

    private fun observeAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            viewModelScope.launch {
                updateUserProfile()
                // Only reload if user is authenticated
                if (auth.currentUser != null) {
                    // If user just signed in (not anonymous), sync favorites
                    if (auth.currentUser?.isAnonymous == false) {
                        syncAnonymousFavoritesToFirebase()
                    }
                    loadUserPreprompts()
                    loadFavorites()
                }
            }
        }
    }

    private fun syncAnonymousFavoritesToFirebase() {
        viewModelScope.launch {
            val anonymousFavorites = dataStoreManager.getAnonymousFavorites()
            if (anonymousFavorites.isNotEmpty()) {
                android.util.Log.d("MarketplaceVM", "Syncing ${anonymousFavorites.size} anonymous favorites to Firebase")
                anonymousFavorites.forEach { prepromptId ->
                    // Check if not already in Firebase favorites
                    val result = marketplaceRepository.isFavorite(currentUserId, prepromptId)
                    if (result is Result.Success && !result.data) {
                        marketplaceRepository.addToFavorites(currentUserId, prepromptId)
                    }
                }
                // Clear anonymous favorites after sync
                dataStoreManager.saveAnonymousFavorites(emptySet())
                loadFavorites()
            }
        }
    }

    private fun updateUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            _currentUser.value = UserProfile(
                uid = user.uid,
                displayName = user.displayName ?: "Anonymous",
                email = user.email,
                photoUrl = user.photoUrl?.toString(),
                isAnonymous = user.isAnonymous
            )
            Log.d("MarketplaceViewModel", "User profile updated: ${user.displayName ?: "Anonymous"} (${if(user.isAnonymous) "Anonymous" else "Logged in"})")
        } else {
            _currentUser.value = null
        }
    }

    fun signInWithEmail(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Check if email exists, if not create account
                try {
                    auth.signInWithEmailAndPassword(email, password).await()
                } catch (e: Exception) {
                    // If sign-in fails, try to create account
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    // Update display name
                    result.user?.updateProfile(
                        com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                    )?.await()
                }

                updateUserProfile()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "✅ Signed in as ${currentUserName}"
                    )
                }
            } catch (e: Exception) {
                Log.e("MarketplaceViewModel", "Email sign-in failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Sign in failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                updateUserProfile()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "✅ Signed in as ${currentUserName}"
                    )
                }
            } catch (e: Exception) {
                Log.e("MarketplaceViewModel", "Google sign-in failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Sign in failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                // Sign in anonymously again
                auth.signInAnonymously().await()
                updateUserProfile()
                _uiState.update {
                    it.copy(successMessage = "Signed out. Continuing as Anonymous")
                }
            } catch (e: Exception) {
                Log.e("MarketplaceViewModel", "Sign out failed", e)
                _uiState.update { it.copy(error = "Sign out failed: ${e.message}") }
            }
        }
    }

    private fun trackInstalledPreprompts() {
        viewModelScope.launch {
            dataStoreManager.prepromptsFlow.collect { preprompts ->
                val triggers = preprompts.map { it.trigger }.toSet()
                _uiState.update { it.copy(installedTriggers = triggers) }
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            marketplaceRepository.observeSyncStatus(currentUserId).collect { status ->
                _syncStatus.value = status
            }
        }
    }

    fun loadFeaturedPreprompts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            android.util.Log.d("MarketplaceVM", "Loading featured preprompts...")

            when (val result = marketplaceRepository.getFeaturedPreprompts()) {
                is Result.Success -> {
                    android.util.Log.d("MarketplaceVM", "Loaded ${result.data.size} preprompts")
                    _uiState.update {
                        it.copy(
                            featuredPreprompts = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    val errorMsg = "Failed to load: ${result.message}"
                    android.util.Log.e("MarketplaceVM", errorMsg, result.exception)
                    _uiState.update {
                        it.copy(
                            error = errorMsg,
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun searchPreprompts(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = marketplaceRepository.searchPreprompts(query, _filter.value)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            searchResults = result.data,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun applyFilter(newFilter: MarketplaceFilter) {
        android.util.Log.d("MarketplaceVM", "Applying filter: category=${newFilter.category?.name}, minDownloads=${newFilter.minDownloads}")
        _filter.value = newFilter
        _uiState.update { it.copy(filter = newFilter) }
        // Always use searchPreprompts (even with empty query) to apply filters
        searchPreprompts(_searchQuery.value)
    }

    fun loadUserPreprompts() {
        viewModelScope.launch {
            // Only load if user is authenticated
            if (auth.currentUser == null) {
                android.util.Log.d("MarketplaceVM", "No authenticated user, skipping user preprompts load")
                _uiState.update { it.copy(userSharedPreprompts = emptyList()) }
                return@launch
            }

            val userId = currentUserId
            val userName = currentUserName
            android.util.Log.d("MarketplaceVM", "Loading user preprompts for: $userId (name: $userName)")
            android.util.Log.d("MarketplaceVM", "Current auth user: ${auth.currentUser?.uid}, isAnonymous: ${auth.currentUser?.isAnonymous}")

            when (val result = marketplaceRepository.getUserSharedPreprompts(userId)) {
                is Result.Success -> {
                    android.util.Log.d("MarketplaceVM", "Loaded ${result.data.size} user preprompts")
                    result.data.forEach { preprompt ->
                        android.util.Log.d("MarketplaceVM", "  - ${preprompt.trigger} (authorId: ${preprompt.authorId}, authorName: ${preprompt.authorName})")
                    }
                    _uiState.update { it.copy(userSharedPreprompts = result.data) }
                }
                is Result.Error -> {
                    android.util.Log.e("MarketplaceVM", "Error loading user preprompts: ${result.message}")
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            when (val result = marketplaceRepository.getFavoritePreprompts(currentUserId)) {
                is Result.Success -> {
                    val favoriteIds = result.data.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            favoritePreprompts = result.data,
                            favoritePrepromptIds = favoriteIds
                        )
                    }
                }
                is Result.Error -> {
                    Log.e("MarketplaceViewModel", "Error loading favorites: ${result.message}")
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun loadAnonymousFavorites() {
        viewModelScope.launch {
            val anonymousFavorites = dataStoreManager.getAnonymousFavorites()
            _uiState.update {
                it.copy(favoritePrepromptIds = it.favoritePrepromptIds + anonymousFavorites)
            }
        }
    }

    fun installPreprompt(sharedPreprompt: SharedPreprompt) {
        viewModelScope.launch {
            try {
                // Convert SharedPreprompt to local Preprompt
                val preprompt = Preprompt(
                    trigger = sharedPreprompt.trigger,
                    instruction = sharedPreprompt.instruction,
                    example = sharedPreprompt.example
                )

                // Get current preprompts and add the new one
                val currentPreprompts = dataStoreManager.prepromptsFlow.first()

                // Check if already exists
                if (currentPreprompts.any { it.trigger == preprompt.trigger }) {
                    _uiState.update { it.copy(error = "Command ${preprompt.trigger} already installed") }
                    return@launch
                }

                // Add to local storage
                val updatedPreprompts = currentPreprompts + preprompt
                dataStoreManager.savePreprompts(updatedPreprompts)

                // Increment download count
                marketplaceRepository.incrementDownloadCount(sharedPreprompt.id)

                _uiState.update {
                    it.copy(
                        error = null,
                        successMessage = "Installed ${preprompt.trigger}"
                    )
                }

                Log.d("MarketplaceViewModel", "Installed preprompt: ${preprompt.trigger}")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Installation failed: ${e.message}") }
                Log.e("MarketplaceViewModel", "Error installing preprompt", e)
            }
        }
    }

    fun toggleFavorite(prepromptId: String) {
        viewModelScope.launch {
            val isAnonymous = auth.currentUser?.isAnonymous == true || auth.currentUser == null

            if (isAnonymous) {
                // For anonymous users, only update likes count on Firebase, store favorites locally
                val isFavorite = _uiState.value.favoritePrepromptIds.contains(prepromptId)

                if (isFavorite) {
                    // Remove from local favorites and decrement likes
                    val newFavorites = _uiState.value.favoritePrepromptIds - prepromptId
                    _uiState.update {
                        it.copy(favoritePrepromptIds = newFavorites)
                    }
                    dataStoreManager.saveAnonymousFavorites(newFavorites)
                    marketplaceRepository.decrementLikes(prepromptId)
                } else {
                    // Add to local favorites and increment likes
                    val newFavorites = _uiState.value.favoritePrepromptIds + prepromptId
                    _uiState.update {
                        it.copy(favoritePrepromptIds = newFavorites)
                    }
                    dataStoreManager.saveAnonymousFavorites(newFavorites)
                    marketplaceRepository.incrementLikes(prepromptId)
                }
            } else {
                // For signed-in users, use Firebase favorites
                val isFavorite = when (val result = marketplaceRepository.isFavorite(currentUserId, prepromptId)) {
                    is Result.Success -> result.data
                    else -> false
                }

                val result = if (isFavorite) {
                    marketplaceRepository.removeFromFavorites(currentUserId, prepromptId)
                } else {
                    marketplaceRepository.addToFavorites(currentUserId, prepromptId)
                }

                when (result) {
                    is Result.Success -> {
                        loadFavorites()
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    fun uploadPreprompt(preprompt: Preprompt, title: String, description: String, category: PrepromptCategory) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Ensure user is authenticated before upload
            try {
                ensureAuthenticated()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Authentication failed: ${e.message}",
                        isLoading = false
                    )
                }
                return@launch
            }

            android.util.Log.d("MarketplaceVM", "Uploading preprompt: ${preprompt.trigger}")

            val sharedPreprompt = SharedPreprompt(
                id = "", // Firestore will auto-generate
                trigger = preprompt.trigger,
                instruction = preprompt.instruction,
                example = preprompt.example,
                category = category,
                authorId = currentUserId,
                authorName = currentUserName,
                title = title.ifBlank { preprompt.trigger },
                description = description.ifBlank { preprompt.instruction.take(100) },
                tags = emptyList(),
                downloads = 0,
                rating = 0f,
                ratingCount = 0,
                isFeatured = false,
                isVerified = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            android.util.Log.d("MarketplaceVM", "Uploading with authorId: ${sharedPreprompt.authorId}, authorName: ${sharedPreprompt.authorName}")

            when (val result = marketplaceRepository.uploadPreprompt(sharedPreprompt)) {
                is Result.Success -> {
                    android.util.Log.d("MarketplaceVM", "Upload successful! ID: ${result.data}")
                    _uiState.update {
                        it.copy(
                            successMessage = "✅ Successfully shared ${preprompt.trigger}!",
                            isLoading = false
                        )
                    }
                    // Refresh both lists
                    loadUserPreprompts()
                    loadFeaturedPreprompts()
                }
                is Result.Error -> {
                    android.util.Log.e("MarketplaceVM", "Upload failed: ${result.message}")
                    _uiState.update {
                        it.copy(
                            error = "Upload failed: ${result.message}",
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun updatePreprompt(prepromptId: String, title: String, description: String, category: PrepromptCategory) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Get existing preprompt to update
            val existingPreprompts = _uiState.value.userSharedPreprompts + _uiState.value.featuredPreprompts
            val existing = existingPreprompts.find { it.id == prepromptId }

            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    description = description,
                    category = category,
                    updatedAt = System.currentTimeMillis()
                )

                when (val result = marketplaceRepository.updatePreprompt(updated)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                successMessage = "✅ Updated successfully!",
                                isLoading = false
                            )
                        }
                        loadUserPreprompts()
                        loadFeaturedPreprompts()
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                error = "Update failed: ${result.message}",
                                isLoading = false
                            )
                        }
                    }
                    is Result.Loading -> {}
                }
            } else {
                _uiState.update {
                    it.copy(
                        error = "Preprompt not found",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deletePreprompt(prepromptId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = marketplaceRepository.deletePreprompt(prepromptId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            successMessage = "Deleted successfully",
                            isLoading = false
                        )
                    }
                    loadUserPreprompts()
                    loadFeaturedPreprompts()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            error = "Delete failed: ${result.message}",
                            isLoading = false
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun syncPreprompts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val localPreprompts = dataStoreManager.prepromptsFlow.first()

            when (val result = marketplaceRepository.syncUserPreprompts(currentUserId, localPreprompts)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Sync completed"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun selectTab(tab: MarketplaceTab) {
        _selectedTab.value = tab
        // Refresh data when tab is selected
        when (tab) {
            MarketplaceTab.BROWSE -> loadFeaturedPreprompts()
            MarketplaceTab.MY_SHARED -> loadUserPreprompts()
            MarketplaceTab.FAVORITES -> loadFavorites()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun showUploadDialog() {
        _showUploadDialog.value = true
    }

    fun hideUploadDialog() {
        _showUploadDialog.value = false
    }

    suspend fun getLocalPreprompts(): List<Preprompt> {
        return dataStoreManager.prepromptsFlow.first()
    }
}

data class MarketplaceUiState(
    val featuredPreprompts: List<SharedPreprompt> = emptyList(),
    val searchResults: List<SharedPreprompt> = emptyList(),
    val userSharedPreprompts: List<SharedPreprompt> = emptyList(),
    val favoritePreprompts: List<SharedPreprompt> = emptyList(),
    val favoritePrepromptIds: Set<String> = emptySet(),
    val installedTriggers: Set<String> = emptySet(),
    val filter: MarketplaceFilter? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

enum class MarketplaceTab {
    BROWSE,
    MY_SHARED,
    FAVORITES
}
