package com.rr.aido.utils

/**
 * Comprehensive word database for auto-suggestions
 * Contains 500+ most commonly used English words
 */
object WordDatabase {
    
    val commonWords = listOf(
        // Very common words (1-50)
        "the", "and", "for", "are", "but", "not", "you", "all", "can", "had",
        "her", "was", "one", "our", "out", "day", "get", "has", "him", "his",
        "how", "man", "new", "now", "old", "see", "two", "way", "who", "boy",
        "did", "its", "let", "put", "say", "she", "too", "use", "will", "have",
        "this", "been", "call", "come", "find", "first", "from", "good", "great", "hand",
        
        // Common words (51-150)
        "here", "just", "know", "last", "like", "little", "long", "look", "made", "make",
        "many", "more", "most", "much", "must", "name", "need", "next", "night", "only",
        "open", "other", "over", "own", "part", "people", "place", "point", "right", "same",
        "seem", "should", "show", "small", "some", "still", "such", "take", "tell", "than",
        "that", "them", "then", "there", "these", "they", "thing", "think", "those", "three",
        "through", "time", "under", "until", "very", "want", "water", "well", "were", "what",
        "when", "where", "which", "while", "white", "with", "work", "world", "would", "write",
        "year", "your", "about", "above", "after", "again", "against", "also", "always", "another",
        "answer", "any", "around", "asked", "away", "back", "because", "before", "began", "begin",
        "being", "below", "between", "both", "bring", "came", "cannot", "change", "children", "city",
        
        // Useful words (151-300)
        "close", "could", "country", "course", "does", "done", "door", "down", "each", "early",
        "earth", "easy", "enough", "even", "every", "example", "face", "family", "far", "feel",
        "feet", "few", "food", "form", "found", "four", "friend", "full", "game", "gave",
        "give", "given", "goes", "going", "gone", "got", "group", "grow", "half", "hard",
        "head", "help", "high", "home", "house", "idea", "important", "inside", "into", "kind",
        "knew", "large", "late", "later", "learn", "leave", "left", "less", "life", "light",
        "line", "list", "listen", "live", "lived", "local", "love", "main", "may", "mean",
        "men", "might", "mind", "miss", "money", "move", "moved", "near", "never", "number",
        "off", "often", "once", "page", "paper", "pass", "passed", "play", "power", "present",
        "program", "question", "quick", "read", "ready", "real", "room", "run", "school", "second",
        "seen", "set", "several", "short", "side", "since", "something", "soon", "sound", "start",
        "state", "story", "study", "system", "table", "taken", "together", "told", "took", "top",
        "toward", "town", "try", "turn", "turned", "upon", "used", "usually", "want", "watch",
        "water", "week", "went", "white", "whole", "whose", "why", "word", "words", "year",
        "yet", "young", "able", "almost", "became", "become", "behind", "better", "book", "called",
        
        // Conversational & tech words (301-450)
        "hello", "please", "thanks", "sorry", "yes", "okay", "sure", "maybe", "welcome", "goodbye",
        "morning", "afternoon", "evening", "tonight", "today", "tomorrow", "yesterday", "weekend", "week", "month",
        "happy", "thank", "love", "miss", "hope", "wish", "nice", "great", "awesome", "cool",
        "wrong", "right", "correct", "true", "false", "real", "fake", "good", "bad", "best",
        "computer", "phone", "mobile", "email", "internet", "website", "online", "app", "software", "hardware",
        "download", "upload", "install", "update", "delete", "save", "open", "close", "send", "receive",
        "message", "chat", "text", "call", "video", "photo", "picture", "image", "file", "folder",
        "document", "window", "screen", "keyboard", "mouse", "click", "type", "search", "find", "copy",
        "paste", "cut", "undo", "redo", "print", "share", "like", "comment", "post", "follow",
        "friend", "family", "work", "job", "office", "meeting", "team", "project", "task", "goal",
        "plan", "idea", "problem", "solution", "help", "support", "service", "customer", "user", "account",
        "login", "logout", "password", "username", "register", "sign", "profile", "settings", "option", "menu",
        "notification", "alert", "reminder", "calendar", "schedule", "appointment", "event", "date", "time", "clock",
        "location", "address", "map", "direction", "distance", "travel", "trip", "vacation", "hotel", "restaurant",
        "food", "drink", "coffee", "tea", "water", "lunch", "dinner", "breakfast", "eat", "cook",
        
        // Additional common words (451-500)
        "shop", "store", "buy", "sell", "price", "cost", "pay", "money", "cash", "card",
        "bank", "payment", "order", "delivery", "shipping", "return", "refund", "discount", "sale", "offer",
        "deal", "gift", "present", "party", "celebrate", "birthday", "holiday", "christmas", "new year", "festival",
        "music", "song", "play", "listen", "watch", "movie", "show", "game", "sport", "exercise",
        "health", "doctor", "hospital", "medicine", "sick", "pain", "care", "treatment", "test", "result"
    )
    
    /**
     * Get word suggestions based on prefix
     * Returns up to maxResults suggestions
     */
    fun getSuggestions(prefix: String, maxResults: Int = 6): List<String> {
        if (prefix.isBlank()) return emptyList()
        
        val lowerPrefix = prefix.lowercase().trim()
        
        // Filter and sort suggestions
        return commonWords
            .filter { it.startsWith(lowerPrefix) && it != lowerPrefix }
            .sortedWith(compareBy(
                { it.length }, // Shorter words first
                { it } // Alphabetically
            ))
            .take(maxResults)
    }
    
    /**
     * Check if a word exists in dictionary
     */
    fun containsWord(word: String): Boolean {
        return commonWords.contains(word.lowercase())
    }
    
    /**
     * Get suggestions with frequency-based ranking
     * More commonly used words appear first
     */
    fun getSmartSuggestions(prefix: String, maxResults: Int = 6): List<String> {
        if (prefix.isBlank()) return emptyList()
        
        val lowerPrefix = prefix.lowercase().trim()
        val prefixLength = lowerPrefix.length
        
        // Filter words that start with prefix
        val matches = commonWords.filter { 
            it.startsWith(lowerPrefix) && it != lowerPrefix 
        }
        
        // Sort by: 1) Index in commonWords (frequency), 2) Length difference
        return matches
            .sortedWith(compareBy(
                { commonWords.indexOf(it) / 10 }, // Group by frequency tier
                { it.length - prefixLength } // Prefer shorter completions
            ))
            .take(maxResults)
    }
}
