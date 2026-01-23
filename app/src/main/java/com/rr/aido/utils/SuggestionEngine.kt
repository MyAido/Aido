package com.rr.aido.utils

object SuggestionEngine {


    fun getPredictions(lastWord: String, maxResults: Int): List<String> {
        return emptyList()
    }


    fun getCompletions(prefix: String, maxResults: Int): List<String> {
        return WordDatabase.getSmartSuggestions(prefix, maxResults)
    }
}
