package com.rr.aido.utils

/**
 * Engine for generating word suggestions and predictions
 */
object SuggestionEngine {
    
    /**
     * Get next word predictions based on the last typed word
     * Currently returns empty as we don't have a bigram model yet.
     * TODO: Implement N-gram model for better predictions
     */
    fun getPredictions(lastWord: String, maxResults: Int): List<String> {
        return emptyList()
    }

    /**
     * Get word completions for a given prefix
     */
    fun getCompletions(prefix: String, maxResults: Int): List<String> {
        return WordDatabase.getSmartSuggestions(prefix, maxResults)
    }
}
