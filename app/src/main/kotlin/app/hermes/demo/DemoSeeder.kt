package app.hermes.demo

import androidx.room.withTransaction
import app.hermes.core.data.HermesDatabase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Loads the Demo database: builds the pipeline-produced rows ([DemoDataBuilder]) and
 * writes them through the real DAOs (so notes/facts flow through the same FTS
 * choke-points as a live capture). Idempotent and single-flight: the [Mutex] serialises
 * concurrent callers and the note-count guard makes a second call a no-op, so re-entering
 * Demo mode never double-seeds. The whole write is one transaction — an interrupted seed
 * leaves the store empty, not half-populated.
 */
@Singleton
class DemoSeeder @Inject constructor(
    private val db: HermesDatabase,
) {
    private val mutex = Mutex()
    private val builder = DemoDataBuilder()

    suspend fun seedIfEmpty() = mutex.withLock {
        if (db.noteDao().count() > 0) return@withLock
        val data = builder.build()
        db.withTransaction {
            data.notes.forEach { db.noteDao().insert(it) }
            data.memoryFacts.forEach { db.memoryCaptureDao().append(it) }
            data.followups.forEach { db.followupDao().insert(it) }
            data.people.forEach { db.personDao().insert(it) }
            data.projects.forEach { db.projectDao().insert(it) }
            data.decisions.forEach { db.decisionDao().insert(it) }
            data.habits.forEach { db.habitDao().insertHabit(it) }
            data.documents.forEach { db.documentDao().insert(it) }
            data.transactions.forEach { db.transactionDao().insertIgnoringDuplicate(it) }
        }
    }
}
