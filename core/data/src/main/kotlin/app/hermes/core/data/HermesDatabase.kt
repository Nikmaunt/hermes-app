package app.hermes.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.hermes.core.data.dao.DecisionDao
import app.hermes.core.data.dao.DocumentDao
import app.hermes.core.data.dao.FollowupDao
import app.hermes.core.data.dao.HabitDao
import app.hermes.core.data.dao.MemoryCaptureDao
import app.hermes.core.data.dao.MemoryConsolidationDao
import app.hermes.core.data.dao.MemoryForgetDao
import app.hermes.core.data.dao.MemoryReadDao
import app.hermes.core.data.dao.NoteDao
import app.hermes.core.data.dao.PersonDao
import app.hermes.core.data.dao.ProjectDao
import app.hermes.core.data.dao.TransactionDao
import app.hermes.core.data.entity.BriefEntity
import app.hermes.core.data.entity.ChatMessageEntity
import app.hermes.core.data.entity.DecisionEntity
import app.hermes.core.data.entity.DocumentEntity
import app.hermes.core.data.entity.FollowupEntity
import app.hermes.core.data.entity.HabitEntity
import app.hermes.core.data.entity.HabitTickEntity
import app.hermes.core.data.entity.MemoryFactEntity
import app.hermes.core.data.entity.MemoryFactFts
import app.hermes.core.data.entity.MemoryFactLiveView
import app.hermes.core.data.entity.NoteEntity
import app.hermes.core.data.entity.NoteFts
import app.hermes.core.data.entity.PersonEntity
import app.hermes.core.data.entity.ProjectEntity
import app.hermes.core.data.entity.SpendLogEntity
import app.hermes.core.data.entity.TransactionEntity

/**
 * Migration policy: exportSchema = true, schema JSON committed under
 * `core/data/schemas/`, DB version is the single [VERSION] constant, and upgrades go
 * through an explicit Migration list verified by MigrationTestHelper. v1 has nothing to
 * migrate from; the harness exists so adding a column later cannot skip a migration test.
 */
@Database(
    entities = [
        NoteEntity::class,
        FollowupEntity::class,
        PersonEntity::class,
        ProjectEntity::class,
        DecisionEntity::class,
        HabitEntity::class,
        HabitTickEntity::class,
        DocumentEntity::class,
        TransactionEntity::class,
        MemoryFactEntity::class,
        BriefEntity::class,
        ChatMessageEntity::class,
        SpendLogEntity::class,
        MemoryFactFts::class,
        NoteFts::class,
    ],
    views = [MemoryFactLiveView::class],
    version = HermesDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(HermesConverters::class)
abstract class HermesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun followupDao(): FollowupDao
    abstract fun transactionDao(): TransactionDao
    abstract fun habitDao(): HabitDao
    abstract fun personDao(): PersonDao
    abstract fun projectDao(): ProjectDao
    abstract fun decisionDao(): DecisionDao
    abstract fun documentDao(): DocumentDao
    abstract fun memoryReadDao(): MemoryReadDao
    abstract fun memoryCaptureDao(): MemoryCaptureDao
    abstract fun memoryConsolidationDao(): MemoryConsolidationDao
    abstract fun memoryForgetDao(): MemoryForgetDao

    companion object {
        const val VERSION = 1
        const val NAME = "hermes.db"

        fun build(context: Context): HermesDatabase = Room.databaseBuilder(context, HermesDatabase::class.java, NAME)
            .build()
    }
}
