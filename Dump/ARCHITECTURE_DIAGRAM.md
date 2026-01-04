# Marketplace Feature Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  HomeScreen  │  │ Marketplace  │  │   Settings   │        │
│  │              │  │    Screen    │  │    Screen    │        │
│  │ • Featured   │  │ • Browse     │  │ • Provider   │        │
│  │   Card       │  │ • My Shared  │  │ • API Key    │        │
│  │ • Quick      │  │ • Favorites  │  │ • Sync       │        │
│  │   Actions    │  │ • Search     │  │              │        │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘        │
│         │                  │                                    │
└─────────┼──────────────────┼────────────────────────────────────┘
          │                  │
          ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      VIEWMODEL LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │            MarketplaceViewModel                         │  │
│  │                                                         │  │
│  │  State:                      Actions:                  │  │
│  │  • featuredPreprompts        • searchPreprompts()      │  │
│  │  • searchResults             • installPreprompt()      │  │
│  │  • userSharedPreprompts      • sharePreprompt()        │  │
│  │  • favoritePreprompts        • toggleFavorite()        │  │
│  │  • isLoading                 • ratePreprompt()         │  │
│  │  • syncStatus                • syncPreprompts()        │  │
│  └────────────────────┬────────────────────────────────────┘  │
│                       │                                        │
└───────────────────────┼────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │         MarketplaceRepositoryImpl                       │  │
│  │                                                         │  │
│  │  Browse & Search:            User Content:             │  │
│  │  • getFeaturedPreprompts()   • getUserSharedPreprompts() │ │
│  │  • searchPreprompts()        • uploadPreprompt()       │  │
│  │  • getPrepromptsByCategory() • updatePreprompt()       │  │
│  │  • getTrendingPreprompts()   • deletePreprompt()       │  │
│  │                                                         │  │
│  │  Favorites:                  Cloud Sync:               │  │
│  │  • getFavoritePreprompts()   • syncUserPreprompts()    │  │
│  │  • addToFavorites()          • downloadUserPreprompts()│  │
│  │  • removeFromFavorites()     • observeSyncStatus()     │  │
│  │                                                         │  │
│  │  Ratings:                                              │  │
│  │  • ratePreprompt()                                     │  │
│  │  • getPrepromptRatings()                               │  │
│  └────────────────────┬────────────────────────────────────┘  │
│                       │                                        │
└───────────────────────┼────────────────────────────────────────┘
                        │
            ┌───────────┴───────────┐
            │                       │
            ▼                       ▼
┌─────────────────────┐   ┌─────────────────────┐
│   DATA LAYER        │   │  BACKGROUND SERVICE │
├─────────────────────┤   ├─────────────────────┤
│                     │   │                     │
│  ┌──────────────┐  │   │ PrepromptSyncService│
│  │ DataStore    │  │   │                     │
│  │ Manager      │  │   │ • Periodic Sync     │
│  │              │  │   │ • Manual Sync       │
│  │ • Preprompts │  │   │ • Notification      │
│  │ • Settings   │  │   │ • Conflict Resolve  │
│  │ • API Keys   │  │   │                     │
│  └──────────────┘  │   └──────────┬──────────┘
│                     │              │
│  ┌──────────────┐  │              │
│  │ AuthManager  │  │              │
│  │              │  │              │
│  │ • Sign In    │  │              │
│  │ • Sign Up    │  │              │
│  │ • Guest Mode │  │              │
│  └──────────────┘  │              │
│                     │              │
└─────────────────────┘              │
                                     │
            ┌────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BACKEND API (Future)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Endpoints:                                                     │
│  • GET  /api/v1/preprompts/featured                            │
│  • GET  /api/v1/preprompts/search?q={query}                    │
│  • POST /api/v1/preprompts                                     │
│  • PUT  /api/v1/preprompts/{id}                                │
│  • GET  /api/v1/users/{userId}/preprompts                      │
│  • POST /api/v1/users/{userId}/favorites/{prepromptId}         │
│  • POST /api/v1/ratings                                        │
│  • GET  /api/v1/sync/{userId}                                  │
│  • POST /api/v1/sync/{userId}                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagram

