package app.hermes.core.brain

import app.hermes.core.model.ChatRole

/**
 * The low-level transport every provider (Fake, OpenRouter, on-device ML Kit GenAI)
 * implements. It returns raw model text plus token usage and knows nothing about
 * prompts, validation, or retries — those live ONCE in the provider-agnostic services
 * (ChatService, ExtractionService). See decision-log D18.
 *
 * Implementations are `suspend` and cancellable: a `CancellationException` MUST
 * propagate (cooperative cancellation) and is never mapped to a [BrainResult.Failure].
 */
interface BrainProvider {
    val descriptor: ProviderDescriptor

    /**
     * Produce one completion. [CompletionRequest.responseFormat] is a hint to the
     * provider only; our own validator never trusts provider-side schema enforcement
     * (untrusted-output policy, D19).
     */
    suspend fun complete(request: CompletionRequest): BrainResult<Completion>
}

/** Identifies the provider/model behind a call; [modelName] feeds `spend_log.model`. */
data class ProviderDescriptor(val id: String, val modelName: String)

data class CompletionRequest(
    val messages: List<ChatMessage>,
    val responseFormat: ResponseFormat = ResponseFormat.Text,
    val maxOutputTokens: Int? = null,
    val temperature: Double? = null,
)

/**
 * How the model should shape its reply. [Json] carries an optional schema hint for
 * providers that support structured output; it is NEVER a substitute for our own
 * validation of the returned text (D19).
 */
sealed interface ResponseFormat {
    data object Text : ResponseFormat

    data class Json(val schemaHint: String? = null) : ResponseFormat
}

data class Completion(val text: String, val usage: TokenUsage)

data class ChatMessage(val role: ChatRole, val content: String)

/** Prompt/completion token counts; maps to `chat_messages` and `spend_log`. */
data class TokenUsage(val promptTokens: Int, val completionTokens: Int) {
    operator fun plus(other: TokenUsage): TokenUsage =
        TokenUsage(promptTokens + other.promptTokens, completionTokens + other.completionTokens)

    companion object {
        val NONE = TokenUsage(promptTokens = 0, completionTokens = 0)
    }
}

/**
 * Outcome of a transport call. [Failure.message] is human-readable and leak-free: a
 * provider must NEVER place the API key, the prompt, or a raw upstream body here
 * (security rule §4). Cancellation is not represented here — it propagates as an
 * exception.
 */
sealed interface BrainResult<out T> {
    data class Ok<T>(val value: T) : BrainResult<T>

    data class Failure(val kind: BrainErrorKind, val message: String) : BrainResult<Nothing>
}

enum class BrainErrorKind { NETWORK, TIMEOUT, AUTH, RATE_LIMIT, INVALID_RESPONSE, UNKNOWN }
