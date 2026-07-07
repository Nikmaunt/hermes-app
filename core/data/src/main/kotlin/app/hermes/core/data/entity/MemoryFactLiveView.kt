package app.hermes.core.data.entity

import androidx.room.DatabaseView
import app.hermes.core.model.MemoryCategory
import app.hermes.core.model.MemorySource

/**
 * The live set of memory the UI and chat retrieval read: current memory state is ONE
 * clear query, not a reconstruction of revision chains. Fields mirror
 * [MemoryFactEntity]; drift is compile-caught by Room (a query returning MemoryFactEntity
 * from this view would fail if columns diverged).
 */
@DatabaseView(
    viewName = "memory_facts_live",
    value = "SELECT * FROM memory_facts WHERE supersededById IS NULL AND forgottenAt IS NULL",
)
data class MemoryFactLiveView(
    val id: String,
    val category: MemoryCategory,
    val topic: String,
    val fact: String,
    val sensitive: Boolean,
    val source: MemorySource,
    val sourceNoteId: String?,
    val revision: Int,
    val supersededById: String?,
    val supersededAt: Long?,
    val forgottenAt: Long?,
    val learnedAt: Long,
    val createdAt: Long,
)
