package app.hermes.demo

import app.hermes.core.data.entity.DecisionEntity
import app.hermes.core.data.entity.DocumentEntity
import app.hermes.core.data.entity.FollowupEntity
import app.hermes.core.data.entity.HabitEntity
import app.hermes.core.data.entity.MemoryFactEntity
import app.hermes.core.data.entity.NoteEntity
import app.hermes.core.data.entity.PersonEntity
import app.hermes.core.data.entity.ProjectEntity
import app.hermes.core.data.entity.TransactionEntity

/**
 * The Room rows the demo pipeline produced, ready to persist. Each list already carries
 * stamped ids/createdAt and, on every extraction-born row, the `sourceNoteId` of the note
 * it came from — so the D8/D21 split (a dated followup + a dateless memory_fact from one
 * note) is visible here as two rows sharing one `sourceNoteId`, exactly as at persist.
 */
data class DemoData(
    val notes: List<NoteEntity> = emptyList(),
    val followups: List<FollowupEntity> = emptyList(),
    val memoryFacts: List<MemoryFactEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val decisions: List<DecisionEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val documents: List<DocumentEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
) {
    val isEmpty: Boolean
        get() = notes.isEmpty()
}
