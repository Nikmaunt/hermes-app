package app.hermes.core.brain.extraction

import app.hermes.core.brain.BrainContext
import app.hermes.core.model.BillingPeriod
import app.hermes.core.model.CalendarDates
import app.hermes.core.model.Criticality
import app.hermes.core.model.DocumentKind
import app.hermes.core.model.HabitCadence
import app.hermes.core.model.MemoryCategory
import app.hermes.core.model.Money
import app.hermes.core.model.ProjectStatus
import app.hermes.core.model.WireEnum
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Bounds on untrusted output, a bumper against a runaway / hallucinating model (D23):
 * over-long lists are capped (with a warning, never silently), absurd amounts and
 * out-of-window dates are rejected.
 */
data class ExtractionCaps(
    val maxItemsPerType: Int = DEFAULT_MAX_ITEMS_PER_TYPE,
    val maxAbsAmountMinor: Long = DEFAULT_MAX_ABS_AMOUNT_MINOR,
    val dateWindowYears: Int = DEFAULT_DATE_WINDOW_YEARS,
) {
    companion object {
        const val DEFAULT_MAX_ITEMS_PER_TYPE = 50
        const val DEFAULT_MAX_ABS_AMOUNT_MINOR = 1_000_000_000_000L
        const val DEFAULT_DATE_WINDOW_YEARS = 100
    }
}

/**
 * Turns a decoded (structurally valid) top-level JSON object into an [ExtractionResult].
 * This is the untrusted-output boundary (§6, D19): it is DELIBERATELY stricter than the
 * storage-read degrade policy (D12) — for a filtering enum an unknown value is a hard
 * violation, not a silent fallback. Every item is decoded and validated INDEPENDENTLY,
 * so one bad element cannot drop a whole array; a hard-violated item is preserved as a
 * [RejectedItem] with its exact raw fragment.
 *
 * Pure: no wall clock, no zone, no I/O — everything time-related comes from [BrainContext].
 */
class ExtractionValidator(private val caps: ExtractionCaps = ExtractionCaps()) {

    fun validate(root: JsonObject, context: BrainContext): ExtractionResult {
        val acc = Accumulator()
        acc.summary = summaryOf(root)
        val zone = ZoneId.of(context.zoneId)
        eachItem(root, WireKeys.MEMORY_FACTS, acc) { el, p -> memoryFact(el, p, acc) }
        eachItem(root, WireKeys.FOLLOWUPS, acc) { el, p -> followup(el, p, context, zone, acc) }
        eachItem(root, WireKeys.PEOPLE, acc) { el, p -> person(el, p, acc) }
        eachItem(root, WireKeys.PROJECTS, acc) { el, p -> project(el, p, acc) }
        eachItem(root, WireKeys.DECISIONS, acc) { el, p -> decision(el, p, context, zone, acc) }
        eachItem(root, WireKeys.HABITS, acc) { el, p -> habit(el, p, acc) }
        eachItem(root, WireKeys.DOCUMENTS, acc) { el, p -> document(el, p, context, acc) }
        eachItem(root, WireKeys.TRANSACTIONS, acc) { el, p -> transaction(el, p, context, zone, acc) }
        return acc.toResult()
    }

    // --- array plumbing -----------------------------------------------------------

    private fun eachItem(root: JsonObject, key: String, acc: Accumulator, item: (JsonElement, String) -> Unit) {
        val raw = root[key] ?: return
        if (raw !is JsonArray) {
            acc.warnings.add(Violation(key, ViolationCode.MALFORMED_JSON, "expected a JSON array"))
            return
        }
        val kept = if (raw.size > caps.maxItemsPerType) {
            acc.warnings.add(
                Violation(key, ViolationCode.TOO_MANY_ITEMS, "${raw.size} items, capped to ${caps.maxItemsPerType}"),
            )
            raw.take(caps.maxItemsPerType)
        } else {
            raw
        }
        kept.forEachIndexed { i, el -> item(el, "$key[$i]") }
    }

