package app.hermes.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.hermes.core.data.entity.MemoryFactEntity
import app.hermes.core.data.entity.MemoryFactFts
import kotlinx.coroutines.flow.Flow

/**
 * Read side. "Current memory" is a single query over the memory_facts_live view; FTS
 * search joins the live-only mirror; [lineage] walks a fact's revision history for audit.
 */
@Dao
interface MemoryReadDao {
    @Query("SELECT * FROM memory_facts_live ORDER BY learnedAt DESC")
    fun observeLive(): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memory_facts_live WHERE category = :category ORDER BY learnedAt DESC")
    suspend fun liveByCategory(category: String): List<MemoryFactEntity>

    @Query(
        """
        SELECT b.* FROM memory_facts b
        JOIN memory_facts_fts f ON f.memId = b.id
        WHERE memory_facts_fts MATCH :query
          AND b.supersededById IS NULL AND b.forgottenAt IS NULL
        """,
    )
    suspend fun searchLive(query: String): List<MemoryFactEntity>

    @Query(
        """
        WITH RECURSIVE chain(id) AS (
            SELECT :liveId
            UNION ALL
            SELECT m.id FROM memory_facts m JOIN chain c ON m.supersededById = c.id
        )
        SELECT * FROM memory_facts WHERE id IN (SELECT id FROM chain) ORDER BY revision DESC
        """,
    )
    suspend fun lineage(liveId: String): List<MemoryFactEntity>
}

/** Capture (M2 extraction): append a fact and index it live. Append-only. */
@Dao
abstract class MemoryCaptureDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertFact(fact: MemoryFactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun indexFts(row: MemoryFactFts)

    @Transaction
    open suspend fun append(fact: MemoryFactEntity) {
        insertFact(fact)
        indexFts(MemoryFactFts(memId = fact.id, topic = fact.topic, fact = fact.fact))
    }
}

/**
 * Consolidation (M4) depends on THIS type only. It has no destructive verb over a
 * fact: [supersede] inserts a new revision, re-indexes, marks the old row's supersede
 * pointer (content untouched), and drops the stale FTS entry. There is no `@Delete`
 * and no `@Update(MemoryFactEntity)` anywhere — the append-only invariant is held by
 * the type, not by discipline.
 */
@Dao
abstract class MemoryConsolidationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertRevision(fact: MemoryFactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun indexFts(row: MemoryFactFts)

    // Marker-only: touches ONLY the supersede pointer, and only while the row is still
    // live (so a fact is superseded at most once — idempotent, no lost updates).
    @Query(
        """
        UPDATE memory_facts SET supersededById = :newId, supersededAt = :at
        WHERE id = :oldId AND supersededById IS NULL
        """,
    )
    abstract suspend fun markSuperseded(oldId: String, newId: String, at: Long)

    @Query("DELETE FROM memory_facts_fts WHERE memId = :oldId")
    abstract suspend fun deIndex(oldId: String)

    @Transaction
    open suspend fun supersede(old: MemoryFactEntity, new: MemoryFactEntity, at: Long) {
        insertRevision(new)
        indexFts(MemoryFactFts(memId = new.id, topic = new.topic, fact = new.fact))
        markSuperseded(old.id, new.id, at)
        deIndex(old.id)
    }
}

/** User "forget" (M3): a non-destructive tombstone. Content is preserved for undo/audit;
 *  the row simply leaves the live set and the FTS index. */
@Dao
abstract class MemoryForgetDao {
    @Query("UPDATE memory_facts SET forgottenAt = :at WHERE id = :id AND forgottenAt IS NULL")
    abstract suspend fun markForgotten(id: String, at: Long)

    @Query("DELETE FROM memory_facts_fts WHERE memId = :id")
    abstract suspend fun deIndex(id: String)

    @Transaction
    open suspend fun forget(id: String, at: Long) {
        markForgotten(id, at)
        deIndex(id)
    }
}
