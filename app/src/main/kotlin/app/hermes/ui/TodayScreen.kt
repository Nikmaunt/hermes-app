package app.hermes.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.core.model.Criticality

/** Today: the pending followups — the only alarm-bearing rows (D8), earliest due first. */
@Composable
fun TodayScreen(modifier: Modifier = Modifier, viewModel: TodayViewModel = hiltViewModel()) {
    val followups by viewModel.followups.collectAsStateWithLifecycle()
    DemoScreen(isEmpty = followups.isEmpty(), emptyIcon = Icons.Filled.Today, modifier = modifier) {
        followups.forEach { followup ->
            InfoCard(
                title = followup.title,
                subtitle = DemoFormat.dueAt(followup.dueAt, followup.dueIsDateOnly, followup.dueTimeZone),
                meta = followup.criticality.takeIf { it != Criticality.NORMAL }?.wire,
            )
        }
    }
}