    private inline fun <reified W> decode(el: JsonElement, kind: ExtractedKind, path: String, acc: Accumulator): W? =
        try {
            ExtractionJson.decodeFromJsonElement<W>(el)
        } catch (_: SerializationException) {
            acc.rejected.add(
                RejectedItem(
                    kind,
                    el.toString(),
                    listOf(Violation(path, ViolationCode.MALFORMED_JSON, "not a valid object shape")),
                ),
            )
            null
        } catch (_: IllegalArgumentException) {
            acc.rejected.add(
                RejectedItem(
                    kind,
                    el.toString(),
                    listOf(Violation(path, ViolationCode.MALFORMED_JSON, "not a valid object shape")),
                ),
            )
            null
        }

    // --- per-type validation ------------------------------------------------------

    private fun memoryFact(el: JsonElement, path: String, acc: Accumulator) {
        val w = decode<WireMemoryFact>(el, ExtractedKind.MEMORY_FACT, path, acc) ?: return
        val v = mutableListOf<Violation>()
        val category = strictEnum<MemoryCategory>(w.category, "$path.category", v)
        val topic = required(w.topic, "$path.topic", v)
        val fact = required(w.fact, "$path.fact", v)
        if (v.isEmpty()) {
            acc.memoryFacts.add(ExtractedMemoryFact(category!!, topic!!, fact!!, w.sensitive ?: false))
        } else {
            acc.rejected.add(RejectedItem(ExtractedKind.MEMORY_FACT, el.toString(), v))
        }
    }

    private fun followup(el: JsonElement, path: String, context: BrainContext, zone: ZoneId, acc: Accumulator) {
        val w = decode<WireFollowup>(el, ExtractedKind.FOLLOWUP, path, acc) ?: return
        val v = mutableListOf<Violation>()
        val warn = mutableListOf<Violation>()
        val title = required(w.title, "$path.title", v)
        val due = dueInstant(w.due, context, zone, "$path.due", v)
        val criticality = softEnum(w.criticality, Criticality.NORMAL, "$path.criticality", warn)
        if (v.isEmpty()) {
            acc.followups.add(ExtractedFollowup(title!!, due!!, criticality, opt(w.note)))
            acc.warnings.addAll(warn)
        } else {
            acc.rejected.add(RejectedItem(ExtractedKind.FOLLOWUP, el.toString(), v))
        }
    }

    private fun person(el: JsonElement, path: String, acc: Accumulator) {
        val w = decode<WirePerson>(el, ExtractedKind.PERSON, path, acc) ?: return
        val v = mutableListOf<Violation>()
        val name = required(w.name, "$path.name", v)
        if (v.isEmpty()) {
            acc.people.add(ExtractedPerson(name!!, opt(w.relation), opt(w.context), opt(w.preferredLanguage)))
        } else {
            acc.rejected.add(RejectedItem(ExtractedKind.PERSON, el.toString(), v))
        }
    }

    private fun project(el: JsonElement, path: String, acc: Accumulator) {
        val w = decode<WireProject>(el, ExtractedKind.PROJECT, path, acc) ?: return
        val v = mutableListOf<Violation>()
        val warn = mutableListOf<Violation>()
        val name = required(w.name, "$path.name", v)
        val status = softEnum(w.status, ProjectStatus.ACTIVE, "$path.status", warn)
        if (v.isEmpty()) {
            acc.projects.add(ExtractedProject(name!!, status, opt(w.nextAction)))
            acc.warnings.addAll(warn)
        } else {
            acc.rejected.add(RejectedItem(ExtractedKind.PROJECT, el.toString(), v))
        }
    }