### 1. Browse Featured Preprompts

```
User Opens Marketplace
        │
        ▼
MarketplaceViewModel.init()
        │
        ▼
MarketplaceRepository.getFeaturedPreprompts()
        │
        ▼
[Currently] Mock Data Returned
[Future] API Call: GET /api/v1/preprompts/featured
        │
        ▼
Result<List<SharedPreprompt>>
        │
        ▼
StateFlow Update
        │
        ▼
UI Recomposes with Featured Preprompts
```

### 2. Install Preprompt

```
User Taps "Install Command"
        │
        ▼
MarketplaceViewModel.installPreprompt()
        │
        ├─→ Get Current Local Preprompts
        │   (DataStoreManager)
        │
        ├─→ Convert SharedPreprompt to Preprompt
        │
        ├─→ Check for Duplicates
        │
        ├─→ Add to Local List
        │
        ├─→ Save to DataStore
        │
        └─→ Increment Download Count
            (MarketplaceRepository)
        │
        ▼
Success Message Shown
        │
        ▼
Command Available System-Wide via
AidoAccessibilityService
```

### 3. Search & Filter

```
User Types in Search Bar
        │
        ▼
MarketplaceViewModel.searchPreprompts(query)
        │
        ▼
MarketplaceRepository.searchPreprompts(query, filter)
        │
        ├─→ Filter by Query String
        ├─→ Apply Category Filter
        ├─→ Apply Rating Filter
        ├─→ Apply Sort Option
        └─→ Return Filtered Results
        │
        ▼
StateFlow Update
        │
        ▼
UI Recomposes with Search Results
```

### 4. Cloud Sync

```
PrepromptSyncService Started
        │
        ▼
Get Local Preprompts from DataStore
        │
        ▼
MarketplaceRepository.syncUserPreprompts()
        │
        ├─→ Upload Local Preprompts Not on Server
        ├─→ Download Server Preprompts Not Local
        ├─→ Resolve Conflicts (Merge Strategy)
        └─→ Return SyncStatus
        │
        ▼
Update Notification
        │
        ▼
MarketplaceRepository.downloadUserPreprompts()
        │
        ├─→ Get Remote Preprompts
        ├─→ Filter Out Existing Triggers
        ├─→ Merge with Local List
        └─→ Save to DataStore
        │
        ▼
Sync Complete Notification
        │
        ▼
Schedule Next Sync (1 hour)
```

### 5. Share Preprompt

```
User Taps "Share New Command"
        │
        ▼
Fill Share Dialog
(Category, Description, Tags)
        │
        ▼
MarketplaceViewModel.sharePreprompt()
        │
        ├─→ Create SharedPreprompt from Local
        ├─→ Add User ID and Metadata
        └─→ MarketplaceRepository.uploadPreprompt()
        │
        ▼
[Currently] Mock Upload
[Future] POST /api/v1/preprompts
        │
        ▼
Success Message
        │
        ▼
Refresh "My Shared" Tab
```

## Component Interaction Matrix

```
┌──────────────────┬──────────┬──────────┬──────────┬──────────┐
│   Component      │   UI     │ ViewModel│Repository│  Service │
├──────────────────┼──────────┼──────────┼──────────┼──────────┤
│ HomeScreen       │    ●     │    →     │          │          │
├──────────────────┼──────────┼──────────┼──────────┼──────────┤
│ MarketplaceScreen│    ●     │    →     │          │          │
├──────────────────┼──────────┼──────────┼──────────┼──────────┤
│ MarketplaceVM    │    ←     │    ●     │    →     │          │
├──────────────────┼──────────┼──────────┼──────────┼──────────┤
│ MarketplaceRepo  │          │    ←     │    ●     │          │
├──────────────────┼──────────┼──────────┼──────────┼──────────┤
│ SyncService      │          │          │    →     │    ●     │
├──────────────────┼──────────┼──────────┼──────────┼──────────┤
│ DataStoreManager │          │    ↔     │    ↔     │    ↔     │
├──────────────────┼──────────┼──────────┼──────────┼──────────┤
│ AuthManager      │          │    ↔     │    ↔     │          │
└──────────────────┴──────────┴──────────┴──────────┴──────────┘

Legend:
  ●  = Primary component
  →  = Calls/Uses
  ←  = Updates
  ↔  = Bidirectional
```

