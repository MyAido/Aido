package com.rr.aido.data.repository

import com.rr.aido.data.models.AiProvider

/**
 * GeminiRepository interface
 * Gemini API calls ke liye contract
 */
interface GeminiRepository {
    
    /**
     * API key test karta hai
     * @return true if valid, false if invalid
     */
    suspend fun testApiKey(provider: AiProvider, apiKey: String, model: String): Result<Boolean>
    
    /**
     * Prompt send karta hai aur response return karta hai
     */
    suspend fun sendPrompt(
        provider: AiProvider, 
        apiKey: String, 
        model: String, 
        prompt: String,
        customApiUrl: String = ""
    ): Result<String>
}

/**
 * Result wrapper for API responses
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
