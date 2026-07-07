plugins {
    alias(libs.plugins.hermes.kotlin.jvm)
    alias(libs.plugins.hermes.quality)
    alias(libs.plugins.kotlin.serialization)
}

// Pure kotlin("jvm"): the BrainProvider contract, validators and the Fake/OpenRouter
// providers stay Android-free (verified by NOT applying AGP — see decision-log D1/§5).
// The on-device ML Kit GenAI provider arrives in M1 behind this contract, its Android
// implementation living in :app. brain depends only on :core:model, never :core:data
// (D18): it knows the semantic model, the Room-row mapping is the data layer's job.
dependencies {
    implementation(project(":core:model"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