    private fun decision(el: JsonElement, path: String, context: BrainContext, zone: ZoneId, acc: Accumulator) {
        val w = decode<WireDecision>(el, ExtractedKind.DECISION, path, acc) ?: return
        val v = mutableListOf<Violation>()
        val warn = mutableListOf<Violation>()
        val title = required(w.title, "$path.title", v)
        val decidedAt = instantOrNow(w.decidedAt, context, zone, "$path.decidedAt", v, warn)
        if (v.isEmpty()) {
            acc.decisions.add(ExtractedDecision(title!!, opt(w.rationale), decidedAt!!, opt(w.projectRef)))
            acc.warnings.addAll(warn)
        } else {
            acc.rejected.add(RejectedItem(ExtractedKind.DECISION, el.toString(), v))
        }
    }

    private fun habit(el: JsonElement, path: String, acc: Accumulator) {
        val w = decode<WireHabit>(el, ExtractedKind.HABIT, path, acc) ?: return
        val v = mutableListOf<Violation>()
        val warn = mutableListOf<Violation>()
        val name = required(w.name, "$path.name", v)
        val cadence = softEnum(w.cadence, HabitCadence.DAILY, "$path.cadence", warn)
        if (v.isEmpty()) {
            acc.habits.add(ExtractedHabit(name!!, cadence, opt(w.schedule)))
            acc.warnings.addAll(warn)
        } else {
            acc.rejected.add(RejectedItem(ExtractedKind.HABIT, el.toString(), v))
        }
    }

    private fun document(el: JsonElement, path: String, context: BrainContext, acc: Accumulator) {
        val w = decode<WireDocument>(el, ExtractedKind.DOCUMENT, path, acc) ?: return
        val v = mutableListOf<Violation>()
        val warn = mutableListOf<Violation>()
        val title = required(w.title, "$path.title", v)
        val kind = strictEnum<DocumentKind>(w.kind, "$path.kind", v)
        // Money is optional on a document: an incomplete amount degrades to null + a
        // warning (the document survives), unlike a transaction where it is required.
        val mp = parseMoney(w.amount, "$path.amount")
        mp.violation?.let { warn.add(it) }
        val renewsOn = dateOnly(w.renewsOn, context, "$path.renewsOn", warn)
        val cancelBy = dateOnly(w.cancelBy, context, "$path.cancelBy", warn)
        val billing = billingPeriod(w.billingPeriod, "$path.billingPeriod", warn)
        if (v.isEmpty()) {
            acc.documents.add(
                ExtractedDocument(
                    title!!,
                    kind!!,
                    opt(w.provider),
                    mp.money,
                    billing,
                    renewsOn,
                    cancelBy,
                    opt(w.notes),
                ),
            )
            acc.warnings.addAll(warn)
        } else {
            acc.rejected.add(RejectedItem(ExtractedKind.DOCUMENT, el.toString(), v))
        }
    }

    private fun transaction(el: JsonElement, path: String, context: BrainContext, zone: ZoneId, acc: Accumulator) {
        val w = decode<WireTransaction>(el, ExtractedKind.TRANSACTION, path, acc) ?: return
        val v = mutableListOf<Violation>()
        val warn = mutableListOf<Violation>()
        val money = requiredMoney(w.amount, "$path.amount", v)
        val occurredAt = instantOrNow(w.occurredAt, context, zone, "$path.occurredAt", v, warn)
        if (v.isEmpty()) {
            acc.transactions.add(ExtractedTransaction(money!!, opt(w.merchant), opt(w.category), occurredAt!!))
            acc.warnings.addAll(warn)
        } else {
            acc.rejected.add(RejectedItem(ExtractedKind.TRANSACTION, el.toString(), v))
        }
    }

    // --- field helpers ------------------------------------------------------------

    private fun summaryOf(root: JsonObject): String? {
        val el = root[WireKeys.SUMMARY] as? JsonPrimitive ?: return null
        return if (el.isString) el.content.trim().ifEmpty { null } else null
    }

    private fun required(raw: String?, path: String, v: MutableList<Violation>): String? {
        val t = raw?.trim()
        return when {
            t == null -> null.also { v.add(Violation(path, ViolationCode.MISSING_REQUIRED, "required")) }
            t.isEmpty() -> null.also { v.add(Violation(path, ViolationCode.BLANK_STRING, "must not be blank")) }
            else -> t
        }
    }

