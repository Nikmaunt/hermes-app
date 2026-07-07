package app.hermes.core.brain.fake

import app.hermes.core.brain.BrainErrorKind
import app.hermes.core.brain.BrainProvider
import app.hermes.core.brain.BrainResult
import app.hermes.core.brain.Completion
import app.hermes.core.brain.CompletionRequest
import app.hermes.core.brain.ProviderDescriptor
import app.hermes.core.brain.TokenUsage

/**
 * Deterministic in-memory provider for tests and the Demo mode. It reads NO clock and
 * NO randomness (D22): every reply comes from the injected [handler], which sees the
 * request and a 0-based call index — so a test can script attempt-1 vs attempt-2 of the
 * retry path, and Demo mode can match on the note content. Fed the same calls, one
 * instance always produces the same output.
 *
 * Because a FakeProvider returns raw text, its output flows through the SAME
 * ExtractionValidator as a real provider — demo fixtures are therefore the genuine
 * product of the pipeline, not a bypass (D18).
 */
class FakeProvider(
    override val descriptor: ProviderDescriptor = DEFAULT_DESCRIPTOR,
    private val handler: (request: CompletionRequest, callIndex: Int) -> BrainResult<Completion>,
) : BrainProvider {

    private var callIndex = 0

    override suspend fun complete(request: CompletionRequest): BrainResult<Completion> = handler(request, callIndex++)

    companion object {
        val DEFAULT_DESCRIPTOR = ProviderDescriptor(id = "fake", modelName = "fake-1")
        val DEFAULT_USAGE = TokenUsage(promptTokens = 12, completionTokens = 34)

        /** Returns [text] for every call. */
        fun single(text: String, usage: TokenUsage = DEFAULT_USAGE): FakeProvider =
            FakeProvider { _, _ -> BrainResult.Ok(Completion(text, usage)) }

        /**
         * The Nth call returns the Nth element; the last element repeats. Scripts the
         * retry path (attempt 1 then attempt 2) deterministically.
         */
        fun sequence(vararg texts: String, usage: TokenUsage = DEFAULT_USAGE): FakeProvider =
            FakeProvider { _, i -> BrainResult.Ok(Completion(texts[minOf(i, texts.lastIndex)], usage)) }

        /** Always fails at the transport layer with a leak-free [message]. */
        fun failing(kind: BrainErrorKind, message: String): FakeProvider =
            FakeProvider { _, _ -> BrainResult.Failure(kind, message) }
    }
}
