package com.rr.aido.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.rr.aido.data.models.UserProfile
import kotlinx.coroutines.tasks.await

/**
 * Firebase-based authentication manager for marketplace
 * Handles user sign in, sign up, and profile management
 */
class AuthManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get current user ID
     */
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Get current username
     */
    fun getUsername(): String? {
        return auth.currentUser?.displayName ?: prefs.getString(KEY_USERNAME, null)
    }

    /**
     * Get current user email
     */
    fun getUserEmail(): String? {
        return auth.currentUser?.email
    }

    /**
     * Sign in with email/password using Firebase Auth
     */
    suspend fun signIn(email: String, password: String): Result<AuthUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Authentication failed"))
            
            val user = AuthUser(
                userId = firebaseUser.uid,
                username = firebaseUser.displayName ?: email.substringBefore("@"),
                email = firebaseUser.email ?: email,
                isGuest = false
            )
            
            saveUserLocally(user)
            Log.d(TAG, "User signed in: ${user.username}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
            Result.failure(e)
        }
    }

    /**
     * Sign up with email/password using Firebase Auth
     */
    suspend fun signUp(username: String, email: String, password: String): Result<AuthUser> {
        return try {
            // Create Firebase user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("User creation failed"))
            
            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Create Firestore user profile
            val userProfile = UserProfile(
                userId = firebaseUser.uid,
                username = username,
                email = email,
                joinedAt = System.currentTimeMillis()
            )
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(userProfile)
                .await()
            
            val user = AuthUser(
                userId = firebaseUser.uid,
                username = username,
                email = email,
                isGuest = false
            )
            
            saveUserLocally(user)
            Log.d(TAG, "User registered: $username")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in anonymously as guest
     */
    suspend fun signInAsGuest(): Result<AuthUser> {
        return try {
            val authResult = auth.signInAnonymously().await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Anonymous sign in failed"))
            
            val user = AuthUser(
                userId = firebaseUser.uid,
                username = "Guest",
                email = "",
                isGuest = true
            )
            
            saveUserLocally(user)
            Log.d(TAG, "Guest user created")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Guest sign in error", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
        prefs.edit().clear().apply()
        Log.d(TAG, "User signed out")
    }

    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): AuthUser? {
        val firebaseUser = auth.currentUser ?: return null
        
        return AuthUser(
            userId = firebaseUser.uid,
            username = firebaseUser.displayName ?: prefs.getString(KEY_USERNAME, null) ?: "User",
            email = firebaseUser.email ?: "",
            isGuest = firebaseUser.isAnonymous
        )
    }

    /**
     * Get Firebase user directly
     */
    fun getFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(username: String? = null, email: String? = null): Result<Boolean> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            // Update Firebase Auth profile
            if (username != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
                prefs.edit().putString(KEY_USERNAME, username).apply()
            }
            
            // Update email if provided
            if (email != null && email != firebaseUser.email) {
                firebaseUser.updateEmail(email).await()
            }
            
            // Update Firestore profile
            val updates = mutableMapOf<String, Any>()
            username?.let { updates["username"] = it }
            email?.let { updates["email"] = it }
            
            if (updates.isNotEmpty()) {
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .update(updates)
                    .await()
            }
            
            Log.d(TAG, "Profile updated")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Profile update error", e)
            Result.failure(e)
        }
    }

    /**
     * Reset password via email
     */
    suspend fun resetPassword(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset error", e)
            Result.failure(e)
        }
    }

    private fun saveUserLocally(user: AuthUser) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.userId)
            putString(KEY_USERNAME, user.username)
            putString(KEY_USER_EMAIL, user.email)
            putBoolean(KEY_IS_GUEST, user.isGuest)
        }.apply()
    }

    companion object {
        private const val TAG = "AuthManager"
        private const val PREFS_NAME = "aido_auth_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_GUEST = "is_guest"
    }
}

/**
 * User authentication data class
 */
data class AuthUser(
    val userId: String,
    val username: String,
    val email: String,
    val isGuest: Boolean = false
)
