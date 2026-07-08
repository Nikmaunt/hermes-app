package app.hermes.demo

import java.time.Instant

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
 *  - [MEDICAL] alone yields BOTH a dated followup AND a dateless memory_fact from one
 *    text — the D8/D21 split, made visible in Demo.
 *  - [MEDICAL]'s fact is sensitive (health) — material for SensitiveContent in M3.
 *  - the others populate Money / People / Projects / Habits / Documents so no seeded
 *    surface is empty.
 *
 * Time is deterministic: everything resolves against [NOW_MILLIS] in [ZONE_ID], and the
 * JSON carries absolute dates, so the seeded rows are byte-for-byte reproducible (and the
 * pipeline test can assert on them). Demo dates are therefore FIXED, not relative to the
 * real "today" — a documented simplification a later milestone can lift.
 */
object DemoNotes {

    /** Fixed "now" the demo pipeline resolves against (2026-07-08 09:00 UTC). */
    val NOW_MILLIS: Long = Instant.parse("2026-07-08T09:00:00Z").toEpochMilli()
    const val ZONE_ID = "Europe/Warsaw"
    const val LANGUAGE = "en"

    /** One text → a dated followup + a dateless, sensitive health memory_fact (D8/D21). */
    val MEDICAL = DemoNote(
        key = "medical",
        text = "Записаться к окулисту на плановую проверку — 15 июля в 9:30.",
        rawModelJson = """
            {
              "summary": "Eye check-up on Jul 15; yearly exam noted.",
              "followups": [
                {"title": "Проверка зрения у окулиста", "due": {"dateTime": "2026-07-15T09:30"}, "criticality": "normal"}
              ],
              "memoryFacts": [
                {"category": "health", "topic": "check-ups", "fact": "Проверка зрения раз в год", "sensitive": true}
              ]
            }
        """.trimIndent(),
    )

    /** A spend with a complete amount + currency → one transaction. */
    val EXPENSE = DemoNote(
        key = "expense",
        text = "Paid 45.99 PLN at Biedronka for groceries this evening.",
        rawModelJson = """
            {
              "summary": "Groceries at Biedronka, 45.99 PLN.",
              "transactions": [
                {
                  "amount": {"amountMinor": 4599, "currency": "PLN"},
                  "merchant": "Biedronka",
                  "category": "groceries",
                  "occurredAt": {"dateTime": "2026-07-08T18:30"}
                }
              ]
            }
        """.trimIndent(),
    )

    /** A person plus a durable fact about them → one person + one memory_fact. */
    val PERSON = DemoNote(
        key = "person",
        text = "Rosa from the climbing gym prefers Spanish. Её сына зовут Mateo.",
        rawModelJson = """
            {
              "summary": "Rosa (climbing partner) prefers Spanish; her son is Mateo.",
              "people": [
                {"name": "Rosa", "relation": "climbing partner", "context": "Climbs together", "preferredLanguage": "es"}
              ],
              "memoryFacts": [
                {"category": "relationships", "topic": "Rosa", "fact": "Rosa's son is named Mateo"}
              ]
            }
        """.trimIndent(),
    )

    /** A project plus the decision that shaped it → one project + one decision (linked). */
    val PROJECT = DemoNote(
        key = "project",
        text = "Decided to build the Hermes sync layer on Kotlin Multiplatform — one codebase for phone and desktop.",
        rawModelJson = """
            {
              "summary": "Chose Kotlin Multiplatform for the Hermes sync layer.",
              "projects": [
                {"name": "Hermes sync layer", "status": "active", "nextAction": "Prototype the shared module"}
              ],
              "decisions": [
                {
                  "title": "Use Kotlin Multiplatform for sync",
                  "rationale": "One codebase for phone and desktop",
                  "decidedAt": {"date": "2026-07-06"},
                  "projectRef": "Hermes sync layer"
                }
              ]
            }
        """.trimIndent(),
    )

    /** A recurring intention → one habit (no date; recurrence is a cadence, not a due). */
    val HABIT = DemoNote(
        key = "habit",
        text = "Хочу медитировать каждое утро по 10 минут.",
        rawModelJson = """
            {
              "summary": "Wants to meditate every morning for 10 minutes.",
              "habits": [
                {"name": "Morning meditation", "cadence": "daily", "schedule": "Every morning, 10 minutes"}
              ]
            }
        """.trimIndent(),
    )

    /** A subscription → one money-bearing document (renewsOn is display + window, not an alarm). */
    val SUBSCRIPTION = DemoNote(
        key = "subscription",
        text = "Netflix — 43 PLN/month, renews on the 20th. Consider downgrading the plan.",
        rawModelJson = """
            {
              "summary": "Netflix subscription, 43 PLN monthly, renews on the 20th.",
              "documents": [
                {
                  "title": "Netflix",
                  "kind": "subscription",
                  "provider": "Netflix",
                  "amount": {"amountMinor": 4300, "currency": "PLN"},
                  "billingPeriod": "monthly",
                  "renewsOn": "2026-07-20",
                  "notes": "Consider downgrading the plan"
                }
              ]
            }
        """.trimIndent(),
    )

    /** Newest first — the order the seeder stamps createdAt in (index 0 = most recent). */
    val ALL: List<DemoNote> = listOf(MEDICAL, EXPENSE, PERSON, PROJECT, HABIT, SUBSCRIPTION)
}
