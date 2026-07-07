package app.hermes.core.ui.theme

import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.pow
import org.junit.Test

/**
 * Objective guard on the design tokens: contrast is measurable, so it gets a test rather
 * than eyeballs. Every text pair must clear WCAG AA 4.5:1 and every accent/border/control
 * pair 3:1, in BOTH themes. A palette edit that dims a role below its threshold fails
 * `./gradlew check` (the Lens regression: a light "raised" surface fell to 2.98:1 and only
 * a token test caught it). Computed straight from [HermesPalette] — the same longs the
 * theme renders.
 */
class ColorTokenContrastTest {

    private data class Pair(val name: String, val fg: Long, val bg: Long)

    private fun textPairs(t: ColorTokens): List<Pair> = listOf(
        Pair("onBackground / background", t.onBackground, t.background),
        Pair("onSurface / surface", t.onSurface, t.surface),
        Pair("onSurface / surfaceRaised", t.onSurface, t.surfaceRaised),
        Pair("onSurfaceVariant / surface", t.onSurfaceVariant, t.surface),
        Pair("onSurfaceVariant / surfaceRaised", t.onSurfaceVariant, t.surfaceRaised),
        Pair("onPrimary / primary", t.onPrimary, t.primary),
        Pair("onDanger / danger", t.onDanger, t.danger),
    )

    private fun controlPairs(t: ColorTokens): List<Pair> = listOf(
        Pair("primary / surface", t.primary, t.surface),
        Pair("primary / surfaceRaised", t.primary, t.surfaceRaised),
        Pair("outline / surface", t.outline, t.surface),
        Pair("outline / surfaceRaised", t.outline, t.surfaceRaised),
        Pair("controlThumb / controlTrack", t.controlThumb, t.controlTrack),
    )

    @Test
    fun `light text pairs meet WCAG AA`() = assertAll(textPairs(HermesPalette.Light), TEXT_MIN)

    @Test
    fun `dark text pairs meet WCAG AA`() = assertAll(textPairs(HermesPalette.Dark), TEXT_MIN)

    @Test
    fun `light accent and control pairs meet WCAG large`() = assertAll(controlPairs(HermesPalette.Light), LARGE_MIN)

    @Test
    fun `dark accent and control pairs meet WCAG large`() = assertAll(controlPairs(HermesPalette.Dark), LARGE_MIN)

    private fun assertAll(pairs: List<Pair>, min: Double) {
        pairs.forEach { p ->
            val ratio = contrastRatio(p.fg, p.bg)
            val message = "${p.name} = ${"%.2f".format(ratio)}:1 (needs >= $min:1)"
            assertWithMessage(message).that(ratio).isAtLeast(min)
        }
    }

    private fun contrastRatio(fg: Long, bg: Long): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun relativeLuminance(argb: Long): Double {
        val r = channel(((argb shr 16) and 0xFF).toInt())
        val g = channel(((argb shr 8) and 0xFF).toInt())
        val b = channel((argb and 0xFF).toInt())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private companion object {
        const val TEXT_MIN = 4.5
        const val LARGE_MIN = 3.0
    }
}
