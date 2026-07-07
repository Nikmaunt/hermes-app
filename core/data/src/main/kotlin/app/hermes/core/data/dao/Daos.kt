package app.hermes.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.hermes.core.data.entity.FollowupEntity
import app.hermes.core.data.entity.HabitEntity
import app.hermes.core.data.entity.HabitTickEntity
import app.hermes.core.data.entity.NoteEntity
import app.hermes.core.data.entity.NoteFts
import app.hermes.core.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** Notes: persist-first. [insert] writes the raw row and indexes its text; the text is
 *  never mutated afterward. */
@Dao
abstract class NoteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun indexFts(row: NoteFts)

    @Transaction
    open suspend fun insert(note: NoteEntity) {
        insertNote(note)
        indexFts(NoteFts(noteId = note.id, text = note.text))
    }

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun getById(id: String): NoteEntity?

    @Query(
        """
        SELECT b.* FROM notes b
        JOIN notes_fts f ON f.noteId = b.id
        WHERE notes_fts MATCH :query AND b.status != 'discarded'
        """,
    )
    abstract suspend fun search(query: String): List<NoteEntity>
}

@Dao
interface FollowupDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(followup: FollowupEntity)

    /** Repository dedup hook: "already extracted from this note for this date?" */
    @Query("SELECT * FROM followups WHERE sourceNoteId = :noteId AND dueAt = :dueAt")
    suspend fun findByNoteAndDue(noteId: String, dueAt: Long): List<FollowupEntity>

    @Query("SELECT * FROM followups WHERE status = 'pending' ORDER BY dueAt ASC")
    fun observePending(): Flow<List<FollowupEntity>>
}

@Dao
interface TransactionDao {
    /** IGNORE on the UNIQUE(dedupKey) collision → a replayed/duplicate transaction is a
     *  no-op. Returns the new rowid, or -1 when the duplicate was ignored. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicate(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE dedupKey = :dedupKey LIMIT 1")
    suspend fun findByDedup(dedupKey: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: HabitEntity)

    /** IGNORE on UNIQUE(habitId, tickDate) → one tick per day, replay-safe. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTick(tick: HabitTickEntity): Long

    @Query("SELECT COUNT(*) FROM habit_ticks WHERE habitId = :habitId")
    suspend fun tickCount(habitId: String): Int
}
