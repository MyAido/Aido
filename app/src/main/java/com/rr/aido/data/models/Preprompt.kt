package com.rr.aido.data.models

data class Preprompt(
    val trigger: String,           // e.g., "@aido", "~test", "!fix"
    val instruction: String,        // Instruction jo prompt ke pehle add hoga
    val example: String = "",       // Example text (optional)
    val isDefault: Boolean = false  // Default preprompts ko delete nahi kar sakte
)

object DefaultPreprompts {
    val list = listOf(
        Preprompt(
            trigger = "@aido",
            instruction = "Provide answers in the same language as the question. Be direct and concise. Skip unnecessary explanations, introductions, or filler text. Give only the essential information needed.",
            example = "What is the capital of USA@aido",
            isDefault = true
        ),
        Preprompt(
            trigger = "@fixg",
            instruction = "Correct all grammar, spelling, and punctuation mistakes in the text. Keep the original meaning and writing style unchanged. Output only the corrected text with no added words or explanations.",
            example = "This sentence are wrong@fixg",
            isDefault = true
        ),
        Preprompt(
            trigger = "@summ",
            instruction = "Summarize the text into exactly one or two short sentences. Capture only the main idea. Be extremely concise. Respond in the same language as the original text. Output ONLY the summary.",
            example = "Long text@summ",
            isDefault = true
        ),
        Preprompt(
            trigger = "@polite",
            instruction = "Rewrite the text in a polite, respectful, and professional tone suitable for formal communication. Keep the original meaning unchanged. Output only the rewritten text with no added words, explanations, or formatting.",
            example = "I need this now@polite",
            isDefault = true
        ),
        Preprompt(
            trigger = "@casual",
            instruction = "Rewrite the text in a natural, casual, and friendly tone, as if speaking to someone you know well. Keep the meaning and clarity of the original text. Output only the rewritten version with no added words, explanations, or formatting.",
            example = "Dear Sir@casual",
            isDefault = true
        ),
        Preprompt(
            trigger = "@expand",
            instruction = "Expand the text by adding relevant details, explanations, or examples while keeping the original meaning and tone. Avoid repetition or unnecessary words. Output only the expanded version with no extra formatting or explanations.",
            example = "AI is useful@expand",
            isDefault = true
        ),
        Preprompt(
            trigger = "@bullet",
            instruction = "Convert the text into clear and concise bullet points. Preserve all key information and meaning. Use a simple hyphen (-) or bullet (•) for each line. Output only the formatted list with no titles, introductions, or explanations.",
            example = "Convert to bullets@bullet",
            isDefault = true
        ),
        Preprompt(
            trigger = "@improve",
            instruction = "Enhance the writing for clarity, flow, and readability while keeping the original meaning and tone. Correct any minor grammar or word-choice issues if needed. Output only the improved text with no added explanations or formatting.",
            example = "Make this better@improve",
            isDefault = true
        ),
        Preprompt(
            trigger = "@rephrase",
            instruction = "Rephrase the text using different wording and sentence structure while keeping the exact same meaning and tone. Do not shorten or expand the content. Output only the rephrased text with no added explanations or formatting.",
            example = "Hello world@rephrase",
            isDefault = true
        ),
        Preprompt(
            trigger = "@emoji",
            instruction = "Add a few relevant emojis inline to make the text more expressive and engaging. Use only emojis that match the meaning and tone of each part of the text. Keep the original wording unchanged and avoid adding extra words. Output only the enhanced text with no explanations or formatting.",
            example = "Great news@emoji",
            isDefault = true
        ),
        Preprompt(
            trigger = "@formal",
            instruction = "Rewrite the text in a clear, formal, and professional business tone suitable for workplace communication. Keep the meaning, intent, and length of the original message. Avoid unnecessary words or overly rigid phrasing. Output only the rewritten text with no explanations or formatting.",
            example = "Hey what's up@formal",
            isDefault = true
        ),
        Preprompt(
            trigger = "@funny",
            instruction = "Rewrite the text with a light, clever, and natural sense of humor while keeping the original meaning and context. The humor should feel conversational and appropriate, not exaggerated or off-topic. Output only the rewritten text with no added explanations or formatting.",
            example = "I am tired@funny",
            isDefault = true
        ),
        Preprompt(
            trigger = "@prompt",
            instruction = "Act as an expert prompt engineer. Rewrite the user's input into a highly effective, clear, and detailed prompt for an AI model. Ensure the intent is preserved but the phrasing is optimized for the best possible AI response. Return ONLY the refined prompt.",
            example = "write email for leave@prompt",
            isDefault = true
        )
    )
}