## State Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│                  MarketplaceUiState                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  featuredPreprompts: List<SharedPreprompt>                 │
│  searchResults: List<SharedPreprompt>                      │
│  userSharedPreprompts: List<SharedPreprompt>               │
│  favoritePreprompts: List<SharedPreprompt>                 │
│  isLoading: Boolean                                        │
│  error: String?                                            │
│  installStatus: InstallStatus?                             │
│  uploadStatus: UploadStatus?                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
           │
           │ MutableStateFlow
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│              Observed by Composables                        │
└─────────────────────────────────────────────────────────────┘
           │
           │ LaunchedEffect / collectAsState
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│                  UI Recomposition                           │
└─────────────────────────────────────────────────────────────┘
```

## Authentication Flow

```
┌──────────────────────────────────────────────────────────┐
│                     App Launch                           │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  AuthManager Check   │
              └──────────┬───────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
┌─────────────────┐            ┌─────────────────┐
│ isAuthenticated │            │   !isAuth       │
│     = true      │            │   = false       │
└────────┬────────┘            └────────┬────────┘
         │                              │
         ▼                              ▼
┌─────────────────┐            ┌─────────────────┐
│  Get User Info  │            │ Show Sign In    │
│  • userId       │            │ Options:        │
│  • username     │            │ • Email/Pass    │
│  • email        │            │ • Guest Mode    │
└────────┬────────┘            └────────┬────────┘
         │                              │
         │                              │ Sign In Success
         │                              │
         └──────────────┬───────────────┘
                        │
                        ▼
              ┌──────────────────────┐
              │  Load Marketplace    │
              │  with User Context   │
              └──────────────────────┘
```

## Sync Conflict Resolution

```
┌──────────────────────────────────────────────────────────┐
│                  Sync Initiated                          │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  Get Local Preprompts│
              │  Get Cloud Preprompts│
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  Compare by Trigger  │
              └──────────┬───────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ Local Only  │  │  Both Exist │  │ Cloud Only  │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       │                │                │
       ▼                ▼                ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  Upload to  │  │ Merge Logic │  │ Download to │
│   Cloud     │  │ • Compare   │  │   Local     │
│             │  │   Updated   │  │             │
│             │  │ • Keep      │  │             │
│             │  │   Newer     │  │             │
└─────────────┘  └─────────────┘  └─────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  Update Local +      │
              │  Cloud Lists         │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  Sync Complete       │
              │  Update Status       │
              └──────────────────────┘
```

## Error Handling Flow

```
Repository Call
       │
       ├─→ Success → Result.success(data)
       │                    │
       │                    ▼
       │            ViewModel Updates State
       │                    │
       │                    ▼
       │                 UI Shows Data
       │
       └─→ Failure → Result.failure(exception)
                            │
                            ▼
                    ViewModel Updates Error State
                            │
                            ▼
                    UI Shows Error Message
                            │
                            ▼
                    User Can Retry or Dismiss
```

## Performance Optimization Points

```
┌─────────────────────────────────────────────────────────┐
│                    Optimization Layer                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  UI Layer:                                              │
│  • LazyColumn for preprompt lists                      │
│  • remember() for expensive computations               │
│  • derivedStateOf for computed values                  │
│  • key() for list item recomposition                   │
│                                                         │
│  ViewModel Layer:                                       │
│  • Coroutine context management                        │
│  • Flow debouncing for search                          │
│  • Cached results                                      │
│                                                         │
│  Repository Layer:                                      │
│  • Suspend functions for async                         │
│  • Result wrapper for error handling                   │
│  • Connection pooling (OkHttp)                         │
│                                                         │
│  Service Layer:                                         │
│  • Background thread execution                         │
│  • Batch operations                                    │
│  • WorkManager integration (future)                    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

This architecture ensures:
- ✅ Clean separation of concerns
- ✅ Testable components
- ✅ Scalable structure
- ✅ Maintainable codebase
- ✅ Efficient performance
