package app.hermes.core.brain.extraction

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lenient JSON reader for UNTRUSTED model output: unknown keys are ignored, absent
 * fields fall back to null/defaults, and mild syntax slack is tolerated. Meaning is
 * checked afterwards by [ExtractionValidator] — decoding is only the first, structural
 * gate (D19).
 */
internal val ExtractionJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

/**
 * Wire form of a date the model emits: exactly one of [date] (YYYY-MM-DD) or
 * [dateTime] (YYYY-MM-DDTHH:MM[:SS], resolved in the context zone). We never accept
 * epoch millis from the model (LLMs are bad at them) and never NL-parse dates in
 * Kotlin — the model resolves relative dates against the injected context (D20).
 */
@Serializable
internal data class WireWhen(val date: String? = null, val dateTime: String? = null)

@Serializable
internal data class WireMoney(val amountMinor: Long? = null, val currency: String? = null)

@Serializable
internal data class WireMemoryFact(
    val category: String? = null,
    val topic: String? = null,
    val fact: String? = null,
    val sensitive: Boolean? = null,
)

@Serializable
internal data class WireFollowup(
    val title: String? = null,
    val due: WireWhen? = null,
    val criticality: String? = null,
    val note: String? = null,
)

@Serializable
internal data class WirePerson(
    val name: String? = null,
    val relation: String? = null,
    val context: String? = null,
    val preferredLanguage: String? = null,
)

@Serializable
internal data class WireProject(
    val name: String? = null,
    val status: String? = null,
    val nextAction: String? = null,
)

@Serializable
internal data class WireDecision(
    val title: String? = null,
    val rationale: String? = null,
    val decidedAt: WireWhen? = null,
    val projectRef: String? = null,
)

@Serializable
internal data class WireHabit(
    val name: String? = null,
    val cadence: String? = null,
    val schedule: String? = null,
)

@Serializable
internal data class WireDocument(
    val title: String? = null,
    val kind: String? = null,
    val provider: String? = null,
    val amount: WireMoney? = null,
    val billingPeriod: String? = null,
    val renewsOn: String? = null,
    val cancelBy: String? = null,
    val notes: String? = null,
)

@Serializable
internal data class WireTransaction(
    val amount: WireMoney? = null,
    val merchant: String? = null,
    val category: String? = null,
    val occurredAt: WireWhen? = null,
)

/** JSON keys of the top-level extraction object, one per target list. */
internal object WireKeys {
    const val SUMMARY = "summary"
    const val MEMORY_FACTS = "memoryFacts"
    const val FOLLOWUPS = "followups"
    const val PEOPLE = "people"
    const val PROJECTS = "projects"
    const val DECISIONS = "decisions"
    const val HABITS = "habits"
    const val DOCUMENTS = "documents"
    const val TRANSACTIONS = "transactions"
}
