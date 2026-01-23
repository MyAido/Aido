package com.rr.aido.data.repository

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.rr.aido.data.models.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gemini API service interface
 * TODO: Real Gemini API endpoints add karne hain
 */
interface GeminiApiService {
    
    // TODO: Replace with actual Gemini API endpoint
    // Example: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
    @POST("v1beta/models/{model}:generateContent")
    @retrofit2.http.Headers("Content-Type: application/json")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}



/**
 * Request body for Gemini API
 */
data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

/**
 * Response from Gemini API
 */
data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

/**
 * OpenAI-compatible API request/response
 * Works with OpenRouter, OpenAI, and other compatible APIs
 */
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val max_tokens: Int? = 1000
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIResponse(
    val choices: List<OpenAIChoice>?
)

data class OpenAIChoice(
    val message: OpenAIMessage?
)

/**
 * GeminiRepositoryImpl - Actual implementation
 * Abhi stub hai, real API integration ke liye TODO marks dekho
 */
class GeminiRepositoryImpl : GeminiRepository {
    
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        // TODO: Replace with actual Gemini API base URL
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val apiService = retrofit.create(GeminiApiService::class.java)
    
    /**
     * Test API key
     * TODO: Implement real API key validation
     */
    override suspend fun testApiKey(provider: AiProvider, apiKey: String, model: String): Result<Boolean> {
        return when (provider) {
            AiProvider.GEMINI -> {
                if (apiKey.isBlank()) {
                    return Result.Error("Please enter a valid Gemini API key")
                }
                try {
                    // Simulated delay
                    delay(1000)
                    
                    // TODO: Uncomment for real API call when backend ready
                    /*
                    val request = GeminiRequest(
                        contents = listOf(
                            Content(
                                parts = listOf(Part("Test"))
                            )
                        )
                    )
                    val response = apiService.generateContent(model, apiKey, request)
                    Result.Success(response.candidates?.isNotEmpty() == true)
                    */
                    
                    // Basic format validation until real API integration
                    if (apiKey.length > 20) {
                        Result.Success(true)
                    } else {
                        Result.Error("Invalid API key format")
                    }
                } catch (e: Exception) {
                    Result.Error("API key test failed: ${e.message}", e)
                }
            }
            AiProvider.CUSTOM -> {
                // For custom API, just validate that key is not empty
                if (apiKey.isBlank()) {
                    Result.Error("Please enter a valid API key")
                } else {
                    Result.Success(true)
                }
            }
        }
    }
    
    /**
     * Send prompt to Gemini
     * Real API implementation
     */
    override suspend fun sendPrompt(
        provider: AiProvider, 
        apiKey: String, 
        model: String, 
        prompt: String,
        customApiUrl: String
    ): Result<String> {
        return when (provider) {
            AiProvider.GEMINI -> sendPromptViaGemini(apiKey, model, prompt)
            AiProvider.CUSTOM -> sendPromptViaCustomAPI(customApiUrl, apiKey, model, prompt)
        }
    }



