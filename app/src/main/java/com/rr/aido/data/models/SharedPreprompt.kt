package com.rr.aido.data.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class SharedPreprompt(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("trigger")
    val trigger: String = "",

    @SerializedName("instruction")
    val instruction: String = "",

    @SerializedName("example")
    val example: String = "",

    @SerializedName("category")
    val category: PrepromptCategory = PrepromptCategory.GENERAL,

    @SerializedName("authorId")
    val authorId: String = "",

    @SerializedName("authorName")
    val authorName: String = "",

    @SerializedName("title")
    val title: String = "",

    @SerializedName("description")
    val description: String = "",

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    @SerializedName("downloads")
    val downloads: Int = 0,

    @SerializedName("likes")
    val likes: Int = 0,

    @SerializedName("rating")
    val rating: Float = 0f,

    @SerializedName("ratingCount")
    val ratingCount: Int = 0,

    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @SerializedName("isFeatured")
    val isFeatured: Boolean = false,

    @SerializedName("isVerified")
    val isVerified: Boolean = false
) {

    fun toLocalPreprompt(): Preprompt {
        return Preprompt(
            trigger = trigger,
            instruction = instruction,
            example = example
        )
    }

    companion object {

        fun fromLocalPreprompt(
            preprompt: Preprompt,
            authorId: String,
            authorName: String,
            category: PrepromptCategory = PrepromptCategory.GENERAL,
            title: String = "",
            description: String = "",
            tags: List<String> = emptyList()
        ): SharedPreprompt {
            return SharedPreprompt(
                trigger = preprompt.trigger,
                instruction = preprompt.instruction,
                example = preprompt.example,
                authorId = authorId,
                authorName = authorName,
                category = category,
                title = title,
                description = description,
                tags = tags
            )
        }
    }
}

enum class PrepromptCategory {
    @SerializedName("general")
    GENERAL,

    @SerializedName("writing")
    WRITING,

    @SerializedName("communication")
    COMMUNICATION,

    @SerializedName("productivity")
    PRODUCTIVITY,

    @SerializedName("creative")
    CREATIVE,

    @SerializedName("technical")
    TECHNICAL,

    @SerializedName("education")
    EDUCATION,

    @SerializedName("business")
    BUSINESS,

    @SerializedName("social")
    SOCIAL,

    @SerializedName("fun")
    FUN;

    fun getDisplayName(): String {
        return when (this) {
            GENERAL -> "General"
            WRITING -> "Writing"
            COMMUNICATION -> "Communication"
            PRODUCTIVITY -> "Productivity"
            CREATIVE -> "Creative"
            TECHNICAL -> "Technical"
            EDUCATION -> "Education"
            BUSINESS -> "Business"
            SOCIAL -> "Social"
            FUN -> "Fun"
        }
    }
}

data class UserProfile(
    @SerializedName("userId")
    val userId: String = "",

    @SerializedName("username")
    val username: String = "Anonymous",

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,

    @SerializedName("bio")
    val bio: String = "",

    @SerializedName("sharedPrepromptsCount")
    val sharedPrepromptsCount: Int = 0,

    @SerializedName("totalDownloads")
    val totalDownloads: Int = 0,

    // Additional fields for Firebase Auth
    val uid: String = userId,
    val displayName: String = username,
    val photoUrl: String? = avatarUrl,
    val isAnonymous: Boolean = false,

    @SerializedName("joinedAt")
    val joinedAt: Long = System.currentTimeMillis(),

    @SerializedName("isVerified")
    val isVerified: Boolean = false
)

data class PrepromptRating(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("prepromptId")
    val prepromptId: String,

    @SerializedName("rating")
    val rating: Float,

    @SerializedName("review")
    val review: String = "",

    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0,
    val pendingUploads: Int = 0,
    val pendingDownloads: Int = 0,
    val error: String? = null
)

data class MarketplaceFilter(
    val category: PrepromptCategory? = null,
    val sortBy: SortOption = SortOption.POPULAR,
    val minRating: Float = 0f,
    val minDownloads: Int = 0,
    val tags: List<String> = emptyList(),
    val showFeaturedOnly: Boolean = false,
    val showVerifiedOnly: Boolean = false
)

enum class SortOption {
    POPULAR,      // By downloads
    TRENDING,     // By recent downloads
    TOP_RATED,    // By rating
    NEWEST,       // By creation date
    RECENTLY_UPDATED
}
