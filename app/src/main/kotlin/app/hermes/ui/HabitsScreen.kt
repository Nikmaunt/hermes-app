package app.hermes.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Habits: recurring intentions. A cadence is recurrence, not a due date — no alarm (D8). */
@Composable
fun HabitsScreen(modifier: Modifier = Modifier, viewModel: HabitsViewModel = hiltViewModel()) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    DemoScreen(isEmpty = habits.isEmpty(), emptyIcon = Icons.Filled.Repeat, modifier = modifier) {
        habits.forEach { habit ->
            InfoCard(title = habit.name, subtitle = habit.schedule, meta = habit.cadence.wire)
        }
    }
}