    private suspend fun sendPromptViaGemini(apiKey: String, model: String, prompt: String): Result<String> {
        return try {
            val requestObj = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(prompt))
                    )
                )
            )

            val jsonBody = gson.toJson(requestObj)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val text = withContext(Dispatchers.IO) {
                client.newCall(httpRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val code = response.code
                        val bodyStr = response.body?.string()
                        throw IOException("Gemini HTTP $code: ${bodyStr ?: "(empty)"}")
                    }
                    val bodyStr = response.body?.string() ?: throw IOException("Empty response body from Gemini")
                    try {
                        val geminiResponse = gson.fromJson(bodyStr, GeminiResponse::class.java)
                        geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    } catch (ex: JsonSyntaxException) {
                        throw IOException("Invalid Gemini response format", ex)
                    }
                }
            }

            if (text != null) {
                Result.Success(text.trim())
            } else {
                Result.Error("No response from Gemini API")
            }
        } catch (e: Exception) {
            return Result.Error("Gemini API error: ${e.message}", e)
        }
    }

    private suspend fun sendPromptViaCustomAPI(baseUrl: String, apiKey: String, model: String, prompt: String): Result<String> {
        return try {
            // Create OpenAI-compatible request
            val requestObj = OpenAIRequest(
                model = model,
                messages = listOf(
                    OpenAIMessage(
                        role = "user",
                        content = prompt
                    )
                ),
                max_tokens = 1000
            )

            val jsonBody = gson.toJson(requestObj)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            // Ensure baseUrl ends with /
            val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val endpoint = "${normalizedBaseUrl}chat/completions"

            val httpRequest = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val text = withContext(Dispatchers.IO) {
                client.newCall(httpRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val code = response.code
                        val bodyStr = response.body?.string()
                        throw IOException("Custom API HTTP $code: ${bodyStr ?: "(empty)"}")
                    }
                    val bodyStr = response.body?.string() ?: throw IOException("Empty response body from custom API")
                    try {
                        val openAIResponse = gson.fromJson(bodyStr, OpenAIResponse::class.java)
                        openAIResponse.choices?.firstOrNull()?.message?.content
                    } catch (ex: JsonSyntaxException) {
                        throw IOException("Invalid custom API response format", ex)
                    }
                }
            }

            if (text != null) {
                Result.Success(text.trim())
            } else {
                Result.Error("No response from custom API")
            }
        } catch (e: Exception) {
            return Result.Error("Custom API error: ${e.message}", e)
        }
    }

    private fun generateFallbackResponse(prompt: String): String {
        return when {
            prompt.contains("Fix grammar", ignoreCase = true) -> {
                val textToFix = prompt.replace("Fix grammar, spelling, and punctuation.", "").trim()
                    .replace("Return only the corrected text.", "").trim()
                simulateGrammarFix(textToFix)
            }
            prompt.contains("Summarize", ignoreCase = true) -> {
                "This is a concise summary of the provided text."
            }
            prompt.contains("polite", ignoreCase = true) -> {
                "I would greatly appreciate your assistance with this matter. Thank you for your time and consideration."
            }
            prompt.contains("casual", ignoreCase = true) -> {
                "Hey! Just wanted to check in on this. Let me know what you think!"
            }
            prompt.contains("capital of France", ignoreCase = true) -> {
                "Paris"
            }
            prompt.contains("capital of", ignoreCase = true) -> {
                "The capital city information for the requested location."
            }
            else -> {
                "AI response: ${prompt.take(50)}..."
            }
        }
    }
    
    /**
     * Simulate grammar fix
     */
    private fun simulateGrammarFix(text: String): String {
        return text
            .replace("are wrong", "is wrong")
            .replace("is mistakes", "are mistakes")
            .replace("has mistakes", "has mistakes")
            .replace("dont", "don't")
            .replace("cant", "can't")
            .replace("wont", "won't")
            .trim()
    }
}

/**
 * INTEGRATION NOTES:
 * 
 * Real Gemini API integration ke liye:
 * 
 * 1. API Key: https://aistudio.google.com se API key generate karo
 * 
 * 2. Base URL: https://generativelanguage.googleapis.com/
 * 
 * 3. Endpoint: /v1beta/models/{model}:generateContent
 * 
 * 4. Headers:
 *    - x-goog-api-key: YOUR_API_KEY
 *    - Content-Type: application/json
 * 
 * 5. Request body:
 *    {
 *      "contents": [{
 *        "parts": [{
 *          "text": "your prompt here"
 *        }]
 *      }]
 *    }
 * 
 * 6. Response:
 *    {
 *      "candidates": [{
 *        "content": {
 *          "parts": [{
 *            "text": "response text"
 *          }]
 *        }
 *      }]
 *    }
 * 
 * 7. TODO marks ko follow karo aur uncomment karo real API calls
 */
