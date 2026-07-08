package app.hermes.demo

import app.hermes.core.brain.BrainContext
import app.hermes.core.brain.BrainResult
import app.hermes.core.brain.extraction.ExtractedDecision
import app.hermes.core.brain.extraction.ExtractedDocument
import app.hermes.core.brain.extraction.ExtractedFollowup
import app.hermes.core.brain.extraction.ExtractedHabit
import app.hermes.core.brain.extraction.ExtractedMemoryFact
import app.hermes.core.brain.extraction.ExtractedPerson
import app.hermes.core.brain.extraction.ExtractedProject
import app.hermes.core.brain.extraction.ExtractedTransaction
import app.hermes.core.brain.extraction.ExtractionOutcome
import app.hermes.core.brain.extraction.ExtractionRequest
import app.hermes.core.brain.extraction.ExtractionResult
import app.hermes.core.brain.extraction.ExtractionService
import app.hermes.core.data.entity.DecisionEntity
import app.hermes.core.data.entity.DocumentEntity
import app.hermes.core.data.entity.FollowupEntity
import app.hermes.core.data.entity.HabitEntity
import app.hermes.core.data.entity.MemoryFactEntity
import app.hermes.core.data.entity.NoteEntity
import app.hermes.core.data.entity.PersonEntity
import app.hermes.core.data.entity.ProjectEntity
import app.hermes.core.data.entity.TransactionEntity
import app.hermes.core.model.DedupKeys
import app.hermes.core.model.FollowupStatus
import app.hermes.core.model.Ids
import app.hermes.core.model.MemorySource
import app.hermes.core.model.NoteSource
import app.hermes.core.model.NoteStatus
import app.hermes.core.model.Sensitivity
import app.hermes.core.model.TransactionSource
import java.util.Locale

/**
 * Runs the demo notes through the real extraction pipeline and maps each accepted
 * `Extracted*` item to its Room row, stamping the id / createdAt / sourceNoteId (and, for
 * transactions, the dedup key) the model is never allowed to emit (D20/D24). This is a
 * DEMO-SCOPED preview of the mapping the M2 data layer will own for real; it lives in
 * `:app`, not `:core:data`, so it does not freeze the M2 contract early (D18).
 */
class DemoDataBuilder(
    private val service: ExtractionService = DemoBrain.service(),
    private val context: BrainContext = DemoBrain.brainContext(),
    private val notes: List<DemoNote> = DemoNotes.ALL,
) {
    suspend fun build(): DemoData {
        val acc = DemoAcc()
        notes.forEachIndexed { index, note ->
            // Deterministic, descending timestamps: index 0 (newest) sits at NOW.
            val createdAt = DemoNotes.NOW_MILLIS - index.toLong() * CREATED_STEP_MS
            persist(note, createdAt, extract(note), acc)
        }
        return acc.toDemoData()
    }

    private suspend fun extract(note: DemoNote): ExtractionResult? {
        val outcome = service.extract(ExtractionRequest(note.text, context))
        return ((outcome as? BrainResult.Ok)?.value as? ExtractionOutcome.Extracted)?.result
    }

    private fun persist(note: DemoNote, createdAt: Long, result: ExtractionResult?, acc: DemoAcc) {
        val noteId = Ids.new("note")
        val sensitive = result?.memoryFacts?.any { it.sensitive } == true
        acc.notes += NoteEntity(
            id = noteId,
            text = note.text,
            source = NoteSource.TEXT,
            status = NoteStatus.FILED,
            sensitivity = if (sensitive) Sensitivity.SENSITIVE else Sensitivity.NORMAL,
            createdAt = createdAt,
            reviewedAt = createdAt,
        )
        result ?: return

        result.memoryFacts.forEach { acc.memoryFacts += it.toEntity(noteId, createdAt) }
        result.followups.forEach { acc.followups += it.toEntity(noteId, createdAt) }
        result.people.forEach { acc.people += it.toEntity(noteId, createdAt) }
        result.habits.forEach { acc.habits += it.toEntity(noteId, createdAt) }
        result.documents.forEach { acc.documents += it.toEntity(noteId, createdAt) }
        result.transactions.forEach { acc.transactions += it.toEntity(noteId, createdAt) }

        // Projects first, so a decision in the same note can resolve its projectRef (a name)
        // to the project's freshly stamped id (M2 resolves across all projects; demo, in-note).
        val projectIdByName = mutableMapOf<String, String>()
        result.projects.forEach { project ->
            val entity = project.toEntity(noteId, createdAt)
            projectIdByName[project.name.lowercase(Locale.ROOT)] = entity.id
            acc.projects += entity
        }
        result.decisions.forEach { decision ->
            val projectId = decision.projectRef?.let { projectIdByName[it.lowercase(Locale.ROOT)] }
            acc.decisions += decision.toEntity(noteId, createdAt, projectId)
        }
    }

    private companion object {
        /** One hour between successive demo notes' createdAt — enough for a stable order. */
        const val CREATED_STEP_MS = 3_600_000L
    }
}

