plugins {
    alias(libs.plugins.hermes.android.library)
    alias(libs.plugins.hermes.room)
    alias(libs.plugins.hermes.quality)
}

android {
    namespace = "app.hermes.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
