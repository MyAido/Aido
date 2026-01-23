package com.rr.aido.data.repository

import com.rr.aido.data.models.*
import kotlinx.coroutines.flow.Flow

interface MarketplaceRepository {

    // Browse and search
    suspend fun getFeaturedPreprompts(): Result<List<SharedPreprompt>>
    suspend fun searchPreprompts(query: String, filter: MarketplaceFilter): Result<List<SharedPreprompt>>
    suspend fun getPrepromptsByCategory(category: PrepromptCategory): Result<List<SharedPreprompt>>
    suspend fun getTrendingPreprompts(limit: Int = 20): Result<List<SharedPreprompt>>
    suspend fun getPrepromptById(id: String): Result<SharedPreprompt>

    // User's shared preprompts
    suspend fun getUserSharedPreprompts(userId: String): Result<List<SharedPreprompt>>
    suspend fun uploadPreprompt(preprompt: SharedPreprompt): Result<String>
    suspend fun updatePreprompt(preprompt: SharedPreprompt): Result<Boolean>
    suspend fun deletePreprompt(prepromptId: String): Result<Boolean>

    // Favorites
    suspend fun getFavoritePreprompts(userId: String): Result<List<SharedPreprompt>>
    suspend fun addToFavorites(userId: String, prepromptId: String): Result<Boolean>
    suspend fun removeFromFavorites(userId: String, prepromptId: String): Result<Boolean>
    suspend fun isFavorite(userId: String, prepromptId: String): Result<Boolean>
    suspend fun incrementLikes(prepromptId: String): Result<Boolean>
    suspend fun decrementLikes(prepromptId: String): Result<Boolean>

    // Ratings and reviews
    suspend fun ratePreprompt(rating: PrepromptRating): Result<Boolean>
    suspend fun getPrepromptRatings(prepromptId: String): Result<List<PrepromptRating>>
    suspend fun getUserRating(userId: String, prepromptId: String): Result<PrepromptRating?>

    // Installation tracking
    suspend fun incrementDownloadCount(prepromptId: String): Result<Boolean>

    // User profile
    suspend fun getUserProfile(userId: String): Result<UserProfile>
    suspend fun updateUserProfile(profile: UserProfile): Result<Boolean>

    // Cloud sync
    suspend fun syncUserPreprompts(userId: String, localPreprompts: List<Preprompt>): Result<SyncStatus>
    suspend fun downloadUserPreprompts(userId: String): Result<List<Preprompt>>
    fun observeSyncStatus(userId: String): Flow<SyncStatus>
}
