package app.hermes.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.hermes.core.data.entity.FollowupEntity
import app.hermes.core.data.entity.HabitEntity
import app.hermes.core.data.entity.HabitTickEntity
import app.hermes.core.data.entity.MemoryFactEntity
import app.hermes.core.data.entity.NoteEntity
import app.hermes.core.data.entity.TransactionEntity
import app.hermes.core.model.CalendarDates
import app.hermes.core.model.DedupKeys
import app.hermes.core.model.Ids
import app.hermes.core.model.MemoryCategory
import app.hermes.core.model.MemorySource
import app.hermes.core.model.NoteSource
import app.hermes.core.model.TransactionSource
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HermesDatabaseTest {

    private lateinit var db: HermesDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HermesDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `schema v1 opens and a raw note round-trips through FTS with diacritic folding`() = runTest {
        val note = NoteEntity(
            id = Ids.new("note"),
            text = "Zapłać za Café subscription",
            source = NoteSource.TEXT,
            createdAt = 1_700_000_000_000,
        )
        db.noteDao().insert(note)

        assertThat(db.noteDao().getById(note.id)).isEqualTo(note)
        // unicode61 remove_diacritics=2: searching "cafe" matches "Café".
        assertThat(db.noteDao().search("cafe").map { it.id }).containsExactly(note.id)
    }

    @Test
    fun `memory is append-only - supersede keeps one live row, FTS live-only, lineage intact`() = runTest {
        val read = db.memoryReadDao()
        val v1 = memoryFact(topic = "coffee", fact = "drinks espresso")
        db.memoryCaptureDao().append(v1)

        assertThat(read.observeLive().first().map { it.id }).containsExactly(v1.id)
        assertThat(read.searchLive("espresso").map { it.id }).containsExactly(v1.id)

        val v2 = memoryFact(topic = "coffee", fact = "switched to oat flat white", revision = 2)
        db.memoryConsolidationDao().supersede(old = v1, new = v2, at = 1_700_000_100_000)

        // Live set is exactly the new revision…
        assertThat(read.observeLive().first().map { it.id }).containsExactly(v2.id)
        // …the dead revision is gone from FTS (live-only index)…
        assertThat(read.searchLive("espresso")).isEmpty()
        assertThat(read.searchLive("oat").map { it.id }).containsExactly(v2.id)
        // …but the full history is still auditable.
        assertThat(read.lineage(v2.id).map { it.id }).containsExactly(v2.id, v1.id).inOrder()
    }

    @Test
    fun `forget removes a fact from the live set without destroying it`() = runTest {
        val read = db.memoryReadDao()
        val fact = memoryFact(topic = "address", fact = "lives on Foo Street")
        db.memoryCaptureDao().append(fact)

        db.memoryForgetDao().forget(fact.id, at = 1_700_000_200_000)

        assertThat(read.observeLive().first()).isEmpty()
        assertThat(read.searchLive("Foo")).isEmpty()
        // Content preserved for undo/audit (lineage still finds it).
        assertThat(read.lineage(fact.id).map { it.id }).containsExactly(fact.id)
    }

    @Test
    fun `transaction dedup key makes a replayed insert a no-op`() = runTest {
        val dao = db.transactionDao()
        val occurredAt = 1_700_000_000_000
        val key = DedupKeys.transaction("notification", occurredAt, 4599, "PLN", "Biedronka")

        dao.insertIgnoringDuplicate(transaction(key, occurredAt, 4599))
        dao.insertIgnoringDuplicate(transaction(key, occurredAt, 4599)) // replay

        assertThat(dao.count()).isEqualTo(1)
        assertThat(dao.findByDedup(key)).isNotNull()
    }

    @Test
    fun `habit ticks are unique per normalized day`() = runTest {
        val dao = db.habitDao()
        val habit = HabitEntity(id = Ids.new("habit"), name = "Read", createdAt = 0)
        dao.insertHabit(habit)

        val day = CalendarDates.toDateMillis(LocalDate.of(2026, 7, 15))
        dao.insertTick(HabitTickEntity(Ids.new("tick"), habit.id, day, createdAt = 1))
        dao.insertTick(HabitTickEntity(Ids.new("tick"), habit.id, day, createdAt = 2)) // same day, ignored
        val nextDay = CalendarDates.toDateMillis(LocalDate.of(2026, 7, 16))
        dao.insertTick(HabitTickEntity(Ids.new("tick"), habit.id, nextDay, createdAt = 3))

        assertThat(dao.tickCount(habit.id)).isEqualTo(2)
    }

    @Test
    fun `followup dedup hint finds an existing extraction for the same note and date`() = runTest {
        val dao = db.followupDao()
        val noteId = Ids.new("note")
        val dueAt = 1_700_500_000_000
        dao.insert(
            FollowupEntity(
                id = Ids.new("fu"),
                title = "Call the clinic",
                dueAt = dueAt,
                sourceNoteId = noteId,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        assertThat(dao.findByNoteAndDue(noteId, dueAt)).hasSize(1)
        assertThat(dao.findByNoteAndDue(noteId, dueAt + 1)).isEmpty()
    }

    private fun memoryFact(topic: String, fact: String, revision: Int = 1) = MemoryFactEntity(
        id = Ids.new("mem"),
        category = MemoryCategory.PREFERENCES,
        topic = topic,
        fact = fact,
        source = MemorySource.CAPTURE,
        revision = revision,
        learnedAt = 1_700_000_000_000,
        createdAt = 1_700_000_000_000,
    )

    private fun transaction(dedupKey: String, occurredAt: Long, amountMinor: Long) = TransactionEntity(
        id = Ids.new("tx"),
        amountMinor = amountMinor,
        currency = "PLN",
        merchant = "Biedronka",
        source = TransactionSource.NOTIFICATION,
        occurredAt = occurredAt,
        dedupKey = dedupKey,
        createdAt = occurredAt,
    )
}