    private fun opt(raw: String?): String? = raw?.trim()?.ifEmpty { null }

    private inline fun <reified E> strictEnum(
        raw: String?,
        path: String,
        v: MutableList<Violation>,
    ): E?
        where E : Enum<E>, E : WireEnum {
        if (raw == null) {
            v.add(Violation(path, ViolationCode.MISSING_REQUIRED, "required"))
            return null
        }
        val match = enumValues<E>().firstOrNull { it.wire == raw }
        if (match == null) v.add(Violation(path, ViolationCode.UNKNOWN_ENUM, "unknown value '$raw'"))
        return match
    }

    private inline fun <reified E> softEnum(
        raw: String?,
        default: E,
        path: String,
        warn: MutableList<Violation>,
    ): E
        where E : Enum<E>, E : WireEnum {
        if (raw == null) return default
        val match = enumValues<E>().firstOrNull { it.wire == raw }
        if (match != null) return match
        warn.add(Violation(path, ViolationCode.ENUM_DEGRADED, "unknown '$raw' -> ${default.wire}"))
        return default
    }

    private fun billingPeriod(raw: String?, path: String, warn: MutableList<Violation>): BillingPeriod? {
        if (raw == null) return null
        val match = BillingPeriod.entries.firstOrNull { it.wire == raw }
        if (match != null) return match
        warn.add(Violation(path, ViolationCode.ENUM_DEGRADED, "unknown '$raw' -> ${BillingPeriod.MONTHLY.wire}"))
        return BillingPeriod.MONTHLY
    }

    // --- date helpers -------------------------------------------------------------

    /** Required due date for a followup. Absent/blank/unparseable/out-of-window all
     *  reject the followup — a dateless commitment is never stored (D8/D21). */
    private fun dueInstant(
        w: WireWhen?,
        context: BrainContext,
        zone: ZoneId,
        path: String,
        v: MutableList<Violation>,
    ): ExtractedInstant? {
        if (w == null || (w.date == null && w.dateTime == null)) {
            v.add(Violation(path, ViolationCode.MISSING_DUE_DATE, "a followup must have a date"))
            return null
        }
        val millis = parseWhenMillis(w, zone) ?: run {
            v.add(Violation(path, ViolationCode.UNPARSEABLE_DATE, "unparseable date"))
            return null
        }
        if (!inWindow(millis, context)) {
            v.add(Violation(path, ViolationCode.DATE_OUT_OF_RANGE, "date out of range"))
            return null
        }
        return ExtractedInstant(millis, isDateOnly = w.dateTime == null, zoneId = context.zoneId)
    }

    /** Optional instant that defaults to now when absent; present-but-unparseable is a
     *  hard violation (a garbage date is a strong signal, unlike a missing one). */
    private fun instantOrNow(
        w: WireWhen?,
        context: BrainContext,
        zone: ZoneId,
        path: String,
        v: MutableList<Violation>,
        warn: MutableList<Violation>,
    ): Long? {
        if (w == null || (w.date == null && w.dateTime == null)) {
            warn.add(Violation(path, ViolationCode.DATE_DEFAULTED, "no date given -> now"))
            return context.nowMillis
        }
        val millis = parseWhenMillis(w, zone) ?: run {
            v.add(Violation(path, ViolationCode.UNPARSEABLE_DATE, "unparseable date"))
            return null
        }
        if (!inWindow(millis, context)) {
            v.add(Violation(path, ViolationCode.DATE_OUT_OF_RANGE, "date out of range"))
            return null
        }
        return millis
    }

    /** Optional date-only column (documents.renewsOn/cancelBy): a bad/out-of-window
     *  value drops to null with a warning — the document itself survives (D8). */
    private fun dateOnly(raw: String?, context: BrainContext, path: String, warn: MutableList<Violation>): Long? {
        if (raw == null) return null
        val millis = try {
            CalendarDates.toDateMillis(LocalDate.parse(raw))
        } catch (_: DateTimeParseException) {
            warn.add(Violation(path, ViolationCode.UNPARSEABLE_DATE, "unparseable date, dropped"))
            return null
        }
        if (!inWindow(millis, context)) {
            warn.add(Violation(path, ViolationCode.DATE_OUT_OF_RANGE, "date out of range, dropped"))
            return null
        }
        return millis
    }

