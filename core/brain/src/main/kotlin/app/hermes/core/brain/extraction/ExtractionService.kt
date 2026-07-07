package app.hermes.core.brain.extraction

import app.hermes.core.brain.BrainContext
import app.hermes.core.brain.BrainProvider
import app.hermes.core.brain.BrainResult
import app.hermes.core.brain.ChatMessage
import app.hermes.core.brain.CompletionRequest
import app.hermes.core.brain.ResponseFormat
import app.hermes.core.brain.TokenUsage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

/**
 * Orchestrates one extraction: build prompt → call the transport → decode → validate →
 * (hardened retry once if the reply was unstructured or every item was rejected) →
 * terminal [ExtractionOutcome]. Written once, provider-agnostic (D18).
 *
 * Determinism: extraction runs at [EXTRACTION_TEMPERATURE] (0.0) so a re-run tends to
 * reproduce the same output. This is only a TENDENCY — the hard idempotency guarantee
 * (a replayed extraction is a no-op) lives in the persistence layer via
 * `transactions.dedupKey` and the followups `(sourceNoteId, dueAt)` repo dedup check
 * (D15), NOT in brain.
 *
 * The source note is preserved by the caller (M2) in every branch — extraction never
 * destroys it (§6): a transport [BrainResult.Failure], an [ExtractionOutcome.Unusable],
 * an [ExtractionOutcome.TooLarge], or a partial result with [RejectedItem]s all leave
 * the note intact for review.
 */
class ExtractionService(
    private val provider: BrainProvider,
    private val validator: ExtractionValidator = ExtractionValidator(),
    private val maxNoteChars: Int = DEFAULT_MAX_NOTE_CHARS,
) {
    suspend fun extract(request: ExtractionRequest): BrainResult<ExtractionOutcome> {
        if (request.noteText.length > maxNoteChars) {
            return BrainResult.Ok(ExtractionOutcome.TooLarge(request.noteText.length, maxNoteChars))
        }
        val first = attempt(ExtractionPrompts.initial(request), request.context)
        if (first is AttemptResult.Transport) return first.failure
        val a1 = first as AttemptResult.Decoded
        if (a1.result != null && !a1.result.allRejected) {
            return BrainResult.Ok(outcome(a1, attempts = 1, usage = a1.usage))
        }
        // Malformed or all-rejected → one hardened retry that names the concrete problems.
        val hints = a1.result?.rejected?.flatMap { it.violations } ?: listOf(MALFORMED)
        val retry = attempt(ExtractionPrompts.hardened(request, hints), request.context)
        val a2 = retry as? AttemptResult.Decoded
        val best = chooseBest(a1, a2)
        val usage = a1.usage + (a2?.usage ?: TokenUsage.NONE)
        return BrainResult.Ok(outcome(best, attempts = ATTEMPTS_WITH_RETRY, usage = usage))
    }

    private suspend fun attempt(messages: List<ChatMessage>, context: BrainContext): AttemptResult {
        val request = CompletionRequest(
            messages = messages,
            responseFormat = ResponseFormat.Json(),
            temperature = EXTRACTION_TEMPERATURE,
        )
        return when (val r = provider.complete(request)) {
            is BrainResult.Failure -> AttemptResult.Transport(r)
            is BrainResult.Ok -> AttemptResult.Decoded(parse(r.value.text, context), r.value.text, r.value.usage)
        }
    }

    private fun parse(text: String, context: BrainContext): ExtractionResult? {
        val root = try {
            ExtractionJson.parseToJsonElement(text) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return null
        return validator.validate(root, context)
    }

    /** Prefer the retry when it decoded and did at least as well; if both replies were
     *  unstructured keep the latest (its raw text is what the user sees). */
    private fun chooseBest(a1: AttemptResult.Decoded, a2: AttemptResult.Decoded?): AttemptResult.Decoded {
        if (a2 == null) return a1
        val acc1 = a1.result?.acceptedCount ?: -1
        val acc2 = a2.result?.acceptedCount ?: -1
        val preferA2 = when {
            a2.result != null -> acc2 >= acc1
            a1.result == null -> true
            else -> false
        }
        return if (preferA2) a2 else a1
    }

    private fun outcome(a: AttemptResult.Decoded, attempts: Int, usage: TokenUsage): ExtractionOutcome {
        val result = a.result ?: return ExtractionOutcome.Unusable(a.rawText, listOf(MALFORMED), usage)
        return if (result.isEmpty) {
            ExtractionOutcome.Empty(usage)
        } else {
            ExtractionOutcome.Extracted(result, usage, attempts)
        }
    }

    private sealed interface AttemptResult {
        data class Transport(val failure: BrainResult.Failure) : AttemptResult
        data class Decoded(val result: ExtractionResult?, val rawText: String, val usage: TokenUsage) : AttemptResult
    }

    companion object {
        /** Deterministic-as-possible extraction (see class doc). */
        const val EXTRACTION_TEMPERATURE = 0.0

        /**
         * Input budget in characters. Above it we return [ExtractionOutcome.TooLarge]
         * WITHOUT calling the model — a shared PDF is never half-lost to a silent
         * truncation (D25). Conservative; refined per real provider limits in M1.
         */
        const val DEFAULT_MAX_NOTE_CHARS = 24_000

        private const val ATTEMPTS_WITH_RETRY = 2
        private val MALFORMED = Violation("response", ViolationCode.MALFORMED_JSON, "reply was not valid JSON")
    }
}
