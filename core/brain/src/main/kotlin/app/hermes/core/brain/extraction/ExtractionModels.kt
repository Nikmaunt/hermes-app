package app.hermes.core.brain.extraction

import app.hermes.core.brain.BrainContext
import app.hermes.core.brain.TokenUsage
import app.hermes.core.model.BillingPeriod
import app.hermes.core.model.Criticality
import app.hermes.core.model.DocumentKind
import app.hermes.core.model.HabitCadence
import app.hermes.core.model.MemoryCategory
import app.hermes.core.model.Money
import app.hermes.core.model.ProjectStatus

/** A note handed to the extractor together with the ambient [context]. */
data class ExtractionRequest(val noteText: String, val context: BrainContext)

/**
 * The parsed, validated form of a due/occurred instant. Maps to
 * `followups.dueAt` / `dueIsDateOnly` / `dueTimeZone`. Date-only values are the
 * UTC-midnight millis produced by CalendarDates; timed values are true instants (D6).
 */
data class ExtractedInstant(val epochMillis: Long, val isDateOnly: Boolean, val zoneId: String)

// --- Extracted domain items -------------------------------------------------------
// The model NEVER emits id / createdAt / sourceNoteId / dedupKey / a source enum:
// those are stamped by the persistence layer (M2) — see D20/D24. Each item below
// carries only the semantic fields the model can legitimately produce.

data class ExtractedMemoryFact(
    val category: MemoryCategory,
    val topic: String,
    val fact: String,
    val sensitive: Boolean = false,
)

data class ExtractedFollowup(
    val title: String,
    /** REQUIRED. A "followup" with no resolvable date is not a followup — it is
     *  rejected, never stored dateless (D8/D21). Never defaulted to now. */
    val due: ExtractedInstant,
    val criticality: Criticality = Criticality.NORMAL,
    val note: String? = null,
)

data class ExtractedPerson(
    val name: String,
    val relation: String? = null,
    val context: String? = null,
    val preferredLanguage: String? = null,
)

data class ExtractedProject(
    val name: String,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val nextAction: String? = null,
)

data class ExtractedDecision(
    val title: String,
    val rationale: String? = null,
    /** Instant millis; defaults to `context.nowMillis` if the model omits it (D23). */
    val decidedAt: Long,
    /** Logical project name-reference, resolved to a projectId in M2. */
    val projectRef: String? = null,
)

data class ExtractedHabit(
    val name: String,
    val cadence: HabitCadence = HabitCadence.DAILY,
    val schedule: String? = null,
)

data class ExtractedDocument(
    val title: String,
    val kind: DocumentKind,
    val provider: String? = null,
    /** Null (no amount) or a complete [Money]; never a coerced zero (D13). */
    val money: Money? = null,
    val billingPeriod: BillingPeriod? = null,
    /** Date-only millis; display + 30-day-window only, never an alarm (D8). */
    val renewsOn: Long? = null,
    val cancelBy: Long? = null,
    val notes: String? = null,
)

data class ExtractedTransaction(
    /** REQUIRED complete money (amount + currency). No `source` — the system stamps
     *  it at persist time (D24). */
    val amount: Money,
    val merchant: String? = null,
    val category: String? = null,
    /** Instant millis; defaults to `context.nowMillis` if the model omits it. */
    val occurredAt: Long,
)

/** Which target type a rejected raw fragment was aiming at. */
enum class ExtractedKind { MEMORY_FACT, FOLLOWUP, PERSON, PROJECT, DECISION, HABIT, DOCUMENT, TRANSACTION, UNKNOWN }

enum class ViolationCode {
    MALFORMED_JSON,
    MISSING_REQUIRED,
    BLANK_STRING,
    UNKNOWN_ENUM,
    UNPARSEABLE_DATE,
    DATE_OUT_OF_RANGE,
    MISSING_DUE_DATE,
    MONEY_INCOMPLETE,
    NUMBER_OUT_OF_RANGE,
    ENUM_DEGRADED,
    DATE_DEFAULTED,
    TOO_MANY_ITEMS,
}

/**
 * One validation problem. [field] is a JSON-ish path (e.g. "followups[0].due").
 * [detail] is leak-free and doubles as the hint fed into the hardened-retry prompt.
 */
data class Violation(val field: String, val code: ViolationCode, val detail: String)

/**
 * A hard-violated item, dropped from the accepted lists but KEPT for the review UI
 * (M2) with its exact raw fragment — never silently discarded (project-wide
 * no-silent-loss principle). [rawJson] is the element exactly as the model emitted it.
 */
data class RejectedItem(val kind: ExtractedKind, val rawJson: String, val violations: List<Violation>)

/**
 * The validated product of one decoded reply. [rejected] carries hard-violated items
 * for review; [warnings] carries soft notes where an item survived with a default
 * (degraded enum, defaulted occurredAt/decidedAt, list-cap overflow) — both are shown
 * to the user, nothing is lost silently.
 */
data class ExtractionResult(
    val summary: String? = null,
    val memoryFacts: List<ExtractedMemoryFact> = emptyList(),
    val followups: List<ExtractedFollowup> = emptyList(),
    val people: List<ExtractedPerson> = emptyList(),
    val projects: List<ExtractedProject> = emptyList(),
    val decisions: List<ExtractedDecision> = emptyList(),
    val habits: List<ExtractedHabit> = emptyList(),
    val documents: List<ExtractedDocument> = emptyList(),
    val transactions: List<ExtractedTransaction> = emptyList(),
    val rejected: List<RejectedItem> = emptyList(),
    val warnings: List<Violation> = emptyList(),
) {
    val acceptedCount: Int
        get() = memoryFacts.size + followups.size + people.size + projects.size +
            decisions.size + habits.size + documents.size + transactions.size

    /** No accepted items AND nothing rejected → the note yielded nothing to file. */
    val isEmpty: Boolean get() = acceptedCount == 0 && rejected.isEmpty()

    /** Items were attempted but all of them hard-violated — a hardened retry is worth
     *  one shot before giving up. */
    val allRejected: Boolean get() = acceptedCount == 0 && rejected.isNotEmpty()
}

/**
 * Terminal result of an extraction attempt. In EVERY case the source note is preserved
 * by the caller (M2): extraction never destroys the note (§6).
 */
sealed interface ExtractionOutcome {
    /** At least one accepted item OR at least one rejected-for-review item. */
    data class Extracted(val result: ExtractionResult, val usage: TokenUsage, val attempts: Int) : ExtractionOutcome

    /** The model replied, but there was nothing to extract (e.g. chit-chat). */
    data class Empty(val usage: TokenUsage) : ExtractionOutcome

    /**
     * The reply could not be structured into items even after a hardened retry.
     * [rawText] is kept for the review UI so the user can still see what came back.
     */
    data class Unusable(val rawText: String, val violations: List<Violation>, val usage: TokenUsage) : ExtractionOutcome

    /**
     * The note is larger than the provider input budget. The model was NOT called and
     * the note is never silently truncated (D25). Chunking is a later milestone.
     */
    data class TooLarge(val noteChars: Int, val limitChars: Int) : ExtractionOutcome
}
