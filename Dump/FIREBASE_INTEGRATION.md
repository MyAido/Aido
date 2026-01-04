# 🔥 Firebase Integration Guide

## Overview

The Aido Marketplace is now fully integrated with Firebase, providing real-time cloud sync, authentication, and data storage.

---

## 🎯 Firebase Services Used

### 1. **Firebase Authentication**
- Email/Password authentication
- Anonymous (Guest) authentication
- User profile management
- Password reset functionality

### 2. **Cloud Firestore**
- Real-time database for preprompts
- User profiles and settings
- Ratings and reviews
- Favorites management

### 3. **Firebase Analytics** (Optional)
- User engagement tracking
- Feature usage statistics

---

## 📊 Firestore Data Structure

```
aido-it (project)
│
├── preprompts/ (collection)
│   └── {prepromptId}/ (document)
│       ├── id: String
│       ├── trigger: String
│       ├── instruction: String
│       ├── example: String
│       ├── category: String (enum)
│       ├── authorId: String
│       ├── authorName: String
│       ├── description: String
│       ├── tags: Array<String>
│       ├── downloads: Number
│       ├── rating: Number (0-5)
│       ├── ratingCount: Number
│       ├── createdAt: Timestamp
│       ├── updatedAt: Timestamp
│       ├── isFeatured: Boolean
│       └── isVerified: Boolean
│
├── users/ (collection)
│   └── {userId}/ (document)
│       ├── userId: String
│       ├── username: String
│       ├── email: String
│       ├── avatarUrl: String (optional)
│       ├── bio: String
│       ├── sharedPrepromptsCount: Number
│       ├── totalDownloads: Number
│       ├── joinedAt: Timestamp
│       ├── isVerified: Boolean
│       │
│       ├── preprompts/ (subcollection) - User's personal preprompts
│       │   └── {prepromptId}/
│       │       ├── trigger: String
│       │       ├── instruction: String
│       │       └── example: String
│       │
│       └── favorites/ (subcollection) - Favorited preprompts
│           └── {prepromptId}/
│               └── addedAt: Timestamp
│
└── ratings/ (collection)
    └── {userId}_{prepromptId}/ (document)
        ├── userId: String
        ├── prepromptId: String
        ├── rating: Number (1-5)
        ├── review: String
        └── createdAt: Timestamp
```

---

## 🔧 Firebase Configuration

### Project Details
```
Project Name: aido-it
Project ID: aido-it
Project Number: 816845839566
Firebase URL: https://aido-it-default-rtdb.firebaseio.com
Storage Bucket: aido-it.firebasestorage.app
```

### App Configuration
```
Package Name: com.rr.aido
App ID: 1:816845839566:android:868f07fcb4bd8fc779849f
```

### Configuration File
The `google-services.json` is already placed in:
```
app/google-services.json
```

---

## 📝 Firestore Security Rules

Add these security rules in Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // Preprompts collection - public read, authenticated write
    match /preprompts/{prepromptId} {
      allow read: if true; // Anyone can browse preprompts
      allow create: if isAuthenticated();
      allow update, delete: if isAuthenticated() && 
                               resource.data.authorId == request.auth.uid;
    }
    
    // Users collection
    match /users/{userId} {
      allow read: if true; // Public profiles
      allow write: if isOwner(userId);
      
      // User's personal preprompts
      match /preprompts/{prepromptId} {
        allow read, write: if isOwner(userId);
      }
      
      // User's favorites
      match /favorites/{prepromptId} {
        allow read, write: if isOwner(userId);
      }
    }
    
    // Ratings collection
    match /ratings/{ratingId} {
      allow read: if true;
      allow create, update: if isAuthenticated() && 
                               ratingId == request.auth.uid + '_' + request.resource.data.prepromptId;
      allow delete: if isAuthenticated() && 
                       resource.data.userId == request.auth.uid;
    }
  }
}
```

---

## 🚀 Setup Instructions

### 1. Enable Authentication

In Firebase Console:
1. Go to **Authentication** → **Sign-in method**
2. Enable **Email/Password**
3. Enable **Anonymous** (for guest mode)

### 2. Create Firestore Database

1. Go to **Firestore Database**
2. Click **Create database**
3. Choose **Start in production mode**
4. Select closest region
5. Apply security rules (see above)

### 3. Add Indexes

For better query performance, add these composite indexes:

```
Collection: preprompts
Fields:
  - category (Ascending)
  - downloads (Descending)

