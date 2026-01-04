# Marketplace Feature - Implementation Guide

## 🎯 Overview

The **Preprompt Marketplace** is a community-driven feature that enables users to:
- **Discover** preprompts created by other users
- **Share** their custom commands with the community  
- **Sync** preprompts across multiple devices
- **Rate & Review** commands to help others find the best ones
- **Browse by Category** to find relevant preprompts quickly

---

## 📁 File Structure

### New Files Created

```
app/src/main/java/com/rr/aido/
├── data/
│   ├── models/
│   │   └── SharedPreprompt.kt          # Marketplace data models
│   └── repository/
│       ├── MarketplaceRepository.kt     # Repository interface
│       └── MarketplaceRepositoryImpl.kt # Repository implementation
├── service/
│   └── PrepromptSyncService.kt         # Background sync service
├── ui/
│   ├── screens/
│   │   └── MarketplaceScreen.kt        # Main marketplace UI
│   └── viewmodels/
│       └── MarketplaceViewModel.kt     # Marketplace logic
└── utils/
    └── AuthManager.kt                  # User authentication
```

### Modified Files

- `MainActivity.kt` - Added marketplace navigation route
- `HomeScreen.kt` - Added featured marketplace card and quick action button
- `AndroidManifest.xml` - Added sync service and permissions

---

## 🚀 Features Implemented

### 1. **Marketplace Data Models** (`SharedPreprompt.kt`)

**Key Classes:**
- `SharedPreprompt` - Complete marketplace preprompt with metadata
- `PrepromptCategory` - 10 categories (Writing, Technical, Business, etc.)
- `UserProfile` - User information and statistics
- `PrepromptRating` - User ratings and reviews
- `SyncStatus` - Cloud sync state tracking
- `MarketplaceFilter` - Search and filter options

**Example:**
```kotlin
val sharedPreprompt = SharedPreprompt(
    trigger = "@translate",
    instruction = "Translate text to specified language",
    category = PrepromptCategory.COMMUNICATION,
    authorId = "user123",
    authorName = "LanguagePro",
    description = "Instantly translate between languages",
    tags = listOf("translation", "language"),
    downloads = 15420,
    rating = 4.8f,
    isFeatured = true
)
```

---

### 2. **Repository Layer** (`MarketplaceRepository.kt`)

**Main Operations:**

#### Browse & Search
```kotlin
suspend fun getFeaturedPreprompts(): Result<List<SharedPreprompt>>
suspend fun searchPreprompts(query: String, filter: MarketplaceFilter): Result<List<SharedPreprompt>>
suspend fun getPrepromptsByCategory(category: PrepromptCategory): Result<List<SharedPreprompt>>
suspend fun getTrendingPreprompts(limit: Int): Result<List<SharedPreprompt>>
```

#### User Content
```kotlin
suspend fun getUserSharedPreprompts(userId: String): Result<List<SharedPreprompt>>
suspend fun uploadPreprompt(preprompt: SharedPreprompt): Result<String>
suspend fun updatePreprompt(preprompt: SharedPreprompt): Result<Boolean>
suspend fun deletePreprompt(prepromptId: String): Result<Boolean>
```

#### Favorites & Ratings
```kotlin
suspend fun addToFavorites(userId: String, prepromptId: String): Result<Boolean>
suspend fun ratePreprompt(rating: PrepromptRating): Result<Boolean>
```

#### Cloud Sync
```kotlin
suspend fun syncUserPreprompts(userId: String, localPreprompts: List<Preprompt>): Result<SyncStatus>
fun observeSyncStatus(userId: String): Flow<SyncStatus>
```

**Current Implementation:**
- Uses mock data for demonstration
- Ready for backend API integration
- Includes realistic featured preprompts
- Implements filtering and sorting logic

---

### 3. **Marketplace UI** (`MarketplaceScreen.kt`)

**Three Main Tabs:**

#### 📱 Browse Tab
- Search bar with real-time filtering
- Featured preprompts section
- Preprompt cards with:
  - Trigger and description
  - Category and tags
  - Download count and ratings
  - Author information
  - Verified badges for quality content
  - Install button
  - Favorite toggle

#### ☁️ My Shared Tab
- List of user's uploaded preprompts
- Statistics (downloads, ratings)
- "Share New Command" button
- Edit/delete options (ready for implementation)

#### ⭐ Favorites Tab
- Quick access to favorited preprompts
- One-click installation
- Remove from favorites option

**UI Components:**
- `PrepromptCard` - Display individual preprompts
- `CategoryChip` - Visual category labels
- `TagChip` - Searchable tags
- `FilterDialog` - Advanced filtering options
- `EmptyState` - Helpful empty state messages

---

### 4. **ViewModel** (`MarketplaceViewModel.kt`)

