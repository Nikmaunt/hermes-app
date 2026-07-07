plugins {
    alias(libs.plugins.hermes.kotlin.jvm)
    alias(libs.plugins.hermes.quality)
}

dependencies {
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}
