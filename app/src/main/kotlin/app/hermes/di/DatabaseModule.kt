package app.hermes.di

import android.content.Context
import app.hermes.core.data.HermesDatabase
import app.hermes.core.data.dao.DecisionDao
import app.hermes.core.data.dao.DocumentDao
import app.hermes.core.data.dao.FollowupDao
import app.hermes.core.data.dao.HabitDao
import app.hermes.core.data.dao.MemoryReadDao
import app.hermes.core.data.dao.NoteDao
import app.hermes.core.data.dao.PersonDao
import app.hermes.core.data.dao.ProjectDao
import app.hermes.core.data.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room database and its DAOs from the composition root. The read screens
 * inject the specific DAO they display; the Demo seeder writes through the database.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HermesDatabase = HermesDatabase.build(context)

    @Provides fun provideNoteDao(db: HermesDatabase): NoteDao = db.noteDao()

    @Provides fun provideFollowupDao(db: HermesDatabase): FollowupDao = db.followupDao()

    @Provides fun provideTransactionDao(db: HermesDatabase): TransactionDao = db.transactionDao()

    @Provides fun provideHabitDao(db: HermesDatabase): HabitDao = db.habitDao()

    @Provides fun providePersonDao(db: HermesDatabase): PersonDao = db.personDao()

    @Provides fun provideProjectDao(db: HermesDatabase): ProjectDao = db.projectDao()

    @Provides fun provideDecisionDao(db: HermesDatabase): DecisionDao = db.decisionDao()

    @Provides fun provideDocumentDao(db: HermesDatabase): DocumentDao = db.documentDao()

    @Provides fun provideMemoryReadDao(db: HermesDatabase): MemoryReadDao = db.memoryReadDao()
}
