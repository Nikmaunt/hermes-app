package app.hermes.demo

import app.hermes.core.model.BillingPeriod
import app.hermes.core.model.DocumentKind
import app.hermes.core.model.HabitCadence
import app.hermes.core.model.MemoryCategory
import app.hermes.core.model.ProjectStatus
import app.hermes.core.model.TransactionSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the demo database is the genuine product of the extraction pipeline, not a
 * hand-filled fixture: the notes are run through the real FakeProvider → ExtractionService
 * → ExtractionValidator, and the resulting rows are asserted here. The marquee assertion is
 * the D8/D21 split — one note births a dated followup AND a dateless, sensitive memory_fact,
 * the two linked only by a shared `sourceNoteId`.
 */
class DemoDataBuilderTest {

    private suspend fun build(): DemoData = DemoDataBuilder().build()

    @Test
    fun `every demo note is captured as a filed note`() = runTest {
        val data = build()
        assertThat(data.notes).hasSize(DemoNotes.ALL.size)
        assertThat(data.notes.map { it.status.wire }.toSet()).containsExactly("filed")
        // Texts round-trip verbatim — the raw capture is never rewritten by extraction.
        assertThat(data.notes.map { it.text }).containsExactlyElementsIn(DemoNotes.ALL.map { it.text })
    }

    @Test
    fun `the medical note births a dated followup AND a dateless sensitive fact from one text (D8 D21)`() = runTest {
        val data = build()
        val note = data.notes.first { it.text == DemoNotes.MEDICAL.text }

        val followups = data.followups.filter { it.sourceNoteId == note.id }
        val facts = data.memoryFacts.filter { it.sourceNoteId == note.id }
        assertThat(followups).hasSize(1)
        assertThat(facts).hasSize(1)

        val followup = followups.single()
        val fact = facts.single()
        // The followup carries the date (a real instant), timed, in the user's zone…
        assertThat(followup.dueAt).isGreaterThan(0L)
        assertThat(followup.dueIsDateOnly).isFalse()
        assertThat(followup.dueTimeZone).isEqualTo(DemoNotes.ZONE_ID)
        // …the fact carries no date and is the sensitive health item…
        assertThat(fact.category).isEqualTo(MemoryCategory.HEALTH)
        assertThat(fact.sensitive).isTrue()
        // …and the two are linked ONLY by the shared source note (D8/D21).
        assertThat(followup.sourceNoteId).isEqualTo(fact.sourceNoteId)
    }

    @Test
    fun `at least one seeded fact is sensitive - material for SensitiveContent`() = runTest {
        assertThat(build().memoryFacts.any { it.sensitive }).isTrue()
    }

    @Test
    fun `the expense note yields one transaction with a complete amount and a dedup key`() = runTest {
        val tx = build().transactions.single()
        assertThat(tx.amountMinor).isEqualTo(4599)
        assertThat(tx.currency).isEqualTo("PLN")
        assertThat(tx.merchant).isEqualTo("Biedronka")
        assertThat(tx.source).isEqualTo(TransactionSource.MANUAL)
        assertThat(tx.dedupKey).isNotEmpty()
    }

    @Test
    fun `the person note yields a person and a relationships fact`() = runTest {
        val data = build()
        val note = data.notes.first { it.text == DemoNotes.PERSON.text }
        val person = data.people.single()
        assertThat(person.name).isEqualTo("Rosa")
        assertThat(person.preferredLanguage).isEqualTo("es")
        assertThat(person.sourceNoteId).isEqualTo(note.id)
        assertThat(data.memoryFacts.filter { it.sourceNoteId == note.id }.single().category)
            .isEqualTo(MemoryCategory.RELATIONSHIPS)
    }

    @Test
    fun `the project note links its decision to the project it references`() = runTest {
        val data = build()
        val project = data.projects.single()
        assertThat(project.name).isEqualTo("Hermes sync layer")
        assertThat(project.status).isEqualTo(ProjectStatus.ACTIVE)
        val decision = data.decisions.single()
        // projectRef (a name) resolved to the project's stamped id (D20).
        assertThat(decision.projectId).isEqualTo(project.id)
        assertThat(decision.decidedAt).isGreaterThan(0L)
    }

    @Test
    fun `the habit note yields a dateless recurring habit`() = runTest {
        val habit = build().habits.single()
        assertThat(habit.name).isEqualTo("Morning meditation")
        assertThat(habit.cadence).isEqualTo(HabitCadence.DAILY)
    }

    @Test
    fun `the subscription note yields a money-bearing document`() = runTest {
        val doc = build().documents.single()
        assertThat(doc.kind).isEqualTo(DocumentKind.SUBSCRIPTION)
        assertThat(doc.provider).isEqualTo("Netflix")
        assertThat(doc.money?.amountMinor).isEqualTo(4300)
        assertThat(doc.money?.currency).isEqualTo("PLN")
        assertThat(doc.billingPeriod).isEqualTo(BillingPeriod.MONTHLY)
        assertThat(doc.renewsOn).isNotNull()
    }

    @Test
    fun `nothing is dropped - the six notes produce the full expected row set`() = runTest {
        val data = build()
        assertThat(data.memoryFacts).hasSize(2) // health (medical) + relationships (person)
        assertThat(data.followups).hasSize(1)
        assertThat(data.transactions).hasSize(1)
        assertThat(data.people).hasSize(1)
        assertThat(data.projects).hasSize(1)
        assertThat(data.decisions).hasSize(1)
        assertThat(data.habits).hasSize(1)
        assertThat(data.documents).hasSize(1)
    }
}
