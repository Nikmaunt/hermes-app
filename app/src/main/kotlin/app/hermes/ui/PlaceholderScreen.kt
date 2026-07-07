package app.hermes.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.hermes.R
import app.hermes.core.ui.component.EmptyState
import app.hermes.core.ui.component.HermesScreen
import app.hermes.navigation.Routes

/**
 * Generic clickable stub for a leaf screen not yet built out. Each one is reachable and
 * shows a route-appropriate empty state, so the whole shell is navigable in M0.
 */
@Composable
fun PlaceholderScreen(route: String, modifier: Modifier = Modifier) {
    HermesScreen(modifier = modifier) {
        EmptyState(
            title = stringResource(R.string.placeholder_title),
            description = stringResource(R.string.placeholder_body),
            icon = iconForRoute(route),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun iconForRoute(route: String): ImageVector = when (route) {
    Routes.TODAY -> Icons.Filled.Today
    Routes.INBOX -> Icons.Filled.Inbox
    Routes.CAPTURE -> Icons.Filled.Add
    Routes.CHAT -> Icons.AutoMirrored.Filled.Chat
    Routes.MEMORY -> Icons.Filled.Psychology
    Routes.DOCUMENTS -> Icons.Filled.Description
    Routes.MONEY -> Icons.Filled.Payments
    Routes.HABITS -> Icons.Filled.Repeat
    Routes.PROJECTS -> Icons.Filled.Folder
    Routes.PEOPLE -> Icons.Filled.People
    Routes.BRIEFS -> Icons.AutoMirrored.Filled.Article
    else -> Icons.Filled.WbSunny
}
