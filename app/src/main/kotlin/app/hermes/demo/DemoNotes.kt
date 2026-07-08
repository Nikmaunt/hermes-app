package app.hermes.demo

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A demo note as it would arrive from the user, paired with the RAW JSON a provider
 * would return for it. The pairing is the whole point: the seeder feeds [rawModelJson]
 * through the SAME FakeProvider → ExtractionService → ExtractionValidator that a real
 * provider's output flows through (D18), so the demo database is the genuine product of
 * the extraction pipeline, never a hand-filled table.
 *
 * The JSON obeys the extraction contract exactly (D20): only semantic fields, dates as
 * `{date|dateTime}` (documents.renewsOn is a bare `YYYY-MM-DD` string), no ids /
 * timestamps / sourceNoteId / source — those are stamped later, at persist.
 */
data class DemoNote(val key: String, val text: String, val rawModelJson: String)

/**
 * The demo corpus: six realistic notes, EN + RU mixed. Chosen to exercise the whole
 * schema and to make the design visible in the running app:
 *  - the medical note alone yields BOTH a dated followup AND a dateless memory_fact from
 *    one text — the D8/D21 split, made visible in Demo.
 *  - its fact is sensitive (health) — material for SensitiveContent in M3.
 *  - the others populate Money / People / Projects / Habits / Documents so no seeded
 *    surface is empty.
 *
 * Dates are RESOLVED RELATIVE TO the [nowMillis] passed to [corpus] (appointment in a
 * week, renewal on the next 20th, a decision two days ago, a spend just now), so a Demo
 * seeded today is always fresh — a showcase must never show a past-due appointment (D39).
 * The note *texts* stay date-free / relative ("через неделю", "on the 20th"), so a text
 * never contradicts the date the pipeline resolves for it. The seeder passes the wall
 * clock; the pipeline test passes [TEST_NOW_MILLIS] for byte-for-byte determinism.
 */
object DemoNotes {

    const val ZONE_ID = "Europe/Warsaw"
    const val LANGUAGE = "en"

    /** Fixed clock the pipeline test resolves against (2026-07-08 09:00 UTC). Runtime
     *  seeding uses the wall clock instead (D39). */
    val TEST_NOW_MILLIS: Long = Instant.parse("2026-07-08T09:00:00Z").toEpochMilli()

    // Capture texts are static (no embedded absolute date), so they read sensibly whenever
    // Demo is seeded and the pipeline test can match on them.
    const val MEDICAL_TEXT =
        "Записаться к окулисту на плановую проверку — через неделю, в 9:30."
    const val EXPENSE_TEXT = "Paid 45.99 PLN at Biedronka for groceries."
    const val PERSON_TEXT = "Rosa from the climbing gym prefers Spanish. Её сына зовут Mateo."
    const val PROJECT_TEXT =
        "Decided to build the Hermes sync layer on Kotlin Multiplatform — one codebase for phone and desktop."
    const val HABIT_TEXT = "Хочу медитировать каждое утро по 10 минут."
    const val SUBSCRIPTION_TEXT = "Netflix — 43 PLN/month, renews on the 20th. Consider downgrading the plan."

    /** The corpus with every date resolved against [nowMillis] in [zoneId]. */
    fun corpus(nowMillis: Long, zoneId: String = ZONE_ID): List<DemoNote> {
        val zone = ZoneId.of(zoneId)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

        val appointment = ISO_DATE_TIME.format(today.plusDays(APPOINTMENT_IN_DAYS).atTime(APPT_HOUR, APPT_MINUTE))
        val spend = ISO_DATE_TIME.format(
            Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime().truncatedTo(ChronoUnit.MINUTES),
        )
        val decided = ISO_DATE.format(today.minusDays(DECISION_AGO_DAYS))
        val thisMonths20th = today.withDayOfMonth(RENEWAL_DAY)
        val renewalDate = if (thisMonths20th.isBefore(today)) thisMonths20th.plusMonths(1) else thisMonths20th
        val renews = ISO_DATE.format(renewalDate)

        return listOf(
            DemoNote("medical", MEDICAL_TEXT, medicalJson(appointment)),
            DemoNote("expense", EXPENSE_TEXT, expenseJson(spend)),
            DemoNote("person", PERSON_TEXT, PERSON_JSON),
            DemoNote("project", PROJECT_TEXT, projectJson(decided)),
            DemoNote("habit", HABIT_TEXT, HABIT_JSON),
            DemoNote("subscription", SUBSCRIPTION_TEXT, subscriptionJson(renews)),
        )
    }

    /** Deterministic corpus for the pipeline test (fixed clock). */
    val ALL: List<DemoNote> get() = corpus(TEST_NOW_MILLIS)

    // --- scripted provider replies (the raw JSON a model would return) ------------------

    private fun medicalJson(appointmentDateTime: String) = """
        {
          "summary": "Eye check-up coming up; yearly exam noted.",
          "followups": [
            {"title": "Проверка зрения у окулиста", "due": {"dateTime": "$appointmentDateTime"}, "criticality": "normal"}
          ],
          "memoryFacts": [
            {"category": "health", "topic": "check-ups", "fact": "Проверка зрения раз в год", "sensitive": true}
          ]
        }
    """.trimIndent()

    private fun expenseJson(occurredDateTime: String) = """
        {
          "summary": "Groceries at Biedronka, 45.99 PLN.",
          "transactions": [
            {
              "amount": {"amountMinor": 4599, "currency": "PLN"},
              "merchant": "Biedronka",
              "category": "groceries",
              "occurredAt": {"dateTime": "$occurredDateTime"}
            }
          ]
        }
    """.trimIndent()

    private val PERSON_JSON = """
        {
          "summary": "Rosa (climbing partner) prefers Spanish; her son is Mateo.",
          "people": [
            {"name": "Rosa", "relation": "climbing partner", "context": "Climbs together", "preferredLanguage": "es"}
          ],
          "memoryFacts": [
            {"category": "relationships", "topic": "Rosa", "fact": "Rosa's son is named Mateo"}
          ]
        }
    """.trimIndent()

    private fun projectJson(decidedDate: String) = """
        {
          "summary": "Chose Kotlin Multiplatform for the Hermes sync layer.",
          "projects": [
            {"name": "Hermes sync layer", "status": "active", "nextAction": "Prototype the shared module"}
          ],
          "decisions": [
            {
              "title": "Use Kotlin Multiplatform for sync",
              "rationale": "One codebase for phone and desktop",
              "decidedAt": {"date": "$decidedDate"},
              "projectRef": "Hermes sync layer"
            }
          ]
        }
    """.trimIndent()

    private val HABIT_JSON = """
        {
          "summary": "Wants to meditate every morning for 10 minutes.",
          "habits": [
            {"name": "Morning meditation", "cadence": "daily", "schedule": "Every morning, 10 minutes"}
          ]
        }
    """.trimIndent()

    private fun subscriptionJson(renewsDate: String) = """
        {
          "summary": "Netflix subscription, 43 PLN monthly, renews on the 20th.",
          "documents": [
            {
              "title": "Netflix",
              "kind": "subscription",
              "provider": "Netflix",
              "amount": {"amountMinor": 4300, "currency": "PLN"},
              "billingPeriod": "monthly",
              "renewsOn": "$renewsDate",
              "notes": "Consider downgrading the plan"
            }
          ]
        }
    """.trimIndent()

    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val ISO_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private const val APPOINTMENT_IN_DAYS = 7L
    private const val DECISION_AGO_DAYS = 2L
    private const val RENEWAL_DAY = 20
    private const val APPT_HOUR = 9
    private const val APPT_MINUTE = 30
}
