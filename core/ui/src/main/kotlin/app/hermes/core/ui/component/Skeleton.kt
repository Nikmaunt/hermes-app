package app.hermes.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hermes.core.ui.theme.HermesSpacing

/**
 * Loading placeholders that mirror the SHAPE of the content they stand in for — a lesson
 * from Lens, where a single jumping grey block read worse than a spinner. Compose your
 * skeleton from the same rows/columns as the real card ([HermesCardSkeleton] is the
 * exemplar), passing ONE shared [Brush] from [rememberShimmerBrush] so the whole card
 * shimmers in sync rather than each block on its own phase.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val band = 320f
    val x = progress * (band * 2) - band
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x, 0f),
        end = Offset(x + band, 0f),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(HermesSpacing.xs),
    brush: Brush = rememberShimmerBrush(),
) {
    Box(modifier.clip(shape).background(brush))
}

/** A placeholder text line; [widthFraction] mimics the length of the real line. */
@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 14.dp,
    brush: Brush = rememberShimmerBrush(),
) {
    SkeletonBox(
        modifier = modifier.fillMaxWidth(widthFraction).height(height),
        shape = RoundedCornerShape(percent = 50),
        brush = brush,
    )
}

@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier, brush: Brush = rememberShimmerBrush()) {
    SkeletonBox(modifier = modifier.size(size), shape = CircleShape, brush = brush)
}

/**
 * Exemplar skeleton shaped like a standard content card (leading avatar + title + two
 * body lines + a short meta line). Feature screens should copy this pattern for their own
 * cards rather than reaching for a bare grey rectangle.
 */
@Composable
fun HermesCardSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Row(
        modifier = modifier.fillMaxWidth().padding(HermesSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(HermesSpacing.md),
    ) {
        SkeletonCircle(size = 40.dp, brush = brush)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HermesSpacing.sm),
        ) {
            SkeletonLine(widthFraction = 0.55f, height = 16.dp, brush = brush)
            SkeletonLine(widthFraction = 0.95f, brush = brush)
            SkeletonLine(widthFraction = 0.80f, brush = brush)
            SkeletonLine(widthFraction = 0.30f, height = 12.dp, brush = brush)
        }
    }
}
