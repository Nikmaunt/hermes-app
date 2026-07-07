plugins {
    alias(libs.plugins.hermes.android.library)
    alias(libs.plugins.hermes.android.compose)
    alias(libs.plugins.hermes.quality)
}

android {
    namespace = "app.hermes.core.ui"
}

// Compose theme, design tokens and the shared UI primitives (Screen, empty-state,
// skeleton, sensitive-masking). The colour palette is stored as plain ARGB longs
// (ColorTokens) so the WCAG contrast test can read the SAME values the theme uses
// from a fast JVM unit test — no device, no Robolectric.
dependencies {
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}