**State Management:**
```kotlin
data class MarketplaceUiState(
    val featuredPreprompts: List<SharedPreprompt> = emptyList(),
    val searchResults: List<SharedPreprompt> = emptyList(),
    val userSharedPreprompts: List<SharedPreprompt> = emptyList(),
    val favoritePreprompts: List<SharedPreprompt> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

**Key Functions:**
- `searchPreprompts(query)` - Search with filters
- `installPreprompt(preprompt)` - Install to local commands
- `sharePreprompt(preprompt)` - Upload to marketplace
- `toggleFavorite(preprompt)` - Add/remove favorites
- `ratePreprompt(id, rating)` - Submit ratings
- `syncPreprompts()` - Manual sync trigger

---

### 5. **Cloud Sync Service** (`PrepromptSyncService.kt`)

**Background Synchronization:**
- Runs as foreground service with notification
- Periodic sync every hour (configurable)
- Manual sync on-demand
- Conflict resolution (local + cloud merge)
- Battery-efficient implementation

**Usage:**
```kotlin
// Start manual sync
PrepromptSyncService.startSync(context)

// Enable auto-sync
PrepromptSyncService.startAutoSync(context)

// Stop sync
PrepromptSyncService.stopSync(context)
```

**Features:**
- Uploads local preprompts not on server
- Downloads cloud preprompts not local
- Prevents duplicates by trigger name
- Shows notification with sync status

---

### 6. **Authentication** (`AuthManager.kt`)

**Simple Authentication System:**
```kotlin
// Sign in
authManager.signIn(email, password)

// Sign up
authManager.signUp(username, email, password)

// Guest mode
authManager.signInAsGuest()

// Get current user
val user = authManager.getCurrentUser()
```

**Current Implementation:**
- Mock authentication for development
- Ready for Firebase Auth integration
- Stores user data in SharedPreferences
- Guest mode supported

---

## 🎨 UI/UX Highlights

### Home Screen Integration

**Featured Marketplace Card:**
- Shows 3 trending preprompts
- Download counts
- Direct link to marketplace

**Quick Actions:**
- Added "Explore Marketplace" button
- Full-width call-to-action
- Prominent placement for discoverability

### Marketplace Screen

**Top Bar:**
- Back button
- Sync status indicator
- Filter button with active filter badge

**Search:**
- Real-time search as you type
- Searches triggers, descriptions, and tags
- Clear button for quick reset

**Filtering:**
- Category selection (10 categories)
- Sort options: Popular, Top Rated, Newest, Trending
- Minimum rating filter
- Featured/Verified only toggles

**Preprompt Cards:**
- Clean, modern design
- Essential information at a glance
- Visual hierarchy (trigger → description → stats)
- One-click installation
- Star rating display

---

## 🔧 Backend Integration Guide

### API Endpoints Needed

```
GET    /api/v1/preprompts/featured
GET    /api/v1/preprompts/search?q={query}&category={cat}&sort={sort}
GET    /api/v1/preprompts/{id}
POST   /api/v1/preprompts
PUT    /api/v1/preprompts/{id}
DELETE /api/v1/preprompts/{id}

GET    /api/v1/users/{userId}/preprompts
GET    /api/v1/users/{userId}/favorites
POST   /api/v1/users/{userId}/favorites/{prepromptId}
DELETE /api/v1/users/{userId}/favorites/{prepromptId}

POST   /api/v1/ratings
GET    /api/v1/preprompts/{id}/ratings

