package app.hermes.core.brain.extraction

import app.hermes.core.brain.BrainContext
import app.hermes.core.model.BillingPeriod
import app.hermes.core.model.Criticality
import app.hermes.core.model.HabitCadence
import app.hermes.core.model.MemoryCategory
import app.hermes.core.model.Money
import app.hermes.core.model.ProjectStatus
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class ExtractionValidatorTest {

    private val validator = ExtractionValidator()
    private val ctx = BrainContext(
        nowMillis = Instant.parse("2026-07-07T12:00:00Z").toEpochMilli(),
        zoneId = "Europe/Warsaw",
        language = "en",
    )

    private fun obj(json: String): JsonObject = ExtractionJson.parseToJsonElement(json).jsonObject
    private fun validate(json: String, v: ExtractionValidator = validator) = v.validate(obj(json), ctx)

    @Test
    fun `timed followup is accepted and resolved in the context zone`() {
        val r = validate(
            """
            {"followups":[{"title":"Call the bank","due":{"dateTime":"2026-07-08T09:00"},"criticality":"high"}]}
            """.trimIndent(),
        )
        assertThat(r.followups).hasSize(1)
        val f = r.followups.single()
        assertThat(f.title).isEqualTo("Call the bank")
        assertThat(f.due.isDateOnly).isFalse()
        // 09:00 in Warsaw (CEST, +02:00) == 07:00Z
        assertThat(f.due.epochMillis).isEqualTo(Instant.parse("2026-07-08T07:00:00Z").toEpochMilli())
        assertThat(f.criticality).isEqualTo(Criticality.HIGH)
        assertThat(r.rejected).isEmpty()
    }

    @Test
    fun `date-only followup is UTC midnight and flagged date-only`() {
        val r = validate("""{"followups":[{"title":"Renew passport","due":{"date":"2026-07-08"}}]}""")
        val f = r.followups.single()
        assertThat(f.due.isDateOnly).isTrue()
        assertThat(f.due.epochMillis).isEqualTo(Instant.parse("2026-07-08T00:00:00Z").toEpochMilli())
        assertThat(f.criticality).isEqualTo(Criticality.NORMAL)
    }

    @Test
    fun `followup without a date is rejected, never stored dateless`() {
        val r = validate("""{"followups":[{"title":"vague reminder"}]}""")
        assertThat(r.followups).isEmpty()
        assertThat(r.rejected).hasSize(1)
        val rej = r.rejected.single()
        assertThat(rej.kind).isEqualTo(ExtractedKind.FOLLOWUP)
        assertThat(rej.violations.map { it.code }).contains(ViolationCode.MISSING_DUE_DATE)
        assertThat(rej.rawJson).contains("vague reminder")
    }

    @Test
    fun `blank required string is rejected`() {
        val r = validate("""{"followups":[{"title":"   ","due":{"date":"2026-07-08"}}]}""")
        assertThat(r.rejected.single().violations.map { it.code }).contains(ViolationCode.BLANK_STRING)
    }

    @Test
    fun `unknown filtering enum is a hard violation, unlike storage degrade`() {
        val r = validate("""{"memoryFacts":[{"category":"banking","topic":"iban","fact":"PL10"}]}""")
        assertThat(r.memoryFacts).isEmpty()
        assertThat(r.rejected.single().violations.map { it.code }).contains(ViolationCode.UNKNOWN_ENUM)
    }

    @Test
    fun `unknown soft enum degrades to default with a warning, item survives`() {
        val r = validate("""{"followups":[{"title":"pay rent","due":{"date":"2026-07-08"},"criticality":"urgent"}]}""")
        assertThat(r.followups.single().criticality).isEqualTo(Criticality.NORMAL)
        assertThat(r.warnings.map { it.code }).contains(ViolationCode.ENUM_DEGRADED)
    }

    @Test
    fun `transaction with incomplete money is rejected`() {
        val r = validate("""{"transactions":[{"amount":{"amountMinor":500}}]}""")
        assertThat(r.transactions).isEmpty()
        assertThat(r.rejected.single().violations.map { it.code }).contains(ViolationCode.MONEY_INCOMPLETE)
    }

    @Test
    fun `transaction with no amount is rejected as missing required`() {
        val r = validate("""{"transactions":[{"merchant":"Cafe"}]}""")
        assertThat(r.rejected.single().violations.map { it.code }).contains(ViolationCode.MISSING_REQUIRED)
    }

    @Test
    fun `valid transaction maps money and defaults occurredAt to now with warning`() {
        val r = validate("""{"transactions":[{"amount":{"amountMinor":1299,"currency":"pln"}}]}""")
        val t = r.transactions.single()
        assertThat(t.amount).isEqualTo(Money(1299, "PLN"))
        assertThat(t.occurredAt).isEqualTo(ctx.nowMillis)
        assertThat(r.warnings.map { it.code }).contains(ViolationCode.DATE_DEFAULTED)
    }

    @Test
    fun `memory fact is accepted with category and sensitivity`() {
        val r =
            validate(
                """{"memoryFacts":[{"category":"health","topic":"eyes","fact":"eye exam","sensitive":true}]}""",
            )
        val m = r.memoryFacts.single()
        assertThat(m.category).isEqualTo(MemoryCategory.HEALTH)
        assertThat(m.sensitive).isTrue()
    }

    @Test
    fun `document keeps money and parses date-only renewsOn`() {
        val r = validate(
            """
            {"documents":[{"title":"Netflix","kind":"subscription","billingPeriod":"monthly",
            "amount":{"amountMinor":4999,"currency":"PLN"},"renewsOn":"2026-08-01"}]}
            """.trimIndent(),
        )
        val d = r.documents.single()
        assertThat(d.money).isEqualTo(Money(4999, "PLN"))
        assertThat(d.billingPeriod).isEqualTo(BillingPeriod.MONTHLY)
        assertThat(d.renewsOn).isEqualTo(Instant.parse("2026-08-01T00:00:00Z").toEpochMilli())
    }

    @Test
    fun `document with unparseable optional date drops it to null and survives`() {
        val r = validate("""{"documents":[{"title":"Gym","kind":"subscription","renewsOn":"soon"}]}""")
        val d = r.documents.single()
        assertThat(d.renewsOn).isNull()
        assertThat(r.warnings.map { it.code }).contains(ViolationCode.UNPARSEABLE_DATE)
    }

    @Test
    fun `unknown document kind is a hard violation`() {
        val r = validate("""{"documents":[{"title":"X","kind":"warranty"}]}""")
        assertThat(r.documents).isEmpty()
        assertThat(r.rejected.single().violations.map { it.code }).contains(ViolationCode.UNKNOWN_ENUM)
    }

    @Test
    fun `one malformed element does not drop the rest of the array`() {
        val r = validate("""{"followups":["garbage",{"title":"real","due":{"date":"2026-07-08"}}]}""")
        assertThat(r.followups.single().title).isEqualTo("real")
        assertThat(r.rejected.single().violations.map { it.code }).contains(ViolationCode.MALFORMED_JSON)
    }

    @Test
    fun `over-cap list is truncated with a warning, not silently`() {
        val capped = ExtractionValidator(ExtractionCaps(maxItemsPerType = 2))
        val json = """{"memoryFacts":[
            {"category":"misc","topic":"a","fact":"1"},
            {"category":"misc","topic":"b","fact":"2"},
            {"category":"misc","topic":"c","fact":"3"}]}"""
        val r = validate(json, capped)
        assertThat(r.memoryFacts).hasSize(2)
        assertThat(r.warnings.map { it.code }).contains(ViolationCode.TOO_MANY_ITEMS)
    }

    @Test
    fun `date far outside the window is rejected`() {
        val r = validate("""{"followups":[{"title":"ancient","due":{"date":"1200-01-01"}}]}""")
        assertThat(r.rejected.single().violations.map { it.code }).contains(ViolationCode.DATE_OUT_OF_RANGE)
    }

    @Test
    fun `absurd amount is rejected`() {
        val r = validate("""{"transactions":[{"amount":{"amountMinor":999999999999999,"currency":"USD"}}]}""")
        assertThat(r.rejected.single().violations.map { it.code }).contains(ViolationCode.NUMBER_OUT_OF_RANGE)
    }

    @Test
    fun `summary is read and an itemless object is empty`() {
        val r = validate("""{"summary":"Bought coffee"}""")
        assertThat(r.summary).isEqualTo("Bought coffee")
        assertThat(r.acceptedCount).isEqualTo(0)
        assertThat(r.isEmpty).isTrue()
    }

    @Test
    fun `people projects habits decisions accept with defaults`() {
        val r = validate(
            """
            {"people":[{"name":"Anna","relation":"sister"}],
            "projects":[{"name":"Kitchen"}],
            "habits":[{"name":"Run","cadence":"weekly"}],
            "decisions":[{"title":"Use Room"}]}
            """.trimIndent(),
        )
        assertThat(r.people.single().relation).isEqualTo("sister")
        assertThat(r.projects.single().status).isEqualTo(ProjectStatus.ACTIVE)
        assertThat(r.habits.single().cadence).isEqualTo(HabitCadence.WEEKLY)
        assertThat(r.decisions.single().decidedAt).isEqualTo(ctx.nowMillis)
    }
}
