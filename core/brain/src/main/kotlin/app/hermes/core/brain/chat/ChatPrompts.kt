package app.hermes.core.brain.chat

import app.hermes.core.brain.ChatMessage
import app.hermes.core.model.ChatRole

/**
 * Builds the grounded system message for a chat turn from [ChatContext]. Minimal in M0
 * (real grounding is M5); the shape is what matters — retrieved memory + pending
 * followups + preferred output language are woven in here, from data passed by the
 * caller, so brain stays free of :core:data.
 */
internal object ChatPrompts {

    fun grounded(history: List<ChatMessage>, context: ChatContext): List<ChatMessage> {
        val system = ChatMessage(ChatRole.SYSTEM, systemPrompt(context))
        return listOf(system) + history
    }

    private fun systemPrompt(context: ChatContext): String = buildString {
        append("You are Hermes, the user's private assistant.")
        context.brain.language?.let { append(" Reply in ").append(it).append('.') }
        if (context.retrievedFacts.isNotEmpty()) {
            append("\nWhat you remember about the user:")
            context.retrievedFacts.forEach {
                append("\n- [").append(it.category.wire).append("] ").append(it.topic).append(": ").append(it.fact)
            }
        }
        if (context.recentFollowups.isNotEmpty()) {
            append("\nPending followups:")
            context.recentFollowups.forEach { append("\n- ").append(it.title) }
        }
    }
}
