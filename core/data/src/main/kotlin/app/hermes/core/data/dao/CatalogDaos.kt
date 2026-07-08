package app.hermes.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.hermes.core.data.entity.DecisionEntity
import app.hermes.core.data.entity.DocumentEntity
import app.hermes.core.data.entity.PersonEntity
import app.hermes.core.data.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

/*
 * Access DAOs for the catalog tables extraction fills (people, projects, decisions,
 * documents). These add only reads/inserts over tables that already exist in schema v1 —
 * no columns, indices or views change, so the exported schema and DB version are untouched.
 * The real extraction→persist mapping (id/createdAt/sourceNoteId stamping, dedup, revision
 * policy) is the M2 data layer's job; these DAOs are the minimal surface it and the Demo
 * seeder write through.
 */

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: PersonEntity)

    @Query("SELECT * FROM people ORDER BY name ASC")
    fun observeAll(): Flow<List<PersonEntity>>
}

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE status != 'archived' ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>
}

@Dao
interface DecisionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(decision: DecisionEntity)

    @Query("SELECT * FROM decisions ORDER BY decidedAt DESC")
    fun observeAll(): Flow<List<DecisionEntity>>
}

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>
}
