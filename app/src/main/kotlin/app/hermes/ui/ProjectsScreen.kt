package app.hermes.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.R
import app.hermes.core.ui.theme.HermesSpacing

/** Projects with the decisions that shaped them, nested under their project — the
 *  projectRef→projectId link the persist layer resolved (D20) made visible. */
@Composable
fun ProjectsScreen(modifier: Modifier = Modifier, viewModel: ProjectsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isEmpty = state.projects.isEmpty() && state.decisions.isEmpty()
    DemoScreen(isEmpty = isEmpty, emptyIcon = Icons.Filled.Folder, modifier = modifier) {
        state.projects.forEach { project ->
            InfoCard(title = project.name, subtitle = project.nextAction, meta = project.status.wire)
            state.decisions.filter { it.projectId == project.id }.forEach { decision ->
                InfoCard(
                    modifier = Modifier.padding(start = HermesSpacing.lg),
                    title = decision.title,
                    subtitle = decision.rationale,
                    meta = stringResource(R.string.demo_decision, DemoFormat.dateOnly(decision.decidedAt)),
                )
            }
        }
    }
}