Collection: preprompts
Fields:
  - isFeatured (Ascending)
  - downloads (Descending)

Collection: preprompts  
Fields:
  - createdAt (Descending)
  - downloads (Descending)
```

Go to: **Firestore Database** → **Indexes** → **Composite** → **Create index**

---

## 💻 Code Implementation

### 1. Firebase Repository

The main repository implementation is in:
```
MarketplaceRepositoryFirebase.kt
```

Key features:
- ✅ Real-time Firestore queries
- ✅ Async/await with Kotlin coroutines
- ✅ Proper error handling
- ✅ Transaction support for atomic updates
- ✅ Batch operations for efficiency

### 2. Firebase Authentication

Authentication is handled in:
```
AuthManager.kt (updated with Firebase Auth)
```

Features:
- ✅ Email/Password sign in
- ✅ Email/Password sign up
- ✅ Anonymous/Guest mode
- ✅ Password reset
- ✅ Profile updates
- ✅ User session management

### 3. ViewModel Integration

The ViewModel uses Firebase repository:
```kotlin
class MarketplaceViewModel(
    private val context: Context,
    private val marketplaceRepository: MarketplaceRepository = MarketplaceRepositoryFirebase(),
    private val dataStoreManager: DataStoreManager,
    private val authManager: AuthManager = AuthManager(context)
)
```

---

## 🔐 Authentication Flow

### Sign Up
```kotlin
viewModel.signUp(username, email, password)
```

**Process:**
1. Create Firebase Auth user
2. Update display name
3. Create Firestore user profile
4. Update local auth state

### Sign In
```kotlin
viewModel.signIn(email, password)
```

**Process:**
1. Authenticate with Firebase
2. Retrieve user data
3. Update local auth state

### Guest Mode
```kotlin
viewModel.signInAsGuest()
```

**Process:**
1. Anonymous Firebase sign in
2. Limited marketplace access
3. Can browse and install
4. Cannot share or favorite

---

## 📤 Data Operations

### Upload Preprompt
```kotlin
// Auto-populates authorId and authorName from Firebase Auth
marketplaceRepository.uploadPreprompt(sharedPreprompt)
```

### Search with Filters
```kotlin
marketplaceRepository.searchPreprompts(
    query = "translate",
    filter = MarketplaceFilter(
        category = PrepromptCategory.COMMUNICATION,
        sortBy = SortOption.POPULAR,
        minRating = 4.0f
    )
)
```

### Toggle Favorite
```kotlin
// Automatically uses current user's ID from Firebase Auth
marketplaceRepository.addToFavorites(userId, prepromptId)
```

### Rate Preprompt
```kotlin
// Updates both rating document and preprompt aggregate
marketplaceRepository.ratePreprompt(PrepromptRating(
    userId = currentUserId,
    prepromptId = prepromptId,
    rating = 5.0f,
    review = "Excellent command!"
))
```

---

## 🔄 Cloud Sync

### Automatic Sync

Background service syncs every hour:
```kotlin
PrepromptSyncService.startAutoSync(context)
```

### Manual Sync
```kotlin
viewModel.syncPreprompts()
```

**Sync Process:**
1. Get local preprompts from DataStore
2. Get cloud preprompts from Firestore
3. Compare by trigger name
4. Upload new local preprompts
5. Download new cloud preprompts
6. Merge without duplicates

---

## 🎯 Testing Firebase Integration

### 1. Test Authentication

```kotlin
// In your test or debug activity
val authManager = AuthManager(context)

// Test sign up
lifecycleScope.launch {
    val result = authManager.signUp("testuser", "test@example.com", "password123")
    result.onSuccess { user ->
        Log.d("Test", "Sign up success: ${user.username}")
    }
}

// Test sign in
lifecycleScope.launch {
    val result = authManager.signIn("test@example.com", "password123")
    result.onSuccess { user ->
        Log.d("Test", "Sign in success: ${user.username}")
    }
}
```

### 2. Test Firestore Operations

```kotlin
val repository = MarketplaceRepositoryFirebase()

// Upload test preprompt
lifecycleScope.launch {
    val testPreprompt = SharedPreprompt(
        trigger = "@test",
        instruction = "This is a test",
        category = PrepromptCategory.GENERAL,
        authorId = "test_user",
        authorName = "Test User",
        description = "Test preprompt"
    )
    
    repository.uploadPreprompt(testPreprompt)
        .onSuccess { id ->
            Log.d("Test", "Uploaded with ID: $id")
        }
}

