package app.hermes.core.data.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/*
 * Standalone FTS4 mirrors (NOT external content). Rationale (decision-log): Room's
 * `contentEntity` FTS requires an INTEGER rowid PK on the content table, which fights
 * our TEXT `id`, and would index every revision including dead ones. Maintaining the
 * FTS by hand at the write choke-points instead keeps a lean, LIVE-ONLY index: rows
 * are inserted on capture and deleted on supersede/forget. The back-link column
 * (memId/noteId) is `notIndexed` so it never pollutes MATCH results.
 *
 * Tokenizer: unicode61 with remove_diacritics=2 — case- and diacritic-insensitive.
 * No ru/pl stemming (prefix search only). Room adds the implicit INTEGER `rowid` PK.
 */

@Fts4(
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=2"],
    notIndexed = ["memId"],
)
@Entity(tableName = "memory_facts_fts")
data class MemoryFactFts(
    val memId: String,
    val topic: String,
    val fact: String,
)

@Fts4(
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=2"],
    notIndexed = ["noteId"],
)
@Entity(tableName = "notes_fts")
data class NoteFts(
    val noteId: String,
    val text: String,
)