/** Row accumulator, kept off the builder so it stays a small orchestrator. */
private class DemoAcc {
    val notes = mutableListOf<NoteEntity>()
    val followups = mutableListOf<FollowupEntity>()
    val memoryFacts = mutableListOf<MemoryFactEntity>()
    val people = mutableListOf<PersonEntity>()
    val projects = mutableListOf<ProjectEntity>()
    val decisions = mutableListOf<DecisionEntity>()
    val habits = mutableListOf<HabitEntity>()
    val documents = mutableListOf<DocumentEntity>()
    val transactions = mutableListOf<TransactionEntity>()

    fun toDemoData() = DemoData(
        notes, followups, memoryFacts, people, projects,
        decisions, habits, documents, transactions,
    )
}

// --- Extracted* → Room row (stamping the provenance the model may not emit, D20/D24) ------

private fun ExtractedMemoryFact.toEntity(noteId: String, createdAt: Long) = MemoryFactEntity(
    id = Ids.new("mem"),
    category = category,
    topic = topic,
    fact = fact,
    sensitive = sensitive,
    source = MemorySource.CAPTURE,
    sourceNoteId = noteId,
    learnedAt = createdAt,
    createdAt = createdAt,
)

private fun ExtractedFollowup.toEntity(noteId: String, createdAt: Long) = FollowupEntity(
    id = Ids.new("fu"),
    title = title,
    dueAt = due.epochMillis,
    dueIsDateOnly = due.isDateOnly,
    dueTimeZone = due.zoneId,
    criticality = criticality,
    status = FollowupStatus.PENDING,
    note = note,
    sourceNoteId = noteId,
    createdAt = createdAt,
    updatedAt = createdAt,
)

private fun ExtractedPerson.toEntity(noteId: String, createdAt: Long) = PersonEntity(
    id = Ids.new("person"),
    name = name,
    relation = relation,
    context = context,
    preferredLanguage = preferredLanguage,
    sourceNoteId = noteId,
    createdAt = createdAt,
    updatedAt = createdAt,
)

private fun ExtractedProject.toEntity(noteId: String, createdAt: Long) = ProjectEntity(
    id = Ids.new("project"),
    name = name,
    status = status,
    nextAction = nextAction,
    sourceNoteId = noteId,
    createdAt = createdAt,
    updatedAt = createdAt,
)

private fun ExtractedDecision.toEntity(noteId: String, createdAt: Long, projectId: String?) = DecisionEntity(
    id = Ids.new("decision"),
    title = title,
    rationale = rationale,
    projectId = projectId,
    decidedAt = decidedAt,
    sourceNoteId = noteId,
    createdAt = createdAt,
)

private fun ExtractedHabit.toEntity(noteId: String, createdAt: Long) = HabitEntity(
    id = Ids.new("habit"),
    name = name,
    cadence = cadence,
    schedule = schedule,
    sourceNoteId = noteId,
    createdAt = createdAt,
)

private fun ExtractedDocument.toEntity(noteId: String, createdAt: Long) = DocumentEntity(
    id = Ids.new("doc"),
    title = title,
    kind = kind,
    provider = provider,
    amountMinor = money?.amountMinor,
    currency = money?.currency,
    billingPeriod = billingPeriod,
    renewsOn = renewsOn,
    cancelBy = cancelBy,
    notes = notes,
    sourceNoteId = noteId,
    createdAt = createdAt,
    updatedAt = createdAt,
)

private fun ExtractedTransaction.toEntity(noteId: String, createdAt: Long) = TransactionEntity(
    id = Ids.new("tx"),
    amountMinor = amount.amountMinor,
    currency = amount.currency,
    merchant = merchant,
    category = category,
    // A transaction extracted from a typed note is user-entered → MANUAL (the system stamps
    // source; the model never sees it, D24).
    source = TransactionSource.MANUAL,
    occurredAt = occurredAt,
    dedupKey = DedupKeys.transaction(
        source = TransactionSource.MANUAL.wire,
        occurredAt = occurredAt,
        amountMinor = amount.amountMinor,
        currency = amount.currency,
        merchant = merchant,
    ),
    sourceNoteId = noteId,
    createdAt = createdAt,
)
