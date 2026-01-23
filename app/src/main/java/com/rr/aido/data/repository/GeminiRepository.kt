package com.rr.aido.data.repository

import com.rr.aido.data.models.AiProvider

interface GeminiRepository {

    suspend fun testApiKey(provider: AiProvider, apiKey: String, model: String): Result<Boolean>

    suspend fun sendPrompt(
        provider: AiProvider,
        apiKey: String,
        model: String,
        prompt: String,
        customApiUrl: String = ""
    ): Result<String>
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
