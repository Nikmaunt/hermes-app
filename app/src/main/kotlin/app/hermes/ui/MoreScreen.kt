package app.hermes.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.hermes.R
import app.hermes.core.ui.component.HermesScreen
import app.hermes.core.ui.theme.HermesSpacing
import app.hermes.navigation.Routes

/** The More hub: a list of the secondary destinations, each opening via [onOpen]. */
@Composable
fun MoreScreen(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    HermesScreen(modifier = modifier) {
        MoreRow(Icons.Filled.Psychology, R.string.title_memory) { onOpen(Routes.MEMORY) }
        MoreRow(Icons.Filled.Description, R.string.title_documents) { onOpen(Routes.DOCUMENTS) }
        MoreRow(Icons.Filled.Payments, R.string.title_money) { onOpen(Routes.MONEY) }
        MoreRow(Icons.Filled.Repeat, R.string.title_habits) { onOpen(Routes.HABITS) }
        MoreRow(Icons.Filled.Folder, R.string.title_projects) { onOpen(Routes.PROJECTS) }
        MoreRow(Icons.Filled.People, R.string.title_people) { onOpen(Routes.PEOPLE) }
        MoreRow(Icons.AutoMirrored.Filled.Article, R.string.title_briefs) { onOpen(Routes.BRIEFS) }
        MoreRow(Icons.Filled.Settings, R.string.title_settings) { onOpen(Routes.SETTINGS) }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, @StringRes labelRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = HermesSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(HermesSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
    }
}
