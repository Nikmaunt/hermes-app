package app.hermes.demo

import app.hermes.core.brain.BrainContext
import app.hermes.core.brain.BrainResult
import app.hermes.core.brain.Completion
import app.hermes.core.brain.CompletionRequest
import app.hermes.core.brain.TokenUsage
import app.hermes.core.brain.extraction.ExtractionService
import app.hermes.core.brain.fake.FakeProvider
import app.hermes.core.model.ChatRole

/**
 * The Demo "brain": a single deterministic [FakeProvider] that answers each demo note
 * with its scripted raw JSON by matching on the note text carried in the request (the
 * content-matching mode the FakeProvider is built for). Because it returns raw text, the
 * output runs through the real [ExtractionService] / ExtractionValidator — the demo
 * database is the genuine product of the pipeline, not a bypass (D18).
 *
 * Everything here is deterministic: no clock, no randomness. Time comes from
 * [brainContext] (D22).
 */
object DemoBrain {

    private val USAGE = TokenUsage(promptTokens = 0, completionTokens = 0)

    /** now/zone/language the demo pipeline resolves relative dates against (D22). */
    fun brainContext(): BrainContext = BrainContext(
        nowMillis = DemoNotes.NOW_MILLIS,
        zoneId = DemoNotes.ZONE_ID,
        language = DemoNotes.LANGUAGE,
    )

    /** A provider that maps each note's text → its scripted reply; an unknown note yields
     *  an empty object (the pipeline then reports Empty), so it can never throw. */
    fun provider(notes: List<DemoNote> = DemoNotes.ALL): FakeProvider {
        val byText: Map<String, String> = notes.associate { it.text to it.rawModelJson }
        return FakeProvider { request, _ ->
            val noteText = request.lastUserText()
            BrainResult.Ok(Completion(byText[noteText] ?: EMPTY_REPLY, USAGE))
        }
    }

    fun service(notes: List<DemoNote> = DemoNotes.ALL): ExtractionService = ExtractionService(provider(notes))

    private fun CompletionRequest.lastUserText(): String? = messages.lastOrNull { it.role == ChatRole.USER }?.content

    private const val EMPTY_REPLY = "{}"
}
