package app.hermes.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Type scale. M0 rides the platform font (no bundled typeface yet) and only firms up the
 * title/label weights so headers read as headers; the full M3 scale is inherited.
 */
val HermesTypography: Typography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}
