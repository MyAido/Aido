package com.rr.aido.utils

import com.rr.aido.data.models.Preprompt

object PromptParser {

    const val DEFAULT_SMART_REPLY_INSTRUCTIONS = """The user is typing a reply in a messaging app.

CRITICAL LANGUAGE DETECTION:
1. Look at the MOST RECENT messages in the conversation (ignore app UI text)
2. Detect the exact language used in the last 2-3 messages
3. The MOST RECENT message has the HIGHEST priority for language detection
4. Common languages: English, Bengali (বাংলা), Hindi (हिन्दी)

CRITICAL LANGUAGE MATCHING:
1. Generate replies in the EXACT SAME language as the most recent messages
2. If the recent messages are in English, reply ONLY in English
3. If the recent messages are in Bengali, reply ONLY in Bengali
4. If the recent messages are in Hindi, reply ONLY in Hindi
5. DO NOT change language or mix languages
6. DO NOT default to any particular language - match what you see

Based on the conversation context, suggest 6 short, natural, and contextually relevant replies.
Output ONLY the replies, one per line. Do not include numbering or quotes."""

    const val DEFAULT_TONE_REWRITE_INSTRUCTIONS = """CRITICAL LANGUAGE INSTRUCTION:
1. First, identify the language of the original text above
2. Rewrite ONLY in that exact same language
3. If original is in Bengali, rewrite in Bengali
4. If original is in English, rewrite in English
5. If original is in Hindi, rewrite in Hindi
6. DO NOT change the language or default to Hindi/English

Rewrite the above text in 6 different tones:
1. Professional
2. Casual/Friendly
3. Witty/Creative
4. Empathetic/Supportive
5. Confident/Assertive
6. Polite/Formal

Output ONLY the rewritten versions, one per line. Do not include numbering or labels."""

    fun extractTrigger(input: String): String? {
        // Koi bhi special symbol + alphanumeric word
        val regex = "[`~!@#$%^&*()\\-_=+\\[\\]{}\\\\|;:'\",<.>/\\?]\\w+".toRegex()
        return regex.findAll(input).lastOrNull()?.value
    }

    fun removeTriger(input: String): String {
        val trigger = extractTrigger(input)
        return if (trigger != null) {
            input.replace(trigger, "").trim()
        } else {
            input.trim()
        }
    }

    fun buildFinalPrompt(text: String, preprompt: Preprompt?): String {
        return if (preprompt != null) {
            "${preprompt.instruction} $text"
        } else {
            text
        }
    }

    fun parseInput(input: String, preprompts: List<Preprompt>): ParseResult {
        val trigger = extractTrigger(input)
        val cleanText = removeTriger(input)

        val matchedPreprompt = if (trigger != null) {
            preprompts.find { it.trigger == trigger }
        } else {
            null
        }

        val finalPrompt = buildFinalPrompt(cleanText, matchedPreprompt)

        return ParseResult(
            originalInput = input,
            trigger = trigger,
            cleanText = cleanText,
            matchedPreprompt = matchedPreprompt,
            finalPrompt = finalPrompt
        )
    }
}

data class ParseResult(
    val originalInput: String,
    val trigger: String?,
    val cleanText: String,
    val matchedPreprompt: Preprompt?,
    val finalPrompt: String
)
