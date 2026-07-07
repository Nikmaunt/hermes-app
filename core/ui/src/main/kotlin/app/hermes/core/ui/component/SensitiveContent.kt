package app.hermes.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.hermes.core.ui.R
import app.hermes.core.ui.theme.HermesSpacing

/**
 * Masks sensitive content until it is revealed — a SHARED primitive, so Memory,
 * Documents and Search never grow three different maskings. The content is blurred and
 * covered by a tap target; a tap calls [onRevealRequest].
 *
 * The reveal decision is HOISTED on purpose: in M0 the caller flips [revealed] straight
 * away, but in M3 [onRevealRequest] is exactly where a biometric prompt is gated —
 * reveal only on success. This primitive never authenticates and never persists the
 * revealed flag; that policy lives in the feature/security layer.
 */
@Composable
fun SensitiveContent(
    revealed: Boolean,
    onRevealRequest: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.hermes_sensitive_reveal_hint),
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(Modifier.blur(if (revealed) 0.dp else BLUR_RADIUS)) {
            content()
        }
        if (!revealed) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClickLabel = hint, onClick = onRevealRequest)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = SCRIM_ALPHA))
                    .padding(HermesSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(HermesSpacing.xs, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Self-managed variant for cases with no external reveal policy: it reveals on tap and
 * re-hides when the composition is recreated (safer default for sensitive data — the
 * revealed flag is intentionally NOT saved across recreation).
 */
@Composable
fun SensitiveContent(
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.hermes_sensitive_reveal_hint),
    content: @Composable () -> Unit,
) {
    var revealed by remember { mutableStateOf(false) }
    SensitiveContent(
        revealed = revealed,
        onRevealRequest = { revealed = true },
        modifier = modifier,
        hint = hint,
        content = content,
    )
}

private val BLUR_RADIUS = 12.dp
private const val SCRIM_ALPHA = 0.35f
