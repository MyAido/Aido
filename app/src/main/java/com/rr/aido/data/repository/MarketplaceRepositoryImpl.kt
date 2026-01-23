package com.rr.aido.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rr.aido.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class MarketplaceRepositoryImpl : MarketplaceRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val prepromptsCollection = firestore.collection("marketplace_preprompts")
    private val syncStatusFlow = MutableStateFlow(SyncStatus())
    private val mockFavorites = mutableSetOf<String>()

    companion object {
        private const val TAG = "MarketplaceRepo"
    }

    override suspend fun getFeaturedPreprompts(): Result<List<SharedPreprompt>> {
        return try {
            Log.d(TAG, "Fetching featured preprompts from Firestore")
            val snapshot = prepromptsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            val preprompts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SharedPreprompt::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing preprompt: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Loaded ${preprompts.size} featured preprompts")
            Result.Success(preprompts)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load featured preprompts", e)
            Result.Error("Failed to load featured preprompts: ${e.message}", e)
        }
    }

    override suspend fun searchPreprompts(query: String, filter: MarketplaceFilter): Result<List<SharedPreprompt>> {
        return try {
            Log.d(TAG, "Searching preprompts: query='$query', category=${filter.category?.name}, minDownloads=${filter.minDownloads}")

            var firestoreQuery: Query = prepromptsCollection

            // Apply category filter
            filter.category?.let { category ->
                Log.d(TAG, "Applying category filter: ${category.name}")
                firestoreQuery = firestoreQuery.whereEqualTo("category", category.name)
            }

            // Apply verified filter
            if (filter.showVerifiedOnly) {
                firestoreQuery = firestoreQuery.whereEqualTo("isVerified", true)
            }

            // Apply featured filter
            if (filter.showFeaturedOnly) {
                firestoreQuery = firestoreQuery.whereEqualTo("isFeatured", true)
            }

            val snapshot = firestoreQuery.get().await()
            var results = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SharedPreprompt::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }

            // Client-side filtering for text search
            if (query.isNotBlank()) {
                results = results.filter {
                    it.trigger.contains(query, ignoreCase = true) ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.instruction.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }

            // Apply downloads filter
            if (filter.minDownloads > 0) {
                Log.d(TAG, "Applying minDownloads filter: ${filter.minDownloads}")
                results = results.filter { it.downloads >= filter.minDownloads }
            }

            // Apply rating filter
            filter.minRating?.let { minRating ->
                results = results.filter { it.rating >= minRating }
            }

            // Apply sorting
            results = when (filter.sortBy) {
                SortOption.POPULAR -> results.sortedByDescending { it.downloads }
                SortOption.TOP_RATED -> results.sortedByDescending { it.rating }
                SortOption.NEWEST -> results.sortedByDescending { it.createdAt }
                SortOption.TRENDING -> results.sortedByDescending { it.downloads + it.rating * 100 }
                SortOption.RECENTLY_UPDATED -> results.sortedByDescending { it.updatedAt }
            }

            Log.d(TAG, "Found ${results.size} matching preprompts after all filters")
            Result.Success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.Error("Search failed: ${e.message}", e)
        }
    }

    override suspend fun getPrepromptsByCategory(category: PrepromptCategory): Result<List<SharedPreprompt>> {
        return try {
            val snapshot = prepromptsCollection
                .whereEqualTo("category", category.name)
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SharedPreprompt::class.java)?.copy(id = doc.id)
            }
            Result.Success(results)
        } catch (e: Exception) {
            Result.Error("Failed to load category: ${e.message}", e)
        }
    }

    override suspend fun getTrendingPreprompts(limit: Int): Result<List<SharedPreprompt>> {
        return try {
            val snapshot = prepromptsCollection
                .orderBy("downloads", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val trending = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SharedPreprompt::class.java)?.copy(id = doc.id)
            }
            Result.Success(trending)
        } catch (e: Exception) {
            Result.Error("Failed to load trending: ${e.message}", e)
        }
    }

    override suspend fun getPrepromptById(id: String): Result<SharedPreprompt> {
        return try {
            val doc = prepromptsCollection.document(id).get().await()
            val preprompt = doc.toObject(SharedPreprompt::class.java)?.copy(id = doc.id)
                ?: return Result.Error("Preprompt not found")
            Result.Success(preprompt)
        } catch (e: Exception) {
            Result.Error("Failed to load preprompt: ${e.message}", e)
        }
    }

    override suspend fun getUserSharedPreprompts(userId: String): Result<List<SharedPreprompt>> {
        return try {
            Log.d(TAG, "Fetching user preprompts for: $userId")
            val snapshot = prepromptsCollection
                .whereEqualTo("authorId", userId)
                .get()
                .await()

            val userPreprompts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SharedPreprompt::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.createdAt }  // Sort in memory instead

            Log.d(TAG, "Found ${userPreprompts.size} user preprompts")
            Result.Success(userPreprompts)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user preprompts", e)
            Result.Error("Failed to load user preprompts: ${e.message}", e)
        }
    }

    override suspend fun uploadPreprompt(preprompt: SharedPreprompt): Result<String> {
        return try {
            Log.d(TAG, "Uploading preprompt: ${preprompt.trigger}")

            // Add to Firestore
            val docRef = prepromptsCollection.add(preprompt).await()
            val documentId = docRef.id

            Log.d(TAG, "Successfully uploaded preprompt with ID: $documentId")
            Result.Success(documentId)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            Result.Error("Upload failed: ${e.message}", e)
        }
    }

    override suspend fun updatePreprompt(preprompt: SharedPreprompt): Result<Boolean> {
        return try {
            prepromptsCollection.document(preprompt.id)
                .set(preprompt.copy(updatedAt = System.currentTimeMillis()))
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Update failed: ${e.message}", e)
        }
    }

    override suspend fun deletePreprompt(prepromptId: String): Result<Boolean> {
        return try {
            prepromptsCollection.document(prepromptId).delete().await()
            Log.d(TAG, "Deleted preprompt: $prepromptId")
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Delete failed: ${e.message}", e)
        }
    }

    override suspend fun getFavoritePreprompts(userId: String): Result<List<SharedPreprompt>> {
        return try {
            Log.d(TAG, "Fetching favorites for user: $userId")

            // Get list of favorite IDs from user's subcollection
            val favoritesSnapshot = firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .await()

            val favoriteIds = favoritesSnapshot.documents.map { it.id }

            if (favoriteIds.isEmpty()) {
                return Result.Success(emptyList())
            }

            // Fetch the actual preprompts
            // Firestore has a limit of 10 items in 'in' queries, so we batch them
            val favorites = mutableListOf<SharedPreprompt>()
            favoriteIds.chunked(10).forEach { chunk ->
                val snapshot = prepromptsCollection
                    .whereIn("__name__", chunk)
                    .get()
                    .await()

                snapshot.documents.mapNotNullTo(favorites) { doc ->
                    doc.toObject(SharedPreprompt::class.java)?.copy(id = doc.id)
                }
            }

            Result.Success(favorites)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load favorites", e)
            Result.Error("Failed to load favorites: ${e.message}", e)
        }
    }

    override suspend fun addToFavorites(userId: String, prepromptId: String): Result<Boolean> {
        return try {
            // Add to user's favorites
            firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document(prepromptId)
                .set(mapOf("addedAt" to System.currentTimeMillis()))
                .await()

            // Increment likes count on the preprompt
            prepromptsCollection.document(prepromptId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Failed to add favorite: ${e.message}", e)
        }
    }

    override suspend fun removeFromFavorites(userId: String, prepromptId: String): Result<Boolean> {
        return try {
            // Remove from user's favorites
            firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document(prepromptId)
                .delete()
                .await()

            // Decrement likes count on the preprompt
            prepromptsCollection.document(prepromptId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()

            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Failed to remove favorite: ${e.message}", e)
        }
    }

    override suspend fun isFavorite(userId: String, prepromptId: String): Result<Boolean> {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("favorites")
                .document(prepromptId)
                .get()
                .await()
            Result.Success(doc.exists())
        } catch (e: Exception) {
            Result.Error("Failed to check favorite: ${e.message}", e)
        }
    }

    override suspend fun incrementLikes(prepromptId: String): Result<Boolean> {
        return try {
            prepromptsCollection.document(prepromptId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment likes", e)
            Result.Error("Failed to update likes: ${e.message}", e)
        }
    }

    override suspend fun decrementLikes(prepromptId: String): Result<Boolean> {
        return try {
            prepromptsCollection.document(prepromptId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrement likes", e)
            Result.Error("Failed to update likes: ${e.message}", e)
        }
    }

    override suspend fun ratePreprompt(rating: PrepromptRating): Result<Boolean> {
        return try {
            delay(300)
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Rating failed", e)
        }
    }

    override suspend fun getPrepromptRatings(prepromptId: String): Result<List<PrepromptRating>> {
        return try {
            delay(300)
            Result.Success(emptyList())
        } catch (e: Exception) {
            Result.Error("Failed to load ratings", e)
        }
    }

    override suspend fun getUserRating(userId: String, prepromptId: String): Result<PrepromptRating?> {
        return try {
            Result.Success(null)
        } catch (e: Exception) {
            Result.Error("Failed to get rating", e)
        }
    }

    override suspend fun incrementDownloadCount(prepromptId: String): Result<Boolean> {
        return try {
            val docRef = prepromptsCollection.document(prepromptId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentDownloads = snapshot.getLong("downloads") ?: 0
                transaction.update(docRef, "downloads", currentDownloads + 1)
            }.await()
            Result.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment download count", e)
            Result.Error("Failed to update count: ${e.message}", e)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            delay(300)
            val profile = UserProfile(
                userId = userId,
                username = "DemoUser",
                email = "user${userId}@example.com",
                avatarUrl = null,
                bio = "Aido marketplace user",
                sharedPrepromptsCount = 5,
                totalDownloads = 1000,
                joinedAt = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000),
                isVerified = false
            )
            Result.Success(profile)
        } catch (e: Exception) {
            Result.Error("Failed to load profile", e)
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<Boolean> {
        return try {
            delay(300)
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error("Failed to update profile", e)
        }
    }

    override suspend fun syncUserPreprompts(userId: String, localPreprompts: List<Preprompt>): Result<SyncStatus> {
        return try {
            delay(1000)
            val status = SyncStatus(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                pendingUploads = 0,
                pendingDownloads = 0,
                error = null
            )
            syncStatusFlow.value = status
            Result.Success(status)
        } catch (e: Exception) {
            Result.Error("Sync failed", e)
        }
    }

    override suspend fun downloadUserPreprompts(userId: String): Result<List<Preprompt>> {
        return try {
            Log.d(TAG, "Downloading user preprompts for: $userId")
            val snapshot = prepromptsCollection
                .whereEqualTo("authorId", userId)
                .get()
                .await()

            // Convert SharedPreprompt to Preprompt format
            val preprompts = snapshot.documents.mapNotNull { doc ->
                try {
                    val shared = doc.toObject(SharedPreprompt::class.java)?.copy(id = doc.id)
                    shared?.let {
                        Preprompt(
                            trigger = it.trigger,
                            instruction = it.instruction,
                            example = it.example
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }

            Log.d(TAG, "Downloaded ${preprompts.size} preprompts")
            Result.Success(preprompts)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.Error("Download failed: ${e.message}", e)
        }
    }

    override fun observeSyncStatus(userId: String): Flow<SyncStatus> {
        return syncStatusFlow
    }
}
