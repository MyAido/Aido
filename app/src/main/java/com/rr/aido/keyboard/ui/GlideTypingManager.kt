package com.rr.aido.keyboard.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.rr.aido.utils.WordDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Manages Glide Typing (Swipe Typing) logic.
 * Tracks user touch path, maps keys to coordinates, and decodes paths to words.
 */
class GlideTypingManager {

    // Map of Character -> Key Boundary (in parent coordinates)
    private val keyLayout = mutableMapOf<Char, Rect>()
    
    // Convert logic: Store center points for faster efficient distance calcs
    private val keyCenters = mutableMapOf<Char, Offset>()

    // Current swipe path
    private val _currentPath = MutableStateFlow<List<Offset>>(emptyList())
    val currentPath: StateFlow<List<Offset>> = _currentPath

    private val rawPoints = mutableListOf<Offset>()

    /**
     * Updates the position of a specific key.
     * Should be called from onGloballyPositioned modifiers on keys.
     */
    fun updateKeyPosition(char: Char, rect: Rect) {
        keyLayout[char] = rect
        // Identify center for distance calculations
        keyCenters[char] = rect.center
    }

    /**
     * Start a new gesture
     */
    fun startGesture(point: Offset) {
        rawPoints.clear()
        rawPoints.add(point)
        _currentPath.value = rawPoints.toList()
    }

    /**
     * Update gesture with new point
     */
    fun updateGesture(point: Offset) {
        // Simple optimization: don't add point if too close to last
        if (rawPoints.isNotEmpty()) {
            val last = rawPoints.last()
            val dist = (point - last).getDistance()
            if (dist < 10f) return // Ignore small jitters
        }
        
        rawPoints.add(point)
        _currentPath.value = rawPoints.toList()
    }

    /**
     * End gesture and decode the word
     */
    fun endGesture(): String? {
        val path = rawPoints.toList()
        _currentPath.value = emptyList() // Clear visual path immediately or animate out? for now clear
        
        if (path.size < 5) return null // Too short to be a swipe
        
        return decodePath(path)
    }

    /**
     * Decodes the geometric path into the most likely word.
     * Falls back to typing the swiped letters if no dictionary match.
     */
    private fun decodePath(path: List<Offset>): String? {
        if (keyCenters.isEmpty()) return null

        // Track which keys were touched during the swipe
        val touchedKeys = mutableListOf<Char>()
        var lastKey: Char? = null
        
        // Sample path at regular intervals to get touched keys
        for (point in path) {
            val nearestKey = findClosestKey(point)
            if (nearestKey != null && nearestKey != lastKey) {
                touchedKeys.add(nearestKey)
                lastKey = nearestKey
            }
        }
        
        // If we didn't touch enough keys, abort
        if (touchedKeys.size < 2) return null

        // 1. Identify Start and End Characters
        val startChar = touchedKeys.first()
        val endChar = touchedKeys.last()

        // 2. Filter Candidate Words from dictionary
        val candidates = WordDatabase.commonWords.filter { word ->
            word.isNotEmpty() &&
            word.first() == startChar &&
            word.last() == endChar &&
            word.length >= 2 // Min length for swipe
        }

        // 3. Score Candidates and find best match
        val bestMatch = candidates.minByOrNull { word ->
            calculatePathCost(word, path)
        }

        // If we have a good dictionary match, use it
        // Otherwise, fallback to typing the literal swiped letters
        return bestMatch ?: touchedKeys.joinToString("")
    }

    private fun findClosestKey(point: Offset): Char? {
        var closestChar: Char? = null
        var minDistance = Float.MAX_VALUE

        keyCenters.forEach { (char, center) ->
            val dist = (point - center).getDistance()
            if (dist < minDistance) {
                minDistance = dist
                closestChar = char
            }
        }
        
        // Threshold: if closest key is too far (e.g. outside keyboard), ignore
        // For now, accept best match
        return closestChar
    }

    /**
     * Calculates the "cost" of matching a word to the path.
     * Lower cost = Better match.
     * 
     * Logic: Sum of minimum distances from each letter in the word to the path.
     * The path must visit the letters in order.
     */
    private fun calculatePathCost(word: String, path: List<Offset>): Double {
        var currentPathIndex = 0
        var totalCost = 0.0
        
        // Cost 1: Letter-to-Path distance
        // For each letter in word, find the closest point in the path *after* the previous letter's match
        for (char in word) {
            val keyCenter = keyCenters[char] ?: continue
            
            // Find closest point in remaining path
            var localMinDist = Double.MAX_VALUE
            var bestIndex = currentPathIndex
            
            // Optimization: Look through the path starting from where we left off
            // We scan a bit forward to find the local minimum
            for (i in currentPathIndex until path.size) {
                val dist = (keyCenter - path[i]).getDistance().toDouble()
                if (dist < localMinDist) {
                    localMinDist = dist
                    bestIndex = i
                }
            }
            
            totalCost += localMinDist
            currentPathIndex = bestIndex // Advance path index (enforce order)
        }

        // Cost 2: Path Length Ration (Did we trace extra loops?)
        // Calculate actual path length
        var pathLength = 0.0
        for (i in 0 until path.size - 1) {
            pathLength += (path[i+1] - path[i]).getDistance()
        }
        
        // Calculate ideal path length (straight lines between keys)
        var idealLength = 0.0
        for (i in 0 until word.length - 1) {
            val p1 = keyCenters[word[i]] ?: Offset.Zero
            val p2 = keyCenters[word[i+1]] ?: Offset.Zero
            idealLength += (p2 - p1).getDistance()
        }
        
        val lengthPenalty = abs(pathLength - idealLength)
        
        return totalCost + lengthPenalty
    }
}
