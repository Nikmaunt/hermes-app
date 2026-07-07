package app.hermes.core.brain.chat

import app.hermes.core.brain.BrainContext
import app.hermes.core.brain.BrainProvider
import app.hermes.core.brain.BrainResult
import app.hermes.core.brain.ChatMessage
import app.hermes.core.brain.Completion
import app.hermes.core.brain.CompletionRequest
import app.hermes.core.brain.ResponseFormat
import app.hermes.core.brain.TokenUsage
import app.hermes.core.model.MemoryCategory

/**
 * The context a chat turn is grounded in. Shaped now (D26) so that M5 retrieval can be
 * injected from the data layer WITHOUT :core:brain ever depending on :core:data (D18):
 * the data layer materializes [retrievedFacts] / [recentFollowups] and passes them in.
 * The [ChatService.reply] signature does not change in M5 — only these lists fill up.
 */
data class ChatContext(
    val brain: BrainContext,
    val retrievedFacts: List<RetrievedFact> = emptyList(),
    val recentFollowups: List<RetrievedFollowup> = emptyList(),
)

/** A memory fact retrieved for grounding (a read-model, not the Room entity). */
data class RetrievedFact(val topic: String, val fact: String, val category: MemoryCategory)

/** A pending followup surfaced for grounding (a read-model, not the Room entity). */
data class RetrievedFollowup(val title: String, val dueAtMillis: Long, val isDateOnly: Boolean)

data class ChatReply(val content: String, val usage: TokenUsage)

/**
 * Chat turn: build a grounded prompt from [ChatContext] and the running [history], call
 * the transport, map to a [ChatReply]. brain does not persist anything — the caller
 * writes `chat_messages` / `spend_log` from the returned usage.
 */
class ChatService(private val provider: BrainProvider) {

    suspend fun reply(history: List<ChatMessage>, context: ChatContext): BrainResult<ChatReply> {
        val request = CompletionRequest(
            messages = ChatPrompts.grounded(history, context),
            responseFormat = ResponseFormat.Text,
        )
        return when (val r = provider.complete(request)) {
            is BrainResult.Ok -> BrainResult.Ok(toReply(r.value))
            is BrainResult.Failure -> r
        }
    }

    private fun toReply(completion: Completion): ChatReply =
        ChatReply(content = completion.text, usage = completion.usage)
}