GET    /api/v1/sync/{userId}
POST   /api/v1/sync/{userId}
```

### Replace Mock Data

In `MarketplaceRepositoryImpl.kt`, update methods to call real API:

```kotlin
override suspend fun getFeaturedPreprompts(): Result<List<SharedPreprompt>> {
    return try {
        val response = client.newCall(
            Request.Builder()
                .url("$baseUrl/preprompts/featured")
                .get()
                .build()
        ).execute()
        
        if (response.isSuccessful) {
            val json = response.body?.string()
            val preprompts = gson.fromJson(json, Array<SharedPreprompt>::class.java).toList()
            Result.success(preprompts)
        } else {
            Result.failure(Exception("HTTP ${response.code}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 🔐 Security Considerations

### Current State (Development)
- Mock authentication
- No encryption for stored data
- Demo API endpoints

### Production Requirements

1. **Authentication:**
   - Integrate Firebase Auth or OAuth 2.0
   - Secure token storage (Android Keystore)
   - Session management

2. **Data Encryption:**
   - Encrypt user credentials
   - HTTPS for all API calls
   - Certificate pinning

3. **API Security:**
   - API key rotation
   - Rate limiting
   - Input validation and sanitization

4. **Privacy:**
   - GDPR compliance
   - User consent for data sharing
   - Option to make preprompts private

---

## 📊 Analytics & Monitoring

### Recommended Metrics

**User Engagement:**
- Marketplace visits per user
- Search queries performed
- Preprompts installed
- Preprompts shared

**Content Quality:**
- Average rating per preprompt
- Download trends
- Report/flag frequency

**Sync Performance:**
- Sync success rate
- Sync duration
- Conflict resolution frequency

---

## 🚦 Testing Strategy

### Unit Tests
- Repository methods
- ViewModel logic
- Data model serialization
- Filter and sort algorithms

### UI Tests
- Search functionality
- Install flow
- Tab navigation
- Filter dialog

### Integration Tests
- API calls (with mock server)
- Sync service behavior
- Authentication flow

---

## 🎯 Future Enhancements

### Phase 2 Features

1. **Advanced Search:**
   - Fuzzy matching
   - Multi-tag filtering
   - Author search
   - Date range filters

2. **Social Features:**
   - Follow creators
   - Comment on preprompts
   - Share to social media
   - Preprompt collections/playlists

3. **Content Moderation:**
   - Report inappropriate content
   - Admin review dashboard
   - Automated spam detection

4. **Analytics Dashboard:**
   - Creator insights
   - Download graphs
   - User demographics
   - Popular categories

5. **Monetization (Optional):**
   - Premium preprompts
   - Creator subscriptions
   - Ad-free experience

6. **Collaboration:**
   - Fork and remix preprompts
   - Version history
   - Collaborative editing

---

## 🐛 Known Limitations

1. **Mock Data:** Currently uses hardcoded preprompts for demonstration
2. **No Real Auth:** Authentication is simulated
3. **Local Favorites:** Favorite status not persisted to backend
4. **No Image Support:** Preprompt cards are text-only
5. **Basic Conflict Resolution:** Last-write-wins sync strategy

---

## 📝 Configuration

### Sync Interval
Change in `PrepromptSyncService.kt`:
```kotlin
private val syncInterval = 60 * 60 * 1000L // 1 hour in milliseconds
```

### API Base URL
Update in `MarketplaceRepositoryImpl.kt`:
```kotlin
private val baseUrl = "https://aido-marketplace-api.example.com/api/v1"
```

### Featured Preprompt Limit
Modify in `MarketplaceViewModel.kt`:
```kotlin
marketplaceRepository.getTrendingPreprompts(limit = 20)
```

---

## 🎓 Usage Examples

### For End Users

**Installing a Preprompt:**
1. Open Marketplace from Home screen
2. Browse or search for preprompts
3. Tap "Install Command" on desired preprompt
4. Use immediately with the trigger

**Sharing a Preprompt:**
1. Go to "My Shared" tab
2. Tap "Share New Command"
3. Fill in details (category, description, tags)
4. Submit to marketplace

**Syncing Across Devices:**
1. Sign in with same account on multiple devices
2. Tap sync icon in Marketplace
3. Preprompts automatically merge

### For Developers

**Adding Categories:**
```kotlin
enum class PrepromptCategory {
    // Add new category here
    CUSTOM_CATEGORY,
    
    fun getDisplayName(): String {
        return when (this) {
            CUSTOM_CATEGORY -> "Custom Category"
            // ...
        }
    }
}
```

**Custom Filters:**
```kotlin
val customFilter = MarketplaceFilter(
    category = PrepromptCategory.TECHNICAL,
    sortBy = SortOption.TOP_RATED,
    minRating = 4.5f,
    showVerifiedOnly = true
)
viewModel.updateFilter(customFilter)
```

---

## 🤝 Contributing

When adding new marketplace features:

1. Update data models in `SharedPreprompt.kt`
2. Add repository methods in `MarketplaceRepository.kt`
3. Implement in `MarketplaceRepositoryImpl.kt`
4. Update ViewModel logic
5. Create/modify UI components
6. Add tests
7. Update this documentation

---

## 📞 Support & Troubleshooting

### Common Issues

**"Sync failed" error:**
- Check internet connection
- Verify API endpoint is reachable
- Check authentication token validity

**"Command already exists" on install:**
- Preprompt with same trigger already installed
- Edit existing or remove before installing

**Empty marketplace:**
- Ensure API returns data
- Check network permissions
- Verify mock data is loaded

---

## 📜 License & Credits

This marketplace feature is part of the Aido project and follows the same licensing terms.

**Technologies Used:**
- Jetpack Compose for UI
- Kotlin Coroutines for async operations
- Retrofit for networking
- DataStore for local persistence
- Material 3 design components

---

## ✅ Implementation Checklist

- [x] Data models created
- [x] Repository interface defined
- [x] Repository implementation with mock data
- [x] ViewModel with state management
- [x] UI screens and components
- [x] Background sync service
- [x] Authentication manager
- [x] Home screen integration
- [x] Navigation setup
- [x] Manifest permissions
- [ ] Backend API integration (pending)
- [ ] Real authentication (pending)
- [ ] Unit tests (pending)
- [ ] UI tests (pending)

---

**Last Updated:** November 16, 2025  
**Version:** 1.0.0  
**Status:** Core implementation complete, ready for backend integration
