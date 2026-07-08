package app.hermes.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.R

/** People: contacts extraction learned about, with an optional preferred language. */
@Composable
fun PeopleScreen(modifier: Modifier = Modifier, viewModel: PeopleViewModel = hiltViewModel()) {
    val people by viewModel.people.collectAsStateWithLifecycle()
    DemoScreen(isEmpty = people.isEmpty(), emptyIcon = Icons.Filled.People, modifier = modifier) {
        people.forEach { person ->
            InfoCard(
                title = person.name,
                subtitle = person.relation,
                meta = person.preferredLanguage?.let { stringResource(R.string.demo_prefers, it) },
            )
        }
    }
}
