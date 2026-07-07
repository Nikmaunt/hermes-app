package app.hermes.core.ui.theme

/**
 * The semantic colour roles of one theme, stored as plain `0xAARRGGBB` longs — NOT
 * Compose `Color`. This is deliberate: the same values feed both the Compose theme
 * (wrapped in `Color(...)`) and the WCAG contrast test, and a plain-long structure lets
 * that test run as a fast JVM unit test with no Android/Compose on the classpath.
 *
 * The pairing rules the test enforces (see ColorTokenContrastTest):
 *  - body text (`on*` on its background) ≥ 4.5:1
 *  - accent, borders and control indicators ≥ 3:1
 * A regression that dims a role below its threshold fails `./gradlew check` — the Lens
 * lesson (a "raised" surface silently dropped to 2.98:1 and only a token test caught it).
 */
data class ColorTokens(
    val background: Long,
    val surface: Long,
    /** Raised cards/sheets sitting above [surface]; text must still hold 4.5:1 on it. */
    val surfaceRaised: Long,
    val onBackground: Long,
    val onSurface: Long,
    /** Secondary / supporting text. */
    val onSurfaceVariant: Long,
    val primary: Long,
    val onPrimary: Long,
    /** Borders, dividers, control outlines. */
    val outline: Long,
    /** Active control track (switch/slider), and the knob on it. */
    val controlTrack: Long,
    val controlThumb: Long,
    /** Error / destructive. */
    val danger: Long,
    val onDanger: Long,
)

/**
 * The two concrete themes. Values are tuned so every pair the contrast test checks meets
 * its WCAG target in BOTH light and dark; change one and re-run `./gradlew check`.
 */
object HermesPalette {

    val Light = ColorTokens(
        background = 0xFFF7F8FA,
        surface = 0xFFFFFFFF,
        surfaceRaised = 0xFFE9ECF2,
        onBackground = 0xFF1A1C1E,
        onSurface = 0xFF1A1C1E,
        onSurfaceVariant = 0xFF44474C,
        primary = 0xFF0B57D0,
        onPrimary = 0xFFFFFFFF,
        outline = 0xFF6B6F76,
        controlTrack = 0xFF0B57D0,
        controlThumb = 0xFFFFFFFF,
        danger = 0xFFBA1A1A,
        onDanger = 0xFFFFFFFF,
    )

    val Dark = ColorTokens(
        background = 0xFF101214,
        surface = 0xFF17191C,
        surfaceRaised = 0xFF22252A,
        onBackground = 0xFFE4E6EA,
        onSurface = 0xFFE4E6EA,
        onSurfaceVariant = 0xFFB4B9C2,
        primary = 0xFF7CACF8,
        onPrimary = 0xFF08213F,
        outline = 0xFF8A9099,
        controlTrack = 0xFF7CACF8,
        controlThumb = 0xFF08213F,
        danger = 0xFFFFB4AB,
        onDanger = 0xFF690005,
    )
}
