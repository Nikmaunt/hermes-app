package app.hermes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.hermes.R
import app.hermes.core.ui.component.EmptyState
import app.hermes.core.ui.component.HermesScreen
import app.hermes.core.ui.component.SensitiveContent
import app.hermes.core.ui.theme.HermesSpacing

/** How long a read screen's StateFlow stays warm after the UI stops collecting. */
internal const val SUBSCRIBE_MS = 5_000L

/**
 * The shared read-screen scaffold: a scrolling stack of cards, or the one shared empty
 * state when there is nothing to show. Outside Demo the database is empty (nothing has
 * been captured yet in M0), so these screens legitimately render the empty state; in Demo
 * they render the rows the pipeline seeded.
 */
@Composable
fun DemoScreen(
    isEmpty: Boolean,
    emptyIcon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    HermesScreen(modifier = modifier) {
        if (isEmpty) {
            EmptyState(
                title = stringResource(R.string.demo_empty_title),
                description = stringResource(R.string.demo_empty_body),
                icon = emptyIcon,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HermesSpacing.md),
                content = content,
            )
        }
    }
}

/**
 * One card in a read screen: a title, an optional subtitle and an optional meta line.
 * When [sensitiveSubtitle] is set the subtitle is masked behind [SensitiveContent] (the
 * shared blur/tap-to-reveal primitive), so health/finance facts are covered until tapped
 * — the seam M3 gates behind biometrics (D29).
 */
@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    sensitiveSubtitle: Boolean = false,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(HermesSpacing.lg).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HermesSpacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { value ->
                if (sensitiveSubtitle) {
                    SensitiveContent {
                        Text(text = value, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            meta?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
