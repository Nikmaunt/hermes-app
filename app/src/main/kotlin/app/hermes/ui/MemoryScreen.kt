package app.hermes.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Memory: the live memory facts (one clear query over the live view, D9). These carry no
 *  date (D8); a sensitive fact is masked behind the shared reveal primitive (D29). */
@Composable
fun MemoryScreen(modifier: Modifier = Modifier, viewModel: MemoryViewModel = hiltViewModel()) {
    val facts by viewModel.facts.collectAsStateWithLifecycle()
    DemoScreen(isEmpty = facts.isEmpty(), emptyIcon = Icons.Filled.Psychology, modifier = modifier) {
        facts.forEach { fact ->
            InfoCard(
                title = fact.topic,
                subtitle = fact.fact,
                meta = fact.category.wire,
                sensitiveSubtitle = fact.sensitive,
            )
        }
    }
}