    /**
     * A timed value ([WireWhen.dateTime]) becomes a true instant in [zone]; a date-only
     * value ([WireWhen.date]) becomes that calendar day's UTC-midnight millis (D6).
     * Returns null on a parse failure. Assumes at least one field is present.
     */
    private fun parseWhenMillis(w: WireWhen, zone: ZoneId): Long? = try {
        if (w.dateTime != null) {
            LocalDateTime.parse(w.dateTime).atZone(zone).toInstant().toEpochMilli()
        } else {
            CalendarDates.toDateMillis(LocalDate.parse(w.date))
        }
    } catch (_: DateTimeParseException) {
        null
    }

    private fun inWindow(millis: Long, context: BrainContext): Boolean {
        val span = caps.dateWindowYears.toLong() * DAYS_PER_YEAR * MILLIS_PER_DAY
        return millis in (context.nowMillis - span)..(context.nowMillis + span)
    }

    // --- money helpers ------------------------------------------------------------

    /** A required amount (transactions): absence is a MissingRequired violation. */
    private fun requiredMoney(w: WireMoney?, path: String, v: MutableList<Violation>): Money? {
        val mp = parseMoney(w, path)
        return when {
            mp.violation != null -> null.also { v.add(mp.violation) }
            mp.money == null -> null.also {
                v.add(
                    Violation(path, ViolationCode.MISSING_REQUIRED, "a transaction needs an amount"),
                )
            }
            else -> mp.money
        }
    }

    /**
     * Parse an optional [WireMoney]. Both fields absent → no money, no violation. A
     * half-present or malformed amount → a [MoneyIncomplete]/[NumberOutOfRange]
     * violation; the caller decides whether that is hard (transaction) or soft (doc).
     */
    private fun parseMoney(w: WireMoney?, path: String): MoneyParse {
        val amount = w?.amountMinor
        val currency = w?.currency?.trim()?.uppercase(Locale.ROOT)
        val violation = when {
            amount == null && currency == null -> null
            amount == null || currency == null ->
                Violation(path, ViolationCode.MONEY_INCOMPLETE, "amount and currency must both be present")
            currency.length != CURRENCY_CODE_LEN ->
                Violation(path, ViolationCode.MONEY_INCOMPLETE, "currency must be an ISO-4217 alpha-3 code")
            abs(amount) > caps.maxAbsAmountMinor ->
                Violation(path, ViolationCode.NUMBER_OUT_OF_RANGE, "amount out of range")
            else -> null
        }
        val money = if (violation == null && amount != null && currency != null) Money(amount, currency) else null
        return MoneyParse(money, violation)
    }

    private data class MoneyParse(val money: Money?, val violation: Violation?)

    private class Accumulator {
        var summary: String? = null
        val memoryFacts = mutableListOf<ExtractedMemoryFact>()
        val followups = mutableListOf<ExtractedFollowup>()
        val people = mutableListOf<ExtractedPerson>()
        val projects = mutableListOf<ExtractedProject>()
        val decisions = mutableListOf<ExtractedDecision>()
        val habits = mutableListOf<ExtractedHabit>()
        val documents = mutableListOf<ExtractedDocument>()
        val transactions = mutableListOf<ExtractedTransaction>()
        val rejected = mutableListOf<RejectedItem>()
        val warnings = mutableListOf<Violation>()

        fun toResult(): ExtractionResult = ExtractionResult(
            summary, memoryFacts, followups, people, projects,
            decisions, habits, documents, transactions, rejected, warnings,
        )
    }

    private companion object {
        const val CURRENCY_CODE_LEN = 3
        const val DAYS_PER_YEAR = 365L
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
