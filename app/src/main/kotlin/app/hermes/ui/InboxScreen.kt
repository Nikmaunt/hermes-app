package app.hermes.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.core.model.Sensitivity

/** Inbox: the raw captured notes, newest first. The note text is the immutable source
 *  extraction reads but never rewrites. */
@Composable
fun InboxScreen(modifier: Modifier = Modifier, viewModel: InboxViewModel = hiltViewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    DemoScreen(isEmpty = notes.isEmpty(), emptyIcon = Icons.Filled.Inbox, modifier = modifier) {
        notes.forEach { note ->
            val flagged = note.sensitivity == Sensitivity.SENSITIVE
            InfoCard(
                title = note.text,
                meta = note.source.wire + if (flagged) " · " + note.sensitivity.wire else "",
            )
        }
    }
}
