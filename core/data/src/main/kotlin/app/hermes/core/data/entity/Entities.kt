package app.hermes.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.hermes.core.model.BillingPeriod
import app.hermes.core.model.ChatRole
import app.hermes.core.model.Criticality
import app.hermes.core.model.DocumentKind
import app.hermes.core.model.FollowupStatus
import app.hermes.core.model.HabitCadence
import app.hermes.core.model.MemoryCategory
import app.hermes.core.model.MemorySource
import app.hermes.core.model.Money
import app.hermes.core.model.NoteSource
import app.hermes.core.model.NoteStatus
import app.hermes.core.model.ProjectStatus
import app.hermes.core.model.Sensitivity
import app.hermes.core.model.SpendKind
import app.hermes.core.model.TransactionSource

/**
 * Raw capture. The [text] is immutable — extraction NEVER rewrites it (persist-first
 * invariant, tested in M2). The row is a soft-delete state machine via [status];
 * there is no hard delete anywhere in this schema.
 */
@Entity(
    tableName = "notes",
    indices = [Index("status"), Index("createdAt")],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val source: NoteSource,
    val attachmentUri: String? = null,
    val attachmentMime: String? = null,
    val status: NoteStatus = NoteStatus.RAW,
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val createdAt: Long,
    val reviewedAt: Long? = null,
)

/**
 * The ONLY home of a dated fact and the ONLY alarm-bearing table: [dueAt] is the
 * exact alarm instant (UTC millis). memory_facts deliberately has no date columns,
 * so a dated commitment never lives in two places. Index (sourceNoteId, dueAt)
 * backs the repository dedup check "already extracted from this note for this date".
 */
@Entity(
    tableName = "followups",
    indices = [
        Index("dueAt"),
        Index("status"),
        Index("sourceNoteId"),
        Index(value = ["sourceNoteId", "dueAt"]),
    ],
)
data class FollowupEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueAt: Long,
    val dueIsDateOnly: Boolean = false,
    val dueTimeZone: String? = null,
    val criticality: Criticality = Criticality.NORMAL,
    val status: FollowupStatus = FollowupStatus.PENDING,
    val snoozedUntil: Long? = null,
    val note: String? = null,
    val sourceNoteId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
)

@Entity(
    tableName = "people",
    indices = [Index("name"), Index("sourceNoteId")],
)
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val relation: String? = null,
    val context: String? = null,
    val preferredLanguage: String? = null,
    val sourceNoteId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastInteractionAt: Long? = null,
)

@Entity(
    tableName = "projects",
    indices = [Index("status"), Index("sourceNoteId")],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val nextAction: String? = null,
    val sourceNoteId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "decisions",
    indices = [Index("projectId"), Index("decidedAt"), Index("sourceNoteId")],
)
data class DecisionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val rationale: String? = null,
    val projectId: String? = null,
    val decidedAt: Long,
    val sourceNoteId: String? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "habits",
    indices = [Index("sourceNoteId")],
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cadence: HabitCadence = HabitCadence.DAILY,
    val schedule: String? = null,
    val sourceNoteId: String? = null,
    val createdAt: Long,
    val archivedAt: Long? = null,
)

/**
 * One tick per habit per day. [tickDate] is date-only millis produced by
 * `CalendarDates.toDateMillis` — the unique index only holds because every writer
 * normalizes the date through that one function.
 */
@Entity(
    tableName = "habit_ticks",
    indices = [Index("habitId"), Index(value = ["habitId", "tickDate"], unique = true)],
)
data class HabitTickEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val tickDate: Long,
    val createdAt: Long,
)

/**
 * Money-bearing documents. Amount is nullable ([amountMinor]/[currency] both null
 * when there is no amount) — [money] returns null vs a real zero, never coerced.
 * renewsOn/cancelBy are date-only millis; they are display + 30-day-window only —
 * anything that must fire an alarm becomes a [FollowupEntity].
 */
@Entity(
    tableName = "documents",
    indices = [Index("kind"), Index("renewsOn"), Index("cancelBy"), Index("sourceNoteId")],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val kind: DocumentKind,
    val provider: String? = null,
    val amountMinor: Long? = null,
    val currency: String? = null,
    val billingPeriod: BillingPeriod? = null,
    val renewsOn: Long? = null,
    val cancelBy: Long? = null,
    val notes: String? = null,
    val sourceNoteId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val money: Money?
        get() = if (amountMinor != null && currency != null) Money(amountMinor, currency) else null
}

/**
 * Spend. [dedupKey] (UNIQUE) is `DedupKeys.transaction(...)` — the idempotency hook
 * that stops a replayed extraction or a doubled payment notification from creating a
 * second row. Raw notification text is never persisted (M8 rule): only parsed fields.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index("occurredAt"),
        Index("currency"),
        Index("source"),
        Index("sourceNoteId"),
        Index(value = ["dedupKey"], unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountMinor: Long,
    val currency: String,
    val merchant: String? = null,
    val category: String? = null,
    val source: TransactionSource,
    val occurredAt: Long,
    val dedupKey: String,
    val sourceNoteId: String? = null,
    val createdAt: Long,
) {
    val money: Money get() = Money(amountMinor, currency)
}

/**
 * Append-only memory with supersede. A row is one REVISION. Live = not superseded and
 * not forgotten (see the memory_facts_live view). Consolidation may only INSERT a new
 * revision and mark old ones — never DELETE/UPDATE a fact's content. [learnedAt] is
 * metadata, NOT a domain date (dated facts live in followups).
 */
@Entity(
    tableName = "memory_facts",
    indices = [
        Index(value = ["supersededById", "forgottenAt"]),
        Index("category"),
        Index("supersededById"),
        Index("sourceNoteId"),
    ],
)
data class MemoryFactEntity(
    @PrimaryKey val id: String,
    val category: MemoryCategory,
    val topic: String,
    val fact: String,
    val sensitive: Boolean = false,
    val source: MemorySource,
    val sourceNoteId: String? = null,
    val revision: Int = 1,
    val supersededById: String? = null,
    val supersededAt: Long? = null,
    val forgottenAt: Long? = null,
    val learnedAt: Long,
    val createdAt: Long,
)

@Entity(
    tableName = "briefs",
    indices = [Index("forDate")],
)
data class BriefEntity(
    @PrimaryKey val id: String,
    val forDate: Long,
    val body: String,
    val generatedAt: Long,
    val readAt: Long? = null,
    val pinnedAt: Long? = null,
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId", "createdAt"])],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val createdAt: Long,
)

/** Token/cost accounting. Cost is per-currency ([costMinor]/[costCurrency]); there is
 *  no aggregate scalar. */
@Entity(
    tableName = "spend_log",
    indices = [Index("occurredAt"), Index("kind")],
)
data class SpendLogEntity(
    @PrimaryKey val id: String,
    val occurredAt: Long,
    val kind: SpendKind,
    val model: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val costMinor: Long? = null,
    val costCurrency: String? = null,
)