// Fetch featured preprompts
lifecycleScope.launch {
    repository.getFeaturedPreprompts()
        .onSuccess { preprompts ->
            Log.d("Test", "Found ${preprompts.size} featured preprompts")
        }
}
```

---

## 🐛 Common Issues & Solutions

### Issue 1: "FirebaseApp is not initialized"

**Solution:**
Ensure `google-services.json` is in the `app/` directory and the Google Services plugin is applied.

### Issue 2: "Permission denied" errors

**Solution:**
1. Check Firestore security rules
2. Verify user is authenticated
3. Check user ID matches authorId

### Issue 3: Indexes not created

**Solution:**
- Firebase will show index creation links in Logcat
- Click the link to auto-create required indexes
- Or manually create in Firebase Console

### Issue 4: Queries timeout

**Solution:**
- Add proper indexes for all query combinations
- Limit query results (use `.limit()`)
- Check network connection

---

## 📊 Monitoring & Analytics

### Firebase Console Monitoring

1. **Authentication**
   - User count
   - Sign-in methods usage
   - Daily active users

2. **Firestore**
   - Document reads/writes
   - Storage usage
   - Query performance

3. **Analytics** (if enabled)
   - Screen views
   - User engagement
   - Feature usage

### Enable Analytics

In your Application class:
```kotlin
class AidoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseAnalytics.getInstance(this)
    }
}
```

---

## 💰 Cost Optimization

### Firebase Free Tier Limits

**Authentication:**
- Unlimited users ✅

**Firestore:**
- 1 GB storage
- 50,000 reads/day
- 20,000 writes/day
- 20,000 deletes/day

### Optimization Tips

1. **Cache Read Data**
   ```kotlin
   // Use offline persistence
   FirebaseFirestore.getInstance().apply {
       firestoreSettings = FirebaseFirestoreSettings.Builder()
           .setPersistenceEnabled(true)
           .build()
   }
   ```

2. **Batch Operations**
   ```kotlin
   val batch = firestore.batch()
   // Add multiple operations
   batch.commit().await()
   ```

3. **Limit Query Results**
   ```kotlin
   prepromptsCollection
       .limit(50) // Don't fetch all documents
       .get()
   ```

4. **Use Subcollections**
   - Organize data hierarchically
   - Reduces unnecessary reads

---

## 🔒 Security Best Practices

### 1. Validate Data

Always validate on the server side using Firestore Rules:
```javascript
allow create: if request.resource.data.trigger.size() > 0 &&
                 request.resource.data.instruction.size() > 0;
```

### 2. Sanitize User Input

```kotlin
fun sanitizeInput(text: String): String {
    return text.trim()
        .replace(Regex("<[^>]*>"), "") // Remove HTML tags
        .take(1000) // Limit length
}
```

### 3. Rate Limiting

Implement client-side rate limiting:
```kotlin
private var lastUploadTime = 0L
private val UPLOAD_COOLDOWN = 60_000L // 1 minute

fun canUpload(): Boolean {
    val now = System.currentTimeMillis()
    if (now - lastUploadTime < UPLOAD_COOLDOWN) {
        return false
    }
    lastUploadTime = now
    return true
}
```

---

## 🚀 Deployment Checklist

- [x] Firebase project configured
- [x] google-services.json added
- [x] Authentication enabled (Email/Password, Anonymous)
- [x] Firestore database created
- [x] Security rules applied
- [x] Indexes created
- [x] Repository implementation complete
- [x] ViewModel integrated
- [x] Auth flow implemented
- [ ] Test all operations
- [ ] Monitor usage in Firebase Console
- [ ] Set up billing alerts

---

## 📚 Additional Resources

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firestore Data Modeling](https://firebase.google.com/docs/firestore/data-model)
- [Firebase Auth Best Practices](https://firebase.google.com/docs/auth/android/manage-users)
- [Security Rules Guide](https://firebase.google.com/docs/firestore/security/get-started)

---

## ✅ Next Steps

1. **Seed Initial Data**
   - Add featured preprompts to Firestore
   - Create admin users with verified badges

2. **Test Thoroughly**
   - Sign up/sign in flows
   - Upload/download preprompts
   - Favorites and ratings
   - Cloud sync

3. **Monitor Performance**
   - Check Firebase Console daily
   - Optimize slow queries
   - Add indexes as needed

4. **Prepare for Production**
   - Review security rules
   - Set up billing alerts
   - Enable monitoring

---

**Firebase integration is complete and ready for testing!** 🎉

Last Updated: November 16, 2025
