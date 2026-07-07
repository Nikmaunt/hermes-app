package app.hermes.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.hermes.core.ui.theme.HermesSpacing

/**
 * The standard page container: fills the window, paints the theme background and applies
 * the shared content padding, so screens stop re-deriving those three things. Content is
 * a [ColumnScope] — the common vertical stack — but a screen that needs a different layout
 * just ignores it and lays out its own children.
 */
@Composable
fun HermesScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(HermesSpacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            content = content,
        )
    }
}
