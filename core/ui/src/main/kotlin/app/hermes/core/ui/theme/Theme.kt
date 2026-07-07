package app.hermes.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Roles Material 3's [ColorScheme] has no slot for. Surfaced through
 * [LocalHermesColors] and read via `HermesTheme.colors`, kept in lock-step with
 * [ColorTokens] so the contrast test still governs them.
 */
data class HermesExtendedColors(
    val surfaceRaised: Color,
    val controlTrack: Color,
    val controlThumb: Color,
)

val LocalHermesColors = staticCompositionLocalOf<HermesExtendedColors> {
    error("HermesExtendedColors not provided — wrap the UI in HermesTheme { }")
}

/**
 * The app theme. Light/dark follow the system by default; the palette comes from
 * [HermesPalette] (the same tokens the WCAG test verifies). Material components read the
 * standard [ColorScheme]; the extra roles ride alongside in [LocalHermesColors].
 */
@Composable
fun HermesTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val tokens = if (darkTheme) HermesPalette.Dark else HermesPalette.Light
    val extended = HermesExtendedColors(
        surfaceRaised = Color(tokens.surfaceRaised),
        controlTrack = Color(tokens.controlTrack),
        controlThumb = Color(tokens.controlThumb),
    )
    CompositionLocalProvider(LocalHermesColors provides extended) {
        MaterialTheme(
            colorScheme = tokens.toColorScheme(darkTheme),
            typography = HermesTypography,
            content = content,
        )
    }
}

/** Accessors for Hermes-specific theme values, mirroring `MaterialTheme.*`. */
object HermesTheme {
    val colors: HermesExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalHermesColors.current
}

private fun ColorTokens.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        onSurfaceVariant = Color(onSurfaceVariant),
        outline = Color(outline),
        error = Color(danger),
        onError = Color(onDanger),
    )
}
